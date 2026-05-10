package com.ibank.axwms.global.logging;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 요청 추적에서 공유하는 header/MDC 키를 한 곳에 고정해 로그 포맷과 필터·AOP 계약이 drift 되지 않게 한다.
 * Properties와 다르게 Constants로 한 이유 : 환경마다 똑같은 코드를 가져야 하기 때문에.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TraceConstants {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String TRACE_ID_MDC_KEY = "traceId";
    public static final String USER_ID_MDC_KEY = "userId";
    public static final String ROLE_CODE_MDC_KEY = "roleCode";
    public static final String NONE_VALUE = "none";
}
