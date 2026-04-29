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
import java.util.List;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

class UserEvaluationRepositoryIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private UserEvaluationRepository userEvaluationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    private User deptHead;
    private User sameDepartmentTarget;
    private User otherDepartmentTarget;

    @BeforeEach
    void setUp() {
        clearDatabase();
        seedScenario();
    }

    @Test
    @DisplayName("projection 은 이름과 본문과 작성시각 필드를 정확히 조인한다")
    void projection_은_이름과_본문과_작성시각_필드를_정확히_조인한다() {
        Page<UserEvaluationSummaryProjection> page =
                userEvaluationRepository.findUserEvaluationPage(new UserEvaluationPageQuery(1, 20, otherDepartmentTarget.getId(), "DESC"));

        assertThat(page.getContent())
                .extracting(
                        UserEvaluationSummaryProjection::evaluateeUserName,
                        UserEvaluationSummaryProjection::evaluatorUserName,
                        UserEvaluationSummaryProjection::content,
                        projection -> projection.createdAt() != null
                )
                .containsExactly(Tuple.tuple("타부서 대상", "개발본부 본부장", "DIRECTOR 타 부서 평가", true));
    }

    @Test
    @DisplayName("page=1 pageSize=20 기본 케이스에서 totalCount totalPages items 수가 일치한다")
    void page_1_pageSize_20_기본_케이스에서_totalCount_totalPages_items_수가_일치한다() {
        Page<UserEvaluationSummaryProjection> page =
                userEvaluationRepository.findUserEvaluationPage(new UserEvaluationPageQuery(1, 20, deptHead.getId(), "DESC"));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getTotalPages()).isEqualTo(1);
        assertThat(page.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("정렬 기본값은 createdAt desc 이후 evaluationId desc tie-breaker 를 따른다")
    void 정렬_기본값은_createdAt_desc_이후_evaluationId_desc_tie_breaker를_따른다() {
        Page<UserEvaluationSummaryProjection> page =
                userEvaluationRepository.findUserEvaluationPage(new UserEvaluationPageQuery(1, 20, deptHead.getId(), "DESC"));

        assertThat(page.getContent())
                .extracting(UserEvaluationSummaryProjection::content)
                .containsExactly("동일 부서 MEMBER 작성 평가", "동일 부서 DIRECTOR 작성 평가");
    }

    @Test
    @DisplayName("invalid 또는 null sortDirection 은 createdAt DESC 기본 정렬로 귀결된다")
    void invalid_또는_null_sortDirection은_createdAt_DESC_기본_정렬로_귀결된다() {
        Page<UserEvaluationSummaryProjection> invalidSortPage =
                userEvaluationRepository.findUserEvaluationPage(new UserEvaluationPageQuery(1, 20, deptHead.getId(), "sideways"));
        Page<UserEvaluationSummaryProjection> nullSortPage =
                userEvaluationRepository.findUserEvaluationPage(new UserEvaluationPageQuery(1, 20, deptHead.getId(), null));

        assertThat(invalidSortPage.getContent())
                .extracting(UserEvaluationSummaryProjection::content)
                .containsExactly("동일 부서 MEMBER 작성 평가", "동일 부서 DIRECTOR 작성 평가");
        assertThat(nullSortPage.getContent())
                .extracting(UserEvaluationSummaryProjection::content)
                .containsExactly("동일 부서 MEMBER 작성 평가", "동일 부서 DIRECTOR 작성 평가");
    }


    private void clearDatabase() {
        userEvaluationRepository.deleteAll();
        userRepository.deleteAll();
        departmentRepository.deleteAll();
    }

    private void seedScenario() {
        Department sameDepartment = departmentRepository.save(Department.create("평가조회개발본부", "평가 조회 통합 테스트용 부서"));
        Department otherDepartment = departmentRepository.save(Department.create("평가조회운영본부", "평가 조회 타 부서 테스트용 부서"));

        User director = userRepository.save(createUser(sameDepartment.getId(), "개발본부 본부장", "repo-director-" + System.nanoTime() + "@ibank.com", UserRole.DIRECTOR));
        deptHead = userRepository.save(createUser(sameDepartment.getId(), "동일 부서 부서장", "repo-head-" + System.nanoTime() + "@ibank.com", UserRole.DEPT_HEAD));
        sameDepartmentTarget = userRepository.save(createUser(sameDepartment.getId(), "동일 부서 대상", "repo-same-target-" + System.nanoTime() + "@ibank.com", UserRole.MEMBER));
        otherDepartmentTarget = userRepository.save(createUser(otherDepartment.getId(), "타부서 대상", "repo-other-target-" + System.nanoTime() + "@ibank.com", UserRole.MEMBER));
        User sameDepartmentMemberEvaluator = userRepository.save(createUser(sameDepartment.getId(), "동일 부서 일반 평가자", "repo-member-evaluator-" + System.nanoTime() + "@ibank.com", UserRole.MEMBER));

        userEvaluationRepository.saveAll(List.of(
                UserEvaluation.create(otherDepartmentTarget.getId(), director.getId(), "DIRECTOR 타 부서 평가"),
                UserEvaluation.create(deptHead.getId(), director.getId(), "동일 부서 DIRECTOR 작성 평가"),
                UserEvaluation.create(deptHead.getId(), sameDepartmentMemberEvaluator.getId(), "동일 부서 MEMBER 작성 평가")
        ));
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
                role == UserRole.DIRECTOR ? "본부장" : "팀장",
                LocalDate.of(2025, 1, 1),
                null,
                null
        );
    }
}
