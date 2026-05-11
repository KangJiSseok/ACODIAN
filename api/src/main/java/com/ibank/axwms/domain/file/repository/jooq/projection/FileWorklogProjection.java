package com.ibank.axwms.domain.file.repository.jooq.projection;

import com.ibank.axwms.global.enums.AiProcessingStatus;
import org.jooq.Field;
import org.jooq.Record;

import java.math.BigDecimal;
import java.time.LocalDate;

import static com.ibank.axwms.global.jooq.Tables.TB_TEAM;
import static com.ibank.axwms.global.jooq.Tables.TB_USER;
import static com.ibank.axwms.global.jooq.Tables.TB_WORKLOG;

/**
 * 파일 목록의 각 행에 함께 노출되는 업무 요약 한 행.
 * 상세 페이지·업무 수정 화면이 필요로 하는 최소 필드와 선행 업무 개수만 담는다.
 */
public record FileWorklogProjection(
        Long worklogId,
        Long teamId,
        String teamName,
        Long authorId,
        String authorName,
        String title,
        String requestContent,
        String workContent,
        String aiSummary,
        AiProcessingStatus aiProcessingStatus,
        LocalDate dueDate,
        BigDecimal actualHours,
        Integer dependencyCount
) {

    public static FileWorklogProjection from(Record record, Field<Integer> dependencyCountField) {
        return new FileWorklogProjection(
                record.get(TB_WORKLOG.WORKLOG_ID),
                record.get(TB_WORKLOG.TEAM_ID),
                record.get(TB_TEAM.TEAM_NAME),
                record.get(TB_WORKLOG.AUTHOR_ID),
                record.get(TB_USER.USER_NAME),
                record.get(TB_WORKLOG.TITLE),
                record.get(TB_WORKLOG.REQUEST_CONTENT),
                record.get(TB_WORKLOG.WORK_CONTENT),
                record.get(TB_WORKLOG.AI_SUMMARY),
                AiProcessingStatus.valueOf(record.get(TB_WORKLOG.AI_PROCESSING_STATUS)),
                record.get(TB_WORKLOG.DUE_DATE),
                record.get(TB_WORKLOG.ACTUAL_HOURS),
                record.get(dependencyCountField)
        );
    }
}
