package com.ibank.axwms.domain.worklog.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class InternalGraphRagWorklogQueryApiDto {

    public enum ScopeMode {
        TEAM_IDS,
        ALL
    }

    /**
     * API 서버가 계산한 권한 범위를 명시적 scopeMode로 고정해 GraphRAG v1 query API에 전달한다.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Request(
            String query,
            ScopeMode scopeMode,
            List<Long> allowedTeamIds
    ) {

        /** 팀 제한 범위는 비어 있으면 AI 호출 전에 API에서 빈 응답으로 끝나야 한다. */
        public static Request forTeamIds(String query, List<Long> teamIds) {
            Objects.requireNonNull(teamIds, "teamIds must not be null");
            if (teamIds.isEmpty()) {
                throw new IllegalArgumentException("teamIds must not be empty");
            }
            return new Request(query, ScopeMode.TEAM_IDS, List.copyOf(teamIds));
        }

        /** 전체 범위는 null 목록이 아니라 scopeMode=ALL로만 표현한다. */
        public static Request forAll(String query) {
            return new Request(query, ScopeMode.ALL, null);
        }
    }

    /**
     * GraphRAG v1 query API가 반환하는 native answer와 reference 목록이다.
     */
    public record Response(
            String answer,
            List<ReferenceItem> references,
            String mode,
            boolean internalOnly
    ) {
    }

    /**
     * GraphRAG reference에서 공개 API로 전달할 provenance 항목이다.
     */
    public record ReferenceItem(
            String referenceId,
            String filePath
    ) {
    }
}
