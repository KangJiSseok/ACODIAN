package com.ibank.axwms.domain.worklog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SearchSemanticWorklogsApiDto {

    private static final int MAX_QUERY_LENGTH = 100;

    @Schema(description = "업무일지 시맨틱 검색 요청 DTO")
    public record Request(
            @Schema(description = "LightRAG에 전달할 자연어 검색어", example = "결산 보고서와 관련된 최근 업무 찾아줘")
            @NotBlank(message = "query는 필수입니다.")
            @Size(max = MAX_QUERY_LENGTH, message = "query 길이는 100자 이하여야 합니다.")
            String query
    ) {
    }

    @Schema(description = "업무일지 시맨틱 검색 응답 DTO")
    public record Response(
            @Schema(description = "LightRAG가 생성한 한국어 답변")
            String answer,
            @Schema(description = "LightRAG가 반환한 업무일지 reference 목록")
            List<Reference> references
    ) {

        /** 내부 LightRAG 응답에서 public API에 노출할 필드만 유지한다. */
        public static Response from(InternalLightRagWorklogQueryApiDto.Response response) {
            List<Reference> references = response.references() == null
                    ? List.of()
                    : response.references().stream()
                    .map(Reference::from)
                    .toList();
            return new Response(response.answer(), references);
        }
    }

    @Schema(description = "LightRAG 업무일지 reference")
    public record Reference(
            @Schema(description = "LightRAG reference ID", example = "ref-1")
            String referenceId,
            @Schema(description = "업무일지 provenance file path", example = "worklog://501")
            String filePath
    ) {

        /** 내부 LightRAG reference를 public API reference로 변환한다. */
        public static Reference from(InternalLightRagWorklogQueryApiDto.ReferenceItem reference) {
            return new Reference(reference.referenceId(), reference.filePath());
        }
    }
}
