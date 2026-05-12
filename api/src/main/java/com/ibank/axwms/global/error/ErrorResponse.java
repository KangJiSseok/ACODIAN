package com.ibank.axwms.global.error;

/**
 * 에러 응답 본문. ApiResponse 의 {@code error} 필드로 직렬화된다.
 * 클라이언트가 어떤 API 에서도 동일한 키({@code code}, {@code message}, {@code statusCode}) 로 파싱할 수 있도록
 * 모든 도메인이 공유하는 고정 포맷을 강제한다.
 */
public record ErrorResponse(
        String code,
        String message,
        int statusCode
) {
}
