package com.ibank.axwms.domain.worklog.dto;

import com.ibank.axwms.domain.worklog.WorklogImportance;
import com.ibank.axwms.domain.worklog.WorklogStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UpdateWorklogApiDto {

    private static final int CONTENT_MAX_LENGTH = 10_000;

    /**
     * 업무일지 부분 수정 요청 본문 (multipart 의 JSON part).
     * 모든 필드는 nullable 이며 null 인 필드는 변경되지 않는다.
     * teamId 는 수정 불가 — 변경이 필요하면 별도 엔드포인트로 분리한다.
     * predecessorWorklogIds 의 시멘틱: null=변경 없음, []=모두 제거, [...]=전체 replace.
     * tagIds 는 수정 후 선택된 전체 태그 ID, removeTagIds 는 연결 해제할 태그 ID.
     * reason 은 statusCode 가 실제 변경될 때만 상태 이력 사유로 기록된다.
     * removeFileIds 는 기존 첨부 파일 중 삭제할 ID 들. 새 파일 추가는 multipart 의 files part 로 별도 전달.
     */
    @Schema(description = "업무일지 부분 수정 요청 (multipart JSON part). null 인 필드는 변경되지 않는다. teamId 는 수정할 수 없다.")
    public record Request(
            @Schema(description = "업무 제목", example = "4월 결산 보고서 작성 (수정)")
            @Size(min = 1, max = 200, message = "title 은 1~200자여야 합니다.")
            String title,

            @Schema(description = "업무 요청/지시 내용")
            @Size(max = CONTENT_MAX_LENGTH, message = "requestContent 는 10000자를 초과할 수 없습니다.")
            String requestContent,

            @Schema(description = "실제 수행 업무 내용")
            @Size(min = 1, max = CONTENT_MAX_LENGTH, message = "workContent 는 1~10000자여야 합니다.")
            String workContent,

            @Schema(description = "업무 상태 코드", example = "IN_PROGRESS")
            WorklogStatus statusCode,

            @Schema(description = "상태 변경 사유. statusCode 가 실제 변경될 때 상태 이력에 기록된다.")
            @Size(max = CONTENT_MAX_LENGTH, message = "reason 은 10000자를 초과할 수 없습니다.")
            String reason,

            @Schema(description = "업무 중요도 코드", example = "HIGH")
            WorklogImportance importanceCode,

            @Schema(description = "업무 스토리 포인트 (시간)", example = "3.10")
            BigDecimal actualHours,

            @Schema(description = "업무 지시 일자", example = "2026-04-22")
            LocalDate instructionDate,

            @Schema(description = "업무 마감 일자", example = "2026-04-25")
            LocalDate dueDate,

            @Schema(description = "선행 업무 ID 목록. null=변경 없음, []=모두 제거, [...]=전체 replace.")
            List<Long> predecessorWorklogIds,

            @Schema(description = "수정 후 선택된 전체 태그 ID 목록. 기존 태그와 새 태그가 함께 들어올 수 있다.")
            List<Long> tagIds,

            @Schema(description = "연결 해제할 태그 ID 목록. null/빈 리스트는 제거 없음.")
            List<Long> removeTagIds,

            @Schema(description = "사용자 편집 AI 요약. 값이 들어오면 aiSummaryEdited 가 true 로 표시된다.")
            @Size(max = CONTENT_MAX_LENGTH, message = "aiSummary 는 10000자를 초과할 수 없습니다.")
            String aiSummary,

            @Schema(description = "삭제할 기존 첨부 파일 ID 목록. null/빈 리스트는 삭제 없음. 각 fileId 는 대상 worklog 에 속해 있어야 한다.")
            List<Long> removeFileIds
    ) {
    }
}
