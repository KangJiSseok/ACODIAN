package com.ibank.axwms.domain.organization.team.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.ibank.axwms.domain.organization.team.dto.GetTeamApiDto;
import com.ibank.axwms.domain.organization.team.dto.GetTeamSummaryApiDto;
import com.ibank.axwms.domain.organization.team.dto.GetTeamUsersApiDto;
import com.ibank.axwms.domain.organization.team.dto.GetTeamsApiDto;
import com.ibank.axwms.domain.organization.team.service.TeamService;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@ExtendWith(MockitoExtension.class)
class TeamControllerTest {

    @Mock
    private TeamService teamService;

    @InjectMocks
    private TeamController teamController;

    @Test
    @DisplayName("팀 컨트롤러는 /teams 기본 경로를 사용한다")
    void 팀_컨트롤러는_teams_기본_경로를_사용한다() {
        RequestMapping requestMapping = TeamController.class.getAnnotation(RequestMapping.class);

        assertThat(requestMapping).isNotNull();
        assertThat(requestMapping.value()).containsExactly("/teams");
    }

    @Test
    @DisplayName("팀 상세 조회 메서드는 id GET 매핑과 공통 역할 권한을 사용한다")
    void 팀_상세_조회_메서드는_id_get_매핑과_공통_역할_권한을_사용한다() throws NoSuchMethodException {
        Method method = TeamController.class.getMethod("getTeam", CustomUserPrincipal.class, Long.class);
        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(getMapping).isNotNull();
        assertThat(getMapping.value()).containsExactly("/{id}");
        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("hasAnyRole('DIRECTOR','DEPT_HEAD','TEAM_LEAD','MEMBER')");
    }

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
    @DisplayName("팀 목록 조회 메서드는 루트 GET 매핑과 공통 역할 권한을 사용한다")
    void 팀_목록_조회_메서드는_루트_get_매핑과_공통_역할_권한을_사용한다() throws NoSuchMethodException {
        Method method = TeamController.class.getMethod(
                "getTeams",
                CustomUserPrincipal.class,
                GetTeamsApiDto.Request.class
        );
        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(getMapping).isNotNull();
        assertThat(getMapping.value()).isEmpty();
        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("hasAnyRole('DIRECTOR','DEPT_HEAD','TEAM_LEAD','MEMBER')");
    }

    @Test
    @DisplayName("팀 상태 요약 조회 메서드는 summary GET 매핑과 공통 역할 권한을 사용한다")
    void 팀_상태_요약_조회_메서드는_summary_get_매핑과_공통_역할_권한을_사용한다() throws NoSuchMethodException {
        Method method = TeamController.class.getMethod("getTeamSummary", CustomUserPrincipal.class);
        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(getMapping).isNotNull();
        assertThat(getMapping.value()).containsExactly("/summary");
        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("hasAnyRole('DIRECTOR','DEPT_HEAD','TEAM_LEAD','MEMBER')");
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
    @DisplayName("팀 사용자 목록 조회 메서드는 users GET 매핑과 공통 역할 권한을 사용한다")
    void 팀_사용자_목록_조회_메서드는_users_get_매핑과_공통_역할_권한을_사용한다() throws NoSuchMethodException {
        Method method = TeamController.class.getMethod("getTeamUsers", CustomUserPrincipal.class, Long.class);
        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(getMapping).isNotNull();
        assertThat(getMapping.value()).containsExactly("/{id}/users");
        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("hasAnyRole('DIRECTOR','DEPT_HEAD','TEAM_LEAD','MEMBER')");
    }

    @Test
    @DisplayName("팀 사용자 목록 조회 Docs 는 principal 을 숨기고 Swagger 계약을 가진다")
    void 팀_사용자_목록_조회_docs는_principal을_숨기고_swagger_계약을_가진다() throws NoSuchMethodException {
        Method method = TeamControllerDocs.class.getMethod("getTeamUsers", CustomUserPrincipal.class, Long.class);
        Operation operation = method.getAnnotation(Operation.class);
        Parameter principalParameter = method.getParameters()[0].getAnnotation(Parameter.class);

        assertThat(operation).isNotNull();
        assertThat(operation.summary()).isEqualTo("팀 사용자 목록 조회");
        assertThat(principalParameter).isNotNull();
        assertThat(principalParameter.hidden()).isTrue();
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
}
