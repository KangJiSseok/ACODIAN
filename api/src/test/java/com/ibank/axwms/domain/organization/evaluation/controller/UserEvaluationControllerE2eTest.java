package com.ibank.axwms.domain.organization.evaluation.controller;

import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ibank.axwms.domain.organization.department.entity.Department;
import com.ibank.axwms.domain.organization.department.repository.DepartmentRepository;
import com.ibank.axwms.domain.organization.evaluation.entity.UserEvaluation;
import com.ibank.axwms.domain.organization.evaluation.repository.UserEvaluationRepository;
import com.ibank.axwms.domain.organization.user.EmploymentStatus;
import com.ibank.axwms.domain.organization.user.UserRole;
import com.ibank.axwms.domain.organization.user.entity.User;
import com.ibank.axwms.domain.organization.user.repository.UserRepository;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import com.ibank.axwms.testsupport.E2eTestSupport;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class UserEvaluationControllerE2eTest extends E2eTestSupport {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserEvaluationRepository userEvaluationRepository;

    private User director;
    private User deptHeadSameDepartment;
    private User sameDepartmentDirector;
    private User sameDepartmentTarget;
    private User otherDepartmentTarget;

    @BeforeEach
    void setUp() {
        clearDatabase();
        seedScenario();
    }

    @Test
    @DisplayName("인증 없이 사용자 평가 조회를 호출하면 AUTH_UNAUTHORIZED 응답을 반환한다")
    void 인증_없이_사용자_평가_조회를_호출하면_AUTH_UNAUTHORIZED_응답을_반환한다() throws Exception {
        mockMvc.perform(apiGet("/users/" + sameDepartmentTarget.getId() + "/evaluations"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("AUTH_UNAUTHORIZED")));
    }

    @Test
    @DisplayName("MEMBER 권한으로 사용자 평가 조회를 호출하면 AUTH_ACCESS_DENIED 응답을 반환한다")
    void MEMBER_권한으로_사용자_평가_조회를_호출하면_AUTH_ACCESS_DENIED_응답을_반환한다() throws Exception {
        User member = userRepository.save(createUser(
                sameDepartmentTarget.getDepartmentId(),
                "평가조회일반사원",
                "evaluation-member-" + System.nanoTime() + "@ibank.com",
                UserRole.MEMBER
        ));

        mockMvc.perform(apiGet("/users/" + sameDepartmentTarget.getId() + "/evaluations")
                        .with(authentication(authenticate(member))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("AUTH_ACCESS_DENIED")));
    }

    @Test
    @DisplayName("DIRECTOR 로그인 후 타 부서 대상 조회에 성공한다")
    void DIRECTOR_로그인_후_타_부서_대상_조회에_성공한다() throws Exception {
        mockMvc.perform(apiGet("/users/" + otherDepartmentTarget.getId() + "/evaluations")
                        .with(authentication(authenticate(director))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.items.length()", is(1)))
                .andExpect(jsonPath("$.data.items[0].content", is("DIRECTOR 타 부서 평가")))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("DEPT_HEAD 로그인 후 같은 부서 대상 조회에 성공한다")
    void DEPT_HEAD_로그인_후_같은_부서_대상_조회에_성공한다() throws Exception {
        mockMvc.perform(apiGet("/users/" + sameDepartmentTarget.getId() + "/evaluations")
                        .with(authentication(authenticate(deptHeadSameDepartment))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalCount", is(1)))
                .andExpect(jsonPath("$.data.items[0].content", is("동일 부서 일반 평가")));
    }

    @Test
    @DisplayName("DEPT_HEAD 로그인 후 같은 부서 DIRECTOR 대상 조회는 EVALUATION_ACCESS_DENIED 응답을 반환한다")
    void DEPT_HEAD_로그인_후_같은_부서_DIRECTOR_대상_조회는_EVALUATION_ACCESS_DENIED_응답을_반환한다() throws Exception {
        mockMvc.perform(apiGet("/users/" + sameDepartmentDirector.getId() + "/evaluations")
                        .with(authentication(authenticate(deptHeadSameDepartment))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("EVALUATION_ACCESS_DENIED")));
    }

    @Test
    @DisplayName("DEPT_HEAD 로그인 후 타 부서 대상 조회는 EVALUATION_ACCESS_DENIED 응답을 반환한다")
    void DEPT_HEAD_로그인_후_타_부서_대상_조회는_EVALUATION_ACCESS_DENIED_응답을_반환한다() throws Exception {
        mockMvc.perform(apiGet("/users/" + otherDepartmentTarget.getId() + "/evaluations")
                        .with(authentication(authenticate(deptHeadSameDepartment))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("EVALUATION_ACCESS_DENIED")));
    }

    @Test
    @DisplayName("DEPT_HEAD 자기 자신 조회 시 평가 전체가 비노출된다")
    void DEPT_HEAD_자기_자신_조회_시_평가_전체가_비노출된다() throws Exception {
        mockMvc.perform(apiGet("/users/" + deptHeadSameDepartment.getId() + "/evaluations")
                        .with(authentication(authenticate(deptHeadSameDepartment))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalCount", is(0)))
                .andExpect(jsonPath("$.data.items.length()", is(0)));
    }

    @Test
    @DisplayName("존재하지 않는 대상 사용자를 조회하면 USER_NOT_FOUND 응답을 반환한다")
    void 존재하지_않는_대상_사용자를_조회하면_USER_NOT_FOUND_응답을_반환한다() throws Exception {
        mockMvc.perform(apiGet("/users/999999/evaluations")
                        .with(authentication(authenticate(director))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("USER_NOT_FOUND")));
    }

    private void clearDatabase() {
        userEvaluationRepository.deleteAll();
        userRepository.deleteAll();
        departmentRepository.deleteAll();
    }

    private void seedScenario() {
        Department sameDepartment = departmentRepository.save(Department.create("평가조회개발본부", "사용자 평가 조회 E2E 테스트용 부서"));
        Department otherDepartment = departmentRepository.save(Department.create("평가조회운영본부", "사용자 평가 조회 타 부서 테스트용 부서"));

        director = userRepository.save(createUser(sameDepartment.getId(), "평가조회DIRECTOR", "evaluation-director-" + System.nanoTime() + "@ibank.com", UserRole.DIRECTOR));
        deptHeadSameDepartment = userRepository.save(createUser(sameDepartment.getId(), "평가조회DEPT_HEAD", "evaluation-head-" + System.nanoTime() + "@ibank.com", UserRole.DEPT_HEAD));
        sameDepartmentDirector = userRepository.save(createUser(sameDepartment.getId(), "같은부서DIRECTOR", "evaluation-same-director-" + System.nanoTime() + "@ibank.com", UserRole.DIRECTOR));
        sameDepartmentTarget = userRepository.save(createUser(sameDepartment.getId(), "평가조회동일부서대상", "evaluation-same-target-" + System.nanoTime() + "@ibank.com", UserRole.MEMBER));
        otherDepartmentTarget = userRepository.save(createUser(otherDepartment.getId(), "평가조회타부서대상", "evaluation-other-target-" + System.nanoTime() + "@ibank.com", UserRole.MEMBER));
        User sameDepartmentMemberEvaluator = userRepository.save(createUser(sameDepartment.getId(), "동일 부서 일반 평가자", "evaluation-member-evaluator-" + System.nanoTime() + "@ibank.com", UserRole.MEMBER));

        userEvaluationRepository.saveAll(List.of(
                UserEvaluation.create(otherDepartmentTarget.getId(), director.getId(), "DIRECTOR 타 부서 평가"),
                UserEvaluation.create(sameDepartmentDirector.getId(), director.getId(), "같은 부서 DIRECTOR 평가"),
                UserEvaluation.create(sameDepartmentTarget.getId(), sameDepartmentMemberEvaluator.getId(), "동일 부서 일반 평가"),
                UserEvaluation.create(deptHeadSameDepartment.getId(), director.getId(), "동일 부서 DIRECTOR 작성 평가"),
                UserEvaluation.create(deptHeadSameDepartment.getId(), sameDepartmentMemberEvaluator.getId(), "동일 부서 MEMBER 작성 평가")
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

    private UsernamePasswordAuthenticationToken authenticate(User user) {
        CustomUserPrincipal principal = new CustomUserPrincipal(user.getId(), user.getEmail(), user.getRoleCode().name());
        return new UsernamePasswordAuthenticationToken(
                principal,
                "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRoleCode().name()))
        );
    }
}
