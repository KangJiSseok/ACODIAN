package com.ibank.axwms.domain.worklog.dto;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class InternalLightRagWorklogQueryApiDto {

    /**
     * API 서버가 권한 범위를 계산한 뒤 LightRAG v3 query API에 전달하는 내부 요청이다.
     */
    public record Request(
            String query,
            List<Long> allowedTeamIds
    ) {

        /** 공개 semantic 요청의 query와 권한 계산 결과를 LightRAG v3 요청 계약으로 고정한다. */
        public static Request of(String query, List<Long> allowedTeamIds) {
            return new Request(
                    query,
                    allowedTeamIds == null ? null : List.copyOf(allowedTeamIds)
            );
        }
    }

    /**
     * LightRAG v3 query API가 반환하는 native answer와 reference 목록이다.
     */
    public record Response(
            String answer,
            List<ReferenceItem> references,
            String mode,
            boolean internalOnly
    ) {
    }

    /**
     * LightRAG reference에서 공개 API로 전달할 provenance 항목이다.
     */
    public record ReferenceItem(
            String referenceId,
            String filePath
    ) {
    }
}
