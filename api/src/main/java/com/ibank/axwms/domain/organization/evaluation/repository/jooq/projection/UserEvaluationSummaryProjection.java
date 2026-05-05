package com.ibank.axwms.domain.organization.evaluation.repository.jooq.projection;

import static com.ibank.axwms.global.jooq.Tables.TB_USER_EVALUATION;

import java.time.LocalDateTime;
import org.jooq.Field;
import org.jooq.Record;

public record UserEvaluationSummaryProjection(
        Long evaluationId,
        Long evaluateeUserId,
        String evaluateeUserName,
        Long evaluatorUserId,
        String evaluatorUserName,
        String content,
        LocalDateTime createdAt
) {

    /** 쿼리에서 만든 사용자명 alias Field 를 명시적으로 받아 문자열 alias drift 없이 조회 결과를 조립한다. */
    public static UserEvaluationSummaryProjection from(
            Record record,
            Field<String> evaluateeUserName,
            Field<String> evaluatorUserName
    ) {
        return new UserEvaluationSummaryProjection(
                record.get(TB_USER_EVALUATION.EVALUATION_ID),
                record.get(TB_USER_EVALUATION.EVALUATEE_USER_ID),
                record.get(evaluateeUserName),
                record.get(TB_USER_EVALUATION.EVALUATOR_USER_ID),
                record.get(evaluatorUserName),
                record.get(TB_USER_EVALUATION.CONTENT),
                record.get(TB_USER_EVALUATION.CREATED_AT)
        );
    }
}
