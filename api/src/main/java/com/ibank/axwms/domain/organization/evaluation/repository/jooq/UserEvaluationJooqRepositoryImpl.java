package com.ibank.axwms.domain.organization.evaluation.repository.jooq;

import static com.ibank.axwms.global.jooq.Tables.TB_USER;
import static com.ibank.axwms.global.jooq.Tables.TB_USER_EVALUATION;

import com.ibank.axwms.domain.organization.evaluation.repository.jooq.projection.UserEvaluationSummaryProjection;
import com.ibank.axwms.domain.organization.evaluation.repository.jooq.query.UserEvaluationPageQuery;
import com.ibank.axwms.global.jooq.tables.TbUser;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.OrderField;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class UserEvaluationJooqRepositoryImpl implements UserEvaluationJooqRepository {

    private static final String SORT_DIRECTION_ASC = "ASC";

    private final DSLContext dsl;

    /** 사용자 평가 이력 페이지를 spec projection/정렬/예외 규칙에 맞춰 조회한다. */
    @Override
    public Page<UserEvaluationSummaryProjection> findUserEvaluationPage(UserEvaluationPageQuery query) {
        TbUser evaluatee = TB_USER.as("evaluatee");
        TbUser evaluator = TB_USER.as("evaluator");

        //피평가자 조회 조건문
        Condition visibilityCondition = visibilityCondition(query);

        long totalCount = dsl.selectCount()
                .from(TB_USER_EVALUATION)
                .where(visibilityCondition)
                .fetchSingle(0, Integer.class)
                .longValue();

        List<UserEvaluationSummaryProjection> items = dsl.select(
                        TB_USER_EVALUATION.EVALUATION_ID,
                        TB_USER_EVALUATION.EVALUATEE_USER_ID,
                        evaluatee.USER_NAME,
                        TB_USER_EVALUATION.EVALUATOR_USER_ID,
                        evaluator.USER_NAME,
                        TB_USER_EVALUATION.CONTENT,
                        TB_USER_EVALUATION.CREATED_AT
                )
                .from(TB_USER_EVALUATION)
                .join(evaluatee).on(TB_USER_EVALUATION.EVALUATEE_USER_ID.eq(evaluatee.USER_ID))
                .join(evaluator).on(TB_USER_EVALUATION.EVALUATOR_USER_ID.eq(evaluator.USER_ID))
                .where(visibilityCondition)
                .orderBy(orderFields(query))
                .limit(query.pageSize())
                .offset((query.page() - 1) * query.pageSize())
                .fetch(record -> UserEvaluationSummaryProjection.from(record, evaluatee, evaluator));

        return new PageImpl<>(items, PageRequest.of(query.page() - 1, query.pageSize()), totalCount);
    }

    /** count/items 쿼리가 동일한 visibility predicate 를 재사용하도록 평가 대상 조건을 한곳에 모은다. */
    private Condition visibilityCondition(UserEvaluationPageQuery query) {
        return TB_USER_EVALUATION.EVALUATEE_USER_ID.eq(query.evaluateeUserId());
    }

    /** 서버는 createdAt 기준 정렬만 허용하고, 항상 evaluationId DESC tie-breaker 를 추가한다. */
    private List<OrderField<?>> orderFields(UserEvaluationPageQuery query) {
        List<OrderField<?>> orderFields = new ArrayList<>();
        boolean ascending = SORT_DIRECTION_ASC.equals(query.sortDirection());
        if (ascending) {
            orderFields.add(TB_USER_EVALUATION.CREATED_AT.asc());
        } else {
            orderFields.add(TB_USER_EVALUATION.CREATED_AT.desc());
        }
        orderFields.add(TB_USER_EVALUATION.EVALUATION_ID.desc());
        return orderFields;
    }
}
