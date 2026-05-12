package com.ibank.axwms.domain.organization.evaluation.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ibank.axwms.domain.organization.department.entity.Department;
import com.ibank.axwms.domain.organization.department.repository.DepartmentRepository;
import com.ibank.axwms.domain.organization.evaluation.entity.UserEvaluation;
import com.ibank.axwms.domain.organization.evaluation.repository.jooq.projection.UserEvaluationSummaryProjection;
import com.ibank.axwms.domain.organization.evaluation.repository.jooq.query.UserEvaluationPageQuery;
import com.ibank.axwms.domain.organization.user.EmploymentStatus;
import com.ibank.axwms.domain.organization.user.UserRole;
import com.ibank.axwms.domain.organization.user.entity.User;
import com.ibank.axwms.domain.organization.user.repository.UserRepository;
import com.ibank.axwms.testsupport.IntegrationTestSupport;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.JdbcTemplate;

class UserEvaluationRepositoryIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private UserEvaluationRepository userEvaluationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User evaluatee;
    private User evaluator;

    @BeforeEach
    void setUp() {
        userEvaluationRepository.deleteAll();
        userRepository.deleteAll();
        departmentRepository.deleteAll();

        Department department = departmentRepository.save(Department.create("평가본부", "평가 repository 테스트용 부서"));
        evaluatee = userRepository.save(createUser(department.getId(), "평가 대상", "evaluatee@ibank.com", UserRole.MEMBER));
        evaluator = userRepository.save(createUser(department.getId(), "평가자", "evaluator@ibank.com", UserRole.DIRECTOR));
    }

    @Test
    @DisplayName("사용자 평가는 대상 사용자와 평가자 ID를 저장한다")
    void 사용자_평가는_대상_사용자와_평가자_ID를_저장한다() {
        UserEvaluation saved = userEvaluationRepository.save(
                UserEvaluation.create(evaluatee.getId(), evaluator.getId(), "평가 내용")
        );

        assertThat(userEvaluationRepository.findById(saved.getId()))
                .get()
                .satisfies(evaluation -> {
                    assertThat(evaluation.getEvaluateeUserId()).isEqualTo(evaluatee.getId());
                    assertThat(evaluation.getEvaluatorUserId()).isEqualTo(evaluator.getId());
                    assertThat(evaluation.getContent()).isEqualTo("평가 내용");
                });
    }

    @Test
    @DisplayName("사용자 평가는 수정 시 작성 시각을 유지하고 내용만 변경한다")
    void 사용자_평가는_수정_시_작성_시각을_유지하고_내용만_변경한다() {
        UserEvaluation saved = userEvaluationRepository.saveAndFlush(
                UserEvaluation.create(evaluatee.getId(), evaluator.getId(), "기존 평가")
        );
        LocalDateTime createdAt = saved.getCreatedAt();

        saved.updateContent("수정된 평가");
        userEvaluationRepository.saveAndFlush(saved);

        assertThat(userEvaluationRepository.findById(saved.getId()))
                .get()
                .satisfies(evaluation -> {
                    assertThat(evaluation.getContent()).isEqualTo("수정된 평가");
                    assertThat(evaluation.getCreatedAt()).isEqualTo(createdAt);
                });
    }

    @Test
    @DisplayName("사용자 평가 이력 페이지는 대상/평가자 이름을 조인하고 createdAt DESC 로 정렬한다")
    void 사용자_평가_이력_페이지는_대상_평가자_이름을_조인하고_createdAt_DESC로_정렬한다() {
        insertEvaluation("오래된 평가", LocalDateTime.of(2026, 4, 20, 9, 0));
        insertEvaluation("최신 평가", LocalDateTime.of(2026, 4, 21, 9, 0));

        Page<UserEvaluationSummaryProjection> result = userEvaluationRepository.findEvaluationPage(
                new UserEvaluationPageQuery(1, 20, evaluatee.getId(), "DESC")
        );

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
                .extracting(UserEvaluationSummaryProjection::evaluateeUserId,
                        UserEvaluationSummaryProjection::evaluateeUserName,
                        UserEvaluationSummaryProjection::evaluatorUserId,
                        UserEvaluationSummaryProjection::evaluatorUserName,
                        UserEvaluationSummaryProjection::content)
                .containsExactly(
                        Tuple.tuple(evaluatee.getId(), "평가 대상", evaluator.getId(), "평가자", "최신 평가"),
                        Tuple.tuple(evaluatee.getId(), "평가 대상", evaluator.getId(), "평가자", "오래된 평가")
                );
    }

    @Test
    @DisplayName("사용자 평가 이력 페이지는 pageSize 와 ASC 정렬 방향을 반영한다")
    void 사용자_평가_이력_페이지는_pageSize와_ASC_정렬_방향을_반영한다() {
        insertEvaluation("첫 번째 평가", LocalDateTime.of(2026, 4, 20, 9, 0));
        insertEvaluation("두 번째 평가", LocalDateTime.of(2026, 4, 21, 9, 0));

        Page<UserEvaluationSummaryProjection> result = userEvaluationRepository.findEvaluationPage(
                new UserEvaluationPageQuery(1, 1, evaluatee.getId(), "ASC")
        );

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.getContent())
                .extracting(UserEvaluationSummaryProjection::content)
                .containsExactly("첫 번째 평가");
    }

    private void insertEvaluation(String content, LocalDateTime createdAt) {
        jdbcTemplate.update("""
                        INSERT INTO tb_user_evaluation (evaluatee_user_id, evaluator_user_id, content, created_at)
                        VALUES (?, ?, ?, ?)
                        """,
                evaluatee.getId(),
                evaluator.getId(),
                content,
                createdAt
        );
    }

    private User createUser(Long departmentId, String userName, String email, UserRole role) {
        return User.create(
                departmentId,
                userName,
                email,
                "$2a$10$abcdefghijklmnopqrstuv",
                role,
                EmploymentStatus.ACTIVE,
                "과장",
                role == UserRole.DIRECTOR ? "본부장" : "팀원",
                LocalDate.of(2025, 1, 1),
                null,
                null
        );
    }
}
