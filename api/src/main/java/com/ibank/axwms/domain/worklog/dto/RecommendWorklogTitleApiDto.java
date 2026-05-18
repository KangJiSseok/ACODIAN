package com.ibank.axwms.domain.worklog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RecommendWorklogTitleApiDto {

    private static final int CONTENT_MAX_LENGTH = 2000;
    private static final int TITLE_MAX_COUNT = 3;

    /** 제목 추천은 저장 전 draft 본문만 다루므로 기존 작성 보조와 같은 입력 경계만 받는다. */
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

    /** 작성 화면이 후보를 바로 렌더링할 수 있도록 최대 3개의 제목만 노출한다. */
    public record Response(
            @Schema(description = "AI가 추천한 업무일지 제목 후보 목록", example = "[\"배치 병목 구간 분석\"]")
            List<String> titles
    ) {

        public Response {
            titles = normalizeTitles(titles);
        }

        /** AI 응답 후보에서 공백 값을 제거하고 화면 계약의 최대 개수만 보존한다. */
        public static Response of(List<String> titles) {
            return new Response(titles);
        }

        /** Upstream 응답을 신뢰하더라도 public API 경계에서 빈 제목과 초과 후보를 한 번 더 차단한다. */
        private static List<String> normalizeTitles(List<String> titles) {
            if (titles == null) {
                return List.of();
            }
            return titles.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(title -> !title.isBlank())
                    .limit(TITLE_MAX_COUNT)
                    .toList();
        }
    }
}
