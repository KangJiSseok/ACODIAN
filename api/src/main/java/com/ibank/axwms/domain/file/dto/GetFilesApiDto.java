package com.ibank.axwms.domain.file.dto;

import com.ibank.axwms.domain.file.FileType;
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
            Integer pageSize,
            @Schema(description = "파일 형식 필터. 생략하면 형식 조건을 적용하지 않는다.", example = "PDF")
            FileType fileType,
            @Schema(description = "파일 등록 시각 기준 최근 N일 기간 필터. 생략하면 기간 조건을 적용하지 않는다.", example = "30")
            Integer period
    ) {
        /**
         * 목록 API 의 1-indexed 기본 페이지 계약을 service/repository 경계에 일관되게 제공한다.
         */
        public int pageOrDefault() {
            return page == null ? DEFAULT_PAGE : page;
        }

        /**
         * 요청이 크기를 생략하면 공통 페이지 기본 크기를 적용해 기존 조회 계약을 유지한다.
         */
        public int pageSizeOrDefault() {
            return pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;
        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Response {

        /**
         * service 가 공개 URL 까지 확정해 조립한 파일 항목 페이지를 공통 페이지 응답으로 감싼다.
         */
        public static PageResponse<Item> fromPage(Page<Item> page) {
            return PageResponse.from(page);
        }

        @Schema(description = "파일 목록 항목")
        public record Item(
                @Schema(description = "파일 ID", example = "101")
                Long id,
                @Schema(description = "소속 worklog ID", example = "501")
                Long worklogId,
                @Schema(description = "원본 파일명", example = "report.pdf")
                String originalName,
                @Schema(description = "파일 공개 접근 URL", example = "https://cdn.example.com/worklog/2026/05/12/report.pdf")
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
            /**
             * repository 조회 shape 와 service 가 계산한 공개 URL 을 결합해 파일 목록 응답 항목을 만든다.
             */
            public static Item from(FileSummaryProjection p,
                                    FileWorklogItem worklog,
                                    String storedPath) {
                return new Item(
                        p.id(),
                        p.worklogId(),
                        p.originalName(),
                        storedPath,
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
