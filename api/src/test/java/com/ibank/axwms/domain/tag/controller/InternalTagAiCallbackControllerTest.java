package com.ibank.axwms.domain.tag.controller;

import com.ibank.axwms.domain.tag.dto.GetTagsAiApiDto;
import com.ibank.axwms.domain.tag.service.InternalTagAiCallbackService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class InternalTagAiCallbackControllerTest {

    @Mock
    private InternalTagAiCallbackService internalTagAiCallbackService;

    @InjectMocks
    private InternalTagAiCallbackController internalTagAiCallbackController;

    @Test
    @DisplayName("내부 태그 AI 콜백 컨트롤러는 /internal/tags 기본 경로를 사용한다")
    void 내부_태그_ai_콜백_컨트롤러는_internal_tags_기본_경로를_사용한다() {
        RequestMapping requestMapping = InternalTagAiCallbackController.class.getAnnotation(RequestMapping.class);

        assertThat(requestMapping).isNotNull();
        assertThat(requestMapping.value()).containsExactly("/internal/tags");
    }

    @Test
    @DisplayName("태그 목록 조회 메서드는 GET 경로를 사용한다")
    void 태그_목록_조회_메서드는_get_경로를_사용한다() throws NoSuchMethodException {
        Method method = InternalTagAiCallbackController.class.getMethod("getTags");
        GetMapping getMapping = method.getAnnotation(GetMapping.class);

        assertThat(getMapping).isNotNull();
        assertThat(getMapping.value()).isEmpty();
    }

    @Test
    @DisplayName("태그 목록 조회 메서드는 서비스를 호출하고 조회 결과를 반환한다")
    void 태그_목록_조회_메서드는_서비스를_호출하고_조회_결과를_반환한다() {
        GetTagsAiApiDto.Response expected = new GetTagsAiApiDto.Response(List.of());
        given(internalTagAiCallbackService.getTags()).willReturn(expected);

        GetTagsAiApiDto.Response response = internalTagAiCallbackController.getTags();

        then(internalTagAiCallbackService).should().getTags();
        assertThat(response).isSameAs(expected);
    }
}
