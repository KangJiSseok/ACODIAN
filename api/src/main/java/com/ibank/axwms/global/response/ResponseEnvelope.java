package com.ibank.axwms.global.response;

import org.springframework.core.MethodParameter;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * 공통 응답 봉투(ApiResponse) 래핑 대상 판정 규칙의 단일 출처.
 * 런타임 래핑({@code GlobalResponseAdvice})과 OpenAPI 스펙 후처리({@code OpenApiConfig})가
 * 서로 다른 지점에서 실행되지만 동일한 규칙을 써야 Swagger 문서와 실제 응답이 일치한다.
 * 규칙이 두 곳에 복제되면 한쪽만 갱신돼 문서/런타임 이탈이 생기므로 이 유틸 한 곳에만 둔다.
 */
public final class ResponseEnvelope {

    private static final String SPRINGDOC_PACKAGE_PREFIX = "org.springdoc";
    private static final String SWAGGER_PACKAGE_PREFIX = "io.swagger";

    private ResponseEnvelope() {
    }

    /** ResponseBodyAdvice 경로에서 호출한다. */
    public static boolean shouldWrap(MethodParameter returnType) {
        return isPublicController(returnType.getContainingClass())
                && isWrappableReturnType(returnType.getParameterType());
    }

    /** springdoc OperationCustomizer 경로에서 호출한다. */
    public static boolean shouldWrap(HandlerMethod handlerMethod) {
        return isPublicController(handlerMethod.getBeanType())
                && isWrappableReturnType(handlerMethod.getReturnType().getParameterType());
    }

    /**
     * 공개 API 컨트롤러인지 판정한다.
     * 내부 연동 전용(Internal* 접두사)과 springdoc/swagger 가 노출하는 문서 컨트롤러는
     * 외부 공개 응답 계약의 대상이 아니므로 래핑 대상에서 제외한다.
     */
    private static boolean isPublicController(Class<?> controllerClass) {
        String simpleName = controllerClass.getSimpleName();
        if (simpleName.startsWith("Internal") && simpleName.endsWith("Controller")) {
            return false;
        }
        Package controllerPackage = controllerClass.getPackage();
        if (controllerPackage == null) {
            return true;
        }
        String packageName = controllerPackage.getName();
        return !packageName.startsWith(SPRINGDOC_PACKAGE_PREFIX)
                && !packageName.startsWith(SWAGGER_PACKAGE_PREFIX);
    }

    /**
     * 봉투로 감쌀 수 있는 반환 타입인지 판정한다.
     * 이미 봉투이거나(ApiResponse) 호출자가 응답 제어권을 가진(ResponseEntity) 경우는 이중 래핑을 막기 위해 제외하고,
     * JSON 객체로 감싸면 직렬화가 깨지는 타입(CharSequence 기반 텍스트, void, Resource 바이너리, SseEmitter/StreamingResponseBody 스트림)도 제외한다.
     */
    private static boolean isWrappableReturnType(Class<?> returnType) {
        return !ApiResponse.class.isAssignableFrom(returnType)
                && !ResponseEntity.class.isAssignableFrom(returnType)
                && !CharSequence.class.isAssignableFrom(returnType)
                && !Void.TYPE.equals(returnType)
                && !Void.class.equals(returnType)
                && !Resource.class.isAssignableFrom(returnType)
                && !SseEmitter.class.isAssignableFrom(returnType)
                && !StreamingResponseBody.class.isAssignableFrom(returnType);
    }
}
