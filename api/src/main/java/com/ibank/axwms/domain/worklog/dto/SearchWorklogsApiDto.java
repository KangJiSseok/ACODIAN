package com.ibank.axwms.domain.worklog.dto;

import com.ibank.axwms.domain.organization.team.TeamStatus;
import com.ibank.axwms.domain.worklog.WorklogImportance;
import com.ibank.axwms.domain.worklog.WorklogStatus;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.WorklogSearchProjection;
import com.ibank.axwms.global.enums.PeriodOption;
import com.ibank.axwms.global.response.PageResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SearchWorklogsApiDto {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_KEYWORD_LENGTH = 100;

    @Schema(description = "업무일지 검색 요청 DTO")
    public record Request(
            @Schema(description = "업무 제목 LIKE 검색 키워드", example = "결산")
            @Size(max = MAX_KEYWORD_LENGTH, message = "keyword 길이는 100자 이하여야 합니다.")
            String keyword,
            @Schema(description = "팀 ID 필터 (단일). 가시 범위와 교집합으로 적용된다.")
            Long teamId,
            @Schema(description = "팀 상태 필터 (단일). ACTIVE / INACTIVE.", example = "ACTIVE")
            TeamStatus teamStatus,
            @Schema(description = "상태 코드 필터 (단일)")
            WorklogStatus statusCode,
            @Schema(description = "중요도 코드 필터 (단일)")
            WorklogImportance importanceCode,
            @Schema(description = "작성자 사용자 ID 필터 (단일)")
            Long authorId,
            @Schema(description = "태그 ID 필터 (단일)")
            Long tagId,
            @Schema(description = "기간 필터 (created_at 기준). 미지정 시 전체 기간.", example = "LAST_30")
            PeriodOption period,
            @Schema(description = "페이지 번호", example = "1")
            @Min(value = 1, message = "page 는 1 이상이어야 합니다.")
            Integer page,
            @Schema(description = "페이지 크기", example = "20")
            @Min(value = 1, message = "pageSize 는 1 이상이어야 합니다.")
            @Max(value = MAX_PAGE_SIZE, message = "pageSize 는 100 이하여야 합니다.")
            Integer pageSize
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
        public static PageResponse<Item> fromPage(Page<WorklogSearchProjection> page) {
            return PageResponse.from(page.map(Item::from));
        }

        @Schema(description = "검색된 업무일지 항목")
        public record Item(
                @Schema(description = "업무 ID", example = "101")
                Long worklogId,
                @Schema(description = "업무 제목", example = "4월 결산 보고서 작성")
                String title,
                @Schema(description = "AI 요약")
                String aiSummary,
                @Schema(description = "업무 상태 코드", example = "IN_PROGRESS")
                String statusCode,
                @Schema(description = "업무 중요도 코드", example = "HIGH")
                String importanceCode,
                @Schema(description = "AI 처리 상태", example = "COMPLETED")
                String aiProcessingStatus,
                @Schema(description = "선행 업무 개수", example = "2")
                Integer predecessorCount,
                @Schema(description = "소속 팀 ID", example = "21")
                Long teamId,
                @Schema(description = "소속 팀명", example = "물류혁신TF")
                String teamName,
                @Schema(description = "작성자 사용자 ID", example = "101")
                Long authorId,
                @Schema(description = "작성자 사용자명", example = "홍길동")
                String authorName,
                @Schema(description = "작성자 프로필 이미지", example = "https://이미지경로")
                String profileImageUrl,
                @Schema(description = "업무 지시 일자", example = "2026-04-22")
                LocalDate instructionDate,
                @Schema(description = "업무 마감 일자", example = "2026-04-25")
                LocalDate dueDate
        ) {

            public static Item from(WorklogSearchProjection projection) {
                return new Item(
                        projection.worklogId(),
                        projection.title(),
                        projection.aiSummary(),
                        projection.statusCode(),
                        projection.importanceCode(),
                        projection.aiProcessingStatus(),
                        projection.predecessorCount(),
                        projection.teamId(),
                        projection.teamName(),
                        projection.authorId(),
                        projection.authorName(),
                        projection.profileImageUrl(),
                        projection.instructionDate(),
                        projection.dueDate()
                );
            }
        }
    }
}
