package com.ibank.axwms.domain.worklog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import com.ibank.axwms.domain.notification.NotificationReferenceType;
import com.ibank.axwms.domain.notification.NotificationType;
import com.ibank.axwms.domain.notification.entity.Notification;
import com.ibank.axwms.domain.notification.repository.NotificationRepository;
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
import com.ibank.axwms.domain.worklog.dto.UpdateWorklogStatusApiDto;
import com.ibank.axwms.domain.worklog.entity.Worklog;
import com.ibank.axwms.domain.worklog.entity.WorklogDependency;
import com.ibank.axwms.domain.worklog.repository.WorklogDependencyRepository;
import com.ibank.axwms.domain.worklog.repository.WorklogRepository;
import com.ibank.axwms.domain.worklog.repository.WorklogStatusHistoryRepository;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import com.ibank.axwms.testsupport.IntegrationTestSupport;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

class WorklogCompletionNotificationIntegrationTest extends IntegrationTestSupport {

    private static final LocalDate TODAY = LocalDate.of(2026, 5, 11);
    private static final String NOTIFICATION_STREAM_KEY = "notifications:stream";

    @Autowired
    private WorklogService worklogService;

    @Autowired
    private WorklogRepository worklogRepository;

    @Autowired
    private WorklogDependencyRepository worklogDependencyRepository;

    @Autowired
    private WorklogStatusHistoryRepository worklogStatusHistoryRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private TeamAdminRepository teamAdminRepository;

    @Autowired
    private UserTeamRepository userTeamRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void setUp() {
        clearDatabase();
        stringRedisTemplate.delete(NOTIFICATION_STREAM_KEY);
    }

