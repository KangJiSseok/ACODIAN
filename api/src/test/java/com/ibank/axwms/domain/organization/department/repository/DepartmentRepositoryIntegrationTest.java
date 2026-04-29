package com.ibank.axwms.domain.organization.department.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ibank.axwms.domain.organization.department.DepartmentStatus;
import com.ibank.axwms.domain.organization.team.UserTeamAuthority;
import com.ibank.axwms.domain.organization.team.UserTeamStatus;
import com.ibank.axwms.domain.organization.department.entity.Department;
import com.ibank.axwms.domain.organization.department.repository.jooq.projection.DepartmentListItemProjection;
import com.ibank.axwms.domain.organization.department.repository.jooq.projection.DepartmentOverviewProjection;
import com.ibank.axwms.domain.organization.team.TeamStatus;
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
    @DisplayName("활성 부서 집계를 조회하면 활성 부서와 팀과 사용자 수만 계산한다")
    void 활성_부서_집계를_조회하면_활성_부서와_팀과_사용자_수만_계산한다() {
        DepartmentOverviewProjection overview = departmentRepository.getActiveDepartmentOverview();

        assertThat(overview.activeDepartmentCount()).isEqualTo(2);
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
                        Tuple.tuple("무부장본부", null, null)
                );
    }

    private Long logisticsHeadUserId;

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
        Team inactiveDepartmentActiveTeam = teamRepository.save(createTeam(inactiveDepartment.getId(), "휴면본부활성팀", TeamStatus.ACTIVE));

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

        userTeamRepository.save(UserTeam.create(activeMemberOne.getId(), logisticsActiveTeam.getId(), UserTeamAuthority.MEMBER, "담당", "주담당", true, UserTeamStatus.ACTIVE));
        userTeamRepository.save(UserTeam.create(activeMemberOne.getId(), emptyHeadActiveTeam.getId(), UserTeamAuthority.MEMBER, "협업", "겸임", false, UserTeamStatus.ACTIVE));
        userTeamRepository.save(UserTeam.create(activeMemberTwo.getId(), emptyHeadActiveTeam.getId(), UserTeamAuthority.LEADER, "리드", "주담당", true, UserTeamStatus.ACTIVE));
        userTeamRepository.save(UserTeam.create(leaveMember.getId(), logisticsActiveTeam.getId(), UserTeamAuthority.MEMBER, "담당", "주담당", true, UserTeamStatus.ACTIVE));
        userTeamRepository.save(UserTeam.create(inactiveTeamOnlyMember.getId(), logisticsInactiveTeam.getId(), UserTeamAuthority.MEMBER, "담당", "주담당", true, UserTeamStatus.ACTIVE));
        userTeamRepository.save(UserTeam.create(inactiveDepartmentTeamMember.getId(), inactiveDepartmentActiveTeam.getId(), UserTeamAuthority.MEMBER, "담당", "주담당", true, UserTeamStatus.ACTIVE));
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
