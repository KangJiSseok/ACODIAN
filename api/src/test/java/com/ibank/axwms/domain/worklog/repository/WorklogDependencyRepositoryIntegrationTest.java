package com.ibank.axwms.domain.worklog.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

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
import com.ibank.axwms.domain.worklog.entity.WorklogDependency;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.WorklogDependencyReadyParentProjection;
import com.ibank.axwms.testsupport.IntegrationTestSupport;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

class WorklogDependencyRepositoryIntegrationTest extends IntegrationTestSupport {

    private static final LocalDate TODAY = LocalDate.of(2026, 5, 11);

    @Autowired
    private WorklogDependencyRepository worklogDependencyRepository;

    @Autowired
    private WorklogRepository worklogRepository;

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

    @BeforeEach
    void setUp() {
        clearDatabase();
    }

    @Test
    @DisplayName("완료된 선행 업무의 직접 부모 중 모든 미삭제 선행이 완료된 부모만 반환한다")
    void 완료된_선행_업무의_직접_부모_중_모든_미삭제_선행이_완료된_부모만_반환한다() {
        // given
        Fixture fixture = seedFixture();
        Worklog completedPredecessor = saveWorklog(fixture.author(), fixture.team(), "완료 선행", WorklogStatus.COMPLETED, false);
        Worklog otherCompletedPredecessor = saveWorklog(fixture.author(), fixture.team(), "다른 완료 선행", WorklogStatus.COMPLETED, false);
        Worklog otherIncompletePredecessor = saveWorklog(fixture.author(), fixture.team(), "다른 미완료 선행", WorklogStatus.IN_PROGRESS, false);
        Worklog readyParent = saveWorklog(fixture.author(), fixture.team(), "준비된 부모", WorklogStatus.IN_PROGRESS, false);
        Worklog blockedParent = saveWorklog(fixture.author(), fixture.team(), "차단된 부모", WorklogStatus.IN_PROGRESS, false);
        Worklog recursiveAncestor = saveWorklog(fixture.author(), fixture.team(), "재귀 조상", WorklogStatus.IN_PROGRESS, false);

        saveDependency(readyParent, completedPredecessor);
        saveDependency(readyParent, otherCompletedPredecessor);
        saveDependency(blockedParent, completedPredecessor);
        saveDependency(blockedParent, otherIncompletePredecessor);
        saveDependency(recursiveAncestor, readyParent);

        // when
        List<WorklogDependencyReadyParentProjection> parents =
                worklogDependencyRepository.findReadyParentsByCompletedPredecessorId(completedPredecessor.getId());

        // then
        assertThat(parents)
                .extracting(
                        WorklogDependencyReadyParentProjection::parentWorklogId,
                        WorklogDependencyReadyParentProjection::parentAuthorId,
                        WorklogDependencyReadyParentProjection::parentTeamId,
                        WorklogDependencyReadyParentProjection::parentDepartmentId,
                        WorklogDependencyReadyParentProjection::teamName,
                        WorklogDependencyReadyParentProjection::parentTitle
                )
                .containsExactly(tuple(
                        readyParent.getId(),
                        fixture.author().getId(),
                        fixture.team().getId(),
                        fixture.department().getId(),
                        fixture.team().getTeamName(),
                        "준비된 부모"
                ));
    }

    @Test
    @DisplayName("삭제된 부모는 제외하고 삭제된 선행 업무는 완료 판정 blocking 대상에서 제외한다")
    void 삭제된_부모는_제외하고_삭제된_선행_업무는_완료_판정_blocking_대상에서_제외한다() {
        // given
        Fixture fixture = seedFixture();
        Worklog completedPredecessor = saveWorklog(fixture.author(), fixture.team(), "완료 선행", WorklogStatus.COMPLETED, false);
        Worklog deletedIncompletePredecessor = saveWorklog(fixture.author(), fixture.team(), "삭제된 미완료 선행", WorklogStatus.IN_PROGRESS, true);
        Worklog visibleParent = saveWorklog(fixture.author(), fixture.team(), "삭제 선행 무시 부모", WorklogStatus.IN_PROGRESS, false);
        Worklog deletedParent = saveWorklog(fixture.author(), fixture.team(), "삭제 부모", WorklogStatus.IN_PROGRESS, true);

        saveDependency(visibleParent, completedPredecessor);
        saveDependency(visibleParent, deletedIncompletePredecessor);
        saveDependency(deletedParent, completedPredecessor);

        // when
        List<WorklogDependencyReadyParentProjection> parents =
                worklogDependencyRepository.findReadyParentsByCompletedPredecessorId(completedPredecessor.getId());

        // then
        assertThat(parents)
                .extracting(WorklogDependencyReadyParentProjection::parentWorklogId)
                .containsExactly(visibleParent.getId());
    }

    /**
     * 다른 통합 테스트 fixture 와 충돌하지 않게 의존성에서 조직 root 순서로 데이터를 지운다.
     */
    private void clearDatabase() {
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
     * JOOQ 조회가 필요한 수신자/조직 snapshot 을 검증할 수 있게 최소 FK 그래프를 구성한다.
     */
    private Fixture seedFixture() {
        Department department = departmentRepository.save(createDepartment());
        User author = userRepository.save(createUser(department.getId()));
        Team team = teamRepository.save(createTeam(department.getId()));
        return new Fixture(department, author, team);
    }

    /**
     * dependency ready 조건만 테스트마다 바꾸기 위해 업무의 공통 필드를 유효한 기본값으로 고정한다.
     */
    private Worklog saveWorklog(User author, Team team, String title, WorklogStatus status, boolean isDeleted) {
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
        ReflectionTestUtils.setField(worklog, "isDeleted", isDeleted);
        return worklogRepository.saveAndFlush(worklog);
    }

    /**
     * 테스트 본문에서 parent/predecessor 방향을 명확히 드러내도록 엔티티 factory 호출을 한 곳에 둔다.
     */
    private void saveDependency(Worklog parent, Worklog predecessor) {
        worklogDependencyRepository.saveAndFlush(WorklogDependency.create(parent.getId(), predecessor.getId()));
    }

    private Department createDepartment() {
        Department department = Department.create("의존성검증본부-" + System.nanoTime(), "의존성 후보 검증용 부서");
        department.changeStatus(DepartmentStatus.ACTIVE);
        return department;
    }

    private User createUser(Long departmentId) {
        String uniqueEmail = "dependency-author-" + System.nanoTime() + "@ibank.com";
        return User.create(
                departmentId,
                "의존성작성자",
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
                "의존성검증팀-" + System.nanoTime(),
                TeamStatus.ACTIVE,
                "의존성 후보 검증용 팀",
                LocalDate.of(2026, 1, 1),
                null
        );
    }

    private record Fixture(Department department, User author, Team team) {
    }
}