    @Test
    @DisplayName("상태 변경 커밋 후 모든 직접 선행이 완료된 부모 업무 작성자에게 ready 알림과 Stream을 발행한다")
    void 상태_변경_커밋_후_모든_직접_선행이_완료된_부모_업무_작성자에게_ready_알림과_Stream을_발행한다() {
        // given
        Fixture fixture = seedFixture();
        Worklog finishingPredecessor = saveWorklog(fixture.predecessorAuthor(), fixture.team(), "마지막 완료 선행", WorklogStatus.IN_PROGRESS);
        Worklog alreadyCompletedPredecessor = saveWorklog(fixture.predecessorAuthor(), fixture.team(), "기존 완료 선행", WorklogStatus.COMPLETED);
        Worklog parent = saveWorklog(fixture.parentAuthor(), fixture.team(), "진행 가능한 부모", WorklogStatus.IN_PROGRESS);
        saveDependency(parent, finishingPredecessor);
        saveDependency(parent, alreadyCompletedPredecessor);

        // when
        worklogService.updateWorklogStatus(
                principal(fixture.predecessorAuthor()),
                finishingPredecessor.getId(),
                new UpdateWorklogStatusApiDto.Request(WorklogStatus.COMPLETED, "완료")
        );

        // then
        assertThat(notificationRepository.findAll())
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
                        fixture.parentAuthor().getId(),
                        fixture.department().getId(),
                        fixture.team().getId(),
                        NotificationType.WORKLOG_DEPENDENCY_READY.name(),
                        NotificationReferenceType.WORKLOG.name(),
                        parent.getId(),
                        Boolean.FALSE
                ));
        assertThat(worklogRepository.findById(parent.getId()).orElseThrow().getStatusCode())
                .isEqualTo(WorklogStatus.IN_PROGRESS);

        List<MapRecord<String, Object, Object>> streamRecords = streamRecords();
        assertThat(streamRecords).hasSize(1);
        Map<Object, Object> streamFields = streamRecords.getFirst().getValue();
        Notification notification = notificationRepository.findAll().getFirst();
        assertThat(streamFields)
                .containsEntry("notificationId", String.valueOf(notification.getId()))
                .containsEntry("userId", String.valueOf(fixture.parentAuthor().getId()))
                .containsEntry("type", NotificationType.WORKLOG_DEPENDENCY_READY.name())
                .containsEntry("referenceType", NotificationReferenceType.WORKLOG.name())
                .containsEntry("referenceId", String.valueOf(parent.getId()));
    }

    @Test
    @DisplayName("상태 변경 커밋 후 다른 직접 선행이 미완료면 ready 알림을 생성하지 않는다")
    void 상태_변경_커밋_후_다른_직접_선행이_미완료면_ready_알림을_생성하지_않는다() {
        // given
        Fixture fixture = seedFixture();
        Worklog finishingPredecessor = saveWorklog(fixture.predecessorAuthor(), fixture.team(), "완료되는 선행", WorklogStatus.IN_PROGRESS);
        Worklog incompletePredecessor = saveWorklog(fixture.predecessorAuthor(), fixture.team(), "남은 미완료 선행", WorklogStatus.IN_PROGRESS);
        Worklog parent = saveWorklog(fixture.parentAuthor(), fixture.team(), "아직 차단된 부모", WorklogStatus.IN_PROGRESS);
        saveDependency(parent, finishingPredecessor);
        saveDependency(parent, incompletePredecessor);

        // when
        worklogService.updateWorklogStatus(
                principal(fixture.predecessorAuthor()),
                finishingPredecessor.getId(),
                new UpdateWorklogStatusApiDto.Request(WorklogStatus.COMPLETED, "완료")
        );

        // then
        assertThat(notificationRepository.findAll()).isEmpty();
        assertThat(streamRecords()).isEmpty();
    }

    /**
     * 커밋 후 이벤트 통합 흐름이 다른 테스트 데이터에 영향받지 않도록 알림부터 조직 root 까지 FK 역순으로 정리한다.
     */
    private void clearDatabase() {
        notificationRepository.deleteAllInBatch();
        worklogStatusHistoryRepository.deleteAllInBatch();
        worklogDependencyRepository.deleteAllInBatch();
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
     * AFTER_COMMIT 이후 Redis Stream 전파까지 같은 동기 이벤트 체인에서 완료됐는지 확인한다.
     */
    private List<MapRecord<String, Object, Object>> streamRecords() {
        return stringRedisTemplate.opsForStream().range(NOTIFICATION_STREAM_KEY, Range.unbounded());
    }

    /**
     * 부모 작성자와 완료 처리자를 분리해 수신자가 완료 업무 작성자가 아니라 부모 작성자임을 검증한다.
     */
    private Fixture seedFixture() {
        Department department = departmentRepository.save(createDepartment());
        User parentAuthor = userRepository.save(createUser(department.getId(), "부모작성자"));
        User predecessorAuthor = userRepository.save(createUser(department.getId(), "선행작성자"));
        Team team = teamRepository.save(createTeam(department.getId()));
        return new Fixture(department, parentAuthor, predecessorAuthor, team);
    }

    /**
     * 실제 상태 전이 트랜잭션에서 JPA dirty checking 상태가 커밋되어 JOOQ 조회에 보이는지 확인하기 위한 업무 fixture 다.
     */
    private Worklog saveWorklog(User author, Team team, String title, WorklogStatus status) {
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
                TODAY.plusDays(1)
        );
        ReflectionTestUtils.setField(worklog, "statusCode", status);
        return worklogRepository.saveAndFlush(worklog);
    }

    /** parent 가 predecessor 를 직접 선행으로 갖는 방향을 테스트 본문에서 읽히게 한다. */
    private void saveDependency(Worklog parent, Worklog predecessor) {
        worklogDependencyRepository.saveAndFlush(WorklogDependency.create(parent.getId(), predecessor.getId()));
    }

    private CustomUserPrincipal principal(User user) {
        return new CustomUserPrincipal(user.getId(), user.getEmail(), "MEMBER");
    }

    private Department createDepartment() {
        Department department = Department.create("완료알림검증본부-" + System.nanoTime(), "완료 알림 검증용 부서");
        department.changeStatus(DepartmentStatus.ACTIVE);
        return department;
    }

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

    private Team createTeam(Long departmentId) {
        return Team.create(
                departmentId,
                "완료알림검증팀-" + System.nanoTime(),
                TeamStatus.ACTIVE,
                "완료 알림 검증용 팀",
                LocalDate.of(2026, 1, 1),
                null
        );
    }

    private record Fixture(Department department, User parentAuthor, User predecessorAuthor, Team team) {
    }
}
