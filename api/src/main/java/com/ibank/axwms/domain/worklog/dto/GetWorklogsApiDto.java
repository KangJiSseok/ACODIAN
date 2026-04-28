package com.ibank.axwms.domain.worklog.dto;

import com.ibank.axwms.domain.worklog.repository.jooq.projection.WorklogListProjection;
import com.ibank.axwms.global.response.PageResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GetWorklogsApiDto {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    @Schema(description = "업무 목록 조회 요청 DTO")
    public record Request(
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
        public static PageResponse<Item> fromPage(Page<WorklogListProjection> page) {
            return PageResponse.from(page.map(Item::from));
        }

        @Schema(description = "업무 목록 항목")
        public record Item(
                @Schema(description = "업무 ID", example = "101")
                Long worklogId,
                @Schema(description = "업무 제목", example = "4월 결산 보고서 작성")
                String title,
                @Schema(description = "업무 상태 코드", example = "IN_PROGRESS")
                String statusCode,
                @Schema(description = "실제 수행 업무 내용")
                String workContent,
                @Schema(description = "업무 스토리 포인트", example = "3.1")
                BigDecimal actualHours,
                @Schema(description = "업무 중요도 코드", example = "HIGH")
                String importanceCode,
                @Schema(description = "AI 요약")
                String aiSummary,
                @Schema(description = "AI 처리 상태", example = "COMPLETED")
                String aiProcessingStatus,
                @Schema(description = "AI 요약을 사용자가 직접 편집했는지 여부", example = "false")
                Boolean aiSummaryEdited,
                @Schema(description = "소속 팀 ID", example = "21")
                Long teamId,
                @Schema(description = "소속 팀명", example = "물류혁신TF")
                String teamName,
                @Schema(description = "작성자 사용자 ID", example = "101")
                Long authorId,
                @Schema(description = "작성자 사용자명", example = "홍길동")
                String authorName,
                @Schema(description = "업무 지시 일자", example = "2026-04-22")
                LocalDate instructionDate,
                @Schema(description = "업무 마감 일자", example = "2026-04-25")
                LocalDate dueDate,
                @Schema(description = "선행 업무 개수", example = "2")
                Integer predecessorCount
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
                        projection.dueDate(),
                        projection.predecessorCount()
                );
            }
        }
    }
}
