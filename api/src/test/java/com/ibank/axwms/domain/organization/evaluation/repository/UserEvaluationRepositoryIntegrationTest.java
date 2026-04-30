package com.ibank.axwms.domain.organization.evaluation.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ibank.axwms.domain.organization.department.entity.Department;
import com.ibank.axwms.domain.organization.department.repository.DepartmentRepository;
import com.ibank.axwms.domain.organization.evaluation.entity.UserEvaluation;
import com.ibank.axwms.domain.organization.user.EmploymentStatus;
import com.ibank.axwms.domain.organization.user.UserRole;
import com.ibank.axwms.domain.organization.user.entity.User;
import com.ibank.axwms.domain.organization.user.repository.UserRepository;
import com.ibank.axwms.testsupport.IntegrationTestSupport;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserEvaluationRepositoryIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private UserEvaluationRepository userEvaluationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

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
