package com.ibank.axwms.domain.notification.service;

import com.ibank.axwms.domain.notification.dto.MarkAllNotificationsReadApiDto;
import com.ibank.axwms.domain.notification.dto.SearchNotificationsApiDto;
import com.ibank.axwms.domain.notification.event.NotificationCreatedEvent;
import com.ibank.axwms.domain.notification.entity.Notification;
import com.ibank.axwms.domain.notification.repository.NotificationRepository;
import com.ibank.axwms.domain.notification.repository.jooq.projection.WorklogDueSoonReminderCandidateProjection;
import com.ibank.axwms.domain.notification.repository.jooq.query.NotificationSearchQuery;
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import com.ibank.axwms.global.response.PageResponse;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private static final int WORKLOG_DUE_SOON_DAYS = 3;
    private static final String WORKLOG_DUE_SOON_TITLE = "업무 마감 3일 전 알림";

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
