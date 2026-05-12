package com.ibank.axwms.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ibank.axwms.global.error.ErrorResponse;
import java.time.Instant;
import java.util.Objects;

/**
 * 모든 API 가 공유하는 응답 봉투(envelope).
 * 클라이언트는 이 객체 한 가지 형태로만 파싱하면 되도록, 성공/실패 어느 경우든 동일한 record 를 사용한다.
 * 성공이면 data 만, 실패면 error 만 직렬화에 포함되도록 NON_NULL 을 적용해 응답 페이로드의 노이즈를 줄인다.
 * 컨트롤러는 이 객체를 직접 만들지 않고 GlobalResponseAdvice 가 단일 지점에서 래핑하도록 위임해 포맷이 갈라지지 않게 한다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        T data,
        ErrorResponse error,
        Instant timestamp
) {

    /** success/data/error 의 상호 배타 불변식을 강제해 잘못된 응답 조합을 컴파일/실행 양쪽에서 차단한다. */
    public ApiResponse {
        Objects.requireNonNull(timestamp, "응답 시각은 null일 수 없습니다.");

        if (success) {
            Objects.requireNonNull(data, "성공 응답 데이터는 null일 수 없습니다.");
            if (error != null) {
                throw new IllegalArgumentException("성공 응답의 에러 정보는 null이어야 합니다.");
            }
        } else {
            Objects.requireNonNull(error, "실패 응답의 에러 정보는 null일 수 없습니다.");
            if (data != null) {
                throw new IllegalArgumentException("실패 응답 데이터는 null이어야 합니다.");
            }
        }
    }

    /** 성공 응답을 생성한다. timestamp 는 호출 시각으로 자동 채운다. */
    public static <T> ApiResponse<T> success(T data) {
        return success(data, Instant.now());
    }

    /** 테스트 등에서 timestamp 를 결정적으로 주입해야 할 때 사용한다. */
    public static <T> ApiResponse<T> success(T data, Instant timestamp) {
        return new ApiResponse<>(true, data, null, timestamp);
    }

    /** 반환할 데이터가 없는 성공 응답. data 가 null 이면 안 되는 불변식을 만족시키기 위해 EmptyResponse 싱글톤을 사용한다. */
    public static ApiResponse<EmptyResponse> empty() {
        return success(EmptyResponse.INSTANCE);
    }

    /** 실패 응답을 생성한다. GlobalExceptionHandler 가 호출한다. */
    public static ApiResponse<EmptyResponse> error(ErrorResponse error) {
        return error(error, Instant.now());
    }

    public static ApiResponse<EmptyResponse> error(ErrorResponse error, Instant timestamp) {
        return new ApiResponse<>(false, null, error, timestamp);
    }
}
