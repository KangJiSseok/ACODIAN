package com.ibank.axwms.domain.organization.team.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.ibank.axwms.domain.organization.team.TeamStatus;
import com.ibank.axwms.domain.organization.team.dto.GetTeamsApiDto;
import com.ibank.axwms.domain.organization.team.entity.Team;
import com.ibank.axwms.domain.organization.team.repository.TeamRepository;
import com.ibank.axwms.domain.organization.team.repository.UserTeamRepository;
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
    @DisplayName("사용자 팀 소속 여부는 repository 결과를 반환한다")
    void 사용자_팀_소속_여부는_repository_결과를_반환한다() {
        given(userTeamRepository.existsByUserIdAndTeamId(101L, 21L)).willReturn(true);

        boolean result = teamService.isMember(101L, 21L);

        assertThat(result).isTrue();
        then(userTeamRepository).should().existsByUserIdAndTeamId(101L, 21L);
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
