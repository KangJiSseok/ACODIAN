package com.ibank.axwms.domain.organization.evaluation.repository;

import com.ibank.axwms.domain.organization.evaluation.entity.UserEvaluation;
import com.ibank.axwms.domain.organization.evaluation.repository.jooq.UserEvaluationJooqRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserEvaluationRepository extends JpaRepository<UserEvaluation, Long>, UserEvaluationJooqRepository {
}
