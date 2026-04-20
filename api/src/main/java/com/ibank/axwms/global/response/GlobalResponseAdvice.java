package com.ibank.axwms.global.response;

import org.springframework.core.MethodParameter;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * 모든 컨트롤러의 반환값을 공통 ApiResponse 봉투로 자동 래핑한다.
 * 컨트롤러마다 직접 래핑하면 누락/포맷 불일치가 생기므로, 단일 advice 에서 일괄 처리해 응답 형태를 강제한다.
 * 외부 공개 API 만 봉투 안에 들어가야 하므로 {@code Internal} 접두사가 붙은 비공개 연동 컨트롤러는 대상에서 제외한다.
 * 이미 ResponseEntity/ApiResponse 로 직접 응답을 만든 경우와 문자열·바이너리·스트림 응답은 래핑하면 직렬화가 깨지므로 함께 제외한다.
 */
@RestControllerAdvice
public class GlobalResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return isPublicController(returnType)
                && isSupportedReturnType(returnType.getParameterType());
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

    private boolean isPublicController(MethodParameter returnType) {
        String controllerSimpleName = returnType.getContainingClass().getSimpleName();
        return !(controllerSimpleName.startsWith("Internal") && controllerSimpleName.endsWith("Controller"));
    }

    private boolean isSupportedReturnType(Class<?> returnType) {
        return !ApiResponse.class.isAssignableFrom(returnType)
                && !ResponseEntity.class.isAssignableFrom(returnType)
                && !CharSequence.class.isAssignableFrom(returnType)
                && !Void.TYPE.equals(returnType)
                && !Void.class.equals(returnType)
                && !Resource.class.isAssignableFrom(returnType)
                && !StreamingResponseBody.class.isAssignableFrom(returnType);
    }
}
