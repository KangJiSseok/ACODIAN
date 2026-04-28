package com.ibank.axwms.global.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 비즈니스 예외의 상태 코드와 메시지를 정의한다.
 * GlobalExceptionHandler 는 여기 정의된 값을 기준으로 오류 응답을 만든다.
 */
@Getter
public enum ErrorCode {

    /** 처리하지 못한 서버 내부 예외의 fallback 코드다. */
    COMMON_INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
    /** 요청 본문 형식 자체가 잘못된 경우 사용한다. */
    COMMON_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    /** Bean Validation 검증 실패에 사용한다. */
    COMMON_VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "요청 값 검증에 실패했습니다."),
    /** 업로드 파일 또는 multipart 전체 크기가 서버 허용 한도를 넘은 경우 사용한다. */
    COMMON_FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "업로드 가능한 파일 크기를 초과했습니다."),
    /** 공통 응답 래핑 테스트용 예외 코드다. */
    RESPONSE_TEST_ERROR(HttpStatus.BAD_REQUEST, "응답 테스트용 예외가 발생했습니다."),
    /** 이메일 또는 비밀번호가 일치하지 않는 경우 사용한다. */
    AUTH_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    /** 비활성 계정 등 로그인 불가 상태에 사용한다. */
    AUTH_LOGIN_NOT_ALLOWED(HttpStatus.FORBIDDEN, "현재 계정 상태로는 로그인할 수 없습니다."),
    /** 요청한 팀 ID 에 해당하는 팀이 없는 경우 사용한다. */
    TEAM_NOT_FOUND(HttpStatus.NOT_FOUND, "팀을 찾을 수 없습니다."),
    /** 이미 삭제된 팀에 삭제 요청이 들어온 경우 사용한다. */
    TEAM_ALREADY_DELETED(HttpStatus.CONFLICT, "이미 삭제된 팀입니다."),
    /** 업무 지시 일자와 마감 일자 범위가 올바르지 않은 경우 사용한다. */
    WORKLOG_INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "업무 날짜 정보가 유효하지 않습니다."),
    /** 로그인 사용자가 대상 팀 소속이 아니어서 업무를 등록/변경할 수 없는 경우 사용한다. */
    WORKLOG_TEAM_FORBIDDEN(HttpStatus.FORBIDDEN, "해당 팀에 대한 권한이 없습니다."),

    /** access token 이 없거나 SecurityContext 에 현재 사용자 principal 이 없어 인증 문맥을 복원할 수 없을 때 사용한다. */
    AUTH_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    /** 인증은 되었지만 현재 요청에 필요한 권한이 없어 접근할 수 없을 때 사용한다. */
    AUTH_ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    /** refresh token 이 없거나 서명/형식/저장소 정합성이 맞지 않아 재발급을 진행할 수 없을 때 사용한다. */
    AUTH_INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 refresh token 입니다."),
    /** access token 으로 복원한 현재 사용자 문맥이 DB 에 존재하지 않을 때 사용한다. */
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    /** 같은 이름의 부서가 이미 존재해 부서를 생성하거나 수정할 수 없을 때 사용한다. */
    DEPARTMENT_DUPLICATE_NAME(HttpStatus.CONFLICT, "같은 이름의 부서가 이미 존재합니다."),
    /** 이미 다른 부서의 부서장으로 지정된 사용자를 다시 지정하려 할 때 사용한다. */
    DEPARTMENT_DUPLICATE_HEAD_USER(HttpStatus.CONFLICT, "이미 다른 부서의 부서장으로 지정된 사용자입니다."),
    /** DIRECTOR 또는 DEPT_HEAD 가 아닌 사용자를 부서장으로 지정하려 할 때 사용한다. */
    DEPARTMENT_HEAD_ROLE_NOT_ALLOWED(HttpStatus.CONFLICT, "부서장은 DIRECTOR 또는 DEPT_HEAD 역할의 사용자만 지정할 수 있습니다."),
    /** 수정 대상 부서와 다른 부서 소속 사용자를 부서장으로 지정하려 할 때 사용한다. */
    DEPARTMENT_HEAD_USER_DEPARTMENT_MISMATCH(HttpStatus.CONFLICT, "부서장은 수정 대상 부서에 소속된 사용자만 지정할 수 있습니다."),
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

    public ErrorResponse toErrorResponse() {
        return new ErrorResponse(name(), message, status.value());
    }
}
