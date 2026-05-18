package com.ibank.axwms.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.ibank.axwms.global.error.ErrorCode;
import com.ibank.axwms.global.response.ApiResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Spring Security 체인에서 발생한 인증/인가 실패를 프로젝트 공통 에러 봉투로 직렬화한다.
 * 컨트롤러 진입 전에 발생한 예외는 GlobalExceptionHandler 를 거치지 않으므로,
 * 여기서 ApiResponse.error(...) 형식으로 맞춰야 프론트엔드가 동일한 키 구조를 재사용할 수 있다.
 */
@Component
public class SecurityExceptionHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
            .findAndAddModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    /**
     * 인증이 없거나 유효하지 않아 보호된 엔드포인트에 진입하지 못했을 때 401 응답을 작성한다.
     * JWT 누락/만료/위조처럼 SecurityContext 복원에 실패한 모든 케이스를 AUTH_UNAUTHORIZED 로 정규화한다.
     */
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        writeErrorResponse(response, ErrorCode.AUTH_UNAUTHORIZED);
    }

    /**
     * 인증은 되었지만 필요한 권한이 부족해 접근이 거부됐을 때 403 응답을 작성한다.
     * 향후 @PreAuthorize/RoleHierarchy 가 추가되더라도 응답 포맷이 흔들리지 않도록 AUTH_ACCESS_DENIED 로 통일한다.
     */
    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {
        writeErrorResponse(response, ErrorCode.AUTH_ACCESS_DENIED);
    }

    /**
     * Security 예외를 공통 ApiResponse 에러 봉투로 기록한다.
     * 이미 커밋된 응답은 컨테이너 에러 dispatch 가 원인을 덮어쓰지 않도록 추가 기록을 건너뛴다.
     * 컨트롤러 advice 를 우회하는 구간이므로 content-type/status/body 를 여기서 모두 완결해야 한다.
     */
    private void writeErrorResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        OBJECT_MAPPER.writeValue(response.getWriter(), ApiResponse.error(errorCode.toErrorResponse()));
    }
}
