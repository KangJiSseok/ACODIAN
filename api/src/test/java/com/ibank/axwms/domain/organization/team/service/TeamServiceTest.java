package com.ibank.axwms.domain.organization.team.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;

import com.ibank.axwms.domain.organization.team.TeamStatus;
import com.ibank.axwms.domain.organization.team.UserTeamStatus;
import com.ibank.axwms.domain.organization.team.dto.GetTeamApiDto;
import com.ibank.axwms.domain.organization.team.dto.GetTeamSummaryApiDto;
import com.ibank.axwms.domain.organization.team.dto.GetTeamsApiDto;
import com.ibank.axwms.domain.organization.team.entity.Team;
import com.ibank.axwms.domain.organization.team.repository.jooq.projection.TeamDetailProjection;
import com.ibank.axwms.domain.organization.team.repository.TeamRepository;
import com.ibank.axwms.domain.organization.team.repository.UserTeamRepository;
import com.ibank.axwms.domain.organization.team.repository.jooq.projection.TeamStatusSummaryProjection;
import com.ibank.axwms.domain.organization.team.repository.jooq.projection.TeamSummaryProjection;
import com.ibank.axwms.domain.organization.team.repository.jooq.query.TeamPageQuery;
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import com.ibank.axwms.global.response.PageResponse;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private UserTeamRepository userTeamRepository;

    @InjectMocks
    private TeamService teamService;

    @Test
    @DisplayName("getTeams 는 principal userId 로 repository 조회 결과를 PageResponse 로 변환한다")
    void getTeams_는_principal_userId로_repository_조회_결과를_PageResponse로_변환한다() {
        CustomUserPrincipal principal = principal();
        GetTeamsApiDto.Request request = new GetTeamsApiDto.Request(2, 10);
        given(teamRepository.findTeamPage(101L, TeamPageQuery.from(request))).willReturn(new PageImpl<>(
                List.of(projection()),
                PageRequest.of(1, 10),
                11
        ));

        PageResponse<GetTeamsApiDto.Response> response = teamService.getTeams(principal, request);

        assertThat(response.page()).isEqualTo(2);
        assertThat(response.pageSize()).isEqualTo(10);
        assertThat(response.totalCount()).isEqualTo(11);
        assertThat(response.items()).singleElement()
                .extracting(
                        GetTeamsApiDto.Response::teamId,
                        GetTeamsApiDto.Response::teamName,
                        GetTeamsApiDto.Response::memberCount,
                        GetTeamsApiDto.Response::myIsLeader,
                        GetTeamsApiDto.Response::allocation
                )
                .containsExactly(21L, "물류혁신TF", 2L, true, "주담당");
        then(teamRepository).should().findTeamPage(101L, TeamPageQuery.from(request));
    }

    @Test
    @DisplayName("getTeams 는 null 요청에도 기본 페이지 값을 가진 요청으로 repository 에 위임한다")
    void getTeams_는_null_요청에도_기본_페이지_값을_가진_요청으로_repository에_위임한다() {
        CustomUserPrincipal principal = principal();
        given(teamRepository.findTeamPage(eq(101L), any()))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        teamService.getTeams(principal, null);

        ArgumentCaptor<TeamPageQuery> requestCaptor = ArgumentCaptor.forClass(TeamPageQuery.class);
        then(teamRepository).should().findTeamPage(eq(101L), requestCaptor.capture());
        assertThat(requestCaptor.getValue().page()).isEqualTo(1);
        assertThat(requestCaptor.getValue().pageSize()).isEqualTo(20);
        assertThat(requestCaptor.getValue().pageIndex()).isZero();
    }

    @Test
    @DisplayName("getTeamSummary 는 principal userId 로 repository 조회 결과를 요약 응답으로 변환한다")
    void getTeamSummary_는_principal_userId로_repository_조회_결과를_요약_응답으로_변환한다() {
        CustomUserPrincipal principal = principal();
        given(teamRepository.countTeamSummary(101L)).willReturn(new TeamStatusSummaryProjection(3L, 1L, 4L));

        GetTeamSummaryApiDto.Response response = teamService.getTeamSummary(principal);

        assertThat(response)
                .extracting(
                        GetTeamSummaryApiDto.Response::activeTeamCount,
                        GetTeamSummaryApiDto.Response::inactiveTeamCount,
                        GetTeamSummaryApiDto.Response::totalTeamCount
                )
                .containsExactly(3L, 1L, 4L);
        then(teamRepository).should().countTeamSummary(101L);
    }

    @Test
    @DisplayName("getTeam 은 principal userId 와 teamId 로 repository 조회 결과를 상세 응답으로 변환한다")
    void getTeam_은_principal_userId와_teamId로_repository_조회_결과를_상세_응답으로_변환한다() {
        CustomUserPrincipal principal = principal();
        given(teamRepository.findTeamDetail(101L, 21L)).willReturn(Optional.of(detailProjection()));

        GetTeamApiDto.Response response = teamService.getTeam(principal, 21L);

        assertThat(response)
                .extracting(
                        GetTeamApiDto.Response::teamId,
                        GetTeamApiDto.Response::teamName,
                        GetTeamApiDto.Response::teamLeaderId,
                        GetTeamApiDto.Response::teamLeaderName,
                        GetTeamApiDto.Response::deptHeadAdminUserId,
                        GetTeamApiDto.Response::deptHeadAdminUsername
                )
                .containsExactly(21L, "물류혁신TF", 101L, "홍길동", 202L, "김사업부장");
        then(teamRepository).should().findTeamDetail(101L, 21L);
        then(teamRepository).should(never()).existsByIdAndDeletedAtIsNull(21L);
    }

    @Test
    @DisplayName("getTeam 대상 팀이 없으면 TEAM_NOT_FOUND 예외를 던진다")
    void getTeam_대상_팀이_없으면_TEAM_NOT_FOUND_예외를_던진다() {
        CustomUserPrincipal principal = principal();
        given(teamRepository.findTeamDetail(101L, 999L)).willReturn(Optional.empty());
        given(teamRepository.existsByIdAndDeletedAtIsNull(999L)).willReturn(false);

        assertThatThrownBy(() -> teamService.getTeam(principal, 999L))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.TEAM_NOT_FOUND);
    }

    @Test
    @DisplayName("getTeam 대상 팀이 visible scope 밖이면 AUTH_ACCESS_DENIED 예외를 던진다")
    void getTeam_대상_팀이_visible_scope_밖이면_AUTH_ACCESS_DENIED_예외를_던진다() {
        CustomUserPrincipal principal = principal();
        given(teamRepository.findTeamDetail(101L, 21L)).willReturn(Optional.empty());
        given(teamRepository.existsByIdAndDeletedAtIsNull(21L)).willReturn(true);

        assertThatThrownBy(() -> teamService.getTeam(principal, 21L))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_ACCESS_DENIED);
    }

    @Test
    @DisplayName("팀 ID가 존재하면 팀 엔티티를 반환한다")
    void 팀_ID가_존재하면_팀_엔티티를_반환한다() {
        Team team = createTeam(21L);
        given(teamRepository.findById(21L)).willReturn(Optional.of(team));

        Team result = teamService.getTeamOrThrow(21L);

        assertThat(result).isSameAs(team);
    }

    @Test
    @DisplayName("팀 ID가 존재하지 않으면 TEAM_NOT_FOUND 예외를 던진다")
    void 팀_ID가_존재하지_않으면_TEAM_NOT_FOUND_예외를_던진다() {
        given(teamRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> teamService.getTeamOrThrow(999L))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.TEAM_NOT_FOUND);
    }

    @Test
    @DisplayName("사용자 팀 소속 여부는 ACTIVE membership 기준 repository 결과를 반환한다")
    void 사용자_팀_소속_여부는_ACTIVE_membership_기준_repository_결과를_반환한다() {
        given(userTeamRepository.existsByUserIdAndTeamIdAndStatusCode(101L, 21L, UserTeamStatus.ACTIVE))
                .willReturn(true);

        boolean result = teamService.isMember(101L, 21L);

        assertThat(result).isTrue();
        then(userTeamRepository).should()
                .existsByUserIdAndTeamIdAndStatusCode(101L, 21L, UserTeamStatus.ACTIVE);
    }

    private CustomUserPrincipal principal() {
        return new CustomUserPrincipal(101L, "user@ibank.com", "MEMBER");
    }

    private TeamSummaryProjection projection() {
        return new TeamSummaryProjection(
                21L,
                "물류혁신TF",
                "ACTIVE",
                "테스트 팀",
                101L,
                "홍길동",
                2L,
                true,
                "플랫폼 총괄",
                "주담당",
                true,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 12, 31)
        );
    }

    private TeamDetailProjection detailProjection() {
        return new TeamDetailProjection(
                21L,
                "물류혁신TF",
                "ACTIVE",
                "테스트 팀",
                101L,
                "홍길동",
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 12, 31),
                202L,
                "김사업부장"
        );
    }

    private Team createTeam(Long id) {
        Team team = Team.create(
                "물류혁신TF",
                TeamStatus.ACTIVE,
                "테스트 팀",
                LocalDate.of(2025, 1, 1),
                null
        );
        ReflectionTestUtils.setField(team, "id", id);
        return team;
    }
}
