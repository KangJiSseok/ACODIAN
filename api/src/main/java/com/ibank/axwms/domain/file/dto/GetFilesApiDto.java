package com.ibank.axwms.domain.file.dto;

import com.ibank.axwms.domain.file.repository.jooq.projection.FileSummaryProjection;
import com.ibank.axwms.global.enums.AiProcessingStatus;
import com.ibank.axwms.global.response.PageResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GetFilesApiDto {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    @Schema(description = "파일 목록 조회 요청 DTO")
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

        /**
         * repository projection 페이지를 API 응답 페이지로 변환하고,
         * worklogId → FileWorklogItem 매핑이 있으면 각 item 에 함께 박아준다.
         * 매핑에 없는 worklog (사용자가 못 보거나 조회 실패) 는 null 로 남는다.
         */
        public static PageResponse<Item> fromPage(Page<FileSummaryProjection> page,
                                                  Map<Long, FileWorklogItem> worklogByWorklogId) {
            return PageResponse.from(page.map(p -> Item.from(p, worklogByWorklogId.get(p.worklogId()))));
        }

        @Schema(description = "파일 목록 항목")
        public record Item(
                @Schema(description = "파일 ID", example = "101")
                Long id,
                @Schema(description = "소속 worklog ID", example = "501")
                Long worklogId,
                @Schema(description = "원본 파일명", example = "report.pdf")
                String originalName,
                @Schema(description = "스토리지 내 저장 경로(key)")
                String storedPath,
                @Schema(description = "확장자", example = "pdf")
                String fileExtension,
                @Schema(description = "파일 크기 (bytes)", example = "245678")
                Long fileSizeBytes,
                @Schema(description = "AI 요약")
                String aiSummary,
                @Schema(description = "AI 처리 상태", example = "COMPLETED")
                AiProcessingStatus aiProcessingStatus,
                @Schema(description = "업로드 시각")
                LocalDateTime createdAt,
                @Schema(description = "소속 업무 요약")
                FileWorklogItem worklog
        ) {
            public static Item from(FileSummaryProjection p, FileWorklogItem worklog) {
                return new Item(
                        p.id(),
                        p.worklogId(),
                        p.originalName(),
                        p.storedPath(),
                        p.fileExtension(),
                        p.fileSizeBytes(),
                        p.aiSummary(),
                        p.aiProcessingStatus(),
                        p.createdAt(),
                        worklog
                );
            }
        }
    }
}
