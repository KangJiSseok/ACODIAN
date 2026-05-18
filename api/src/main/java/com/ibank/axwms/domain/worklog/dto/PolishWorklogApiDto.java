package com.ibank.axwms.domain.worklog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PolishWorklogApiDto {

    private static final int CONTENT_MAX_LENGTH = 2000;

    /** 작성 보조는 저장 전 draft 본문만 다루므로 팀/상태 같은 저장 필드를 받지 않는다. */
    public record Request(
            @Schema(description = "업무 요청/지시 내용", example = "고객사 재고 동기화 지연 원인을 정리해 주세요.")
            @Size(max = CONTENT_MAX_LENGTH, message = "requestContent 는 2000자를 초과할 수 없습니다.")
            String requestContent,

            @Schema(description = "사용자가 초안으로 작성한 실제 수행 업무 내용", example = "배치 실행 시간을 확인하고 병목 구간을 분리했습니다.")
            @NotBlank(message = "workContent 는 비어 있을 수 없습니다.")
            @Size(max = CONTENT_MAX_LENGTH, message = "workContent 는 2000자를 초과할 수 없습니다.")
            String workContent
    ) {
    }

    /** 작성 화면이 바로 소비할 수 있도록 AI가 다듬은 본문만 노출한다. */
    public record Response(
            @Schema(description = "AI가 다듬은 업무 수행 내용", example = "배치 실행 시간을 점검하고 병목 구간을 분리했습니다.")
            String workContent
    ) {

        /** AI가 다듬은 본문 값을 작성 화면 응답 계약으로 감싼다. */
        public static Response of(String workContent) {
            return new Response(workContent);
        }
    }
}
