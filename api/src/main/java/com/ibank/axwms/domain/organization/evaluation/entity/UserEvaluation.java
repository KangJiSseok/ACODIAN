package com.ibank.axwms.domain.organization.evaluation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_user_evaluation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "evaluation_id")
    private Long id;

    @Column(name = "evaluatee_user_id", nullable = false)
    private Long evaluateeUserId;

    @Column(name = "evaluator_user_id", nullable = false)
    private Long evaluatorUserId;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 신규 사용자 평가를 생성한다.
     * evaluatee/evaluator 는 모두 유효한 tb_user row 여야 하며, 내용은 비어 있지 않은 문자열이어야 한다.
     */
    public static UserEvaluation create(Long evaluateeUserId, Long evaluatorUserId, String content) {
        UserEvaluation evaluation = new UserEvaluation();
        evaluation.evaluateeUserId = evaluateeUserId;
        evaluation.evaluatorUserId = evaluatorUserId;
        evaluation.content = content;
        return evaluation;
    }
}
