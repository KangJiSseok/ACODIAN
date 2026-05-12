package com.ibank.axwms.domain.notification.service;

import com.ibank.axwms.domain.notification.NotificationReferenceType;
import com.ibank.axwms.domain.notification.NotificationType;
import com.ibank.axwms.domain.notification.entity.Notification;
import com.ibank.axwms.domain.notification.repository.NotificationRepository;
import com.ibank.axwms.domain.notification.repository.jooq.projection.WorklogDueSoonReminderCandidateProjection;
import com.ibank.axwms.domain.notification.repository.jooq.projection.WorklogOverdueReminderCandidateProjection;
import com.ibank.axwms.domain.organization.department.DepartmentStatus;
import com.ibank.axwms.domain.organization.department.entity.Department;
import com.ibank.axwms.domain.organization.department.repository.DepartmentRepository;
import com.ibank.axwms.domain.organization.team.TeamStatus;
import com.ibank.axwms.domain.organization.team.entity.Team;
import com.ibank.axwms.domain.organization.team.repository.TeamAdminRepository;
import com.ibank.axwms.domain.organization.team.repository.TeamRepository;
import com.ibank.axwms.domain.organization.team.repository.UserTeamRepository;
import com.ibank.axwms.domain.organization.user.EmploymentStatus;
import com.ibank.axwms.domain.organization.user.UserRole;
import com.ibank.axwms.domain.organization.user.entity.User;
import com.ibank.axwms.domain.organization.user.repository.UserRepository;
import com.ibank.axwms.domain.worklog.WorklogImportance;
import com.ibank.axwms.domain.worklog.WorklogStatus;
import com.ibank.axwms.domain.worklog.entity.Worklog;
import com.ibank.axwms.domain.worklog.repository.WorklogRepository;
import com.ibank.axwms.testsupport.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Range;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

class NotificationServiceIntegrationTest extends IntegrationTestSupport {

    private static final LocalDate TODAY = LocalDate.of(2026, 5, 11);
    private static final LocalDate TARGET_DUE_DATE = TODAY.plusDays(3);
    private static final String WORKLOG_DUE_SOON_NOTIFICATION_TYPE = NotificationType.WORKLOG_DUE_SOON.name();
    private static final String WORKLOG_DUE_TODAY_NOTIFICATION_TYPE = NotificationType.WORKLOG_DUE_TODAY.name();
    private static final String WORKLOG_OVERDUE_NOTIFICATION_TYPE = NotificationType.WORKLOG_OVERDUE.name();
    private static final String WORKLOG_REFERENCE_TYPE = NotificationReferenceType.WORKLOG.name();
    private static final String NOTIFICATION_STREAM_KEY = "notifications:stream";

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private WorklogRepository worklogRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamAdminRepository teamAdminRepository;

