package com.ibank.axwms.domain.organization.team.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.ibank.axwms.domain.organization.team.dto.BulkUpsertTeamUsersApiDto;
import com.ibank.axwms.domain.organization.team.dto.CreateTeamApiDto;
import com.ibank.axwms.domain.organization.team.dto.GetTeamsSummaryApiDto;
import com.ibank.axwms.domain.organization.team.dto.UpdateTeamApiDto;
import com.ibank.axwms.domain.organization.team.dto.UpdateTeamStatusApiDto;
import com.ibank.axwms.global.response.EmptyResponse;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Parameter;
import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

class TeamControllerContractTest {

    @Test
    @DisplayName("팀 생성 메서드는 201 Created 와 EmptyResponse 계약을 가진다")
    void 팀_생성_메서드는_201_Created_와_EmptyResponse_계약을_가진다() throws NoSuchMethodException {
        Method method = TeamController.class.getMethod("createTeam", CustomUserPrincipal.class, CreateTeamApiDto.Request.class);

        ResponseStatus responseStatus = method.getAnnotation(ResponseStatus.class);

        assertThat(method.getReturnType()).isEqualTo(EmptyResponse.class);
        assertThat(responseStatus).isNotNull();
        assertThat(responseStatus.value()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    @DisplayName("팀 summary 메서드는 summary 경로와 전체 읽기 역할을 노출한다")
    void 팀_summary_메서드는_summary_경로와_전체_읽기_역할을_노출한다() throws NoSuchMethodException {
        Method method = TeamController.class.getMethod("getTeamSummary", CustomUserPrincipal.class, GetTeamsSummaryApiDto.Request.class);

        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(getMapping).isNotNull();
        assertThat(getMapping.value()).containsExactly("/summary");
        assertThat(preAuthorize.value()).isEqualTo("hasAnyRole('DIRECTOR','DEPT_HEAD','TEAM_LEAD','MEMBER')");
    }

    @Test
    @DisplayName("팀 write 메서드는 EmptyResponse 를 반환한다")
    void 팀_write_메서드는_EmptyResponse_를_반환한다() throws NoSuchMethodException {
        assertThat(TeamController.class.getMethod("updateTeam", CustomUserPrincipal.class, Long.class, UpdateTeamApiDto.Request.class).getReturnType()).isEqualTo(EmptyResponse.class);
        assertThat(TeamController.class.getMethod("updateTeamStatus", CustomUserPrincipal.class, Long.class, UpdateTeamStatusApiDto.Request.class).getReturnType()).isEqualTo(EmptyResponse.class);
        assertThat(TeamController.class.getMethod("bulkUpsertTeamUsers", CustomUserPrincipal.class, Long.class, BulkUpsertTeamUsersApiDto.Request.class).getReturnType()).isEqualTo(EmptyResponse.class);
        assertThat(TeamController.class.getMethod("deleteTeam", CustomUserPrincipal.class, Long.class).getReturnType()).isEqualTo(EmptyResponse.class);
    }

    @Test
    @DisplayName("TeamControllerDocs principal 파라미터는 Swagger 에서 숨긴다")
    void TeamControllerDocs_principal_파라미터는_Swagger_에서_숨긴다() throws NoSuchMethodException {
        assertPrincipalHidden(TeamControllerDocs.class.getMethod("getTeams", CustomUserPrincipal.class, com.ibank.axwms.domain.organization.team.dto.GetTeamsApiDto.Request.class));
        assertPrincipalHidden(TeamControllerDocs.class.getMethod("getTeamSummary", CustomUserPrincipal.class, GetTeamsSummaryApiDto.Request.class));
        assertPrincipalHidden(TeamControllerDocs.class.getMethod("getTeamDetail", CustomUserPrincipal.class, Long.class));
        assertPrincipalHidden(TeamControllerDocs.class.getMethod("getTeamUsers", CustomUserPrincipal.class, Long.class, com.ibank.axwms.domain.organization.team.dto.GetTeamUsersApiDto.Request.class));
        assertPrincipalHidden(TeamControllerDocs.class.getMethod("getTeamWorklogs", CustomUserPrincipal.class, Long.class, com.ibank.axwms.domain.organization.team.dto.GetTeamWorklogsApiDto.Request.class));
        assertPrincipalHidden(TeamControllerDocs.class.getMethod("createTeam", CustomUserPrincipal.class, CreateTeamApiDto.Request.class));
        assertPrincipalHidden(TeamControllerDocs.class.getMethod("updateTeam", CustomUserPrincipal.class, Long.class, UpdateTeamApiDto.Request.class));
        assertPrincipalHidden(TeamControllerDocs.class.getMethod("updateTeamStatus", CustomUserPrincipal.class, Long.class, UpdateTeamStatusApiDto.Request.class));
        assertPrincipalHidden(TeamControllerDocs.class.getMethod("bulkUpsertTeamUsers", CustomUserPrincipal.class, Long.class, BulkUpsertTeamUsersApiDto.Request.class));
        assertPrincipalHidden(TeamControllerDocs.class.getMethod("deleteTeam", CustomUserPrincipal.class, Long.class));
    }

    private void assertPrincipalHidden(Method method) {
        Parameter parameter = method.getParameters()[0].getAnnotation(Parameter.class);
        assertThat(parameter).isNotNull();
        assertThat(parameter.hidden()).isTrue();
    }
}
