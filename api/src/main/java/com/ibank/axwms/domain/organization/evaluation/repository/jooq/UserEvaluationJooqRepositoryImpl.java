package com.ibank.axwms.domain.organization.evaluation.repository.jooq;

import static com.ibank.axwms.global.jooq.Tables.TB_USER;
import static com.ibank.axwms.global.jooq.Tables.TB_USER_EVALUATION;

import com.ibank.axwms.domain.organization.evaluation.repository.jooq.projection.UserEvaluationSummaryProjection;
import com.ibank.axwms.domain.organization.evaluation.repository.jooq.query.UserEvaluationPageQuery;
import com.ibank.axwms.global.jooq.tables.TbUser;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.SortField;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserEvaluationJooqRepositoryImpl implements UserEvaluationJooqRepository {

    private final DSLContext dsl;

    /** 특정 피평가자 평가 이력을 created_at 기준으로 정렬하고 사용자명을 조인해 페이지로 반환한다. */
    @Override
    public Page<UserEvaluationSummaryProjection> findEvaluationPage(UserEvaluationPageQuery query) {
        int pageIndex = query.pageIndex();
        int pageSize = query.pageSize();
        PageRequest pageRequest = PageRequest.of(pageIndex, pageSize);

        long total = dsl.selectCount()
                .from(TB_USER_EVALUATION)
                .where(TB_USER_EVALUATION.EVALUATEE_USER_ID.eq(query.evaluateeUserId()))
                .fetchSingle(0, Integer.class)
                .longValue();

        if (total == 0) {
            return new PageImpl<>(List.of(), pageRequest, 0);
        }

        TbUser evaluatee = TB_USER.as("evaluatee_user");
        TbUser evaluator = TB_USER.as("evaluator_user");
        Field<String> evaluateeUserName = evaluatee.USER_NAME.as("evaluatee_user_name");
        Field<String> evaluatorUserName = evaluator.USER_NAME.as("evaluator_user_name");
        SortField<?> createdAtOrder = query.ascending()
                ? TB_USER_EVALUATION.CREATED_AT.asc()
                : TB_USER_EVALUATION.CREATED_AT.desc();

        List<UserEvaluationSummaryProjection> items = dsl.select(
                        TB_USER_EVALUATION.EVALUATION_ID,
                        TB_USER_EVALUATION.EVALUATEE_USER_ID,
                        evaluateeUserName,
                        TB_USER_EVALUATION.EVALUATOR_USER_ID,
                        evaluatorUserName,
                        TB_USER_EVALUATION.CONTENT,
                        TB_USER_EVALUATION.CREATED_AT
                )
                .from(TB_USER_EVALUATION)
                .join(evaluatee).on(evaluatee.USER_ID.eq(TB_USER_EVALUATION.EVALUATEE_USER_ID))
                .join(evaluator).on(evaluator.USER_ID.eq(TB_USER_EVALUATION.EVALUATOR_USER_ID))
                .where(TB_USER_EVALUATION.EVALUATEE_USER_ID.eq(query.evaluateeUserId()))
                .orderBy(createdAtOrder, TB_USER_EVALUATION.EVALUATION_ID.desc())
                .limit(pageSize)
                .offset((long) pageIndex * pageSize)
                .fetch(record -> UserEvaluationSummaryProjection.from(record, evaluateeUserName, evaluatorUserName));

        return new PageImpl<>(items, pageRequest, total);
    }
}
