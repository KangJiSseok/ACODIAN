package com.ibank.axwms.domain.organization.team.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import com.ibank.axwms.domain.organization.department.DepartmentStatus;
import com.ibank.axwms.domain.organization.department.entity.Department;
import com.ibank.axwms.domain.organization.department.repository.DepartmentRepository;
import com.ibank.axwms.domain.organization.team.TeamStatus;
import com.ibank.axwms.domain.organization.team.UserTeamStatus;
import com.ibank.axwms.domain.organization.team.dto.GetTeamsApiDto;
import com.ibank.axwms.domain.organization.team.entity.Team;
import com.ibank.axwms.domain.organization.team.entity.TeamAdmin;
import com.ibank.axwms.domain.organization.team.entity.UserTeam;
import com.ibank.axwms.domain.organization.team.repository.jooq.projection.TeamSummaryProjection;
import com.ibank.axwms.domain.organization.team.repository.jooq.query.TeamPageQuery;
import com.ibank.axwms.domain.organization.user.EmploymentStatus;
import com.ibank.axwms.domain.organization.user.UserRole;
import com.ibank.axwms.domain.organization.user.entity.User;
import com.ibank.axwms.domain.organization.user.repository.UserRepository;
import com.ibank.axwms.testsupport.IntegrationTestSupport;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

class TeamRepositoryIntegrationTest extends IntegrationTestSupport {

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
    @DisplayName("팀명으로 로컬 시드 재사용 대상 팀을 조회한다")
    void 팀명으로_로컬_시드_재사용_대상_팀을_조회한다() {
        Team team = teamRepository.save(createTeam("물류혁신TF", TeamStatus.ACTIVE));

        assertThat(teamRepository.findFirstByTeamNameOrderByDeletedAtDesc("물류혁신TF"))
                .get()
                .extracting(Team::getId)
                .isEqualTo(team.getId());
    }

    @Test
    @DisplayName("팀명이 일치하지 않으면 빈 Optional 을 반환한다")
    void 팀명이_일치하지_않으면_빈_Optional을_반환한다() {
        teamRepository.save(createTeam("물류혁신TF", TeamStatus.ACTIVE));

        assertThat(teamRepository.findFirstByTeamNameOrderByDeletedAtDesc("운영지원TF"))
                .isEmpty();
    }

    @Test
    @DisplayName("visible scope 팀 목록은 admin grant 와 ACTIVE membership 합집합으로 조회하고 정렬/집계를 적용한다")
    void visible_scope_팀_목록은_admin_grant와_ACTIVE_membership_합집합으로_조회하고_정렬_집계를_적용한다() {
        TeamFixture fixture = seedVisibleScopeFixture();

        Page<TeamSummaryProjection> result = teamRepository.findTeamPage(
                fixture.callerId(),
                TeamPageQuery.from(new GetTeamsApiDto.Request(1, 20))
        );

        assertThat(result.getTotalElements()).isEqualTo(4);
        assertThat(result.getTotalPages()).isEqualTo(1);
        assertThat(result.getContent())
                .extracting(
                        TeamSummaryProjection::teamId,
                        TeamSummaryProjection::teamName,
                        TeamSummaryProjection::statusCode,
                        TeamSummaryProjection::teamLeaderId,
                        TeamSummaryProjection::teamLeaderName,
                        TeamSummaryProjection::memberCount,
                        TeamSummaryProjection::myIsLeader,
                        TeamSummaryProjection::teamRole,
                        TeamSummaryProjection::allocation,
                        TeamSummaryProjection::isPrimary
                )
                .containsExactly(
                        tuple(fixture.leaderTeamId(), "리더팀", "ACTIVE", fixture.callerId(), "호출자", 2L, true, "리더", "SECONDARY", false),
                        tuple(fixture.primaryTeamId(), "주담당팀", "ACTIVE", null, null, 1L, false, "주담당", "PRIMARY", true),
                        tuple(fixture.adminOnlyTeamId(), "관리전용팀", "ACTIVE", fixture.adminOnlyLeaderId(), "관리전용리더", 1L, false, null, null, false),
                        tuple(fixture.inactiveDuplicateTeamId(), "중복권한비활성팀", "INACTIVE", fixture.callerId(), "호출자", 1L, true, "중복", "PRIMARY", true)
                );
        assertThat(result.getContent())
                .extracting(TeamSummaryProjection::teamId)
                .doesNotContain(fixture.leftOnlyTeamId(), fixture.deletedTeamId());
    }

    private void clearDatabase() {
        List<Department> departments = departmentRepository.findAll();
        departments.forEach(department -> department.assignHeadUserId(null));
        departmentRepository.saveAll(departments);
        departmentRepository.flush();

        teamAdminRepository.deleteAll();
        userTeamRepository.deleteAll();
        teamRepository.deleteAll();
        userRepository.deleteAll();
        departmentRepository.deleteAll();
    }

