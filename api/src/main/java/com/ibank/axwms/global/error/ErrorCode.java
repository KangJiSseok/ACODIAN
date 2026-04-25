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
    COMMON_VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "요청 값 검증에 실패했습니다."),
    /** 로그인 후 응답 봉투 테스트용 보호 엔드포인트가 의도적으로 비즈니스 예외를 발생시킬 때 사용한다. */
    RESPONSE_TEST_ERROR(HttpStatus.BAD_REQUEST, "응답 테스트용 예외가 발생했습니다."),
    /** access token 이 없거나 SecurityContext 에 현재 사용자 principal 이 없어 인증 문맥을 복원할 수 없을 때 사용한다. */
    AUTH_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    /** 인증은 되었지만 현재 요청에 필요한 권한이 없어 접근할 수 없을 때 사용한다. */
    AUTH_ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    /** 이메일이 존재하지 않거나 비밀번호가 일치하지 않아 인증할 수 없을 때 사용한다. */
    AUTH_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    /** refresh token 이 없거나 서명/형식/저장소 정합성이 맞지 않아 재발급을 진행할 수 없을 때 사용한다. */
    AUTH_INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 refresh token 입니다."),
    /** 재직 상태가 ACTIVE 가 아니어서 로그인할 수 없을 때 사용한다. */
    AUTH_LOGIN_NOT_ALLOWED(HttpStatus.FORBIDDEN, "현재 계정 상태로는 로그인할 수 없습니다."),
    /** access token 으로 복원한 현재 사용자 문맥이 DB 에 존재하지 않을 때 사용한다. */
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    /** 같은 이름의 부서가 이미 존재해 부서를 생성하거나 수정할 수 없을 때 사용한다. */
    DEPARTMENT_DUPLICATE_NAME(HttpStatus.CONFLICT, "같은 이름의 부서가 이미 존재합니다."),
    /** 이미 다른 부서의 부서장으로 지정된 사용자를 다시 지정하려 할 때 사용한다. */
    DEPARTMENT_DUPLICATE_HEAD_USER(HttpStatus.CONFLICT, "이미 다른 부서의 부서장으로 지정된 사용자입니다."),
    /** DIRECTOR 또는 DEPT_HEAD 가 아닌 사용자를 부서장으로 지정하려 할 때 사용한다. */
    DEPARTMENT_HEAD_ROLE_NOT_ALLOWED(HttpStatus.CONFLICT, "부서장은 DIRECTOR 또는 DEPT_HEAD 역할의 사용자만 지정할 수 있습니다."),
    /** 활성 부서를 찾지 못했거나 요청한 부서 자체가 존재하지 않을 때 사용한다. */
    DEPARTMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "부서를 찾을 수 없습니다."),
    /** 활성 팀이 남아 있어 부서를 비활성화할 수 없을 때 사용한다. */
    DEPARTMENT_HAS_ACTIVE_TEAMS(HttpStatus.CONFLICT, "활성 팀이 남아 있어 부서를 비활성화할 수 없습니다."),
    /** 부서장 후보 사용자의 역할이 DIRECTOR 또는 DEPT_HEAD 가 아닐 때 사용한다. */
    DEPARTMENT_INVALID_HEAD_USER_ROLE(HttpStatus.BAD_REQUEST, "부서장으로 지정할 수 없는 사용자 역할입니다.");

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
