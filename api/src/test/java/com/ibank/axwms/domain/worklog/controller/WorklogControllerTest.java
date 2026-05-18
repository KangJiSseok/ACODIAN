package com.ibank.axwms.domain.worklog.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.ibank.axwms.domain.worklog.WorklogStatus;
import com.ibank.axwms.domain.worklog.dto.PolishWorklogApiDto;
import com.ibank.axwms.domain.worklog.dto.RecommendWorklogTitleApiDto;
import com.ibank.axwms.domain.worklog.dto.UpdateWorklogStatusApiDto;
import com.ibank.axwms.domain.worklog.service.WorklogService;
import com.ibank.axwms.global.response.EmptyResponse;
import com.ibank.axwms.global.response.GlobalResponseAdvice;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@ExtendWith(MockitoExtension.class)
class WorklogControllerTest {

    private ObjectMapper objectMapper;
    private MockMvc mockMvc;

    @Mock
    private WorklogService worklogService;

    @Mock
    private com.ibank.axwms.domain.worklog.service.search.WorklogSearchService worklogSearchService;

    @InjectMocks
    private WorklogController worklogController;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mockMvc = MockMvcBuilders.standaloneSetup(worklogController)
                .setControllerAdvice(new GlobalResponseAdvice())
                .build();
    }

    @Test
    @DisplayName("업무일지 컨트롤러는 /worklogs 기본 경로를 사용한다")
    void 업무일지_컨트롤러는_worklogs_기본_경로를_사용한다() {
        RequestMapping requestMapping = WorklogController.class.getAnnotation(RequestMapping.class);

        assertThat(requestMapping).isNotNull();
        assertThat(requestMapping.value()).containsExactly("/worklogs");
    }


    @Test
    @DisplayName("업무일지 작성 보조 메서드는 polish POST 경로와 MEMBER 이상 role gate 를 사용한다")
    void 업무일지_작성_보조_메서드는_polish_post_경로와_role_gate를_사용한다() throws NoSuchMethodException {
        Method method = WorklogController.class.getMethod(
                "polishWorklog",
                PolishWorklogApiDto.Request.class
        );
        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(postMapping).isNotNull();
        assertThat(postMapping.value()).containsExactly("/polish");
        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("hasAnyRole('DIRECTOR','DEPT_HEAD','TEAM_LEAD','MEMBER')");
        assertThat(method.getParameters()[0].getAnnotation(RequestBody.class)).isNotNull();
    }

    @Test
    @DisplayName("업무일지 작성 보조 메서드는 업무일지 서비스로 위임한다")
    void 업무일지_작성_보조_메서드는_업무일지_서비스로_위임한다() {
        PolishWorklogApiDto.Request request = new PolishWorklogApiDto.Request(
                "장애 분석 요청",
                "로그를 확인하고 장애 구간을 분리했습니다."
        );
        PolishWorklogApiDto.Response expected = PolishWorklogApiDto.Response.of(
                "로그를 확인하고 장애 구간을 분리했습니다."
        );
        given(worklogService.polishWorklog(request)).willReturn(expected);

        PolishWorklogApiDto.Response response = worklogController.polishWorklog(request);

        assertThat(response).isEqualTo(expected);
        then(worklogService).should().polishWorklog(request);
    }

    @Test
    @DisplayName("업무일지 작성 보조 HTTP 응답은 workContent 만 data 로 반환한다")
    void 업무일지_작성_보조_http_응답은_work_content만_data로_반환한다() throws Exception {
        given(worklogService.polishWorklog(new PolishWorklogApiDto.Request(
                "장애 분석 요청",
                "로그를 확인하고 장애 구간을 분리했습니다."
        ))).willReturn(PolishWorklogApiDto.Response.of(
                "로그를 확인하고 장애 구간을 분리했습니다."
        ));

        MvcResult result = mockMvc.perform(post("/worklogs/polish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestContent": "장애 분석 요청",
                                  "workContent": "로그를 확인하고 장애 구간을 분리했습니다."
                                }
                                """.getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");

        assertThat(data.has("title")).isFalse();
        assertThat(data.get("workContent").asText()).isEqualTo("로그를 확인하고 장애 구간을 분리했습니다.");
        assertThat(data.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("업무일지 작성 보조 요청에서 workContent 가 비어 있으면 400을 반환한다")
    void 업무일지_작성_보조_요청에서_work_content가_비어_있으면_400을_반환한다() throws Exception {
        mockMvc.perform(post("/worklogs/polish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestContent": "장애 분석 요청",
                                  "workContent": ""
                                }
                                """.getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("업무일지 제목 추천 메서드는 title-recommendations POST 경로와 MEMBER 이상 role gate 를 사용한다")
    void 업무일지_제목_추천_메서드는_title_recommendations_post_경로와_role_gate를_사용한다() throws NoSuchMethodException {
        Method method = WorklogController.class.getMethod(
                "recommendWorklogTitles",
                RecommendWorklogTitleApiDto.Request.class
        );
        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(postMapping).isNotNull();
        assertThat(postMapping.value()).containsExactly("/title-recommendations");
        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("hasAnyRole('DIRECTOR','DEPT_HEAD','TEAM_LEAD','MEMBER')");
        assertThat(method.getParameters()[0].getAnnotation(RequestBody.class)).isNotNull();
    }

    @Test
    @DisplayName("업무일지 제목 추천 메서드는 업무일지 서비스로 위임한다")
    void 업무일지_제목_추천_메서드는_업무일지_서비스로_위임한다() {
        RecommendWorklogTitleApiDto.Request request = new RecommendWorklogTitleApiDto.Request(
                "장애 분석 요청",
                "로그를 확인하고 장애 구간을 분리했습니다."
        );
        RecommendWorklogTitleApiDto.Response expected = RecommendWorklogTitleApiDto.Response.of(
                List.of("장애 구간 로그 확인")
        );
        given(worklogService.recommendWorklogTitles(request)).willReturn(expected);

        RecommendWorklogTitleApiDto.Response response = worklogController.recommendWorklogTitles(request);

        assertThat(response).isEqualTo(expected);
        then(worklogService).should().recommendWorklogTitles(request);
    }

    @Test
    @DisplayName("업무일지 제목 추천 HTTP 응답은 titles 만 data 로 반환한다")
    void 업무일지_제목_추천_http_응답은_titles만_data로_반환한다() throws Exception {
        given(worklogService.recommendWorklogTitles(new RecommendWorklogTitleApiDto.Request(
                "장애 분석 요청",
                "로그를 확인하고 장애 구간을 분리했습니다."
        ))).willReturn(RecommendWorklogTitleApiDto.Response.of(
                List.of("장애 구간 로그 확인", "장애 원인 분석")
        ));

        MvcResult result = mockMvc.perform(post("/worklogs/title-recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestContent": "장애 분석 요청",
                                  "workContent": "로그를 확인하고 장애 구간을 분리했습니다."
                                }
                                """.getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");

        assertThat(data.has("workContent")).isFalse();
        assertThat(data.get("titles")).hasSize(2);
        assertThat(data.get("titles").get(0).asText()).isEqualTo("장애 구간 로그 확인");
        assertThat(data.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("업무 상태 변경 메서드는 status PATCH 경로와 MEMBER 이상 role gate 를 사용한다")
    void 업무_상태_변경_메서드는_status_patch_경로와_role_gate를_사용한다() throws NoSuchMethodException {
        Method method = WorklogController.class.getMethod(
                "updateWorklogStatus",
                CustomUserPrincipal.class,
                Long.class,
                UpdateWorklogStatusApiDto.Request.class
        );
        PatchMapping patchMapping = method.getAnnotation(PatchMapping.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(patchMapping).isNotNull();
        assertThat(patchMapping.value()).containsExactly("/{worklogId}/status");
        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("hasAnyRole('DIRECTOR','DEPT_HEAD','TEAM_LEAD','MEMBER')");
        assertThat(method.getParameters()[2].getAnnotation(RequestBody.class)).isNotNull();
    }

    @Test
    @DisplayName("업무 상태 변경 메서드는 서비스를 호출하고 EmptyResponse 를 반환한다")
    void 업무_상태_변경_메서드는_서비스를_호출하고_empty_response를_반환한다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(101L, "member@ibank.com", "MEMBER");
        UpdateWorklogStatusApiDto.Request request = new UpdateWorklogStatusApiDto.Request(
                WorklogStatus.COMPLETED,
                "완료 처리"
        );

        EmptyResponse response = worklogController.updateWorklogStatus(principal, 501L, request);

        assertThat(response).isSameAs(EmptyResponse.INSTANCE);
        then(worklogService).should().updateWorklogStatus(principal, 501L, request);
    }

    @Test
    @DisplayName("업무 상태 변경 HTTP 응답은 빈 data 객체로 래핑된다")
    void 업무_상태_변경_http_응답은_빈_data_객체로_래핑된다() throws Exception {
        MvcResult result = mockMvc.perform(patch("/worklogs/501/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "statusCode": "COMPLETED",
                                  "reason": "완료 처리"
                                }
                                """.getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());

        assertThat(json.get("success").asBoolean()).isTrue();
        assertThat(json.get("data").isObject()).isTrue();
        assertThat(json.get("data").size()).isZero();
        assertThat(json.get("timestamp").asText()).isNotBlank();
        then(worklogService).should().updateWorklogStatus(
                new CustomUserPrincipal(null, null, null),
                501L,
                new UpdateWorklogStatusApiDto.Request(WorklogStatus.COMPLETED, "완료 처리")
        );
    }
}