    @Autowired
    private UserTeamRepository userTeamRepository;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        clearDatabase();
        stringRedisTemplate.delete(NOTIFICATION_STREAM_KEY);
    }

    @Test
    @DisplayName("D-3 미삭제 진행 대상 업무만 authorId 수신자 후보로 조회한다")
    void d3_미삭제_진행_대상_업무만_authorId_수신자_후보로_조회한다() {
        // given
        ReminderBaseFixture fixture = seedReminderBaseFixture();
        Worklog pending = saveWorklog(fixture.author(), fixture.team(), "PENDING 대상", WorklogStatus.PENDING, TARGET_DUE_DATE, false);
        Worklog inProgress = saveWorklog(fixture.author(), fixture.team(), "IN_PROGRESS 대상", WorklogStatus.IN_PROGRESS, TARGET_DUE_DATE, false);
        Worklog onHold = saveWorklog(fixture.author(), fixture.team(), "ON_HOLD 대상", WorklogStatus.ON_HOLD, TARGET_DUE_DATE, false);
        saveWorklog(fixture.author(), fixture.team(), "D-2 제외", WorklogStatus.IN_PROGRESS, TODAY.plusDays(2), false);
        saveWorklog(fixture.author(), fixture.team(), "D-4 제외", WorklogStatus.IN_PROGRESS, TODAY.plusDays(4), false);
        saveWorklog(fixture.author(), fixture.team(), "마감일 없음 제외", WorklogStatus.IN_PROGRESS, null, false);
        saveWorklog(fixture.author(), fixture.team(), "삭제 업무 제외", WorklogStatus.IN_PROGRESS, TARGET_DUE_DATE, true);
        saveWorklog(fixture.author(), fixture.team(), "완료 업무 제외", WorklogStatus.COMPLETED, TARGET_DUE_DATE, false);
        saveWorklog(fixture.author(), fixture.team(), "취소 업무 제외", WorklogStatus.CANCELLED, TARGET_DUE_DATE, false);

        // when
        List<WorklogDueSoonReminderCandidateProjection> candidates =
                notificationService.findWorklogDueSoonReminderCandidates(TODAY);

        // then
        assertThat(candidates)
                .extracting(
                        WorklogDueSoonReminderCandidateProjection::worklogId,
                        WorklogDueSoonReminderCandidateProjection::recipientUserId,
                        WorklogDueSoonReminderCandidateProjection::departmentId,
                        WorklogDueSoonReminderCandidateProjection::teamId,
                        WorklogDueSoonReminderCandidateProjection::dueDate,
                        WorklogDueSoonReminderCandidateProjection::notificationType,
                        WorklogDueSoonReminderCandidateProjection::referenceType,
                        WorklogDueSoonReminderCandidateProjection::referenceId
                )
                .containsExactly(
                        tuple(
                                pending.getId(),
                                fixture.author().getId(),
                                fixture.department().getId(),
                                fixture.team().getId(),
                                TARGET_DUE_DATE,
                                WORKLOG_DUE_SOON_NOTIFICATION_TYPE,
                                WORKLOG_REFERENCE_TYPE,
                                pending.getId()
                        ),
                        tuple(
                                inProgress.getId(),
                                fixture.author().getId(),
                                fixture.department().getId(),
                                fixture.team().getId(),
                                TARGET_DUE_DATE,
                                WORKLOG_DUE_SOON_NOTIFICATION_TYPE,
                                WORKLOG_REFERENCE_TYPE,
                                inProgress.getId()
                        ),
                        tuple(
                                onHold.getId(),
                                fixture.author().getId(),
                                fixture.department().getId(),
                                fixture.team().getId(),
                                TARGET_DUE_DATE,
                                WORKLOG_DUE_SOON_NOTIFICATION_TYPE,
                                WORKLOG_REFERENCE_TYPE,
                                onHold.getId()
                        )
                );
    }

    @Test
    @DisplayName("동일 업무 마감 알림이 이미 있으면 후보에서 제외한다")
    void 동일_업무_마감_알림이_이미_있으면_후보에서_제외한다() {
        // given
        ReminderBaseFixture fixture = seedReminderBaseFixture();
        Worklog worklog = saveWorklog(fixture.author(), fixture.team(), "중복 제외 대상", WorklogStatus.PENDING, TARGET_DUE_DATE, false);
        notificationRepository.save(Notification.createWorklogDueSoonReminder(
                fixture.author().getId(),
                fixture.department().getId(),
                fixture.team().getId(),
                worklog.getId(),
                "기존 알림",
                "이미 생성된 알림"
        ));

        // when
        List<WorklogDueSoonReminderCandidateProjection> candidates =
                notificationService.findWorklogDueSoonReminderCandidates(TODAY);

        // then
        assertThat(candidates).isEmpty();
    }

    @Test
    @DisplayName("D-3 후보를 Notification row로 저장한다")
    void d3_후보를_Notification_row로_저장한다() {
        // given
        ReminderBaseFixture fixture = seedReminderBaseFixture();
        Worklog worklog = saveWorklog(fixture.author(), fixture.team(), "저장 대상 업무", WorklogStatus.IN_PROGRESS, TARGET_DUE_DATE, false);

        // when
        int createdCount = notificationService.createWorklogDueSoonReminderNotifications(TODAY);

        // then
        assertThat(createdCount).isOne();
        List<Notification> notifications = notificationRepository.findAll();
        assertThat(notifications)
                .extracting(
                        Notification::getUserId,
                        Notification::getDepartmentId,
                        Notification::getTeamId,
                        Notification::getNotificationType,
                        Notification::getReferenceType,
                        Notification::getReferenceId,
                        Notification::getIsRead
                )
                .containsExactly(tuple(
                        fixture.author().getId(),
                        fixture.department().getId(),
                        fixture.team().getId(),
                        WORKLOG_DUE_SOON_NOTIFICATION_TYPE,
                        WORKLOG_REFERENCE_TYPE,
                        worklog.getId(),
                        Boolean.FALSE
                ));
        Notification notification = notifications.getFirst();
        assertThat(notification.getTitle()).isEqualTo("업무 마감 3일 전 알림");
        assertThat(notification.getContent()).isEqualTo(
                fixture.team().getTeamName() + "의 저장 대상 업무 마감일이 3일 남았습니다. 마감일 : " + TARGET_DUE_DATE
        );

        List<MapRecord<String, Object, Object>> streamRecords = streamRecords();
        assertThat(streamRecords).hasSize(1);
        Map<Object, Object> streamFields = streamRecords.getFirst().getValue();
        assertThat(streamFields)
                .containsEntry("notificationId", String.valueOf(notification.getId()))
                .containsEntry("userId", String.valueOf(fixture.author().getId()))
                .containsEntry("type", WORKLOG_DUE_SOON_NOTIFICATION_TYPE)
                .containsEntry("title", "업무 마감 3일 전 알림")
                .containsEntry("content", fixture.team().getTeamName()
                        + "의 저장 대상 업무 마감일이 3일 남았습니다. 마감일 : " + TARGET_DUE_DATE)
                .containsEntry("referenceType", WORKLOG_REFERENCE_TYPE)
                .containsEntry("referenceId", String.valueOf(worklog.getId()));
        assertThat(streamFields.get("createdAt")).isEqualTo(notification.getCreatedAt().toString());
    }

    @Test
    @DisplayName("같은 기준일로 반복 실행해도 동일 identity 알림은 중복 저장하지 않는다")
    void 같은_기준일로_반복_실행해도_동일_identity_알림은_중복_저장하지_않는다() {
        // given
        ReminderBaseFixture fixture = seedReminderBaseFixture();
        saveWorklog(fixture.author(), fixture.team(), "반복 실행 대상", WorklogStatus.ON_HOLD, TARGET_DUE_DATE, false);

        // when
        int firstCreatedCount = notificationService.createWorklogDueSoonReminderNotifications(TODAY);
        int secondCreatedCount = notificationService.createWorklogDueSoonReminderNotifications(TODAY);

        // then
        assertThat(firstCreatedCount).isOne();
        assertThat(secondCreatedCount).isZero();
        assertThat(notificationRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("알림 생성 트랜잭션이 rollback 되면 Redis Stream record를 발행하지 않는다")
    void 알림_생성_트랜잭션이_rollback_되면_Redis_Stream_record를_발행하지_않는다() {
        // given
        ReminderBaseFixture fixture = seedReminderBaseFixture();
        saveWorklog(fixture.author(), fixture.team(), "rollback 대상", WorklogStatus.PENDING, TARGET_DUE_DATE, false);

        // when & then
        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            notificationService.createWorklogDueSoonReminderNotifications(TODAY);
            throw new RuntimeException("rollback");
        })).isInstanceOf(RuntimeException.class)
                .hasMessage("rollback");
        assertThat(notificationRepository.findAll()).isEmpty();
        assertThat(streamRecords()).isEmpty();
    }

    @Test
    @DisplayName("당일과 overdue 미삭제 진행 대상 업무만 authorId 수신자 후보로 조회한다")
    void 당일과_overdue_미삭제_진행_대상_업무만_authorId_수신자_후보로_조회한다() {
        // given
        ReminderBaseFixture fixture = seedReminderBaseFixture();
        LocalDate overdueDate = TODAY.minusDays(1);
        Worklog pending = saveWorklog(fixture.author(), fixture.team(), "PENDING 초과 대상", WorklogStatus.PENDING, overdueDate, false);
        Worklog inProgress = saveWorklog(fixture.author(), fixture.team(), "IN_PROGRESS 초과 대상", WorklogStatus.IN_PROGRESS, overdueDate, false);
        Worklog onHold = saveWorklog(fixture.author(), fixture.team(), "ON_HOLD 초과 대상", WorklogStatus.ON_HOLD, overdueDate, false);
        Worklog dueToday = saveWorklog(fixture.author(), fixture.team(), "오늘 마감 대상", WorklogStatus.IN_PROGRESS, TODAY, false);
        saveWorklog(fixture.author(), fixture.team(), "미래 마감 제외", WorklogStatus.IN_PROGRESS, TODAY.plusDays(1), false);
        saveWorklog(fixture.author(), fixture.team(), "마감일 없음 제외", WorklogStatus.IN_PROGRESS, null, false);
        saveWorklog(fixture.author(), fixture.team(), "삭제 업무 제외", WorklogStatus.IN_PROGRESS, overdueDate, true);
        saveWorklog(fixture.author(), fixture.team(), "완료 업무 제외", WorklogStatus.COMPLETED, overdueDate, false);
        saveWorklog(fixture.author(), fixture.team(), "취소 업무 제외", WorklogStatus.CANCELLED, overdueDate, false);

        // when
        List<WorklogOverdueReminderCandidateProjection> candidates =
                notificationService.findWorklogOverdueReminderCandidates(TODAY);

        // then
        assertThat(candidates)
                .extracting(
                        WorklogOverdueReminderCandidateProjection::recipientUserId,
                        WorklogOverdueReminderCandidateProjection::departmentId,
                        WorklogOverdueReminderCandidateProjection::teamId,
                        WorklogOverdueReminderCandidateProjection::dueDate,
                        WorklogOverdueReminderCandidateProjection::notificationType,
                        WorklogOverdueReminderCandidateProjection::referenceType,
                        WorklogOverdueReminderCandidateProjection::referenceId
                )
                .containsExactly(
                        tuple(
                                fixture.author().getId(),
                                fixture.department().getId(),
                                fixture.team().getId(),
                                overdueDate,
                                WORKLOG_OVERDUE_NOTIFICATION_TYPE,
                                WORKLOG_REFERENCE_TYPE,
                                pending.getId()
                        ),
                        tuple(
                                fixture.author().getId(),
                                fixture.department().getId(),
                                fixture.team().getId(),
                                overdueDate,
                                WORKLOG_OVERDUE_NOTIFICATION_TYPE,
                                WORKLOG_REFERENCE_TYPE,
                                inProgress.getId()
                        ),
                        tuple(
                                fixture.author().getId(),
                                fixture.department().getId(),
                                fixture.team().getId(),
                                overdueDate,
                                WORKLOG_OVERDUE_NOTIFICATION_TYPE,
                                WORKLOG_REFERENCE_TYPE,
                                onHold.getId()
                        ),
                        tuple(
                                fixture.author().getId(),
                                fixture.department().getId(),
                                fixture.team().getId(),
                                TODAY,
                                WORKLOG_DUE_TODAY_NOTIFICATION_TYPE,
                                WORKLOG_REFERENCE_TYPE,
                                dueToday.getId()
                        )
                );
    }

    @Test
    @DisplayName("overdue 후보를 새 Notification row로 저장하되 Redis Stream을 발행하지 않는다")
    void overdue_후보를_새_Notification_row로_저장하되_Redis_Stream을_발행하지_않는다() {
        // given
        ReminderBaseFixture fixture = seedReminderBaseFixture();
        Worklog worklog = saveWorklog(fixture.author(), fixture.team(), "신규 초과 대상", WorklogStatus.IN_PROGRESS, TODAY.minusDays(1), false);

        // when
        int affectedCount = notificationService.createOrUpdateWorklogOverdueReminderNotifications(TODAY);

        // then
        assertThat(affectedCount).isOne();
        List<Notification> notifications = notificationRepository.findAll();
        assertThat(notifications)
                .extracting(
                        Notification::getUserId,
                        Notification::getDepartmentId,
                        Notification::getTeamId,
                        Notification::getNotificationType,
                        Notification::getReferenceType,
                        Notification::getReferenceId,
                        Notification::getIsRead
                )
                .containsExactly(tuple(
                        fixture.author().getId(),
                        fixture.department().getId(),
                        fixture.team().getId(),
                        WORKLOG_OVERDUE_NOTIFICATION_TYPE,
                        WORKLOG_REFERENCE_TYPE,
                        worklog.getId(),
                        Boolean.FALSE
                ));
        Notification notification = notifications.getFirst();
        assertThat(notification.getReadAt()).isNull();
        assertThat(notification.getTitle()).isEqualTo("업무 마감일 초과 알림");
        assertThat(notification.getContent()).isEqualTo(
                fixture.team().getTeamName() + "의 신규 초과 대상 마감일이 1일 지났습니다. 마감일 : "
                        + TODAY.minusDays(1) + ", 확인 기준일 : " + TODAY
        );
        assertThat(streamRecords()).isEmpty();
    }

    @Test
    @DisplayName("오늘 마감 후보는 due today 타입과 오늘까지 문구로 저장한다")
    void 오늘_마감_후보는_due_today_타입과_오늘까지_문구로_저장한다() {
        // given
        ReminderBaseFixture fixture = seedReminderBaseFixture();
        Worklog worklog = saveWorklog(fixture.author(), fixture.team(), "당일 마감 대상", WorklogStatus.IN_PROGRESS, TODAY, false);

        // when
        int affectedCount = notificationService.createOrUpdateWorklogOverdueReminderNotifications(TODAY);

        // then
        assertThat(affectedCount).isOne();
        Notification notification = notificationRepository.findAll().getFirst();
        assertThat(notification.getNotificationType()).isEqualTo(WORKLOG_DUE_TODAY_NOTIFICATION_TYPE);
        assertThat(notification.getTitle()).isEqualTo("업무 마감 오늘까지 알림");
        assertThat(notification.getContent()).isEqualTo(
                fixture.team().getTeamName() + "의 당일 마감 대상 마감일이 오늘까지입니다. 오늘 안에 업무를 완료해 주세요. 마감일 : " + TODAY
        );
        assertThat(notification.getReferenceId()).isEqualTo(worklog.getId());
        assertThat(streamRecords()).isEmpty();
    }

    @Test
    @DisplayName("overdue 실행은 기존 D-3 row를 갱신하고 생성 시각과 읽음 상태를 보존한다")
    void overdue_실행은_기존_D3_row를_갱신하고_생성_시각과_읽음_상태를_보존한다() {
        // given
        ReminderBaseFixture fixture = seedReminderBaseFixture();
        Worklog worklog = saveWorklog(fixture.author(), fixture.team(), "기존 초과 대상", WorklogStatus.PENDING, TODAY.minusDays(2), false);
        LocalDateTime readAt = LocalDateTime.of(2026, 5, 10, 12, 30);
        Notification existingNotification = notificationRepository.saveAndFlush(Notification.create(
                fixture.author().getId(),
                fixture.department().getId(),
                fixture.team().getId(),
                WORKLOG_DUE_SOON_NOTIFICATION_TYPE,
                "기존 D-3 알림",
                "기존 본문",
                WORKLOG_REFERENCE_TYPE,
                worklog.getId(),
                true,
                readAt
        ));
        Long existingNotificationId = existingNotification.getId();
        LocalDateTime createdAt = existingNotification.getCreatedAt();
        LocalDateTime oldUpdatedAt = LocalDateTime.of(2026, 5, 1, 9, 0);
        forceNotificationUpdatedAt(existingNotificationId, oldUpdatedAt);

        // when
        int affectedCount = notificationService.createOrUpdateWorklogOverdueReminderNotifications(TODAY);

        // then
        assertThat(affectedCount).isOne();
        assertThat(notificationRepository.findAll()).hasSize(1);
        Notification updatedNotification = notificationRepository.findById(existingNotificationId).orElseThrow();
        assertThat(updatedNotification.getNotificationType()).isEqualTo(WORKLOG_OVERDUE_NOTIFICATION_TYPE);
        assertThat(updatedNotification.getTitle()).isEqualTo("업무 마감일 초과 알림");
        assertThat(updatedNotification.getContent()).contains("기존 초과 대상 마감일이 2일 지났습니다.");
        assertThat(updatedNotification.getCreatedAt()).isEqualTo(createdAt);
        assertThat(updatedNotification.getUpdatedAt()).isAfter(oldUpdatedAt);
        assertThat(updatedNotification.getIsRead()).isTrue();
        assertThat(updatedNotification.getReadAt()).isEqualTo(readAt);
        assertThat(streamRecords()).isEmpty();
    }

    @Test
    @DisplayName("overdue 반복 실행은 같은 업무 알림 row를 추가하지 않는다")
    void overdue_반복_실행은_같은_업무_알림_row를_추가하지_않는다() {
        // given
        ReminderBaseFixture fixture = seedReminderBaseFixture();
        saveWorklog(fixture.author(), fixture.team(), "반복 초과 대상", WorklogStatus.ON_HOLD, TODAY.minusDays(3), false);

        // when
        int firstAffectedCount = notificationService.createOrUpdateWorklogOverdueReminderNotifications(TODAY);
        int secondAffectedCount = notificationService.createOrUpdateWorklogOverdueReminderNotifications(TODAY);

        // then
        assertThat(firstAffectedCount).isOne();
        assertThat(secondAffectedCount).isOne();
        assertThat(notificationRepository.findAll()).hasSize(1);
        assertThat(streamRecords()).isEmpty();
    }

    /**
     * 다른 통합 테스트가 같은 컨테이너를 공유해도 FK 제약에 걸리지 않도록 알림과 팀/사용자 연결 데이터를 먼저 제거한다.
     */
    private void clearDatabase() {
        notificationRepository.deleteAllInBatch();
        worklogRepository.deleteAllInBatch();
        teamAdminRepository.deleteAllInBatch();
        userTeamRepository.deleteAllInBatch();

        List<Department> departments = departmentRepository.findAll();
        departments.forEach(department -> department.assignHeadUserId(null));
        departmentRepository.saveAll(departments);
        departmentRepository.flush();

        teamRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        departmentRepository.deleteAllInBatch();
    }

    /**
     * Stream 검증은 Phase 3 계약 field 만 확인하고 후속 phase 의 구독 동작은 포함하지 않는다.
     */
    private List<MapRecord<String, Object, Object>> streamRecords() {
        return stringRedisTemplate.opsForStream().range(NOTIFICATION_STREAM_KEY, Range.unbounded());
    }

    /**
     * JPA auditing 이 갱신한 시각과 overdue update 이후 시각을 결정적으로 비교하기 위해 DB 값을 직접 고정한다.
     */
    private void forceNotificationUpdatedAt(Long notificationId, LocalDateTime updatedAt) {
        jdbcTemplate.update(
                "UPDATE tb_notification SET updated_at = ? WHERE notification_id = ?",
                updatedAt,
                notificationId
        );
    }

    /**
     * 업무 FK를 만족하는 최소 조직 데이터를 구성해 후보 조건 외 변수가 테스트 결과에 섞이지 않게 한다.
     */
    private ReminderBaseFixture seedReminderBaseFixture() {
        Department department = departmentRepository.save(createDepartment());
        User author = userRepository.save(createUser(department.getId(), "작성자"));
        Team team = teamRepository.save(createTeam(department.getId()));
        return new ReminderBaseFixture(department, author, team);
    }

    /**
     * D-3 후보 필터만 바꿔가며 검증할 수 있도록 나머지 업무 필드는 같은 기본값으로 고정한다.
     */
    private Worklog saveWorklog(User author,
                                Team team,
                                String title,
                                WorklogStatus status,
                                LocalDate dueDate,
                                boolean isDeleted) {
        Worklog worklog = Worklog.create(
                author.getId(),
                team.getId(),
                title,
                "요청 내용",
                "업무 내용",
                status,
                WorklogImportance.NORMAL,
                BigDecimal.ZERO,
                TODAY,
                dueDate
        );
        ReflectionTestUtils.setField(worklog, "statusCode", status);
        ReflectionTestUtils.setField(worklog, "isDeleted", isDeleted);
        return worklogRepository.save(worklog);
    }

    /**
     * 부서 상태를 명시해 조직 fixture 가 기본값 변경에 흔들리지 않게 한다.
     */
    private Department createDepartment() {
        Department department = Department.create("알림검증본부-" + System.nanoTime(), "알림 후보 검증용 부서");
        department.changeStatus(DepartmentStatus.ACTIVE);
        return department;
    }

    /**
     * 같은 통합 테스트 컨테이너 안에서 사용자 email UNIQUE 제약에 걸리지 않도록 호출마다 고유 이메일을 부여한다.
     */
    private User createUser(Long departmentId, String userName) {
        String uniqueEmail = userName + "-" + System.nanoTime() + "@ibank.com";
        return User.create(
                departmentId,
                userName,
                uniqueEmail,
                "$2a$10$abcdefghijklmnopqrstuv",
                UserRole.MEMBER,
                EmploymentStatus.ACTIVE,
                "사원",
                null,
                LocalDate.of(2025, 1, 1),
                null,
                null
        );
    }

    /**
     * Phase 1은 팀 상태 정책을 판단하지 않으므로 FK 충족용 ACTIVE 팀 하나만 둔다.
     */
    private Team createTeam(Long departmentId) {
        return Team.create(
                departmentId,
                "알림검증팀-" + System.nanoTime(),
                TeamStatus.ACTIVE,
                "알림 후보 검증용 팀",
                LocalDate.of(2026, 1, 1),
                null
        );
    }

    private record ReminderBaseFixture(Department department, User author, Team team) {
    }
}
