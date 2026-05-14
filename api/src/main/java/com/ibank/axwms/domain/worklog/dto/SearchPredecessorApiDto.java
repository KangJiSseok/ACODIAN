package com.ibank.axwms.domain.worklog.dto;

import com.ibank.axwms.domain.worklog.repository.jooq.projection.WorklogListProjection;
import com.ibank.axwms.global.response.PageResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.LocalDate;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SearchPredecessorApiDto {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_QUERY_LENGTH = 100;

    @Schema(description = "선행 업무 후보 검색 요청 DTO")
    public record Request(
            @Schema(description = "선행 후보를 좁힐 팀 ID. 요청자는 해당 팀의 ACTIVE 멤버여야 한다.", example = "21")
            @NotNull(message = "teamId 는 필수입니다.")
            Long teamId,
            @Schema(description = "업무 제목 LIKE 검색어", example = "결산")
            @Size(max = MAX_QUERY_LENGTH, message = "query 길이는 100자 이하여야 합니다.")
            String query,
            @Schema(description = "후보에서 제외할 worklog ID (수정 화면에서 자기 자신 제외용)", example = "501")
            Long excludeWorklogId,
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

        public static PageResponse<Item> fromPage(Page<WorklogListProjection> page) {
            return PageResponse.from(page.map(Item::from));
        }

        @Schema(description = "선행 업무 후보 항목")
        public record Item(
                @Schema(description = "업무 ID", example = "501")
                Long worklogId,
                @Schema(description = "업무 제목", example = "4월 결산 보고서 작성")
                String title,
                @Schema(description = "업무 상태 코드", example = "IN_PROGRESS")
                String statusCode,
                @Schema(description = "업무 내용 본문", example = "재무 데이터를 집계해 보고서 초안을 작성한다.")
                String workContent,
                @Schema(description = "업무 스토리 포인트 (시간)", example = "3.10")
                BigDecimal actualHours,
                @Schema(description = "중요도 코드", example = "HIGH")
                String importanceCode,
                @Schema(description = "AI 요약", example = "재무팀 요청 결산 보고서.")
                String aiSummary,
                @Schema(description = "AI 처리 상태", example = "COMPLETED")
                String aiProcessingStatus,
                @Schema(description = "AI 요약 사용자 편집 여부", example = "false")
                Boolean aiSummaryEdited,
                @Schema(description = "팀 ID", example = "21")
                Long teamId,
                @Schema(description = "팀명", example = "물류혁신TF")
                String teamName,
                @Schema(description = "작성자 ID", example = "101")
                Long authorId,
                @Schema(description = "작성자명", example = "홍길동")
                String authorName,
                @Schema(description = "지시 일자", example = "2026-04-22")
                LocalDate instructionDate,
                @Schema(description = "마감 일자", example = "2026-04-25")
                LocalDate dueDate
        ) {

            public static Item from(WorklogListProjection projection) {
                return new Item(
                        projection.worklogId(),
                        projection.title(),
                        projection.statusCode(),
                        projection.workContent(),
                        projection.actualHours(),
                        projection.importanceCode(),
                        projection.aiSummary(),
                        projection.aiProcessingStatus(),
                        projection.aiSummaryEdited(),
                        projection.teamId(),
                        projection.teamName(),
                        projection.authorId(),
                        projection.authorName(),
                        projection.instructionDate(),
                        projection.dueDate()
                );
            }
        }
    }
}
