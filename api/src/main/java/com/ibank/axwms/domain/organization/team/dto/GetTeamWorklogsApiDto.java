package com.ibank.axwms.domain.organization.team.dto;

import com.ibank.axwms.domain.organization.team.repository.jooq.TeamWorklogProjection;
import com.ibank.axwms.global.response.PageResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GetTeamWorklogsApiDto {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    @Schema(description = "팀 업무일지 목록 조회 요청 DTO")
    public record Request(
            @Min(1) Integer page,
            @Min(1) @Max(MAX_PAGE_SIZE) Integer pageSize,
            String sortBy,
            String sortDirection,
            String statusCode,
            String keyword
    ) {
        public int pageOrDefault() {
            return page == null ? DEFAULT_PAGE : page;
        }

        public int pageSizeOrDefault() {
            return pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;
        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Response {

        public static PageResponse<Item> fromPage(Page<TeamWorklogProjection> page) {
            return PageResponse.from(page.map(Item::from));
        }

        @Schema(description = "팀 업무일지 목록 항목")
        public record Item(
                Long worklogId,
                String title,
                String statusCode,
                Long authorUserId,
                String authorUserName,
                LocalDateTime createdAt,
                LocalDateTime updatedAt
        ) {

            public static Item from(TeamWorklogProjection projection) {
                return new Item(
                        projection.worklogId(),
                        projection.title(),
                        projection.statusCode(),
                        projection.authorUserId(),
                        projection.authorUserName(),
                        projection.createdAt(),
                        projection.updatedAt()
                );
            }
        }
    }
}
