package com.ibank.axwms.global.error;

import java.util.Objects;

/**
 * 비즈니스 규칙 위반/도메인 제약 실패 시 throw 한다.
 * 도메인 코드는 RuntimeException 이나 임의 문자열을 직접 던지지 않고 항상 이 예외를 사용한다.
 * 그래야 GlobalExceptionHandler 가 ErrorCode 의 status/message 로 응답을 일관되게 매핑할 수 있다.
 * 메시지는 ErrorCode 에 모아두므로 super 에 위임하고 필드로는 ErrorCode 만 보존한다.
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(Objects.requireNonNull(errorCode, "에러 코드는 null일 수 없습니다.").getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(Objects.requireNonNull(errorCode, "에러 코드는 null일 수 없습니다.").getMessage(), cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
