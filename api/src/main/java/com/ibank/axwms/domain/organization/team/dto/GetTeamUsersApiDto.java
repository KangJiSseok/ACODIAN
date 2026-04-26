package com.ibank.axwms.domain.organization.team.dto;

import com.ibank.axwms.domain.organization.team.repository.jooq.projection.TeamUserProjection;
import com.ibank.axwms.global.response.PageResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GetTeamUsersApiDto {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    @Schema(description = "팀 사용자 목록 조회 요청 DTO")
    public record Request(
            @Min(1) Integer page,
            @Min(1) @Max(MAX_PAGE_SIZE) Integer pageSize
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

        public static PageResponse<Item> fromPage(Page<TeamUserProjection> page) {
            return PageResponse.from(page.map(Item::from));
        }

        @Schema(description = "팀 사용자 목록 항목")
        public record Item(
                Boolean teamLeader,
                Long userId,
                String userName,
                String positionName,
                String email,
                String teamRole,
                String allocation
        ) {

            public static Item from(TeamUserProjection projection) {
                return new Item(
                        projection.teamLeader(),
                        projection.userId(),
                        projection.userName(),
                        projection.positionName(),
                        projection.email(),
                        projection.teamRole(),
                        projection.allocation()
                );
            }
        }
    }
}
