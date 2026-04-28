package com.ibank.axwms.domain.organization.evaluation.repository.jooq;

import com.ibank.axwms.domain.organization.evaluation.repository.jooq.projection.UserEvaluationSummaryProjection;
import com.ibank.axwms.domain.organization.evaluation.repository.jooq.query.UserEvaluationPageQuery;
import org.springframework.data.domain.Page;

public interface UserEvaluationJooqRepository {

    /** 특정 사용자의 평가 이력 페이지를 visibility/sort contract 에 맞춰 조회한다. */
    Page<UserEvaluationSummaryProjection> findUserEvaluationPage(UserEvaluationPageQuery query);
}
