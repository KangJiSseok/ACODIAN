package com.ibank.axwms.domain.worklog.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ibank.axwms.domain.organization.team.TeamStatus;
import com.ibank.axwms.domain.worklog.WorklogImportance;
import com.ibank.axwms.domain.worklog.WorklogStatus;
import com.ibank.axwms.domain.worklog.repository.jooq.query.WorklogSearchQuery;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class InternalSemanticWorklogSearchApiDto {

    /**
     * API 서버가 권한/필터 정규화를 끝낸 뒤 AI 서버에 전달하는 내부 검색 요청이다.
     */
    public record Request(
            String keyword,
            Long teamId,
            TeamStatus teamStatus,
            WorklogStatus statusCode,
            WorklogImportance importanceCode,
            Long authorId,
            Long tagId,
            LocalDate createdFrom,
            List<Long> allowedTeamIds,
            int page,
            int pageSize
    ) {

        /** API 검색 query 와 권한 계산 결과를 AI 서버의 내부 검색 요청으로 변환한다. */
        public static Request from(WorklogSearchQuery query, List<Long> allowedTeamIds) {
            return new Request(
                    query.keyword(),
                    query.teamId(),
                    query.teamStatus(),
                    query.statusCode(),
                    query.importanceCode(),
                    query.authorId(),
                    query.tagId(),
                    query.createdFrom(),
                    allowedTeamIds == null ? null : List.copyOf(allowedTeamIds),
                    query.page(),
                    query.pageSize()
            );
        }
    }

    /**
     * AI 서버가 의미 유사도 ranking 과 페이지 메타데이터만 반환하는 내부 검색 응답이다.
     */
    public record Response(
            List<Item> items,
            int page,
            int pageSize,
            long totalCount,
            int totalPages,
            @JsonProperty("isFirst")
            boolean isFirst,
            @JsonProperty("isLast")
            boolean isLast,
            boolean hasNext,
            boolean hasPrevious
    ) {
    }

    /**
     * API 서버가 최신 업무 projection 을 재조회할 수 있게 ranking 근거와 업무 ID 를 담는다.
     */
    public record Item(
            Long worklogId,
            double score,
            int chunkIndex,
            String matchedChunk,
            List<Long> predecessorWorklogIds
    ) {
    }
}
