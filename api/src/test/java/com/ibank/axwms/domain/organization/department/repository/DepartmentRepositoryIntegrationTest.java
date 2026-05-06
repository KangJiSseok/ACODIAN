package com.ibank.axwms.domain.organization.department.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ibank.axwms.domain.organization.department.DepartmentStatus;
import com.ibank.axwms.domain.organization.department.entity.Department;
import com.ibank.axwms.domain.organization.department.repository.jooq.projection.DepartmentDetailHeaderProjection;
import com.ibank.axwms.domain.organization.department.repository.jooq.projection.DepartmentDetailTeamProjection;
import com.ibank.axwms.domain.organization.department.repository.jooq.projection.DepartmentListItemProjection;
import com.ibank.axwms.domain.organization.department.repository.jooq.projection.DepartmentOverviewProjection;
import com.ibank.axwms.domain.organization.team.TeamStatus;
import com.ibank.axwms.domain.organization.team.UserTeamStatus;
import com.ibank.axwms.domain.organization.team.entity.Team;
import com.ibank.axwms.domain.organization.team.entity.UserTeam;
import com.ibank.axwms.domain.organization.team.repository.TeamRepository;
import com.ibank.axwms.domain.organization.team.repository.UserTeamRepository;
import com.ibank.axwms.domain.organization.user.EmploymentStatus;
import com.ibank.axwms.domain.organization.user.UserRole;
import com.ibank.axwms.domain.organization.user.entity.User;
import com.ibank.axwms.domain.organization.user.repository.UserRepository;
import com.ibank.axwms.testsupport.IntegrationTestSupport;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DepartmentRepositoryIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserTeamRepository userTeamRepository;

    @BeforeEach
    void setUp() {
        clearDatabase();
        seedDepartments();
    }

    @Test
    @DisplayName("부서 목록 집계를 조회하면 활성 부서와 소유 ACTIVE 팀과 활성 사용자를 계산한다")
    void 부서_목록_집계를_조회하면_활성_부서와_소유_active_팀과_활성_사용자를_계산한다() {
        DepartmentOverviewProjection overview = departmentRepository.getActiveDepartmentOverview();

        assertThat(overview.activeDepartmentCount()).isEqualTo(3);
        assertThat(overview.activeTeamCount()).isEqualTo(2);
        assertThat(overview.activeUserCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("활성 부서 목록을 조회하면 부서장 이름을 left join으로 함께 반환한다")
    void 활성_부서_목록을_조회하면_부서장_이름을_left_join으로_함께_반환한다() {
        List<DepartmentListItemProjection> departments = departmentRepository.findActiveDepartments();

        assertThat(departments)
                .extracting(
                        DepartmentListItemProjection::departmentName,
                        DepartmentListItemProjection::departmentHeadUserId,
                        DepartmentListItemProjection::departmentHeadUserName
                )
                .containsExactly(
                        Tuple.tuple("물류본부", logisticsHeadUserId, "박본부"),
                        Tuple.tuple("무부장본부", null, null),
                        Tuple.tuple("빈본부", null, null)
                );
    }

    @Test
    @DisplayName("활성 부서 상세 header를 조회하면 부서장 이름을 left join으로 반환한다")
    void 활성_부서_상세_header를_조회하면_부서장_이름을_left_join으로_반환한다() {
        DepartmentDetailHeaderProjection header = departmentRepository
                .findActiveDepartmentDetailHeader(logisticsDepartmentId)
                .orElseThrow();

        assertThat(header.departmentId()).isEqualTo(logisticsDepartmentId);
        assertThat(header.departmentName()).isEqualTo("물류본부");
        assertThat(header.departmentHeadUserId()).isEqualTo(logisticsHeadUserId);
        assertThat(header.departmentHeadUserName()).isEqualTo("박본부");
    }

    @Test
    @DisplayName("부서장이 없는 활성 부서 상세 header는 head 필드를 null로 반환한다")
    void 부서장이_없는_활성_부서_상세_header는_head_필드를_null로_반환한다() {
        DepartmentDetailHeaderProjection header = departmentRepository
                .findActiveDepartmentDetailHeader(emptyHeadDepartmentId)
                .orElseThrow();

        assertThat(header.departmentHeadUserId()).isNull();
        assertThat(header.departmentHeadUserName()).isNull();
    }

    @Test
    @DisplayName("inactive 부서 상세 header는 empty를 반환한다")
    void inactive_부서_상세_header는_empty를_반환한다() {
        assertThat(departmentRepository.findActiveDepartmentDetailHeader(inactiveDepartmentId)).isEmpty();
    }

    @Test
    @DisplayName("부서 상세 팀 목록은 같은 부서가 소유한 ACTIVE non-deleted 팀만 teamId 오름차순으로 반환한다")
    void 부서_상세_팀_목록은_같은_부서가_소유한_active_non_deleted_팀만_teamId_오름차순으로_반환한다() {
        List<DepartmentDetailTeamProjection> teams = departmentRepository.findActiveDepartmentDetailTeams(logisticsDepartmentId);

        assertThat(teams)
                .extracting(DepartmentDetailTeamProjection::teamId, DepartmentDetailTeamProjection::teamName)
                .containsExactly(Tuple.tuple(logisticsActiveTeamId, "물류혁신TF"));
    }

    @Test
    @DisplayName("부서 상세 팀 목록은 ACTIVE membership count와 leader 없음을 반환한다")
    void 부서_상세_팀_목록은_active_membership_count와_leader_없음을_반환한다() {
        DepartmentDetailTeamProjection team = departmentRepository.findActiveDepartmentDetailTeams(logisticsDepartmentId).get(0);

        assertThat(team.memberCount()).isEqualTo(1);
        assertThat(team.leaderId()).isNull();
        assertThat(team.leaderName()).isNull();
    }

    @Test
    @DisplayName("부서 상세 팀 목록은 ACTIVE leader가 있으면 leader 사용자명을 반환한다")
    void 부서_상세_팀_목록은_active_leader가_있으면_leader_사용자명을_반환한다() {
        DepartmentDetailTeamProjection team = departmentRepository.findActiveDepartmentDetailTeams(emptyHeadDepartmentId).get(0);

        assertThat(team.leaderId()).isNotNull();
        assertThat(team.leaderName()).isEqualTo("활성사용자2");
        assertThat(team.memberCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("ACTIVE owned team이 있으면 existsActiveOwnedTeam은 true를 반환한다")
    void active_owned_team이_있으면_existsActiveOwnedTeam은_true를_반환한다() {
        assertThat(departmentRepository.existsActiveOwnedTeam(logisticsDepartmentId)).isTrue();
    }

    @Test
    @DisplayName("소유 ACTIVE 팀이 없는 부서는 existsActiveOwnedTeam에서 false를 반환한다")
    void 소유_active_팀이_없는_부서는_existsActiveOwnedTeam에서_false를_반환한다() {
        assertThat(departmentRepository.existsActiveOwnedTeam(headlessDepartmentId)).isFalse();
    }

    private Long logisticsHeadUserId;
    private Long logisticsDepartmentId;
    private Long emptyHeadDepartmentId;
    private Long inactiveDepartmentId;
    private Long headlessDepartmentId;
    private Long logisticsActiveTeamId;

    /** FK 제약을 피하기 위해 부서장의 head reference 를 먼저 해제한 뒤 테스트 데이터를 비운다. */
    private void clearDatabase() {
        List<Department> departments = departmentRepository.findAll();
        departments.forEach(department -> department.assignHeadUserId(null));
        departmentRepository.saveAll(departments);
        departmentRepository.flush();

        userTeamRepository.deleteAll();
        teamRepository.deleteAll();
        userRepository.deleteAll();
        departmentRepository.deleteAll();
    }

    /** 집계 조건과 LEFT JOIN 부서장 노출 규칙을 함께 검증할 수 있는 통합 테스트 데이터를 구성한다. */
    private void seedDepartments() {
        Department logisticsDepartment = departmentRepository.save(createDepartment("물류본부", DepartmentStatus.ACTIVE));
        Department emptyHeadDepartment = departmentRepository.save(createDepartment("무부장본부", DepartmentStatus.ACTIVE));
        Department inactiveDepartment = departmentRepository.save(createDepartment("휴면본부", DepartmentStatus.INACTIVE));
        Department headlessDepartment = departmentRepository.save(createDepartment("빈본부", DepartmentStatus.ACTIVE));
        logisticsDepartmentId = logisticsDepartment.getId();
        emptyHeadDepartmentId = emptyHeadDepartment.getId();
        inactiveDepartmentId = inactiveDepartment.getId();
        headlessDepartmentId = headlessDepartment.getId();

        User departmentHead = userRepository.save(createUser(
                logisticsDepartment.getId(),
                "박본부",
                "department-head-" + System.nanoTime() + "@ibank.com",
                EmploymentStatus.ACTIVE,
                UserRole.DIRECTOR
        ));
        logisticsHeadUserId = departmentHead.getId();
        logisticsDepartment.assignHeadUserId(logisticsHeadUserId);
        departmentRepository.save(logisticsDepartment);

        Team logisticsActiveTeam = teamRepository.save(createTeam(logisticsDepartment.getId(), "물류혁신TF", TeamStatus.ACTIVE));
        Team emptyHeadActiveTeam = teamRepository.save(createTeam(emptyHeadDepartment.getId(), "운영지원TF", TeamStatus.ACTIVE));
        Team logisticsInactiveTeam = teamRepository.save(createTeam(logisticsDepartment.getId(), "휴면팀", TeamStatus.INACTIVE));
        teamRepository.save(createTeam(inactiveDepartment.getId(), "휴면본부활성팀", TeamStatus.ACTIVE));
        teamRepository.save(createTeam(null, "미배정활성팀", TeamStatus.ACTIVE));
        Team softDeletedActiveTeam = createTeam(logisticsDepartment.getId(), "삭제된활성팀", TeamStatus.ACTIVE);
        softDeletedActiveTeam.markDeleted(LocalDateTime.of(2026, 5, 1, 0, 0));
        teamRepository.save(softDeletedActiveTeam);
        logisticsActiveTeamId = logisticsActiveTeam.getId();

        User activeMemberOne = userRepository.save(createUser(
                logisticsDepartment.getId(),
                "활성사용자1",
                "active-one-" + System.nanoTime() + "@ibank.com",
                EmploymentStatus.ACTIVE,
                UserRole.MEMBER
        ));
        User activeMemberTwo = userRepository.save(createUser(
                emptyHeadDepartment.getId(),
                "활성사용자2",
                "active-two-" + System.nanoTime() + "@ibank.com",
                EmploymentStatus.ACTIVE,
                UserRole.MEMBER
        ));
        User leaveMember = userRepository.save(createUser(
                logisticsDepartment.getId(),
                "휴직사용자",
                "leave-" + System.nanoTime() + "@ibank.com",
                EmploymentStatus.LEAVE,
                UserRole.MEMBER
        ));
        User inactiveTeamOnlyMember = userRepository.save(createUser(
                logisticsDepartment.getId(),
                "비활성팀전용사용자",
                "inactive-team-only-" + System.nanoTime() + "@ibank.com",
                EmploymentStatus.ACTIVE,
                UserRole.MEMBER
        ));
        User inactiveDepartmentTeamMember = userRepository.save(createUser(
                inactiveDepartment.getId(),
                "비활성부서팀사용자",
                "inactive-department-team-" + System.nanoTime() + "@ibank.com",
                EmploymentStatus.ACTIVE,
                UserRole.MEMBER
        ));

        userTeamRepository.save(UserTeam.create(activeMemberOne.getId(), logisticsActiveTeam.getId(), false, "담당", "주담당", true, UserTeamStatus.ACTIVE));
        userTeamRepository.save(UserTeam.create(activeMemberTwo.getId(), emptyHeadActiveTeam.getId(), true, "리드", "주담당", true, UserTeamStatus.ACTIVE));
        userTeamRepository.save(UserTeam.create(inactiveTeamOnlyMember.getId(), logisticsInactiveTeam.getId(), false, "담당", "주담당", true, UserTeamStatus.ACTIVE));
        userTeamRepository.save(UserTeam.create(leaveMember.getId(), logisticsActiveTeam.getId(), true, "이탈리더", "겸임", false, UserTeamStatus.LEFT));
    }

    /** 테스트용 부서를 상태까지 포함해 생성한다. */
    private Department createDepartment(String departmentName, DepartmentStatus status) {
        Department department = Department.create(
                departmentName,
                departmentName + " 설명"
        );
        department.changeStatus(status);
        return department;
    }

    /** 테스트용 팀 엔티티를 부서와 상태 기준으로 생성한다. */
    private Team createTeam(Long departmentId, String teamName, TeamStatus status) {
        return Team.create(
                departmentId,
                teamName,
                status,
                teamName + " 설명",
                LocalDate.of(2026, 4, 1),
                null
        );
    }

    /** 테스트용 사용자를 소속 부서/재직 상태/권한 기준으로 생성한다. */
    private User createUser(Long departmentId,
                            String userName,
                            String email,
                            EmploymentStatus employmentStatus,
                            UserRole role) {
        return User.create(
                departmentId,
                userName,
                email,
                "$2a$10$abcdefghijklmnopqrstuv",
                role,
                employmentStatus,
                "사원",
                null,
                LocalDate.of(2025, 1, 1),
                null,
                null
        );
    }
}
