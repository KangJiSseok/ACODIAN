package com.ibank.axwms.domain.worklog.dto;

import com.ibank.axwms.domain.tag.repository.jooq.projection.MetaTagDetailProjection;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.WorklogListProjection;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GetWorklogCreateOptionsApiDto {

    @Schema(description = "업무일지 등록 화면 진입 시 사용할 폼 옵션 응답")
    public record Response(
            @Schema(description = "사용자가 선행 업무로 지정할 수 있는 worklog 후보 (미삭제 + 미완료 + 사용자 접근 가능 팀)")
            List<PredecessorCandidate> predecessorCandidates,
            @Schema(description = "전체 메타 태그 목록")
            List<TagItem> tags
    ) {

        public static Response of(List<WorklogListProjection> worklogRows,
                                  List<MetaTagDetailProjection> tagRows) {
            return new Response(toCandidates(worklogRows), toTags(tagRows));
        }

        private static List<PredecessorCandidate> toCandidates(List<WorklogListProjection> rows) {
            return rows.stream().map(PredecessorCandidate::from).toList();
        }

        private static List<TagItem> toTags(List<MetaTagDetailProjection> rows) {
            return rows.stream().map(TagItem::from).toList();
        }
    }

    @Schema(description = "선행 업무 후보 항목")
    public record PredecessorCandidate(
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
        public static PredecessorCandidate from(WorklogListProjection projection) {
            return new PredecessorCandidate(
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

    @Schema(description = "태그 항목 (전체 필드)")
    public record TagItem(
            @Schema(description = "태그 ID", example = "1")
            Long tagId,
            @Schema(description = "태그 이름", example = "결산")
            String tagName,
            @Schema(description = "사용 횟수", example = "12")
            Integer usageCount,
            @Schema(description = "태그 생성 시각")
            LocalDateTime createdAt,
            @Schema(description = "태그 수정 시각")
            LocalDateTime updatedAt
    ) {
        public static TagItem from(MetaTagDetailProjection projection) {
            return new TagItem(
                    projection.tagId(),
                    projection.tagName(),
                    projection.usageCount(),
                    projection.createdAt(),
                    projection.updatedAt()
            );
        }
    }
}
