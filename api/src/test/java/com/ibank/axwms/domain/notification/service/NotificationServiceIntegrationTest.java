package com.ibank.axwms.domain.notification.service;

import com.ibank.axwms.domain.notification.NotificationReferenceType;
import com.ibank.axwms.domain.notification.NotificationType;
import com.ibank.axwms.domain.notification.repository.NotificationRepository;
import com.ibank.axwms.domain.notification.repository.jooq.projection.WorklogDueSoonReminderCandidateProjection;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

class NotificationServiceIntegrationTest extends IntegrationTestSupport {

    private static final LocalDate TODAY = LocalDate.of(2026, 5, 11);
    private static final LocalDate TARGET_DUE_DATE = TODAY.plusDays(3);
    private static final String WORKLOG_DUE_SOON_NOTIFICATION_TYPE = NotificationType.WORKLOG_DUE_SOON.name();
    private static final String WORKLOG_REFERENCE_TYPE = NotificationReferenceType.WORKLOG.name();

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

    @BeforeEach
    void setUp() {
        clearDatabase();
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
                                fixture.team().getId(),
                                TARGET_DUE_DATE,
                                WORKLOG_DUE_SOON_NOTIFICATION_TYPE,
                                WORKLOG_REFERENCE_TYPE,
                                pending.getId()
                        ),
                        tuple(
                                inProgress.getId(),
                                fixture.author().getId(),
                                fixture.team().getId(),
                                TARGET_DUE_DATE,
                                WORKLOG_DUE_SOON_NOTIFICATION_TYPE,
                                WORKLOG_REFERENCE_TYPE,
                                inProgress.getId()
                        ),
                        tuple(
                                onHold.getId(),
                                fixture.author().getId(),
                                fixture.team().getId(),
                                TARGET_DUE_DATE,
                                WORKLOG_DUE_SOON_NOTIFICATION_TYPE,
                                WORKLOG_REFERENCE_TYPE,
                                onHold.getId()
                        )
                );
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
                WorklogImportance.NORMAL,
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
