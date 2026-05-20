package com.ibank.axwms.domain.worklog.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ibank.axwms.domain.worklog.dto.PolishWorklogApiDto;
import com.ibank.axwms.domain.worklog.dto.RecommendWorklogTitleApiDto;
import com.ibank.axwms.domain.worklog.service.WorklogService;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import com.ibank.axwms.testsupport.E2eTestSupport;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

class WorklogControllerSecurityE2eTest extends E2eTestSupport {

    @MockitoBean
    private WorklogService worklogService;

    @Test
    @DisplayName("인증 없이 업무일지 작성 보조를 호출하면 AUTH_UNAUTHORIZED 응답을 반환한다")
    void 인증_없이_업무일지_작성_보조를_호출하면_AUTH_UNAUTHORIZED_응답을_반환한다() throws Exception {
        mockMvc.perform(apiPost("/worklogs/polish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workContent\":\"수행 내용\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("AUTH_UNAUTHORIZED")))
                .andExpect(jsonPath("$.error.statusCode", is(401)))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("허용되지 않은 role 은 업무일지 작성 보조에서 AUTH_ACCESS_DENIED 응답을 반환한다")
    void 허용되지_않은_role은_업무일지_작성_보조에서_AUTH_ACCESS_DENIED_응답을_반환한다() throws Exception {
        mockMvc.perform(apiPost("/worklogs/polish")
                        .with(principal(101L, "viewer@ibank.com", "VIEWER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workContent\":\"수행 내용\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("AUTH_ACCESS_DENIED")))
                .andExpect(jsonPath("$.error.statusCode", is(403)))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("허용 role 은 업무일지 작성 보조를 호출할 수 있다")
    void 허용_role은_업무일지_작성_보조를_호출할_수_있다() throws Exception {
        given(worklogService.polishWorklog(any())).willReturn(
                PolishWorklogApiDto.Response.of("로그를 확인했습니다.")
        );

        mockMvc.perform(apiPost("/worklogs/polish")
                        .with(principal(101L, "member@ibank.com", "MEMBER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestContent": "장애 분석 요청",
                                  "workContent": "로그를 확인했습니다."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.title").doesNotExist())
                .andExpect(jsonPath("$.data.workContent", is("로그를 확인했습니다.")));
    }

    @Test
    @DisplayName("허용 role 은 업무일지 제목 추천을 호출할 수 있다")
    void 허용_role은_업무일지_제목_추천을_호출할_수_있다() throws Exception {
        given(worklogService.recommendWorklogTitles(any())).willReturn(
                RecommendWorklogTitleApiDto.Response.of(List.of("로그 확인 결과 정리"))
        );

        mockMvc.perform(apiPost("/worklogs/title-recommendations")
                        .with(principal(101L, "member@ibank.com", "MEMBER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestContent": "장애 분석 요청",
                                  "workContent": "로그를 확인했습니다."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.workContent").doesNotExist())
                .andExpect(jsonPath("$.data.titles[0]", is("로그 확인 결과 정리")));
    }

    /** @AuthenticationPrincipal 과 role gate 를 동시에 통과할 수 있는 테스트 인증 주체를 구성한다. */
    private RequestPostProcessor principal(Long userId, String email, String role) {
        CustomUserPrincipal principal = new CustomUserPrincipal(userId, email, role);
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                principal,
                "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
        return authentication(authenticationToken);
    }
}
