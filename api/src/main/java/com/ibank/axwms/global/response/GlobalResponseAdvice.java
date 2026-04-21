package com.ibank.axwms.global.response;

import org.springframework.core.MethodParameter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 모든 컨트롤러의 반환값을 공통 ApiResponse 봉투로 자동 래핑한다.
 * 컨트롤러마다 직접 래핑하면 누락/포맷 불일치가 생기므로, 단일 advice 에서 일괄 처리해 응답 형태를 강제한다.
 * 래핑 대상 판정 규칙은 {@link ResponseEnvelopePolicy} 에 단일 정의되어 있고, 문서 경로(OperationCustomizer)도 같은 규칙을 사용한다.
 */
@RestControllerAdvice
public class GlobalResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return ResponseEnvelopePolicy.shouldWrap(returnType);
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            org.springframework.http.MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response
    ) {
        return body == null ? ApiResponse.empty() : ApiResponse.success(body);
    }
}
