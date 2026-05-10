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
    /** 회원가입 시 이미 사용 중인 이메일로 계정을 만들려는 경우 사용한다. */
    AUTH_SIGNUP_DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    /** 회원가입 시 지원하지 않는 title_name 으로 role 매핑을 만들 수 없을 때 사용한다. */
    AUTH_SIGNUP_INVALID_TITLE_NAME(HttpStatus.BAD_REQUEST, "지원하지 않는 직책명입니다."),
    /** 회원가입 프로필 이미지 업로드를 스토리지 장애로 처리할 수 없을 때 사용한다. */
    AUTH_SIGNUP_PROFILE_IMAGE_UPLOAD_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "프로필 이미지 업로드를 처리할 수 없습니다. 잠시 후 다시 시도해 주세요."),
    /** 비활성 계정 등 로그인 불가 상태에 사용한다. */
    AUTH_LOGIN_NOT_ALLOWED(HttpStatus.FORBIDDEN, "현재 계정 상태로는 로그인할 수 없습니다."),
    /** access token 이 없거나 SecurityContext 에 현재 사용자 principal 이 없어 인증 문맥을 복원할 수 없을 때 사용한다. */
    AUTH_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    /** 인증은 되었지만 현재 요청에 필요한 권한이 없어 접근할 수 없을 때 사용한다. */
    AUTH_ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    /** refresh token 이 없거나 서명/형식/저장소 정합성이 맞지 않아 재발급을 진행할 수 없을 때 사용한다. */
    AUTH_INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 refresh token 입니다."),
    /** 본인 비밀번호 변경 요청에서 현재 비밀번호가 일치하지 않을 때 사용한다. */
    AUTH_PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다."),
    /** 본인 비밀번호 변경 요청의 새 비밀번호가 보안 정책을 만족하지 못할 때 사용한다. */
    AUTH_PASSWORD_POLICY_VIOLATION(HttpStatus.BAD_REQUEST, "비밀번호는 8~100자이며 영문, 숫자, 특수문자를 포함해야 합니다."),
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
    DEPARTMENT_INVALID_HEAD_USER_ROLE(HttpStatus.BAD_REQUEST, "부서장으로 지정할 수 없는 사용자 역할입니다."),
    /** 요청한 팀 ID 에 해당하는 팀이 없는 경우 사용한다. */
    TEAM_NOT_FOUND(HttpStatus.NOT_FOUND, "팀을 찾을 수 없습니다."),
    /** soft-delete 되지 않은 동일 팀명이 이미 존재할 때 사용한다. */
    TEAM_DUPLICATE_NAME(HttpStatus.CONFLICT, "같은 이름의 팀이 이미 존재합니다."),
    /** 팀 관련 요청에서 리더가 정확히 한 명이 아닐 때 사용한다. */
    TEAM_LEADER_COUNT_INVALID(HttpStatus.BAD_REQUEST, "리더는 1명이어야 합니다."),
    /** access token 으로 복원한 현재 사용자 문맥이 DB 에 존재하지 않을 때 사용한다. */
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    /** 현재 사용자 프로필 수정 시 지원하지 않는 title_name 으로 role 매핑을 만들 수 없을 때 사용한다. */
    USER_INVALID_TITLE_NAME(HttpStatus.BAD_REQUEST, "지원하지 않는 직책명입니다."),
    /** 현재 사용자 프로필 이미지 final 업로드를 스토리지 장애로 처리할 수 없을 때 사용한다. */
    USER_PROFILE_IMAGE_UPLOAD_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "프로필 이미지 업로드를 처리할 수 없습니다. 잠시 후 다시 시도해 주세요."),
    /** 같은 사용자가 이미 보유한 스킬명을 다시 등록하려 할 때 사용한다. */
    USER_SKILL_DUPLICATE_NAME(HttpStatus.CONFLICT, "이미 등록된 사용자 스킬입니다."),
    /** 사용자 path 에 속한 스킬 레코드를 찾을 수 없을 때 사용한다. */
    USER_SKILL_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자 스킬을 찾을 수 없습니다."),
    /** 평가 조회/등록 대상에 대한 역할/부서 접근 범위를 만족하지 못할 때 사용한다. */
    EVALUATION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 평가에 접근할 권한이 없습니다."),
    /** 수정 대상 평가 레코드가 존재하지 않아 소유권 검증을 진행할 수 없을 때 사용한다. */
    EVALUATION_NOT_FOUND(HttpStatus.NOT_FOUND, "평가를 찾을 수 없습니다."),
    /** 현재 로그인 사용자가 자기 자신에게 평가를 작성하려 할 때 사용한다. */
    EVALUATION_SELF_WRITE_FORBIDDEN(HttpStatus.FORBIDDEN, "자기 자신에게 평가는 작성할 수 없습니다."),
    /** 업무 지시 일자와 마감 일자 범위가 올바르지 않은 경우 사용한다. */
    WORKLOG_INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "업무 날짜 정보가 유효하지 않습니다."),
    /** 로그인 사용자가 대상 팀 소속이 아니어서 업무를 등록/변경할 수 없는 경우 사용한다. */
    WORKLOG_TEAM_FORBIDDEN(HttpStatus.FORBIDDEN, "해당 팀에 대한 권한이 없습니다."),
    /** 업무일지가 존재하지 않는 경우 사용한다. */
    WORKLOG_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 업무일지입니다."),
    /** 선행 업무로 지정한 worklog 가 존재하지 않거나 접근 권한이 없을 때 사용한다. 존재 여부와 권한 여부는 정보 누출 방지를 위해 단일 코드로 통합한다. */
    WORKLOG_PREDECESSOR_NOT_ACCESSIBLE(HttpStatus.FORBIDDEN, "선행 업무로 지정한 업무일지에 접근할 수 없습니다."),
    /** 대시보드 scope 와 함께 들어온 파라미터가 누락되었거나 형식이 올바르지 않을 때 사용한다. */
    DASHBOARD_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "대시보드 요청 파라미터가 올바르지 않습니다."),
    /** 사용자 role 자체로 해당 dashboard scope 를 조회할 수 없을 때 사용한다. */
    DASHBOARD_FORBIDDEN(HttpStatus.FORBIDDEN, "대시보드를 조회할 권한이 없습니다."),
    /** role 은 허용되지만 특정 부서/팀 자원에 대한 접근 권한이 없거나 자원이 없을 때 사용한다. */
    DASHBOARD_NOT_FOUND(HttpStatus.NOT_FOUND, "대시보드를 찾을 수 없습니다.");

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
