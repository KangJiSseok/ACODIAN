package com.ibank.axwms.domain.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MarkAllNotificationsReadApiDto {

    @Schema(description = "내 안읽은 알림 전체 읽음 처리 응답 DTO")
    public record Response(
            @Schema(description = "이번 요청으로 읽음 처리된 알림 수", example = "5")
            int updatedCount
    ) {

        /** 변경된 행 수를 클라이언트가 배지 갱신에 사용할 수 있는 응답으로 감싼다. */
        public static Response of(int updatedCount) {
            return new Response(updatedCount);
        }
    }
}
