package com.ibank.axwms.domain.worklog.repository.jooq.projection;

import static com.ibank.axwms.global.jooq.Tables.TB_TEAM;
import static com.ibank.axwms.global.jooq.Tables.TB_USER;
import static com.ibank.axwms.global.jooq.Tables.TB_WORKLOG;

import java.time.LocalDate;
import org.jooq.Field;
import org.jooq.Record;

/** 업무일지 검색 결과 한 행에 필요한 필드만 담는 읽기 전용 projection 이다. */
public record WorklogSearchProjection(
        Long worklogId,
        String title,
        String aiSummary,
        String statusCode,
        String importanceCode,
        String aiProcessingStatus,
        Integer predecessorCount,
        Long teamId,
        String teamName,
        Long authorId,
        String authorName,
        String profileImageUrl,
        LocalDate instructionDate,
        LocalDate dueDate
) {

    /** JOOQ 조회 결과 record 를 팀 상세 projection 으로 조립한다. */
    public static WorklogSearchProjection from(Record record, Field<Integer> predecessorCountField) {
        return new WorklogSearchProjection(
                record.get(TB_WORKLOG.WORKLOG_ID),
                record.get(TB_WORKLOG.TITLE),
                record.get(TB_WORKLOG.AI_SUMMARY),
                record.get(TB_WORKLOG.STATUS_CODE),
                record.get(TB_WORKLOG.IMPORTANCE_CODE),
                record.get(TB_WORKLOG.AI_PROCESSING_STATUS),
                record.get(predecessorCountField),
                record.get(TB_WORKLOG.TEAM_ID),
                record.get(TB_TEAM.TEAM_NAME),
                record.get(TB_WORKLOG.AUTHOR_ID),
                record.get(TB_USER.USER_NAME),
                record.get(TB_USER.PROFILE_IMAGE_URL),
                record.get(TB_WORKLOG.INSTRUCTION_DATE),
                record.get(TB_WORKLOG.DUE_DATE)
        );
    }
}
