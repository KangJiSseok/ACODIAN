package com.ibank.axwms.domain.organization.team.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import com.ibank.axwms.domain.organization.team.TeamStatus;
import com.ibank.axwms.domain.organization.team.UserTeamStatus;
import com.ibank.axwms.domain.organization.team.dto.BulkUpsertTeamUsersApiDto;
import com.ibank.axwms.domain.organization.team.dto.CreateTeamApiDto;
import com.ibank.axwms.domain.organization.team.dto.GetTeamDetailApiDto;
import com.ibank.axwms.domain.organization.team.dto.GetTeamUsersApiDto;
import com.ibank.axwms.domain.organization.team.dto.GetTeamWorklogsApiDto;
import com.ibank.axwms.domain.organization.team.dto.GetTeamsApiDto;
import com.ibank.axwms.domain.organization.team.dto.GetTeamsSummaryApiDto;
import com.ibank.axwms.domain.organization.team.dto.UpdateTeamApiDto;
import com.ibank.axwms.domain.organization.team.entity.Team;
import com.ibank.axwms.domain.organization.team.entity.UserTeam;
import com.ibank.axwms.domain.organization.team.repository.TeamRepository;
import com.ibank.axwms.domain.organization.team.repository.UserTeamRepository;
import com.ibank.axwms.domain.organization.team.repository.jooq.projection.TeamDetailProjection;
import com.ibank.axwms.domain.organization.team.repository.jooq.projection.TeamListProjection;
import com.ibank.axwms.domain.organization.team.repository.jooq.projection.TeamSummaryProjection;
import com.ibank.axwms.domain.organization.team.repository.jooq.projection.TeamUserProjection;
import com.ibank.axwms.domain.organization.team.repository.jooq.projection.TeamWorklogProjection;
import com.ibank.axwms.domain.organization.team.repository.jooq.query.TeamPageQuery;
import com.ibank.axwms.domain.organization.team.repository.jooq.query.TeamSummaryQuery;
import com.ibank.axwms.domain.organization.team.repository.jooq.query.TeamUsersQuery;
import com.ibank.axwms.domain.organization.team.repository.jooq.query.TeamWorklogsQuery;
import com.ibank.axwms.domain.organization.user.EmploymentStatus;
import com.ibank.axwms.domain.organization.user.UserRole;
import com.ibank.axwms.domain.organization.user.entity.User;
import com.ibank.axwms.domain.organization.user.repository.UserRepository;
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import com.ibank.axwms.global.response.EmptyResponse;
import com.ibank.axwms.global.response.PageResponse;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private UserTeamRepository userTeamRepository;

    @Spy
    private TeamAccessPolicy teamAccessPolicy = new TeamAccessPolicy();

    @InjectMocks
    private TeamService teamService;

    @Test
    @DisplayName("DEPT_HEAD 팀 목록 조회는 최신 spec query 를 repository query 로 정규화한다")
    void DEPT_HEAD_팀_목록_조회는_최신_spec_query_를_repository_query_로_정규화한다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(101L, "head@ibank.com", "DEPT_HEAD");
        GetTeamsApiDto.Request request = new GetTeamsApiDto.Request(1, 20, null);
        TeamPageQuery query = new TeamPageQuery(1, 20, null, 10L, 101L, UserRole.DEPT_HEAD);
        given(userRepository.findById(101L)).willReturn(Optional.of(createUser(101L, 10L, UserRole.DEPT_HEAD)));
        given(teamRepository.findTeamPage(query)).willReturn(new PageImpl<>(
                List.of(new TeamListProjection(
                        21L,
                        "물류혁신TF",
                        TeamStatus.ACTIVE,
                        10L,
                        "물류본부",
                        "창고 자동화 개선 전담",
                        1001L,
                        "박본부",
                        201L,
                        "홍길동",
                        8,
                        true,
                        "플랫폼 총괄",
                        "PRIMARY",
                        true,
                        LocalDate.of(2026, 4, 1),
                        LocalDate.of(2026, 12, 31)
                )),
                PageRequest.of(0, 20),
                1
        ));

        PageResponse<GetTeamsApiDto.Response.Item> response = teamService.getTeams(principal, request);

        assertThat(response.items()).singleElement().satisfies(item -> {
            assertThat(item.teamId()).isEqualTo(21L);
            assertThat(item.departmentHeadUserName()).isEqualTo("박본부");
            assertThat(item.teamLeaderName()).isEqualTo("홍길동");
            assertThat(item.myTeamLeader()).isTrue();
            assertThat(item.teamRole()).isEqualTo("플랫폼 총괄");
        });
    }

    @Test
    @DisplayName("TEAM_LEAD 팀 목록 조회는 departmentId 필터와 principal 문맥을 repository query 로 정규화한다")
    void TEAM_LEAD_팀_목록_조회는_departmentId_필터와_principal_문맥을_repository_query_로_정규화한다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(101L, "lead@ibank.com", "TEAM_LEAD");
        GetTeamsApiDto.Request request = new GetTeamsApiDto.Request(2, 10, 20L);
        TeamPageQuery query = new TeamPageQuery(2, 10, 20L, null, 101L, UserRole.TEAM_LEAD);
        given(teamRepository.findTeamPage(query)).willReturn(new PageImpl<>(
                List.of(new TeamListProjection(
                        22L,
                        "타부서TF",
                        TeamStatus.ACTIVE,
                        20L,
                        "운영본부",
                        "외부 협업",
                        1002L,
                        "윤본부",
                        202L,
                        "타부서",
                        3,
                        false,
                        "협업",
                        "SECONDARY",
                        false,
                        LocalDate.of(2026, 4, 1),
                        null
                )),
                PageRequest.of(1, 10),
                1
        ));

        PageResponse<GetTeamsApiDto.Response.Item> response = teamService.getTeams(principal, request);

        assertThat(response.items()).singleElement().satisfies(item -> {
            assertThat(item.teamId()).isEqualTo(22L);
            assertThat(item.departmentId()).isEqualTo(20L);
            assertThat(item.teamLeaderName()).isEqualTo("타부서");
            assertThat(item.myTeamLeader()).isFalse();
            assertThat(item.teamRole()).isEqualTo("협업");
        });
    }

    @Test
    @DisplayName("DEPT_HEAD 팀 요약 조회는 본인 부서를 기본 departmentId 로 사용한다")
    void DEPT_HEAD_팀_요약_조회는_본인_부서를_기본_departmentId_로_사용한다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(101L, "head@ibank.com", "DEPT_HEAD");
        TeamSummaryQuery query = new TeamSummaryQuery(10L, null);
        given(userRepository.findById(101L)).willReturn(Optional.of(createUser(101L, 10L, UserRole.DEPT_HEAD)));
        given(teamRepository.findTeamSummary(query)).willReturn(new TeamSummaryProjection(3L, 5L, 12L, 14L, 16L));

        GetTeamsSummaryApiDto.Response response = teamService.getTeamSummary(principal, new GetTeamsSummaryApiDto.Request(null));

        assertThat(response.activeTeamCount()).isEqualTo(3L);
        assertThat(response.allTeamUserCount()).isEqualTo(16L);
    }

    @Test
    @DisplayName("DEPT_HEAD 는 자기 부서로 팀을 생성할 수 있고 EmptyResponse 를 반환한다")
    void DEPT_HEAD_는_자기_부서로_팀을_생성할_수_있고_EmptyResponse_를_반환한다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(101L, "head@ibank.com", "DEPT_HEAD");
        given(userRepository.findById(101L)).willReturn(Optional.of(createUser(101L, 10L, UserRole.DEPT_HEAD)));

        EmptyResponse response = teamService.createTeam(principal, new CreateTeamApiDto.Request(
                10L,
                "물류혁신TF",
                "설명",
                201L,
                TeamStatus.ACTIVE,
                LocalDate.of(2025, 1, 1),
                null
        ));

        assertThat(response).isSameAs(EmptyResponse.INSTANCE);
    }

    @Test
    @DisplayName("DEPT_HEAD 는 다른 부서로 팀을 생성할 수 없다")
    void DEPT_HEAD_는_다른_부서로_팀을_생성할_수_없다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(101L, "head@ibank.com", "DEPT_HEAD");
        given(userRepository.findById(101L)).willReturn(Optional.of(createUser(101L, 10L, UserRole.DEPT_HEAD)));

        assertThatThrownBy(() -> teamService.createTeam(principal, new CreateTeamApiDto.Request(
                20L,
                "물류혁신TF",
                "설명",
                201L,
                TeamStatus.ACTIVE,
                LocalDate.of(2025, 1, 1),
                null
        )))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_ACCESS_DENIED);
    }

    @Test
    @DisplayName("DEPT_HEAD 는 다른 부서 팀을 수정할 수 없다")
    void DEPT_HEAD_는_다른_부서_팀을_수정할_수_없다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(101L, "head@ibank.com", "DEPT_HEAD");
        given(userRepository.findById(101L)).willReturn(Optional.of(createUser(101L, 10L, UserRole.DEPT_HEAD)));
        given(teamRepository.findById(31L)).willReturn(Optional.of(createTeam(31L, 20L, null)));

        assertThatThrownBy(() -> teamService.updateTeam(principal, 31L, new UpdateTeamApiDto.Request(
                20L,
                "타부서팀",
                "설명",
                201L,
                TeamStatus.ACTIVE,
                LocalDate.of(2025, 1, 1),
                null
        )))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_ACCESS_DENIED);
    }

    @Test
    @DisplayName("TEAM_LEAD 는 자기 팀 상세 projection 을 최신 API 응답으로 변환한다")
    void TEAM_LEAD_는_자기_팀_상세_projection_을_최신_API_응답으로_변환한다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(101L, "lead@ibank.com", "TEAM_LEAD");
        given(userRepository.findById(101L)).willReturn(Optional.of(createUser(101L, 10L, UserRole.TEAM_LEAD)));
        given(teamRepository.findById(21L)).willReturn(Optional.of(createTeam(21L, 10L, null)));
        given(userTeamRepository.findFirstByUserIdAndStatusCodeOrderByIsPrimaryDesc(101L, UserTeamStatus.ACTIVE))
                .willReturn(Optional.of(UserTeam.create(101L, 21L, true, "플랫폼 총괄", "주담당", true, UserTeamStatus.ACTIVE)));
        given(teamRepository.findTeamDetail(21L)).willReturn(Optional.of(new TeamDetailProjection(
                21L,
                "물류혁신TF",
                TeamStatus.ACTIVE,
                "설명",
                10L,
                "물류본부",
                201L,
                "홍길동",
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 12, 31),
                28L,
                11L
        )));

        GetTeamDetailApiDto.Response response = teamService.getTeamDetail(principal, 21L);

        assertThat(response.teamId()).isEqualTo(21L);
        assertThat(response.teamLeaderName()).isEqualTo("홍길동");
        assertThat(response.totalWorklogCount()).isEqualTo(28L);
        assertThat(response.completedWorklogCount()).isEqualTo(11L);
    }

    @Test
    @DisplayName("TEAM_LEAD 는 다른 팀 상세를 조회할 수 없다")
    void TEAM_LEAD_는_다른_팀_상세를_조회할_수_없다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(101L, "lead@ibank.com", "TEAM_LEAD");
        given(userRepository.findById(101L)).willReturn(Optional.of(createUser(101L, 10L, UserRole.TEAM_LEAD)));
        given(teamRepository.findById(22L)).willReturn(Optional.of(createTeam(22L, 10L, null)));
        given(userTeamRepository.findFirstByUserIdAndStatusCodeOrderByIsPrimaryDesc(101L, UserTeamStatus.ACTIVE))
                .willReturn(Optional.of(UserTeam.create(101L, 21L, true, "플랫폼 총괄", "주담당", true, UserTeamStatus.ACTIVE)));

        assertThatThrownBy(() -> teamService.getTeamDetail(principal, 22L))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_ACCESS_DENIED);
    }

    @Test
    @DisplayName("TEAM_LEAD 는 자기 팀 사용자 projection 을 최신 API 응답으로 변환한다")
    void TEAM_LEAD_는_자기_팀_사용자_projection_을_최신_API_응답으로_변환한다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(101L, "lead@ibank.com", "TEAM_LEAD");
        TeamUsersQuery query = new TeamUsersQuery(1, 20);
        given(userRepository.findById(101L)).willReturn(Optional.of(createUser(101L, 10L, UserRole.TEAM_LEAD)));
        given(teamRepository.findById(21L)).willReturn(Optional.of(createTeam(21L, 10L, null)));
        given(userTeamRepository.findFirstByUserIdAndStatusCodeOrderByIsPrimaryDesc(101L, UserTeamStatus.ACTIVE))
                .willReturn(Optional.of(UserTeam.create(101L, 21L, true, "플랫폼 총괄", "주담당", true, UserTeamStatus.ACTIVE)));
        given(userTeamRepository.findTeamUserPage(21L, query)).willReturn(new PageImpl<>(
                List.of(new TeamUserProjection(true, 101L, "홍길동", "과장", "hong@axwms.com", "플랫폼 총괄", "PRIMARY")),
                PageRequest.of(0, 20),
                1
        ));

        PageResponse<GetTeamUsersApiDto.Response.Item> response = teamService.getTeamUsers(principal, 21L, new GetTeamUsersApiDto.Request(1, 20));

        assertThat(response.items()).singleElement().satisfies(item -> {
            assertThat(item.userName()).isEqualTo("홍길동");
            assertThat(item.email()).isEqualTo("hong@axwms.com");
            assertThat(item.teamRole()).isEqualTo("플랫폼 총괄");
        });
    }

    @Test
    @DisplayName("TEAM_LEAD 는 자기 팀 업무일지 projection 을 최신 API 응답으로 변환한다")
    void TEAM_LEAD_는_자기_팀_업무일지_projection_을_최신_API_응답으로_변환한다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(101L, "lead@ibank.com", "TEAM_LEAD");
        TeamWorklogsQuery query = new TeamWorklogsQuery(1, 20);
        given(userRepository.findById(101L)).willReturn(Optional.of(createUser(101L, 10L, UserRole.TEAM_LEAD)));
        given(teamRepository.findById(21L)).willReturn(Optional.of(createTeam(21L, 10L, null)));
        given(userTeamRepository.findFirstByUserIdAndStatusCodeOrderByIsPrimaryDesc(101L, UserTeamStatus.ACTIVE))
                .willReturn(Optional.of(UserTeam.create(101L, 21L, true, "플랫폼 총괄", "주담당", true, UserTeamStatus.ACTIVE)));
        given(teamRepository.findTeamWorklogPage(21L, query)).willReturn(new PageImpl<>(
                List.of(new TeamWorklogProjection(301L, "재고 점검", "요청 본문", "작업 본문", "AI 요약", "IN_PROGRESS", "HIGH")),
                PageRequest.of(0, 20),
                1
        ));

        PageResponse<GetTeamWorklogsApiDto.Response.Item> response = teamService.getTeamWorklogs(principal, 21L, new GetTeamWorklogsApiDto.Request(1, 20));

        assertThat(response.items()).singleElement().satisfies(item -> {
            assertThat(item.worklogId()).isEqualTo(301L);
            assertThat(item.requestContent()).isEqualTo("요청 본문");
            assertThat(item.aiSummary()).isEqualTo("AI 요약");
            assertThat(item.importanceCode()).isEqualTo("HIGH");
        });
    }

    @Test
    @DisplayName("팀 사용자 일괄 반영은 EmptyResponse 를 반환한다")
    void 팀_사용자_일괄_반영은_EmptyResponse_를_반환한다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(101L, "head@ibank.com", "DEPT_HEAD");
        given(userRepository.findById(101L)).willReturn(Optional.of(createUser(101L, 10L, UserRole.DEPT_HEAD)));
        given(teamRepository.findById(21L)).willReturn(Optional.of(createTeam(21L, 10L, null)));

        EmptyResponse response = teamService.bulkUpsertTeamUsers(principal, 21L, new BulkUpsertTeamUsersApiDto.Request(
                List.of(new BulkUpsertTeamUsersApiDto.Request.AddUser(102L, false, "WMS 운영", "SECONDARY", false, LocalDate.of(2026, 4, 10))),
                List.of(104L)
        ));

        assertThat(response).isSameAs(EmptyResponse.INSTANCE);
    }

    @Test
    @DisplayName("DEPT_HEAD 는 자기 부서 팀을 soft-delete 하고 membership 은 건드리지 않는다")
    void DEPT_HEAD_는_자기_부서_팀을_soft_delete_하고_membership_은_건드리지_않는다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(101L, "head@ibank.com", "DEPT_HEAD");
        Team team = createTeam(21L, 10L, null);
        given(userRepository.findById(101L)).willReturn(Optional.of(createUser(101L, 10L, UserRole.DEPT_HEAD)));
        given(teamRepository.findById(21L)).willReturn(Optional.of(team));

        teamService.deleteTeam(principal, 21L);

        assertThat(team.getDeletedAt()).isNotNull();
        verifyNoInteractions(userTeamRepository);
    }

    @Test
    @DisplayName("팀 삭제는 이미 soft-delete 된 팀을 TEAM_ALREADY_DELETED 로 거절한다")
    void 팀_삭제는_이미_soft_delete_된_팀을_TEAM_ALREADY_DELETED_로_거절한다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(101L, "head@ibank.com", "DEPT_HEAD");
        given(userRepository.findById(101L)).willReturn(Optional.of(createUser(101L, 10L, UserRole.DEPT_HEAD)));
        given(teamRepository.findById(21L)).willReturn(Optional.of(createTeam(21L, 10L, LocalDateTime.of(2026, 4, 25, 0, 0))));

        assertThatThrownBy(() -> teamService.deleteTeam(principal, 21L))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.TEAM_ALREADY_DELETED);
    }

    @Test
    @DisplayName("팀 삭제는 존재하지 않는 팀을 TEAM_NOT_FOUND 로 거절한다")
    void 팀_삭제는_존재하지_않는_팀을_TEAM_NOT_FOUND_로_거절한다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(101L, "head@ibank.com", "DEPT_HEAD");
        given(userRepository.findById(101L)).willReturn(Optional.of(createUser(101L, 10L, UserRole.DEPT_HEAD)));
        given(teamRepository.findById(21L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> teamService.deleteTeam(principal, 21L))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.TEAM_NOT_FOUND);
    }

    @Test
    @DisplayName("soft-delete 된 팀 상세는 최신 계약에서도 성공 응답을 주지 않는다")
    void soft_delete_된_팀_상세는_최신_계약에서도_성공_응답을_주지_않는다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(101L, "head@ibank.com", "DEPT_HEAD");
        given(userRepository.findById(101L)).willReturn(Optional.of(createUser(101L, 10L, UserRole.DEPT_HEAD)));
        given(teamRepository.findById(21L)).willReturn(Optional.of(createTeam(21L, 10L, LocalDateTime.of(2026, 4, 25, 0, 0))));

        assertThatThrownBy(() -> teamService.getTeamDetail(principal, 21L))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_ACCESS_DENIED);
    }

    private User createUser(Long id, Long departmentId, UserRole role) {
        User user = User.create(
                departmentId,
                "사용자",
                "user@ibank.com",
                "$2a$10$fake-hashed",
                role,
                EmploymentStatus.ACTIVE,
                "과장",
                "팀장",
                LocalDate.of(2025, 1, 1)
        );
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Team createTeam(Long id, Long departmentId, LocalDateTime deletedAt) {
        Team team = Team.create(
                departmentId,
                "물류혁신TF",
                TeamStatus.ACTIVE,
                "테스트 팀",
                LocalDate.of(2025, 1, 1),
                null
        );
        ReflectionTestUtils.setField(team, "id", id);
        ReflectionTestUtils.setField(team, "deletedAt", deletedAt);
        return team;
    }
}
