package com.ibank.axwms.domain.organization.evaluation.repository.jooq;

import com.ibank.axwms.domain.organization.evaluation.repository.jooq.projection.UserEvaluationSummaryProjection;
import com.ibank.axwms.domain.organization.evaluation.repository.jooq.query.UserEvaluationPageQuery;
import org.springframework.data.domain.Page;

public interface UserEvaluationJooqRepository {

    /** 특정 피평가자의 평가 이력을 평가자/피평가자 이름과 함께 페이지로 조회한다. */
    Page<UserEvaluationSummaryProjection> findEvaluationPage(UserEvaluationPageQuery query);
}
