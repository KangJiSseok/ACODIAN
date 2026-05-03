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
import com.ibank.axwms.domain.organization.team.repository.jooq.projection.TeamDetailProjection;
import com.ibank.axwms.domain.organization.team.repository.jooq.projection.TeamStatusSummaryProjection;
import com.ibank.axwms.domain.organization.team.repository.jooq.projection.TeamSummaryProjection;
import com.ibank.axwms.domain.organization.team.repository.jooq.projection.TeamUserSummaryProjection;
import com.ibank.axwms.domain.organization.team.repository.jooq.query.TeamPageQuery;
import com.ibank.axwms.domain.organization.user.EmploymentStatus;
import com.ibank.axwms.domain.organization.user.UserRole;
import com.ibank.axwms.domain.organization.user.entity.User;
import com.ibank.axwms.domain.organization.user.repository.UserRepository;
import com.ibank.axwms.domain.worklog.WorklogImportance;
import com.ibank.axwms.domain.worklog.WorklogStatus;
import com.ibank.axwms.domain.worklog.entity.Worklog;
import com.ibank.axwms.domain.worklog.repository.WorklogRepository;
import com.ibank.axwms.testsupport.IntegrationTestSupport;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.test.util.ReflectionTestUtils;

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

    @Autowired
    private WorklogRepository worklogRepository;

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
                        tuple(fixture.leaderTeamId(), "리더팀", "ACTIVE", fixture.callerId(), "호출자", 2L, true, "리더", "겸임", false),
                        tuple(fixture.primaryTeamId(), "주담당팀", "ACTIVE", null, null, 1L, false, "주담당", "주담당", true),
                        tuple(fixture.adminOnlyTeamId(), "관리전용팀", "ACTIVE", fixture.adminOnlyLeaderId(), "관리전용리더", 1L, false, null, null, false),
                        tuple(fixture.inactiveDuplicateTeamId(), "중복권한비활성팀", "INACTIVE", fixture.callerId(), "호출자", 1L, true, "중복", "주담당", true)
                );
        assertThat(result.getContent())
                .extracting(TeamSummaryProjection::teamId)
                .doesNotContain(fixture.leftOnlyTeamId(), fixture.deletedTeamId());
    }

    @Test
    @DisplayName("visible scope 팀 상세는 기본 정보와 DEPT_HEAD 팀 관리자를 조회한다")
    void visible_scope_팀_상세는_기본_정보와_DEPT_HEAD_팀_관리자를_조회한다() {
        TeamFixture fixture = seedVisibleScopeFixture();

        TeamDetailProjection result = teamRepository.findTeamDetail(fixture.callerId(), fixture.leaderTeamId())
                .orElseThrow();

        assertThat(result)
                .extracting(
                        TeamDetailProjection::teamId,
                        TeamDetailProjection::teamName,
                        TeamDetailProjection::statusCode,
                        TeamDetailProjection::teamLeaderId,
                        TeamDetailProjection::teamLeaderName,
                        TeamDetailProjection::deptHeadAdminUserId,
                        TeamDetailProjection::deptHeadAdminUsername
                )
                .containsExactly(
                        fixture.leaderTeamId(),
                        "리더팀",
                        "ACTIVE",
                        fixture.callerId(),
                        "호출자",
                        fixture.deptHeadAdminId(),
                        "사업부장관리자"
                );
        assertThat(result.startDate()).isEqualTo(LocalDate.of(2026, 4, 1));
        assertThat(result.expectedEndDate()).isEqualTo(LocalDate.of(2026, 12, 31));
    }

    @Test
    @DisplayName("visible scope 팀 상태 요약은 DISTINCT 팀 기준으로 soft-delete 팀을 제외하고 집계한다")
    void visible_scope_팀_상태_요약은_distinct_팀_기준으로_soft_delete_팀을_제외하고_집계한다() {
        TeamFixture fixture = seedVisibleScopeFixture();

        TeamStatusSummaryProjection result = teamRepository.countTeamSummary(fixture.callerId());

        assertThat(result)
                .extracting(
                        TeamStatusSummaryProjection::activeTeamCount,
                        TeamStatusSummaryProjection::inactiveTeamCount,
                        TeamStatusSummaryProjection::totalTeamCount
                )
                .containsExactly(3L, 1L, 4L);
    }

    @Test
    @DisplayName("visible scope 팀 상태 요약은 visible 팀이 없으면 모든 집계를 0으로 반환한다")
    void visible_scope_팀_상태_요약은_visible_팀이_없으면_모든_집계를_0으로_반환한다() {
        Department department = departmentRepository.save(createDepartment("빈요약검증본부"));
        User caller = userRepository.save(createUser(department.getId(), "빈요약호출자", UserRole.MEMBER));
        Team hiddenTeam = teamRepository.save(createTeam("권한없는요약팀", TeamStatus.ACTIVE));
        userTeamRepository.save(UserTeam.create(caller.getId(), hiddenTeam.getId(), false, "이탈", "겸임", false, UserTeamStatus.LEFT));

        TeamStatusSummaryProjection result = teamRepository.countTeamSummary(caller.getId());

        assertThat(result)
                .extracting(
                        TeamStatusSummaryProjection::activeTeamCount,
                        TeamStatusSummaryProjection::inactiveTeamCount,
                        TeamStatusSummaryProjection::totalTeamCount
                )
                .containsExactly(0L, 0L, 0L);
    }

    @Test
    @DisplayName("팀 상세는 admin grant 만 있는 팀도 visible scope 로 조회한다")
    void 팀_상세는_admin_grant만_있는_팀도_visible_scope로_조회한다() {
        TeamFixture fixture = seedVisibleScopeFixture();

        TeamDetailProjection result = teamRepository.findTeamDetail(fixture.callerId(), fixture.adminOnlyTeamId())
                .orElseThrow();

        assertThat(result)
                .extracting(
                        TeamDetailProjection::teamId,
                        TeamDetailProjection::teamName,
                        TeamDetailProjection::teamLeaderId,
                        TeamDetailProjection::teamLeaderName,
                        TeamDetailProjection::deptHeadAdminUserId,
                        TeamDetailProjection::deptHeadAdminUsername
                )
                .containsExactly(
                        fixture.adminOnlyTeamId(),
                        "관리전용팀",
                        fixture.adminOnlyLeaderId(),
                        "관리전용리더",
                        null,
                        null
                );
    }

    @Test
    @DisplayName("팀 상세는 LEFT membership 전용 팀과 soft-delete 팀을 반환하지 않는다")
    void 팀_상세는_left_membership_전용_팀과_soft_delete_팀을_반환하지_않는다() {
        TeamFixture fixture = seedVisibleScopeFixture();

        assertThat(teamRepository.findTeamDetail(fixture.callerId(), fixture.leftOnlyTeamId())).isEmpty();
        assertThat(teamRepository.findTeamDetail(fixture.callerId(), fixture.deletedTeamId())).isEmpty();
    }

    @Test
    @DisplayName("visible scope 팀 사용자 목록은 ACTIVE membership 만 리더 우선 사용자 ID 순으로 조회한다")
    void visible_scope_팀_사용자_목록은_ACTIVE_membership만_리더_우선_사용자_ID순으로_조회한다() {
        TeamFixture fixture = seedVisibleScopeFixture();

        List<TeamUserSummaryProjection> result = teamRepository.findTeamUsers(fixture.callerId(), fixture.leaderTeamId()).orElseThrow();

        assertThat(result)
                .extracting(
                        TeamUserSummaryProjection::isLeader,
                        TeamUserSummaryProjection::userId,
                        TeamUserSummaryProjection::userName,
                        TeamUserSummaryProjection::positionName,
                        TeamUserSummaryProjection::teamRole
                )
                .containsExactly(
                        tuple(true, fixture.callerId(), "호출자", "사원", "리더"),
                        tuple(false, fixture.activeMemberId(), "활성멤버", "사원", "구성원")
                );
        assertThat(result)
                .extracting(TeamUserSummaryProjection::userId)
                .doesNotContain(fixture.leftMemberId());
    }

    private void clearDatabase() {
        worklogRepository.deleteAll();
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
        User deptHeadAdmin = userRepository.save(createUser(department.getId(), "사업부장관리자", UserRole.DEPT_HEAD));

        Team leaderTeam = teamRepository.save(createTeam("리더팀", TeamStatus.ACTIVE));
        Team primaryTeam = teamRepository.save(createTeam("주담당팀", TeamStatus.ACTIVE));
        Team adminOnlyTeam = teamRepository.save(createTeam("관리전용팀", TeamStatus.ACTIVE));
        Team inactiveDuplicateTeam = teamRepository.save(createTeam("중복권한비활성팀", TeamStatus.INACTIVE));
        Team leftOnlyTeam = teamRepository.save(createTeam("이탈소속팀", TeamStatus.ACTIVE));
        Team deletedTeam = teamRepository.save(createDeletedTeam("삭제팀"));

        userTeamRepository.save(UserTeam.create(caller.getId(), leaderTeam.getId(), true, "리더", "겸임", false, UserTeamStatus.ACTIVE));
        userTeamRepository.save(UserTeam.create(activeMember.getId(), leaderTeam.getId(), false, "구성원", "겸임", false, UserTeamStatus.ACTIVE));
        userTeamRepository.save(UserTeam.create(leftMember.getId(), leaderTeam.getId(), false, "이탈", "겸임", false, UserTeamStatus.LEFT));
        userTeamRepository.save(UserTeam.create(caller.getId(), primaryTeam.getId(), false, "주담당", "주담당", true, UserTeamStatus.ACTIVE));
        userTeamRepository.save(UserTeam.create(adminOnlyLeader.getId(), adminOnlyTeam.getId(), true, "관리전용", "겸임", false, UserTeamStatus.ACTIVE));
        userTeamRepository.save(UserTeam.create(caller.getId(), inactiveDuplicateTeam.getId(), true, "중복", "주담당", true, UserTeamStatus.ACTIVE));
        userTeamRepository.save(UserTeam.create(caller.getId(), leftOnlyTeam.getId(), false, "이탈", "주담당", true, UserTeamStatus.LEFT));

        teamAdminRepository.save(TeamAdmin.grant(caller.getId(), adminOnlyTeam.getId()));
        teamAdminRepository.save(TeamAdmin.grant(caller.getId(), inactiveDuplicateTeam.getId()));
        teamAdminRepository.save(TeamAdmin.grant(caller.getId(), deletedTeam.getId()));
        teamAdminRepository.save(TeamAdmin.grant(deptHeadAdmin.getId(), leaderTeam.getId()));

        return new TeamFixture(
                caller.getId(),
                activeMember.getId(),
                leftMember.getId(),
                adminOnlyLeader.getId(),
                deptHeadAdmin.getId(),
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

    private Worklog createWorklog(Long authorId, Long teamId, WorklogStatus statusCode, boolean isDeleted) {
        Worklog worklog = Worklog.create(
                authorId,
                teamId,
                "팀 상세 집계 검증",
                "요청 내용",
                "업무 내용",
                WorklogImportance.NORMAL,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 2)
        );
        ReflectionTestUtils.setField(worklog, "statusCode", statusCode);
        ReflectionTestUtils.setField(worklog, "isDeleted", isDeleted);
        return worklog;
    }

    private record TeamFixture(Long callerId,
                               Long activeMemberId,
                               Long leftMemberId,
                               Long adminOnlyLeaderId,
                               Long deptHeadAdminId,
                               Long leaderTeamId,
                               Long primaryTeamId,
                               Long adminOnlyTeamId,
                               Long inactiveDuplicateTeamId,
                               Long leftOnlyTeamId,
                               Long deletedTeamId) {
    }
}
