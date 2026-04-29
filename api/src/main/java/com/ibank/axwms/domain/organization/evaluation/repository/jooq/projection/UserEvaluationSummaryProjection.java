package com.ibank.axwms.domain.organization.evaluation.repository.jooq.projection;

import com.ibank.axwms.global.jooq.Tables;
import com.ibank.axwms.global.jooq.tables.TbUser;
import java.time.LocalDateTime;
import org.jooq.Record;

/** 평가 이력 조회 화면에 필요한 read-only projection 이다. */
public record UserEvaluationSummaryProjection(
        Long evaluationId,
        Long evaluateeUserId,
        String evaluateeUserName,
        Long evaluatorUserId,
        String evaluatorUserName,
        String content,
        LocalDateTime createdAt
) {

    /** jOOQ 조회 row 하나를 평가 요약 projection 으로 명시 매핑한다. */
    public static UserEvaluationSummaryProjection from(Record record, TbUser evaluatee, TbUser evaluator) {
        return new UserEvaluationSummaryProjection(
                record.get(Tables.TB_USER_EVALUATION.EVALUATION_ID),
                record.get(Tables.TB_USER_EVALUATION.EVALUATEE_USER_ID),
                record.get(evaluatee.USER_NAME),
                record.get(Tables.TB_USER_EVALUATION.EVALUATOR_USER_ID),
                record.get(evaluator.USER_NAME),
                record.get(Tables.TB_USER_EVALUATION.CONTENT),
                record.get(Tables.TB_USER_EVALUATION.CREATED_AT)
        );
    }
}
