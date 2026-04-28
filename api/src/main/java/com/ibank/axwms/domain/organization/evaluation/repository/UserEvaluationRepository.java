package com.ibank.axwms.domain.organization.evaluation.repository;

import com.ibank.axwms.domain.organization.evaluation.entity.UserEvaluation;
import com.ibank.axwms.domain.organization.evaluation.repository.jooq.UserEvaluationJooqRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserEvaluationRepository extends JpaRepository<UserEvaluation, Long>, UserEvaluationJooqRepository {

    /** 로컬 시드가 같은 평가 fixture 를 중복 삽입하지 않도록 기존 row 를 탐색한다. */
    Optional<UserEvaluation> findByEvaluateeUserIdAndEvaluatorUserIdAndContent(Long evaluateeUserId,
                                                                                Long evaluatorUserId,
                                                                                String content);
}
