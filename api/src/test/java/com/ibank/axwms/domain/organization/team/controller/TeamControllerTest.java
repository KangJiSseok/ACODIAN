package com.ibank.axwms.domain.organization.team.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.ibank.axwms.domain.organization.team.TeamStatus;
import com.ibank.axwms.domain.organization.team.dto.CreateTeamApiDto;
import com.ibank.axwms.domain.organization.team.dto.GetTeamApiDto;
import com.ibank.axwms.domain.organization.team.dto.GetTeamSummaryApiDto;
import com.ibank.axwms.domain.organization.team.dto.GetTeamUsersApiDto;
import com.ibank.axwms.domain.organization.team.service.TeamService;
import com.ibank.axwms.global.response.EmptyResponse;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TeamControllerTest {

    @Mock
    private TeamService teamService;

    @InjectMocks
    private TeamController teamController;

    @Test
    @DisplayName("팀 상세 조회 메서드는 서비스 결과를 그대로 반환한다")
    void 팀_상세_조회_메서드는_서비스_결과를_그대로_반환한다() {
        CustomUserPrincipal principal = principal();
        GetTeamApiDto.Response serviceResponse = teamDetailResponse();
        given(teamService.getTeam(principal, 21L)).willReturn(serviceResponse);

        GetTeamApiDto.Response response = teamController.getTeam(principal, 21L);

        then(teamService).should().getTeam(principal, 21L);
        assertThat(response).isSameAs(serviceResponse);
    }

    @Test
    @DisplayName("팀 상태 요약 조회 메서드는 서비스 결과를 그대로 반환한다")
    void 팀_상태_요약_조회_메서드는_서비스_결과를_그대로_반환한다() {
        CustomUserPrincipal principal = principal();
        GetTeamSummaryApiDto.Response serviceResponse = new GetTeamSummaryApiDto.Response(3L, 1L, 4L);
        given(teamService.getTeamSummary(principal)).willReturn(serviceResponse);

        GetTeamSummaryApiDto.Response response = teamController.getTeamSummary(principal);

        then(teamService).should().getTeamSummary(principal);
        assertThat(response).isSameAs(serviceResponse);
    }

    @Test
    @DisplayName("팀 사용자 목록 조회 메서드는 서비스 결과를 그대로 반환한다")
    void 팀_사용자_목록_조회_메서드는_서비스_결과를_그대로_반환한다() {
        CustomUserPrincipal principal = principal();
        GetTeamUsersApiDto.Response serviceResponse = teamUsersResponse();
        given(teamService.getTeamUsers(principal, 21L)).willReturn(serviceResponse);

        GetTeamUsersApiDto.Response response = teamController.getTeamUsers(principal, 21L);

        then(teamService).should().getTeamUsers(principal, 21L);
        assertThat(response).isSameAs(serviceResponse);
    }

    @Test
    @DisplayName("팀 생성 메서드는 서비스에 위임하고 빈 응답을 반환한다")
    void 팀_생성_메서드는_서비스에_위임하고_빈_응답을_반환한다() {
        CreateTeamApiDto.Request request = createTeamRequest();

        EmptyResponse response = teamController.createTeam(request);

        then(teamService).should().createTeam(request);
        assertThat(response).isSameAs(EmptyResponse.INSTANCE);
    }

    private CustomUserPrincipal principal() {
        return new CustomUserPrincipal(101L, "user@ibank.com", "MEMBER");
    }

    private GetTeamApiDto.Response teamDetailResponse() {
        return new GetTeamApiDto.Response(
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

    private GetTeamUsersApiDto.Response teamUsersResponse() {
        return new GetTeamUsersApiDto.Response(List.of(new GetTeamUsersApiDto.Item(
                true,
                101L,
                "홍길동",
                "과장",
                "플랫폼 총괄"
        )));
    }

    private CreateTeamApiDto.Request createTeamRequest() {
        return new CreateTeamApiDto.Request(
                "물류혁신TF",
                "창고 자동화 및 운영 고도화",
                100L,
                List.of(new CreateTeamApiDto.AddUser(102L, true, "WMS 운영")),
                TeamStatus.ACTIVE,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 12, 31)
        );
    }
}
