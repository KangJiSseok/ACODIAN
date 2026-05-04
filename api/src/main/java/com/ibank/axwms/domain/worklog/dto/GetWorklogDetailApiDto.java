package com.ibank.axwms.domain.worklog.dto;

import com.ibank.axwms.domain.worklog.repository.jooq.projection.WorklogDependencyProjection;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.WorklogDetailProjection;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.WorklogFileProjection;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.WorklogStatusHistoryProjection;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GetWorklogDetailApiDto {

    @Schema(description = "업무 상세 응답")
    public record Response(
            @Schema(description = "업무 ID", example = "501")
            Long worklogId,
            @Schema(description = "소속 팀 ID", example = "21")
            Long teamId,
            @Schema(description = "소속 팀명", example = "물류혁신TF")
            String teamName,
            @Schema(description = "작성자 사용자 ID", example = "101")
            Long authorId,
            @Schema(description = "작성자 사용자명", example = "홍길동")
            String authorName,
            @Schema(description = "업무 제목")
            String title,
            @Schema(description = "업무 요청/지시 내용")
            String requestContent,
            @Schema(description = "실제 수행 업무 내용")
            String workContent,
            @Schema(description = "업무 상태 코드", example = "IN_PROGRESS")
            String statusCode,
            @Schema(description = "실제 소요 시간(시간.분)", example = "3.10")
            BigDecimal actualHours,
            @Schema(description = "업무 지시 일자")
            LocalDate instructionDate,
            @Schema(description = "업무 마감 일자")
            LocalDate dueDate,
            @Schema(description = "첨부 파일 목록")
            List<FileItem> files,
            @Schema(description = "태그 이름 목록", example = "[\"결산\", \"보고서\"]")
            List<String> tags,
            @Schema(description = "선행 업무 목록 (직접 연결된 1단계)")
            List<DependencyItem> dependOnWorklogs,
            @Schema(description = "상태 변경 이력 (시간 오름차순)")
            List<StatusHistoryItem> statusHistories
    ) {

        public static Response of(
                WorklogDetailProjection detail,
                List<WorklogFileProjection> files,
                List<String> tags,
                List<WorklogDependencyProjection> dependencies,
                List<WorklogStatusHistoryProjection> statusHistories
        ) {
            return new Response(
                    detail.worklogId(),
                    detail.teamId(),
                    detail.teamName(),
                    detail.authorId(),
                    detail.authorName(),
                    detail.title(),
                    detail.requestContent(),
                    detail.workContent(),
                    detail.statusCode(),
                    detail.actualHours(),
                    detail.instructionDate(),
                    detail.dueDate(),
                    files.stream().map(FileItem::from).toList(),
                    tags,
                    dependencies.stream().map(DependencyItem::from).toList(),
                    statusHistories.stream().map(StatusHistoryItem::from).toList()
            );
        }

        @Schema(description = "첨부 파일 항목")
        public record FileItem(
                @Schema(description = "파일 ID", example = "9001")
                Long fileId,
                @Schema(description = "원본 파일명", example = "report.pdf")
                String originalName,
                @Schema(description = "저장 경로", example = "/uploads/2026/04/abc.pdf")
                String storedPath,
                @Schema(description = "파일 확장자", example = "pdf")
                String fileExtension,
                @Schema(description = "파일 크기(byte)", example = "204800")
                Long fileSizeBytes
        ) {
            public static FileItem from(WorklogFileProjection p) {
                return new FileItem(p.fileId(), p.originalName(), p.storedPath(), p.fileExtension(), p.fileSizeBytes());
            }
        }

        @Schema(description = "선행 업무 항목")
        public record DependencyItem(
                @Schema(description = "선행 업무 ID", example = "412")
                Long worklogId,
                @Schema(description = "선행 업무 제목")
                String title,
                @Schema(description = "선행 업무 상태 코드", example = "COMPLETED")
                String statusCode
        ) {
            public static DependencyItem from(WorklogDependencyProjection p) {
                return new DependencyItem(p.worklogId(), p.title(), p.statusCode());
            }
        }

        @Schema(description = "상태 변경 이력 항목")
        public record StatusHistoryItem(
                @Schema(description = "이력 ID", example = "7001")
                Long historyId,
                @Schema(description = "이전 상태 코드", example = "PENDING")
                String previousStatusCode,
                @Schema(description = "변경 후 상태 코드", example = "IN_PROGRESS")
                String newStatusCode,
                @Schema(description = "변경 사유")
                String reason,
                @Schema(description = "변경 시각")
                LocalDateTime changedAt,
                @Schema(description = "변경자 사용자 ID", example = "101")
                Long changedBy,
                @Schema(description = "변경자 사용자명", example = "홍길동")
                String changedByName
        ) {
            public static StatusHistoryItem from(WorklogStatusHistoryProjection p) {
                return new StatusHistoryItem(
                        p.historyId(),
                        p.previousStatusCode(),
                        p.newStatusCode(),
                        p.reason(),
                        p.changedAt(),
                        p.changedBy(),
                        p.changedByName()
                );
            }
        }
    }
}
