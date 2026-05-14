package com.ibank.axwms.domain.worklog.dto;

import com.ibank.axwms.domain.worklog.WorklogStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UpdateWorklogStatusApiDto {

    private static final int CONTENT_MAX_LENGTH = 10_000;

    /**
     * 업무 상태만 변경하는 전용 요청 본문.
     * 본문 수정 API 와 분리해 상태 전이/이력 기록만 한 트랜잭션에서 처리한다.
     */
    @Schema(description = "업무 상태 변경 요청")
    public record Request(
            @Schema(description = "변경할 업무 상태 코드", example = "COMPLETED")
            @NotNull(message = "statusCode 는 필수입니다.")
            WorklogStatus statusCode,

            @Schema(description = "상태 변경 사유. 공백이면 이력에 null 로 저장된다.")
            @Size(max = CONTENT_MAX_LENGTH, message = "reason 은 10000자를 초과할 수 없습니다.")
            String reason
    ) {
    }
}
