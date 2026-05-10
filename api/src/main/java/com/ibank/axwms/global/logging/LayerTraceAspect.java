package com.ibank.axwms.global.logging;

import com.ibank.axwms.global.error.BusinessException;
import java.lang.reflect.Method;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Spring bean 으로 호출되는 Controller/Service/Repository public 경계를 traceId 로 연결한다.
 * 인자·반환값·예외 메시지는 기록하지 않아 계층 추적이 body/토큰/비밀번호 노출 경로가 되지 않도록 한다.
 */
@Aspect
@Component
public class LayerTraceAspect {

    private static final Logger log = LoggerFactory.getLogger(LayerTraceAspect.class);
    private static final String CONTROLLER_LAYER = "CONTROLLER";
    private static final String SERVICE_LAYER = "SERVICE";
    private static final String REPOSITORY_LAYER = "REPOSITORY";
    private static final String UNKNOWN_LAYER = "UNKNOWN";

    /** bean 이름으로 Controller 후보를 먼저 좁혀 Spring Data generic repository 초기화 비용이 계층 전체로 번지는 것을 막는다. */
    @Pointcut("bean(*Controller) && execution(public * *(..))")
    void controllerLayer() {
    }

    /** bean 이름으로 Service 후보를 먼저 좁힌 뒤 런타임 패키지 gate 로 domain 계층만 기록한다. */
    @Pointcut("bean(*Service) && execution(public * *(..))")
    void serviceLayer() {
    }

    /** Spring Data repository proxy 는 generic signature 검사가 비싸므로 bean 이름 후보로 좁혀 public 경계만 기록한다. */
    @Pointcut("bean(*Repository) && execution(public * *(..))")
    void repositoryLayer() {
    }

    /**
     * AOP proxy 가 감싼 public 계층 호출에 enter/exit/exception 이벤트를 붙이고 원래 반환·예외 계약은 그대로 보존한다.
     */
    @Around("(controllerLayer() || serviceLayer() || repositoryLayer()) && !within(com.ibank.axwms.global.logging..*)")
    public Object traceLayer(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        if (shouldSkip(method)) {
            return joinPoint.proceed();
        }

        if (!isDomainTarget(joinPoint)) {
            return joinPoint.proceed();
        }

        String layer = determineLayer(joinPoint);
        String className = className(joinPoint);
        String methodName = method.getName();
        long startedAtNanos = System.nanoTime();

        log.debug("event=layer.enter layer={} className={} methodName={}",
                layer,
                className,
                methodName);
        try {
            Object result = joinPoint.proceed();
            log.debug("event=layer.exit layer={} className={} methodName={} durationMs={}",
                    layer,
                    className,
                    methodName,
                    durationMs(startedAtNanos));
            return result;
        } catch (Throwable exception) {
            logLayerException(layer, className, methodName, exception);
            throw exception;
        }
    }

    /** bridge/synthetic/Object 메서드는 애플리케이션 계층 호출이 아니므로 추적 노이즈에서 제외한다. */
    private boolean shouldSkip(Method method) {
        return method.isBridge() || method.isSynthetic() || method.getDeclaringClass().equals(Object.class);
    }

    /** bean 이름 pointcut 의 넓은 후보 중 실제 domain 패키지 경계만 남겨 global/test infrastructure 로그 유입을 차단한다. */
    private boolean isDomainTarget(ProceedingJoinPoint joinPoint) {
        return className(joinPoint).startsWith("com.ibank.axwms.domain.")
                || joinPoint.getSignature().getDeclaringTypeName().startsWith("com.ibank.axwms.domain.");
    }


    /** 패키지 경계를 기준으로 사람이 읽을 수 있는 계층명을 정규화해 프록시 클래스명에 따른 drift 를 줄인다. */
    private String determineLayer(ProceedingJoinPoint joinPoint) {
        String declaringTypeName = joinPoint.getSignature().getDeclaringTypeName();
        if (declaringTypeName.contains(".controller.")) {
            return CONTROLLER_LAYER;
        }
        if (declaringTypeName.contains(".service.")) {
            return SERVICE_LAYER;
        }
        if (declaringTypeName.contains(".repository.")) {
            return REPOSITORY_LAYER;
        }
        return UNKNOWN_LAYER;
    }

    /** 프록시 구현체명 대신 선언 타입명을 남겨 Spring Data repository 도 사람이 읽을 수 있는 경계로 표시한다. */
    private String className(ProceedingJoinPoint joinPoint) {
        return joinPoint.getSignature().getDeclaringTypeName();
    }

    /** 처리 시간은 밀리초 정수만 남겨 세부 인자·반환값 없이 병목 계층 식별에 필요한 최소값만 제공한다. */
    private long durationMs(long startedAtNanos) {
        return (System.nanoTime() - startedAtNanos) / 1_000_000L;
    }

    /** BusinessException 은 ErrorCode 만 추가하고 모든 예외에서 raw message·stack trace 인자를 의도적으로 넘기지 않는다. */
    private void logLayerException(String layer,
                                   String className,
                                   String methodName,
                                   Throwable exception) {
        if (exception instanceof BusinessException businessException) {
            log.warn("event=layer.exception layer={} className={} methodName={} exceptionClass={} errorCode={}",
                    layer,
                    className,
                    methodName,
                    exception.getClass().getName(),
                    businessException.getErrorCode().name());
            return;
        }
        log.warn("event=layer.exception layer={} className={} methodName={} exceptionClass={}",
                layer,
                className,
                methodName,
                exception.getClass().getName());
    }
}
