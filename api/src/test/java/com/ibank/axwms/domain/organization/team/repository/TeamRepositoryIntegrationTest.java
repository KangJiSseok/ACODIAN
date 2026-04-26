package com.ibank.axwms.domain.organization.team.repository;

import static com.ibank.axwms.global.jooq.Tables.TB_WORKLOG;
import static org.assertj.core.api.Assertions.assertThat;

import com.ibank.axwms.domain.organization.department.DepartmentStatus;
import com.ibank.axwms.domain.organization.department.entity.Department;
import com.ibank.axwms.domain.organization.department.repository.DepartmentRepository;
import com.ibank.axwms.domain.organization.team.TeamStatus;
import com.ibank.axwms.domain.organization.team.UserTeamStatus;
import com.ibank.axwms.domain.organization.team.entity.Team;
import com.ibank.axwms.domain.organization.team.entity.UserTeam;
import com.ibank.axwms.domain.organization.team.repository.jooq.projection.TeamDetailProjection;
import com.ibank.axwms.domain.organization.team.repository.jooq.projection.TeamListProjection;
import com.ibank.axwms.domain.organization.team.repository.jooq.projection.TeamSummaryProjection;
import com.ibank.axwms.domain.organization.team.repository.jooq.projection.TeamUserProjection;
import com.ibank.axwms.domain.organization.team.repository.jooq.projection.TeamWorklogProjection;
import com.ibank.axwms.domain.organization.team.repository.jooq.query.TeamPageQuery;
import com.ibank.axwms.domain.organization.team.repository.jooq.query.TeamUsersQuery;
import com.ibank.axwms.domain.organization.team.repository.jooq.query.TeamWorklogsQuery;
import com.ibank.axwms.domain.organization.user.EmploymentStatus;
import com.ibank.axwms.domain.organization.user.UserRole;
import com.ibank.axwms.domain.organization.user.entity.User;
import com.ibank.axwms.domain.organization.user.repository.UserRepository;
import com.ibank.axwms.domain.worklog.WorklogImportance;
import com.ibank.axwms.domain.worklog.WorklogStatus;
import com.ibank.axwms.global.enums.AiProcessingStatus;
import com.ibank.axwms.testsupport.IntegrationTestSupport;
import java.time.LocalDate;
import java.util.List;
import org.assertj.core.groups.Tuple;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

class TeamRepositoryIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserTeamRepository userTeamRepository;

    @Autowired
    private DSLContext dsl;

    private Long logisticsDepartmentId;
    private Long principalUserId;
    private Long activeLeadTeamId;

    @BeforeEach
    void setUp() {
        clearDatabase();
        seedTeams();
    }

    @Test
    @DisplayName("팀 목록 조회는 최신 spec 필드와 정렬 우선순위를 반영한다")
    void 팀_목록_조회는_최신_spec_필드와_정렬_우선순위를_반영한다() {
        Page<TeamListProjection> page = teamRepository.findTeamPage(new TeamPageQuery(1, 20, logisticsDepartmentId, null, principalUserId));

        assertThat(page.getContent())
                .extracting(
                        TeamListProjection::teamName,
                        TeamListProjection::teamLeaderName,
                        TeamListProjection::memberCount,
                        TeamListProjection::myTeamLeader,
                        TeamListProjection::teamRole,
                        TeamListProjection::allocation,
                        TeamListProjection::isPrimary
                )
                .containsExactly(
                        Tuple.tuple("물류혁신TF", "홍길동", 2, true, "플랫폼 총괄", "PRIMARY", true),
                        Tuple.tuple("운영지원TF", "김서포트", 2, false, "협업", "SECONDARY", false),
                        Tuple.tuple("휴면TF", "이휴면", 2, false, null, null, false)
                );
    }

    @Test
    @DisplayName("팀 요약 조회는 visible scope 기준 집계를 계산한다")
    void 팀_요약_조회는_visible_scope_기준_집계를_계산한다() {
        TeamSummaryProjection summary = teamRepository.findTeamSummary(new com.ibank.axwms.domain.organization.team.repository.jooq.query.TeamSummaryQuery(logisticsDepartmentId, null));

        assertThat(summary.activeTeamCount()).isEqualTo(2L);
        assertThat(summary.totalTeamCount()).isEqualTo(3L);
        assertThat(summary.activeUserCount()).isEqualTo(4L);
        assertThat(summary.activeTeamUserCount()).isEqualTo(3L);
        assertThat(summary.allTeamUserCount()).isEqualTo(5L);
    }

    @Test
    @DisplayName("팀 상세 조회는 팀장과 업무일지 집계를 함께 반환한다")
    void 팀_상세_조회는_팀장과_업무일지_집계를_함께_반환한다() {
        TeamDetailProjection detail = teamRepository.findTeamDetail(activeLeadTeamId).orElseThrow();

        assertThat(detail.teamName()).isEqualTo("물류혁신TF");
        assertThat(detail.teamLeaderName()).isEqualTo("홍길동");
        assertThat(detail.totalWorklogCount()).isEqualTo(5L);
        assertThat(detail.completedWorklogCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("팀 사용자 조회는 팀장 우선 정렬과 이메일 필드를 반환한다")
    void 팀_사용자_조회는_팀장_우선_정렬과_이메일_필드를_반환한다() {
        Page<TeamUserProjection> page = userTeamRepository.findTeamUserPage(activeLeadTeamId, new TeamUsersQuery(1, 20));

        assertThat(page.getContent())
                .extracting(
                        TeamUserProjection::teamLeader,
                        TeamUserProjection::userName,
                        TeamUserProjection::email,
                        TeamUserProjection::teamRole,
                        TeamUserProjection::allocation
                )
                .containsExactly(
                        Tuple.tuple(true, "홍길동", "leader@ibank.com", "플랫폼 총괄", "PRIMARY"),
                        Tuple.tuple(false, "김영희", "member@ibank.com", "WMS 운영", "SECONDARY")
                );
    }

    @Test
    @DisplayName("팀 업무일지 조회는 상태 우선순위와 최신 필드를 반영한다")
    void 팀_업무일지_조회는_상태_우선순위와_최신_필드를_반영한다() {
        Page<TeamWorklogProjection> page = teamRepository.findTeamWorklogPage(activeLeadTeamId, new TeamWorklogsQuery(1, 20));

        assertThat(page.getContent())
                .extracting(
                        TeamWorklogProjection::title,
                        TeamWorklogProjection::requestContent,
                        TeamWorklogProjection::aiSummary,
                        TeamWorklogProjection::statusCode,
                        TeamWorklogProjection::importanceCode
                )
                .containsExactly(
                        Tuple.tuple("진행중 업무", "진행중 요청", "진행중 요약", WorklogStatus.IN_PROGRESS.name(), WorklogImportance.HIGH.name()),
                        Tuple.tuple("대기 업무", "대기 요청", "대기 요약", WorklogStatus.PENDING.name(), WorklogImportance.NORMAL.name()),
                        Tuple.tuple("보류 업무", "보류 요청", "보류 요약", WorklogStatus.ON_HOLD.name(), WorklogImportance.LOW.name()),
                        Tuple.tuple("완료 업무", "완료 요청", "완료 요약", WorklogStatus.COMPLETED.name(), WorklogImportance.URGENT.name()),
                        Tuple.tuple("취소 업무", "취소 요청", "취소 요약", WorklogStatus.CANCELLED.name(), WorklogImportance.NORMAL.name())
                );
    }

    private void clearDatabase() {
        dsl.deleteFrom(TB_WORKLOG).execute();

        List<Department> departments = departmentRepository.findAll();
        departments.forEach(department -> department.assignHeadUserId(null));
        departmentRepository.saveAll(departments);
        departmentRepository.flush();

        userTeamRepository.deleteAll();
        teamRepository.deleteAll();
        userRepository.deleteAll();
        departmentRepository.deleteAll();
    }

    private void seedTeams() {
        Department logisticsDepartment = departmentRepository.save(createDepartment("물류본부", DepartmentStatus.ACTIVE));
        Department operationsDepartment = departmentRepository.save(createDepartment("운영본부", DepartmentStatus.ACTIVE));
        logisticsDepartmentId = logisticsDepartment.getId();

        User logisticsHead = userRepository.save(createUser(logisticsDepartmentId, "박본부", "dept-head@ibank.com", EmploymentStatus.ACTIVE, UserRole.DIRECTOR));
        logisticsDepartment.assignHeadUserId(logisticsHead.getId());
        departmentRepository.save(logisticsDepartment);

        User teamLeader = userRepository.save(createUser(logisticsDepartmentId, "홍길동", "leader@ibank.com", EmploymentStatus.ACTIVE, UserRole.TEAM_LEAD));
        User activeMember = userRepository.save(createUser(logisticsDepartmentId, "김영희", "member@ibank.com", EmploymentStatus.ACTIVE, UserRole.MEMBER));
        User inactiveTeamMember = userRepository.save(createUser(logisticsDepartmentId, "이휴면", "inactive-team@ibank.com", EmploymentStatus.ACTIVE, UserRole.MEMBER));
        User secondaryTeamLeader = userRepository.save(createUser(logisticsDepartmentId, "김서포트", "support@ibank.com", EmploymentStatus.ACTIVE, UserRole.MEMBER));
        User leaveUser = userRepository.save(createUser(logisticsDepartmentId, "정휴직", "leave@ibank.com", EmploymentStatus.LEAVE, UserRole.MEMBER));
        Long operationsDepartmentId = operationsDepartment.getId();
        User otherDeptUser = userRepository.save(createUser(operationsDepartmentId, "타부서", "other-dept@ibank.com", EmploymentStatus.ACTIVE, UserRole.MEMBER));
        principalUserId = teamLeader.getId();

        Team activeLeadTeam = teamRepository.save(createTeam(logisticsDepartmentId, "물류혁신TF", TeamStatus.ACTIVE));
        Team activeSecondaryTeam = teamRepository.save(createTeam(logisticsDepartmentId, "운영지원TF", TeamStatus.ACTIVE));
        Team inactiveTeam = teamRepository.save(createTeam(logisticsDepartmentId, "휴면TF", TeamStatus.INACTIVE));
        Team otherDepartmentTeam = teamRepository.save(createTeam(operationsDepartmentId, "타부서TF", TeamStatus.ACTIVE));
        activeLeadTeamId = activeLeadTeam.getId();
        Long activeSecondaryTeamId = activeSecondaryTeam.getId();
        Long inactiveTeamId = inactiveTeam.getId();

        userTeamRepository.save(UserTeam.create(teamLeader.getId(), activeLeadTeamId, true, "플랫폼 총괄", "PRIMARY", true, UserTeamStatus.ACTIVE));
        userTeamRepository.save(UserTeam.create(activeMember.getId(), activeLeadTeamId, false, "WMS 운영", "SECONDARY", false, UserTeamStatus.ACTIVE));
        userTeamRepository.save(UserTeam.create(teamLeader.getId(), activeSecondaryTeamId, false, "협업", "SECONDARY", false, UserTeamStatus.ACTIVE));
        userTeamRepository.save(UserTeam.create(secondaryTeamLeader.getId(), activeSecondaryTeamId, true, "운영 지원", "PRIMARY", true, UserTeamStatus.ACTIVE));
        userTeamRepository.save(UserTeam.create(inactiveTeamMember.getId(), inactiveTeamId, true, "휴면 담당", "PRIMARY", true, UserTeamStatus.ACTIVE));
        userTeamRepository.save(UserTeam.create(leaveUser.getId(), inactiveTeamId, false, "휴직 담당", "SECONDARY", false, UserTeamStatus.ACTIVE));
        userTeamRepository.save(UserTeam.create(otherDeptUser.getId(), otherDepartmentTeam.getId(), false, "타부서 담당", "PRIMARY", true, UserTeamStatus.ACTIVE));
        userTeamRepository.save(UserTeam.create(otherDeptUser.getId(), activeLeadTeamId, false, "과거 소속", "SECONDARY", false, UserTeamStatus.LEFT));

        insertWorklog(teamLeader.getId(), activeLeadTeamId, "완료 업무", "완료 요청", "완료 본문", WorklogStatus.COMPLETED, WorklogImportance.URGENT, "완료 요약", false);
        insertWorklog(teamLeader.getId(), activeLeadTeamId, "진행중 업무", "진행중 요청", "진행중 본문", WorklogStatus.IN_PROGRESS, WorklogImportance.HIGH, "진행중 요약", false);
        insertWorklog(teamLeader.getId(), activeLeadTeamId, "대기 업무", "대기 요청", "대기 본문", WorklogStatus.PENDING, WorklogImportance.NORMAL, "대기 요약", false);
        insertWorklog(teamLeader.getId(), activeLeadTeamId, "보류 업무", "보류 요청", "보류 본문", WorklogStatus.ON_HOLD, WorklogImportance.LOW, "보류 요약", false);
        insertWorklog(teamLeader.getId(), activeLeadTeamId, "취소 업무", "취소 요청", "취소 본문", WorklogStatus.CANCELLED, WorklogImportance.NORMAL, "취소 요약", false);
        insertWorklog(teamLeader.getId(), activeLeadTeamId, "삭제 업무", "삭제 요청", "삭제 본문", WorklogStatus.COMPLETED, WorklogImportance.NORMAL, "삭제 요약", true);
        insertWorklog(teamLeader.getId(), activeSecondaryTeamId, "다른 팀 업무", "다른 팀 요청", "다른 팀 본문", WorklogStatus.IN_PROGRESS, WorklogImportance.NORMAL, "다른 팀 요약", false);
    }

    private void insertWorklog(Long authorId,
                               Long teamId,
                               String title,
                               String requestContent,
                               String workContent,
                               WorklogStatus status,
                               WorklogImportance importance,
                               String aiSummary,
                               boolean isDeleted) {
        dsl.insertInto(TB_WORKLOG)
                .set(TB_WORKLOG.AUTHOR_ID, authorId)
                .set(TB_WORKLOG.TEAM_ID, teamId)
                .set(TB_WORKLOG.TITLE, title)
                .set(TB_WORKLOG.REQUEST_CONTENT, requestContent)
                .set(TB_WORKLOG.WORK_CONTENT, workContent)
                .set(TB_WORKLOG.STATUS_CODE, status.name())
                .set(TB_WORKLOG.IMPORTANCE_CODE, importance.name())
                .set(TB_WORKLOG.AI_SUMMARY, aiSummary)
                .set(TB_WORKLOG.AI_SUMMARY_EDITED, false)
                .set(TB_WORKLOG.AI_PROCESSING_STATUS, AiProcessingStatus.COMPLETED.name())
                .set(TB_WORKLOG.IS_DELETED, isDeleted)
                .execute();
    }

    private Department createDepartment(String departmentName, DepartmentStatus status) {
        Department department = Department.create(departmentName, departmentName + " 설명");
        department.changeStatus(status);
        return department;
    }

    private Team createTeam(Long departmentId, String teamName, TeamStatus status) {
        return Team.create(
                departmentId,
                teamName,
                status,
                teamName + " 설명",
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 12, 31)
        );
    }

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
                LocalDate.of(2025, 1, 1)
        );
    }
}
