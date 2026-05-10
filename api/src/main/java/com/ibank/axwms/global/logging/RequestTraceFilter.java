package com.ibank.axwms.global.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * SecurityFilterChain 앞단에서 요청 traceId 를 확정해 JWT 인증·컨트롤러·예외 응답까지 같은 MDC 문맥으로 묶는다.
 * body/query/header 전체는 읽지 않고 path-only URI 와 인증 요약만 남겨 추적성과 민감정보 비노출을 함께 지킨다.
 * OncePerRequestFilter: 하나의 HTTP 요청에 대해 이 필터 로직이 “한 번만” 실행되도록 보장해주는 필터
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RequestTraceFilter extends OncePerRequestFilter {

    private static final int SERVER_ERROR_STATUS = 500;

    private final TracePrincipalResolver tracePrincipalResolver;

    /**
     * 요청당 한 번 traceId 를 MDC 와 응답 header 에 싣고, 체인 성공·예외와 무관하게 종료 로그와 MDC 정리를 보장한다.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long startedAtNanos = System.nanoTime();
        String traceId = resolveTraceId(request);
        Throwable failure = null;

        putTraceMdc(traceId);
        response.setHeader(TraceConstants.TRACE_ID_HEADER, traceId);
        logRequestStart(request);

        try {
            filterChain.doFilter(request, response);
        } catch (ServletException | IOException | RuntimeException exception) {
            failure = exception;
            logRequestException(request, exception);
            throw exception;
        } finally {
            applyPrincipalMdc();
            logRequestEnd(request, response, startedAtNanos, failure);
            clearTraceMdc();
        }
    }

    /** 앞단에서 검증한 trace header 를 신뢰하되, 없으면 API 계층이 요청 추적 키를 직접 생성한다. */
    private String resolveTraceId(HttpServletRequest request) {
        String inboundTraceId = request.getHeader(TraceConstants.TRACE_ID_HEADER);
        if (StringUtils.hasText(inboundTraceId)) {
            return inboundTraceId.trim();
        }
        return UUID.randomUUID().toString();
    }

    /** traceId 만 MDC 에 넣어 이후 JWT 필터와 AOP 로그가 같은 요청 키를 공유하도록 한다. */
    private void putTraceMdc(String traceId) {
        MDC.put(TraceConstants.TRACE_ID_MDC_KEY, traceId);
    }

    /** 요청 시작 로그는 method 와 path-only URI 로 제한해 query/header/body 값이 최초 이벤트에 섞이지 않게 한다. */
    private void logRequestStart(HttpServletRequest request) {
        log.info("event=request.start method={} path={}",
                request.getMethod(),
                request.getRequestURI());
    }

    /** 체인에서 전파된 예외는 class 이름만 남기고 raw message·stack trace 는 기존 예외 처리 정책에 맡긴다. */
    private void logRequestException(HttpServletRequest request, Throwable exception) {
        log.warn("event=request.exception method={} path={} exceptionClass={}",
                request.getMethod(),
                request.getRequestURI(),
                exception.getClass().getName());
    }

    /** 인증 필터 뒤에서 복원된 principal 만 종료 로그 MDC 에 보강해 시작 로그와 인증 전 상태가 충돌하지 않게 한다. */
    private void applyPrincipalMdc() {
        tracePrincipalResolver.resolve(SecurityContextHolder.getContext().getAuthentication())
                .ifPresent(summary -> {
                    if (summary.userId() != null) {
                        MDC.put(TraceConstants.USER_ID_MDC_KEY, summary.userId().toString());
                    }
                    if (summary.roleCode() != null) {
                        MDC.put(TraceConstants.ROLE_CODE_MDC_KEY, summary.roleCode());
                    }
                });
    }

    /** 종료 로그는 query string 없이 현재 응답 status 와 처리 시간을 기록해 응답 계약을 바꾸지 않고 흐름을 닫는다. */
    private void logRequestEnd(HttpServletRequest request,
                               HttpServletResponse response,
                               long startedAtNanos,
                               Throwable failure) {
        long durationMs = (System.nanoTime() - startedAtNanos) / 1_000_000L;
        log.info("event=request.end method={} path={} status={} durationMs={} userId={} roleCode={}",
                request.getMethod(),
                request.getRequestURI(),
                resolveStatus(response, failure),
                durationMs,
                mdcValueOrNone(TraceConstants.USER_ID_MDC_KEY),
                mdcValueOrNone(TraceConstants.ROLE_CODE_MDC_KEY));
    }

    /** MDC pattern 과 메시지 필드가 같은 기본값을 쓰도록 인증 요약 누락을 none 으로 통일한다. */
    private String mdcValueOrNone(String key) {
        String value = MDC.get(key);
        return value == null ? TraceConstants.NONE_VALUE : value;
    }

    /** 아직 컨테이너가 500 으로 바꾸기 전 전파 예외가 보이면 로그상 종료 상태만 서버 오류로 보정한다. */
    private int resolveStatus(HttpServletResponse response, Throwable failure) {
        if (failure != null && response.getStatus() < 400) {
            return SERVER_ERROR_STATUS;
        }
        return response.getStatus();
    }

    /** 요청 완료 후 이 필터가 사용한 MDC 키만 제거해 thread 재사용 시 trace/user 문맥이 다음 요청으로 새지 않게 한다. */
    private void clearTraceMdc() {
        MDC.remove(TraceConstants.TRACE_ID_MDC_KEY);
        MDC.remove(TraceConstants.USER_ID_MDC_KEY);
        MDC.remove(TraceConstants.ROLE_CODE_MDC_KEY);
    }
}
