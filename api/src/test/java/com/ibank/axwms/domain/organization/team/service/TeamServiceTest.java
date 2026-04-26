package com.ibank.axwms.domain.organization.team.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.ibank.axwms.domain.organization.team.TeamStatus;
import com.ibank.axwms.domain.organization.team.UserTeamStatus;
import com.ibank.axwms.domain.organization.team.dto.CreateTeamApiDto;
import com.ibank.axwms.domain.organization.team.dto.GetTeamDetailApiDto;
import com.ibank.axwms.domain.organization.team.dto.GetTeamUsersApiDto;
import com.ibank.axwms.domain.organization.team.dto.GetTeamWorklogsApiDto;
import com.ibank.axwms.domain.organization.team.dto.GetTeamsApiDto;
import com.ibank.axwms.domain.organization.team.entity.Team;
import com.ibank.axwms.domain.organization.team.entity.UserTeam;
import com.ibank.axwms.domain.organization.team.repository.TeamRepository;
import com.ibank.axwms.domain.organization.team.repository.UserTeamRepository;
import com.ibank.axwms.domain.organization.team.repository.jooq.TeamDetailProjection;
import com.ibank.axwms.domain.organization.team.repository.jooq.TeamListProjection;
import com.ibank.axwms.domain.organization.team.repository.jooq.TeamUserProjection;
import com.ibank.axwms.domain.organization.team.repository.jooq.TeamWorklogProjection;
import com.ibank.axwms.domain.organization.user.EmploymentStatus;
import com.ibank.axwms.domain.organization.user.UserRole;
import com.ibank.axwms.domain.organization.user.entity.User;
import com.ibank.axwms.domain.organization.user.repository.UserRepository;
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import com.ibank.axwms.global.response.PageResponse;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    @DisplayName("GET /teams 는 repository projection 을 API 응답으로 변환한다")
    void getTeams_는_repository_projection_을_API_응답으로_변환한다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(101L, "head@ibank.com", "DEPT_HEAD");
        GetTeamsApiDto.Request request = new GetTeamsApiDto.Request(1, 20, "teamName", "ASC", 10L, TeamStatus.ACTIVE, "혁신");
        given(userRepository.findById(101L)).willReturn(Optional.of(createUser(101L, 10L, UserRole.DEPT_HEAD)));
        given(teamRepository.findTeamPage(request)).willReturn(new PageImpl<>(
                java.util.List.of(new TeamListProjection(21L, "물류혁신TF", 10L, "물류본부", TeamStatus.ACTIVE, null, 201L, "홍길동", 8)),
                PageRequest.of(0, 20),
                1
        ));

        PageResponse<GetTeamsApiDto.Response.Item> response = teamService.getTeams(principal, request);

        assertThat(response.items()).singleElement().satisfies(item -> {
            assertThat(item.teamId()).isEqualTo(21L);
            assertThat(item.teamName()).isEqualTo("물류혁신TF");
            assertThat(item.departmentName()).isEqualTo("물류본부");
            assertThat(item.leaderUserName()).isEqualTo("홍길동");
            assertThat(item.memberCount()).isEqualTo(8);
        });
    }

    @Test
    @DisplayName("DEPT_HEAD 는 자기 부서로 팀을 생성할 수 있다")
    void DEPT_HEAD_는_자기_부서로_팀을_생성할_수_있다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(101L, "head@ibank.com", "DEPT_HEAD");
        given(userRepository.findById(101L)).willReturn(Optional.of(createUser(101L, 10L, UserRole.DEPT_HEAD)));

        CreateTeamApiDto.Response response = teamService.createTeam(principal, new CreateTeamApiDto.Request(
                10L,
                "물류혁신TF",
                "설명",
                LocalDate.of(2025, 1, 1),
                null,
                201L,
                "팀장",
                "주담당",
                true
        ));

        assertThat(response.teamId()).isNull();
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
                LocalDate.of(2025, 1, 1),
                null,
                201L,
                "팀장",
                "주담당",
                true
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

        assertThatThrownBy(() -> teamService.updateTeam(principal, 31L, new com.ibank.axwms.domain.organization.team.dto.UpdateTeamApiDto.Request(
                "타부서팀",
                "설명",
                LocalDate.of(2025, 1, 1),
                null,
                201L,
                "팀장",
                "주담당",
                true
        )))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_ACCESS_DENIED);
    }

    @Test
    @DisplayName("TEAM_LEAD 는 자기 팀 상세 projection 을 API 응답으로 변환한다")
    void TEAM_LEAD_는_자기_팀_상세_projection_을_API_응답으로_변환한다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(101L, "lead@ibank.com", "TEAM_LEAD");
        given(userRepository.findById(101L)).willReturn(Optional.of(createUser(101L, 10L, UserRole.TEAM_LEAD)));
        given(teamRepository.findById(21L)).willReturn(Optional.of(createTeam(21L, 10L, null)));
        given(userTeamRepository.findFirstByUserIdAndStatusCodeOrderByIsPrimaryDesc(101L, UserTeamStatus.ACTIVE))
                .willReturn(Optional.of(UserTeam.create(101L, 21L, true, "플랫폼 총괄", "주담당", true, UserTeamStatus.ACTIVE)));
        given(teamRepository.findTeamDetail(21L)).willReturn(Optional.of(new TeamDetailProjection(
                21L,
                "물류혁신TF",
                10L,
                "물류본부",
                TeamStatus.ACTIVE,
                "설명",
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 12, 31),
                null,
                201L,
                "홍길동",
                "과장",
                "팀장",
                "플랫폼 총괄",
                "주담당",
                true
        )));

        GetTeamDetailApiDto.Response response = teamService.getTeamDetail(principal, 21L);

        assertThat(response.teamId()).isEqualTo(21L);
        assertThat(response.teamName()).isEqualTo("물류혁신TF");
        assertThat(response.leader()).isNotNull();
        assertThat(response.leader().userName()).isEqualTo("홍길동");
        assertThat(response.leader().teamRole()).isEqualTo("플랫폼 총괄");
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
    @DisplayName("TEAM_LEAD 는 자기 팀 사용자 projection 을 API 응답으로 변환한다")
    void TEAM_LEAD_는_자기_팀_사용자_projection_을_API_응답으로_변환한다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(101L, "lead@ibank.com", "TEAM_LEAD");
        GetTeamUsersApiDto.Request request = new GetTeamUsersApiDto.Request(1, 20, null, null, null, UserTeamStatus.ACTIVE);
        given(userRepository.findById(101L)).willReturn(Optional.of(createUser(101L, 10L, UserRole.TEAM_LEAD)));
        given(teamRepository.findById(21L)).willReturn(Optional.of(createTeam(21L, 10L, null)));
        given(userTeamRepository.findFirstByUserIdAndStatusCodeOrderByIsPrimaryDesc(101L, UserTeamStatus.ACTIVE))
                .willReturn(Optional.of(UserTeam.create(101L, 21L, true, "플랫폼 총괄", "주담당", true, UserTeamStatus.ACTIVE)));
        given(userTeamRepository.findTeamUserPage(21L, request)).willReturn(new PageImpl<>(
                java.util.List.of(new TeamUserProjection(101L, "홍길동", "과장", "팀장", true, "플랫폼 총괄", "주담당", true, UserTeamStatus.ACTIVE)),
                PageRequest.of(0, 20),
                1
        ));

        PageResponse<GetTeamUsersApiDto.Response.Item> response = teamService.getTeamUsers(principal, 21L, request);

        assertThat(response.items()).singleElement().satisfies(item -> {
            assertThat(item.userName()).isEqualTo("홍길동");
            assertThat(item.teamRole()).isEqualTo("플랫폼 총괄");
            assertThat(item.statusCode()).isEqualTo(UserTeamStatus.ACTIVE);
        });
    }

    @Test
    @DisplayName("TEAM_LEAD 는 자기 팀 업무일지 projection 을 API 응답으로 변환한다")
    void TEAM_LEAD_는_자기_팀_업무일지_projection_을_API_응답으로_변환한다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(101L, "lead@ibank.com", "TEAM_LEAD");
        GetTeamWorklogsApiDto.Request request = new GetTeamWorklogsApiDto.Request(1, 20, null, null, null, null);
        LocalDateTime now = LocalDateTime.of(2026, 4, 26, 1, 0);
        given(userRepository.findById(101L)).willReturn(Optional.of(createUser(101L, 10L, UserRole.TEAM_LEAD)));
        given(teamRepository.findById(21L)).willReturn(Optional.of(createTeam(21L, 10L, null)));
        given(userTeamRepository.findFirstByUserIdAndStatusCodeOrderByIsPrimaryDesc(101L, UserTeamStatus.ACTIVE))
                .willReturn(Optional.of(UserTeam.create(101L, 21L, true, "플랫폼 총괄", "주담당", true, UserTeamStatus.ACTIVE)));
        given(teamRepository.findTeamWorklogPage(21L, request)).willReturn(new PageImpl<>(
                java.util.List.of(new TeamWorklogProjection(301L, "재고 점검", "IN_PROGRESS", 101L, "홍길동", now.minusHours(1), now)),
                PageRequest.of(0, 20),
                1
        ));

        PageResponse<GetTeamWorklogsApiDto.Response.Item> response = teamService.getTeamWorklogs(principal, 21L, request);

        assertThat(response.items()).singleElement().satisfies(item -> {
            assertThat(item.worklogId()).isEqualTo(301L);
            assertThat(item.title()).isEqualTo("재고 점검");
            assertThat(item.authorUserName()).isEqualTo("홍길동");
        });
    }

    @Test
    @DisplayName("soft-delete 된 팀 상세는 skeleton 단계에서도 성공 응답을 주지 않는다")
    void soft_delete_된_팀_상세는_성공_응답을_주지_않는다() {
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
