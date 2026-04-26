package com.ibank.axwms.domain.organization.team.dto;

import com.ibank.axwms.domain.organization.team.TeamStatus;
import com.ibank.axwms.domain.organization.team.repository.jooq.TeamListProjection;
import com.ibank.axwms.global.response.PageResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GetTeamsApiDto {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    @Schema(description = "팀 목록 조회 요청 DTO")
    public record Request(
            @Schema(description = "페이지 번호", example = "1")
            @Min(value = 1, message = "page 는 1 이상이어야 합니다.")
            Integer page,
            @Schema(description = "페이지 크기", example = "20")
            @Min(value = 1, message = "pageSize 는 1 이상이어야 합니다.")
            @Max(value = MAX_PAGE_SIZE, message = "pageSize 는 100 이하여야 합니다.")
            Integer pageSize,
            @Schema(description = "정렬 필드", example = "teamName")
            String sortBy,
            @Schema(description = "정렬 방향", example = "ASC")
            String sortDirection,
            @Schema(description = "부서 ID", example = "10")
            Long departmentId,
            @Schema(description = "팀 상태", example = "ACTIVE")
            TeamStatus statusCode,
            @Schema(description = "검색 키워드", example = "혁신")
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

        /** repository projection 페이지를 API 응답 페이지로 변환한다. */
        public static PageResponse<Item> fromPage(Page<TeamListProjection> page) {
            return PageResponse.from(page.map(Item::from));
        }

        @Schema(description = "팀 목록 항목")
        public record Item(
                @Schema(description = "팀 ID", example = "21")
                Long teamId,
                @Schema(description = "팀명", example = "물류혁신TF")
                String teamName,
                @Schema(description = "부서 ID", example = "10")
                Long departmentId,
                @Schema(description = "부서명", example = "물류본부")
                String departmentName,
                @Schema(description = "운영 상태", example = "ACTIVE")
                TeamStatus statusCode,
                @Schema(description = "soft-delete 시각", nullable = true)
                LocalDateTime deletedAt,
                @Schema(description = "대표 리더 사용자 ID", example = "101")
                Long leaderUserId,
                @Schema(description = "대표 리더 사용자명", example = "홍길동")
                String leaderUserName,
                @Schema(description = "활성 사용자 수", example = "8")
                Integer memberCount
        ) {

            public static Item from(TeamListProjection projection) {
                return new Item(
                        projection.teamId(),
                        projection.teamName(),
                        projection.departmentId(),
                        projection.departmentName(),
                        projection.statusCode(),
                        projection.deletedAt(),
                        projection.leaderUserId(),
                        projection.leaderUserName(),
                        projection.memberCount()
                );
            }
        }
    }
}
