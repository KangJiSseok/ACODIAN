package com.ibank.axwms.global.error;

import com.ibank.axwms.global.response.ApiResponse;
import com.ibank.axwms.global.response.EmptyResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 컨트롤러에서 빠져나오는 모든 예외를 클라이언트가 동일한 키로 파싱할 수 있는 ApiResponse 에러 봉투로 변환한다.
 * BusinessException 은 도메인이 의도적으로 던진 신호이므로 ErrorCode 의 status/message 를 그대로 노출한다.
 * Spring 의 요청 파싱·검증 예외는 사용자 입력 문제이므로 400 계열의 공용 코드로 정규화한다.
 * 그 외 예상치 못한 예외는 스택트레이스나 내부 메시지가 응답으로 새지 않도록 500 계열 공용 코드로 마스킹한다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<EmptyResponse>> handleBusinessException(BusinessException exception) {
        return buildErrorResponse(exception.getErrorCode());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<EmptyResponse>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException exception
    ) {
        return buildErrorResponse(ErrorCode.COMMON_INVALID_REQUEST);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class, ConstraintViolationException.class})
    public ResponseEntity<ApiResponse<EmptyResponse>> handleValidationException(Exception exception) {
        return buildErrorResponse(ErrorCode.COMMON_VALIDATION_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<EmptyResponse>> handleException(Exception exception) {
        return buildErrorResponse(ErrorCode.COMMON_INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ApiResponse<EmptyResponse>> buildErrorResponse(ErrorCode errorCode) {
        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponse.error(errorCode.toErrorResponse()));
    }
}