    private TeamFixture seedVisibleScopeFixture() {
        Department department = departmentRepository.save(createDepartment("팀목록검증본부"));
        User caller = userRepository.save(createUser(department.getId(), "호출자", UserRole.MEMBER));
        User activeMember = userRepository.save(createUser(department.getId(), "활성멤버", UserRole.MEMBER));
        User leftMember = userRepository.save(createUser(department.getId(), "이탈멤버", UserRole.MEMBER));
        User adminOnlyLeader = userRepository.save(createUser(department.getId(), "관리전용리더", UserRole.MEMBER));

        Team leaderTeam = teamRepository.save(createTeam("리더팀", TeamStatus.ACTIVE));
        Team primaryTeam = teamRepository.save(createTeam("주담당팀", TeamStatus.ACTIVE));
        Team adminOnlyTeam = teamRepository.save(createTeam("관리전용팀", TeamStatus.ACTIVE));
        Team inactiveDuplicateTeam = teamRepository.save(createTeam("중복권한비활성팀", TeamStatus.INACTIVE));
        Team leftOnlyTeam = teamRepository.save(createTeam("이탈소속팀", TeamStatus.ACTIVE));
        Team deletedTeam = teamRepository.save(createDeletedTeam("삭제팀"));

        userTeamRepository.save(UserTeam.create(caller.getId(), leaderTeam.getId(), true, "리더", "SECONDARY", false, UserTeamStatus.ACTIVE));
        userTeamRepository.save(UserTeam.create(activeMember.getId(), leaderTeam.getId(), false, "구성원", "SECONDARY", false, UserTeamStatus.ACTIVE));
        userTeamRepository.save(UserTeam.create(leftMember.getId(), leaderTeam.getId(), false, "이탈", "SECONDARY", false, UserTeamStatus.LEFT));
        userTeamRepository.save(UserTeam.create(caller.getId(), primaryTeam.getId(), false, "주담당", "PRIMARY", true, UserTeamStatus.ACTIVE));
        userTeamRepository.save(UserTeam.create(adminOnlyLeader.getId(), adminOnlyTeam.getId(), true, "관리전용", "SECONDARY", false, UserTeamStatus.ACTIVE));
        userTeamRepository.save(UserTeam.create(caller.getId(), inactiveDuplicateTeam.getId(), true, "중복", "PRIMARY", true, UserTeamStatus.ACTIVE));
        userTeamRepository.save(UserTeam.create(caller.getId(), leftOnlyTeam.getId(), false, "이탈", "PRIMARY", true, UserTeamStatus.LEFT));

        teamAdminRepository.save(TeamAdmin.grant(caller.getId(), adminOnlyTeam.getId()));
        teamAdminRepository.save(TeamAdmin.grant(caller.getId(), inactiveDuplicateTeam.getId()));
        teamAdminRepository.save(TeamAdmin.grant(caller.getId(), deletedTeam.getId()));

        return new TeamFixture(
                caller.getId(),
                adminOnlyLeader.getId(),
                leaderTeam.getId(),
                primaryTeam.getId(),
                adminOnlyTeam.getId(),
                inactiveDuplicateTeam.getId(),
                leftOnlyTeam.getId(),
                deletedTeam.getId()
        );
    }

    private Department createDepartment(String departmentName) {
        Department department = Department.create(departmentName, departmentName + " 설명");
        department.changeStatus(DepartmentStatus.ACTIVE);
        return department;
    }

    private User createUser(Long departmentId, String userName, UserRole role) {
        String unique = userName + "-" + System.nanoTime() + "@ibank.com";
        return User.create(
                departmentId,
                userName,
                unique,
                "$2a$10$abcdefghijklmnopqrstuv",
                role,
                EmploymentStatus.ACTIVE,
                "사원",
                null,
                LocalDate.of(2025, 1, 1),
                null,
                null
        );
    }

    private Team createTeam(String teamName, TeamStatus status) {
        return Team.create(
                teamName,
                status,
                teamName + " 설명",
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 12, 31)
        );
    }

    private Team createDeletedTeam(String teamName) {
        Team team = createTeam(teamName, TeamStatus.ACTIVE);
        team.markDeleted(LocalDateTime.of(2026, 5, 1, 0, 0));
        return team;
    }

    private record TeamFixture(Long callerId,
                               Long adminOnlyLeaderId,
                               Long leaderTeamId,
                               Long primaryTeamId,
                               Long adminOnlyTeamId,
                               Long inactiveDuplicateTeamId,
                               Long leftOnlyTeamId,
                               Long deletedTeamId) {
    }
}
