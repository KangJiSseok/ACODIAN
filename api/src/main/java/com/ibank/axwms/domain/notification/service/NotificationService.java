package com.ibank.axwms.domain.notification.service;

import com.ibank.axwms.domain.notification.NotificationReferenceType;
import com.ibank.axwms.domain.notification.NotificationType;
import com.ibank.axwms.domain.notification.dto.MarkAllNotificationsReadApiDto;
import com.ibank.axwms.domain.notification.dto.SearchNotificationsApiDto;
import com.ibank.axwms.domain.notification.event.NotificationCreatedEvent;
import com.ibank.axwms.domain.notification.entity.Notification;
import com.ibank.axwms.domain.notification.repository.NotificationRepository;
import com.ibank.axwms.domain.notification.repository.jooq.projection.WorklogDueSoonReminderCandidateProjection;
import com.ibank.axwms.domain.notification.repository.jooq.projection.WorklogOverdueReminderCandidateProjection;
import com.ibank.axwms.domain.notification.repository.jooq.query.NotificationSearchQuery;
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import com.ibank.axwms.global.response.PageResponse;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private static final int WORKLOG_DUE_SOON_DAYS = 3;
    private static final String WORKLOG_DUE_SOON_TITLE = "업무 마감 3일 전 알림";
    private static final String WORKLOG_DUE_TODAY_TITLE = "업무 마감 오늘까지 알림";
    private static final String WORKLOG_OVERDUE_TITLE = "업무 마감일 초과 알림";
    private static final String WORKLOG_DEPENDENCY_READY_TITLE = "선행 업무 완료 알림";
    private static final List<String> WORKLOG_REMINDER_NOTIFICATION_TYPES = List.of(
            NotificationType.WORKLOG_DUE_SOON.name(),
            NotificationType.WORKLOG_DUE_TODAY.name(),
            NotificationType.WORKLOG_OVERDUE.name()
    );

    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 현재 로그인 사용자가 수신자인 알림만 조회한다.
     * isRead 가 null 이면 읽음 상태 조건을 걸지 않고, true/false 일 때만 해당 상태로 좁힌다.
     */
    public PageResponse<SearchNotificationsApiDto.Response.Item> searchNotifications(CustomUserPrincipal principal, SearchNotificationsApiDto.Request request) {
        NotificationSearchQuery query = NotificationSearchQuery.from(request);
        return SearchNotificationsApiDto.Response.fromPage(
                notificationRepository.searchNotifications(principal.userId(), query)
        );
    }

    /**
     * 현재 로그인 사용자의 안읽은 알림만 읽음 처리한다.
     * 이미 읽은 알림의 readAt 은 보존하고, 변경된 건수만 응답해 클라이언트가 배지를 즉시 갱신할 수 있게 한다.
     */
    @Transactional
    public MarkAllNotificationsReadApiDto.Response markAllNotificationsRead(CustomUserPrincipal principal) {
        int updatedCount = notificationRepository.markUnreadAsReadByUserId(principal.userId(), LocalDateTime.now());
        return MarkAllNotificationsReadApiDto.Response.of(updatedCount);
    }

    /**
     * 알림 수신자 본인만 읽음 처리할 수 있게 notificationId 와 principal userId 를 같은 조회 경계에서 검증한다.
     *
     * @throws BusinessException NOTIFICATION_NOT_FOUND 알림이 없거나 현재 사용자의 알림이 아닐 때
     */
    @Transactional
    public void markNotificationAsRead(CustomUserPrincipal principal, Long notificationId) {
        Notification notification = getNotificationOrThrow(notificationId, principal.userId());
        notification.markAsRead(LocalDateTime.now());
    }

    /**
     * 기준일을 외부에서 주입받아 배치/테스트가 같은 날짜 계산으로 D-3 업무 알림 후보를 검수하게 한다.
     */
    public List<WorklogDueSoonReminderCandidateProjection> findWorklogDueSoonReminderCandidates(LocalDate today) {
        LocalDate targetDueDate = today.plusDays(WORKLOG_DUE_SOON_DAYS);
        return notificationRepository.findWorklogDueSoonReminderCandidates(targetDueDate);
    }

    /**
     * 기준일이 마감일 당일이거나 이미 지난 업무를 저장-only 마감 배치 후보로 조회한다.
     */
    public List<WorklogOverdueReminderCandidateProjection> findWorklogOverdueReminderCandidates(LocalDate today) {
        return notificationRepository.findWorklogOverdueReminderCandidates(today);
    }

    /**
     * 스케줄러가 주입한 기준일로 후보 산출과 저장을 같은 트랜잭션에서 처리해 재실행 시 중복 저장을 줄인다.
     */
    @Transactional
    public int createWorklogDueSoonReminderNotifications(LocalDate today) {
        List<Notification> notifications = findWorklogDueSoonReminderCandidates(today).stream()
                .map(candidate -> Notification.createWorklogDueSoonReminder(
                        candidate.recipientUserId(),
                        candidate.departmentId(),
                        candidate.teamId(),
                        candidate.referenceId(),
                        WORKLOG_DUE_SOON_TITLE,
                        candidate.teamName() + "의 " + candidate.worklogTitle()
                                + " 마감일이 3일 남았습니다. 마감일 : " + candidate.dueDate()
                ))
                .toList();
        List<Notification> savedNotifications = notificationRepository.saveAllAndFlush(notifications);
        publishNotificationCreatedEvents(savedNotifications);
        return notifications.size();
    }

    /**
     * 당일/초과 마감 배치는 같은 업무 reminder row 를 갱신한다.
     */
    @Transactional
    public int createOrUpdateWorklogOverdueReminderNotifications(LocalDate today) {
        List<WorklogOverdueReminderCandidateProjection> candidates = findWorklogOverdueReminderCandidates(today);
        candidates.forEach(candidate -> {
            Optional<Notification> existingNotification = findExistingWorklogReminder(candidate);
            NotificationType notificationType = resolveWorklogDeadlineNotificationType(candidate, today);
            String title = createWorklogDeadlineTitle(notificationType);
            String content = createWorklogOverdueContent(candidate, today);
            if (existingNotification.isPresent()) {
                existingNotification.get().updateWorklogDeadlineReminder(
                        notificationType,
                        title,
                        content
                );
                return;
            }
            notificationRepository.save(createWorklogDeadlineReminder(
                    candidate.recipientUserId(),
                    candidate.departmentId(),
                    candidate.teamId(),
                    candidate.referenceId(),
                    notificationType,
                    title,
                    content
            ));
        });
        notificationRepository.flush();
        return candidates.size();
    }

    /**
     * 부모 업무 기준 1회 알림 계약을 DB upsert no-op 과 이벤트 발행 조건으로 함께 고정한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<Notification> createWorklogDependencyReadyNotification(Long recipientUserId,
                                                                           Long departmentId,
                                                                           Long teamId,
                                                                           Long parentWorklogId,
                                                                           String teamName,
                                                                           String parentTitle) {
        String referenceType = NotificationReferenceType.WORKLOG.name();
        String notificationType = NotificationType.WORKLOG_DEPENDENCY_READY.name();
        if (notificationRepository.existsByUserIdAndReferenceTypeAndReferenceIdAndNotificationType(
                recipientUserId,
                referenceType,
                parentWorklogId,
                notificationType
        )) {
            return Optional.empty();
        }

        Notification notification = Notification.createWorklogDependencyReady(
                recipientUserId,
                departmentId,
                teamId,
                parentWorklogId,
                WORKLOG_DEPENDENCY_READY_TITLE,
                createWorklogDependencyReadyContent(teamName, parentTitle)
        );
        int insertedCount = notificationRepository.insertWorklogDependencyReadyNotificationIfAbsent(
                notification.getUserId(),
                notification.getDepartmentId(),
                notification.getTeamId(),
                notification.getNotificationType(),
                notification.getTitle(),
                notification.getContent(),
                notification.getReferenceType(),
                notification.getReferenceId()
        );
        if (insertedCount == 0) {
            return Optional.empty();
        }

        Optional<Notification> savedNotification = notificationRepository
                .findFirstByUserIdAndReferenceTypeAndReferenceIdAndNotificationTypeOrderByIdAsc(
                        recipientUserId,
                        referenceType,
                        parentWorklogId,
                        notificationType
                );
        savedNotification.ifPresent(saved -> publishNotificationCreatedEvents(List.of(saved)));
        return savedNotification;
    }

    /**
     * 팀명/부모 제목이 누락되어도 알림 본문이 빈 값으로 깨지지 않도록 운영 메시지의 최소 문맥을 보존한다.
     */
    private String createWorklogDependencyReadyContent(String teamName, String parentTitle) {
        return defaultIfBlank(teamName, "소속 팀") + "의 " + defaultIfBlank(parentTitle, "대상 업무")
                + " 선행 업무가 모두 완료되었습니다. 업무를 진행해 주세요.";
    }

    /**
     * 커밋 후 snapshot 이벤트의 선택 필드가 null 이어도 사용자 메시지 조립 책임을 listener 로 새지 않게 한다.
     */
    private String defaultIfBlank(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }

    /**
     * D-3, 당일, 초과를 같은 업무 reminder family 로 묶어 row 누적 대신 기존 row 갱신을 선택한다.
     */
    private Optional<Notification> findExistingWorklogReminder(WorklogOverdueReminderCandidateProjection candidate) {
        return notificationRepository.findFirstByUserIdAndReferenceTypeAndReferenceIdAndNotificationTypeInOrderByIdAsc(
                candidate.recipientUserId(),
                NotificationReferenceType.WORKLOG.name(),
                candidate.referenceId(),
                WORKLOG_REMINDER_NOTIFICATION_TYPES
        );
    }

    /**
     * 마감일과 기준일의 관계를 저장 타입으로 고정해 같은 배치에서 당일/초과 메시지를 분리한다.
     */
    private NotificationType resolveWorklogDeadlineNotificationType(WorklogOverdueReminderCandidateProjection candidate, LocalDate today) {
        if (candidate.dueDate().isEqual(today)) {
            return NotificationType.WORKLOG_DUE_TODAY;
        }
        return NotificationType.WORKLOG_OVERDUE;
    }

    /**
     * 알림 타입별 제목을 한 곳에서 정해 저장과 기존 row 갱신이 같은 문구를 쓰게 한다.
     */
    private String createWorklogDeadlineTitle(NotificationType notificationType) {
        if (notificationType == NotificationType.WORKLOG_DUE_TODAY) {
            return WORKLOG_DUE_TODAY_TITLE;
        }
        return WORKLOG_OVERDUE_TITLE;
    }

    /**
     * 신규 당일/초과 알림 생성 시 알림 타입별 factory 를 명시해 도메인 생성 의도를 보존한다.
     */
    private Notification createWorklogDeadlineReminder(Long userId,
                                                       Long departmentId,
                                                       Long teamId,
                                                       Long worklogId,
                                                       NotificationType notificationType,
                                                       String title,
                                                       String content) {
        if (notificationType == NotificationType.WORKLOG_DUE_TODAY) {
            return Notification.createWorklogDueTodayReminder(userId, departmentId, teamId, worklogId, title, content);
        }
        return Notification.createWorklogOverdueReminder(userId, departmentId, teamId, worklogId, title, content);
    }

    /**
     * 오늘 마감은 당일 완료 요청을, 지난 마감은 경과 일수를 포함해 후속 조치 우선순위를 드러낸다.
     */
    private String createWorklogOverdueContent(WorklogOverdueReminderCandidateProjection candidate, LocalDate today) {
        if (candidate.dueDate().isEqual(today)) {
            return candidate.teamName() + "의 " + candidate.worklogTitle()
                    + " 마감일이 오늘까지입니다. 오늘 안에 업무를 완료해 주세요. 마감일 : " + candidate.dueDate();
        }

        long overdueDays = ChronoUnit.DAYS.between(candidate.dueDate(), today);
        return candidate.teamName() + "의 " + candidate.worklogTitle()
                + " 마감일이 " + overdueDays + "일 지났습니다. 마감일 : " + candidate.dueDate()
                + ", 확인 기준일 : " + today;
    }

    /**
     * DB flush 로 확정된 식별자/생성시각만 snapshot 으로 발행해 AFTER_COMMIT 리스너가 JPA 엔티티에 의존하지 않게 한다.
     */
    private void publishNotificationCreatedEvents(List<Notification> notifications) {
        notifications.stream()
                .map(notification -> new NotificationCreatedEvent(
                        notification.getId(),
                        notification.getUserId(),
                        notification.getNotificationType(),
                        notification.getTitle(),
                        notification.getContent(),
                        notification.getReferenceType(),
                        notification.getReferenceId(),
                        notification.getCreatedAt()
                ))
                .forEach(applicationEventPublisher::publishEvent);
    }

    /**
     * 알림 존재 여부와 소유권 실패를 같은 NOT_FOUND 로 접어 사용자 간 알림 ID 탐색을 차단한다.
     */
    private Notification getNotificationOrThrow(Long notificationId, Long userId) {
        return notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));
    }

}
