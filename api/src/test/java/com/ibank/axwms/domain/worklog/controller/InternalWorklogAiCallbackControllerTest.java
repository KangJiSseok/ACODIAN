package com.ibank.axwms.domain.worklog.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

import com.ibank.axwms.domain.tag.service.InternalTagAiCallbackService;
import com.ibank.axwms.domain.worklog.dto.ApplyWorklogAiTagsApiDto;
import com.ibank.axwms.domain.worklog.dto.UpdateWorklogAiApiDto;
import com.ibank.axwms.domain.worklog.service.InternalWorklogAiCallbackService;
import com.ibank.axwms.global.enums.AiProcessingStatus;
import com.ibank.axwms.global.response.EmptyResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@ExtendWith(MockitoExtension.class)
class InternalWorklogAiCallbackControllerTest {

    @Mock
    private InternalWorklogAiCallbackService internalWorklogAiCallbackService;

    @Mock
    private InternalTagAiCallbackService internalTagAiCallbackService;

    @InjectMocks
    private InternalWorklogAiCallbackController internalWorklogAiCallbackController;

    @Test
    @DisplayName("내부 업무일지 AI 콜백 컨트롤러는 /internal/worklogs 기본 경로를 사용한다")
    void 내부_업무일지_ai_콜백_컨트롤러는_internal_worklogs_기본_경로를_사용한다() {
        RequestMapping requestMapping = InternalWorklogAiCallbackController.class.getAnnotation(RequestMapping.class);

        assertThat(requestMapping).isNotNull();
        assertThat(requestMapping.value()).containsExactly("/internal/worklogs");
    }

    @Test
    @DisplayName("AI 결과 수정 메서드는 PATCH 경로와 검증 파라미터를 사용한다")
    void AI_결과_수정_메서드는_patch_경로와_검증_파라미터를_사용한다() throws NoSuchMethodException {
        Method method = InternalWorklogAiCallbackController.class.getMethod(
                "updateWorklogAiResult",
                Long.class,
                UpdateWorklogAiApiDto.Request.class
        );
        PatchMapping patchMapping = method.getAnnotation(PatchMapping.class);
        Parameter requestParameter = method.getParameters()[1];

        assertThat(patchMapping).isNotNull();
        assertThat(patchMapping.value()).containsExactly("/{worklogId}/ai-result");
        assertThat(requestParameter.getAnnotation(Valid.class)).isNotNull();
    }

    @Test
    @DisplayName("AI 결과 수정 메서드는 서비스를 호출하고 EmptyResponse 를 반환한다")
    void AI_결과_수정_메서드는_서비스를_호출하고_empty_response_를_반환한다() {
        UpdateWorklogAiApiDto.Request request = new UpdateWorklogAiApiDto.Request(
                "AI 업무 요약",
                AiProcessingStatus.COMPLETED
        );

        EmptyResponse response = internalWorklogAiCallbackController.updateWorklogAiResult(501L, request);

        then(internalWorklogAiCallbackService).should().updateAiResult(501L, request);
        assertThat(response).isSameAs(EmptyResponse.INSTANCE);
    }

    @Test
    @DisplayName("AI 태그 적용 메서드는 POST 경로와 검증 파라미터를 사용한다")
    void AI_태그_적용_메서드는_post_경로와_검증_파라미터를_사용한다() throws NoSuchMethodException {
        Method method = InternalWorklogAiCallbackController.class.getMethod(
                "applyAiGeneratedTags",
                Long.class,
                ApplyWorklogAiTagsApiDto.Request.class
        );
        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        Parameter requestParameter = method.getParameters()[1];

        assertThat(postMapping).isNotNull();
        assertThat(postMapping.value()).containsExactly("/{worklogId}/ai-tags");
        assertThat(requestParameter.getAnnotation(Valid.class)).isNotNull();
    }

    @Test
    @DisplayName("AI 태그 적용 메서드는 서비스를 호출하고 EmptyResponse 를 반환한다")
    void AI_태그_적용_메서드는_서비스를_호출하고_empty_response_를_반환한다() {
        ApplyWorklogAiTagsApiDto.Request request = new ApplyWorklogAiTagsApiDto.Request(
                List.of(1L, 2L),
                List.of("신규 태그")
        );

        EmptyResponse response = internalWorklogAiCallbackController.applyAiGeneratedTags(501L, request);

        then(internalTagAiCallbackService).should().applyAiGeneratedTags(501L, request);
        assertThat(response).isSameAs(EmptyResponse.INSTANCE);
    }
}
