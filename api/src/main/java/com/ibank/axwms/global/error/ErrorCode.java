package com.ibank.axwms.global.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 비즈니스 예외 식별자. GlobalExceptionHandler 가 status/message 매핑에 사용한다.
 * 도메인별 코드는 {@code {DOMAIN}_{REASON}} 패턴으로 추가한다.
 */
@Getter
public enum ErrorCode {

    /** 처리되지 않은 서버 측 예외의 fallback. GlobalExceptionHandler 의 catch-all 에서 사용. */
    COMMON_INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
    /** 요청 본문 파싱 실패 등 형식 자체가 잘못된 요청. */
    COMMON_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    /** Bean Validation/바인딩 실패. @Valid, @Validated 위반 시 사용. */
    COMMON_VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "요청 값 검증에 실패했습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    /** 에러 응답 본문으로 변환한다. enum 이름을 code 로, status 를 statusCode 로 사용한다. */
    public ErrorResponse toErrorResponse() {
        return new ErrorResponse(name(), message, status.value());
    }
}
