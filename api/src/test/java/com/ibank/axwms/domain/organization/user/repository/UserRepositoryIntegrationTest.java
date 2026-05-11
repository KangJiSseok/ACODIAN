package com.ibank.axwms.domain.organization.user.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import com.ibank.axwms.domain.organization.department.DepartmentStatus;
import com.ibank.axwms.domain.organization.department.entity.Department;
import com.ibank.axwms.domain.organization.department.repository.DepartmentRepository;
import com.ibank.axwms.domain.organization.team.TeamStatus;
import com.ibank.axwms.domain.organization.team.UserTeamStatus;
import com.ibank.axwms.domain.organization.team.entity.Team;
import com.ibank.axwms.domain.organization.team.entity.UserTeam;
import com.ibank.axwms.domain.organization.team.repository.TeamAdminRepository;
import com.ibank.axwms.domain.organization.team.repository.TeamRepository;
import com.ibank.axwms.domain.organization.team.repository.UserTeamRepository;
import com.ibank.axwms.domain.organization.user.EmploymentStatus;
import com.ibank.axwms.domain.organization.user.UserRole;
import com.ibank.axwms.domain.organization.user.entity.User;
import com.ibank.axwms.domain.organization.user.repository.jooq.projection.UserSummaryProjection;
import com.ibank.axwms.domain.organization.user.repository.jooq.query.UserListQuery;
import com.ibank.axwms.domain.worklog.repository.WorklogRepository;
import com.ibank.axwms.testsupport.IntegrationTestSupport;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

class UserRepositoryIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamAdminRepository teamAdminRepository;

    @Autowired
    private UserTeamRepository userTeamRepository;

    @Autowired
    private WorklogRepository worklogRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        clearDatabase();
    }

    @Test
    @DisplayName("사용자 목록은 기본 재직 상태와 role 우선순위로 조회하고 대표 팀을 함께 반환한다")
    void 사용자_목록은_기본_재직_상태와_role_우선순위로_조회하고_대표_팀을_함께_반환한다() {
        UserListFixture fixture = seedUserListFixture();

        Page<UserSummaryProjection> result = userRepository.findUsers(new UserListQuery(null, null, null, null, 1, 20));

        assertThat(result.getContent())
                .extracting(
                        UserSummaryProjection::userId,
                        UserSummaryProjection::userName,
                        UserSummaryProjection::email,
                        UserSummaryProjection::phone,
                        UserSummaryProjection::departmentId,
                        UserSummaryProjection::departmentName,
                        UserSummaryProjection::teamId,
                        UserSummaryProjection::teamName,
                        UserSummaryProjection::positionName,
                        UserSummaryProjection::titleName,
                        UserSummaryProjection::employmentStatus
                )
                .containsExactly(
                        tuple(fixture.directorId(), "본부장", "director@example.com", "010-0000-0001", fixture.logisticsDepartmentId(), "물류본부", null, null, "임원", "본부장", EmploymentStatus.ACTIVE),
                        tuple(fixture.deptHeadId(), "부서장", "dept-head@example.com", "010-0000-0002", fixture.logisticsDepartmentId(), "물류본부", fixture.primaryTeamId(), "대표팀", "부장", "사업부장", EmploymentStatus.ACTIVE),
                        tuple(fixture.teamLeadId(), "팀장", "team-lead@example.com", "010-0000-0003", fixture.supportDepartmentId(), "지원본부", fixture.secondaryTeamId(), "지원팀", "과장", "팀장", EmploymentStatus.LEAVE),
                        tuple(fixture.memberId(), "팀원", "member@example.com", "010-0000-0004", fixture.supportDepartmentId(), "지원본부", null, null, "사원", "팀원", EmploymentStatus.ACTIVE)
                );
        assertThat(result.getContent())
                .extracting(UserSummaryProjection::userName)
                .doesNotContain("퇴직자");
        assertThat(result.getTotalElements()).isEqualTo(4);
        assertThat(result.getNumber()).isZero();
        assertThat(result.getSize()).isEqualTo(20);
    }

    @Test
    @DisplayName("사용자 목록은 사용자명 부서 직급 재직 상태 조합 필터를 적용한다")
    void 사용자_목록은_사용자명_부서_직급_재직_상태_조합_필터를_적용한다() {
        UserListFixture fixture = seedUserListFixture();

        Page<UserSummaryProjection> result = userRepository.findUsers(new UserListQuery(
                "팀장",
                fixture.supportDepartmentId(),
                "과장",
                EmploymentStatus.LEAVE,
                1,
                20
        ));

        assertThat(result.getContent())
                .extracting(UserSummaryProjection::userId,
                        UserSummaryProjection::userName,
                        UserSummaryProjection::departmentId,
                        UserSummaryProjection::positionName,
                        UserSummaryProjection::employmentStatus)
                .containsExactly(tuple(fixture.teamLeadId(), "팀장", fixture.supportDepartmentId(), "과장", EmploymentStatus.LEAVE));
    }

    @Test
    @DisplayName("사용자 목록은 RETIRED 필터가 전달되어도 퇴직자를 반환하지 않는다")
    void 사용자_목록은_RETIRED_필터가_전달되어도_퇴직자를_반환하지_않는다() {
        seedUserListFixture();

        Page<UserSummaryProjection> result = userRepository.findUsers(
                new UserListQuery(null, null, null, EmploymentStatus.RETIRED, 1, 20));

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("사용자 목록은 페이지 번호와 페이지 크기를 적용하고 전체 건수를 유지한다")
    void 사용자_목록은_페이지_번호와_페이지_크기를_적용하고_전체_건수를_유지한다() {
        UserListFixture fixture = seedUserListFixture();

        Page<UserSummaryProjection> result = userRepository.findUsers(
                new UserListQuery(null, null, null, null, 2, 2));

        assertThat(result.getContent())
                .extracting(UserSummaryProjection::userId, UserSummaryProjection::userName)
                .containsExactly(
                        tuple(fixture.teamLeadId(), "팀장"),
                        tuple(fixture.memberId(), "팀원")
                );
        assertThat(result.getTotalElements()).isEqualTo(4);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.getNumber()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(2);
    }

    @Test
    @DisplayName("사용자 목록은 복수 팀 소속 사용자를 한 행으로 조회하고 대표 팀을 표시한다")
    void 사용자_목록은_복수_팀_소속_사용자를_한_행으로_조회하고_대표_팀을_표시한다() {
        Department department = departmentRepository.save(createDepartment("물류본부"));
        Team primaryTeam = teamRepository.save(createTeam("대표팀"));
        Team nonPrimaryTeam = teamRepository.save(createTeam("겸임팀"));
        User user = userRepository.save(createUser(department.getId(), "복수소속", "multi@example.com", UserRole.MEMBER, EmploymentStatus.ACTIVE, "사원", "팀원", "010-0000-0021"));

        userTeamRepository.save(UserTeam.create(user.getId(), primaryTeam.getId(), false, "주담당", "100%", true, UserTeamStatus.ACTIVE));
        userTeamRepository.save(UserTeam.create(user.getId(), nonPrimaryTeam.getId(), false, "겸임", "20%", false, UserTeamStatus.ACTIVE));

        Page<UserSummaryProjection> result = userRepository.findUsers(new UserListQuery(null, null, null, null, 1, 20));

        assertThat(result.getContent())
                .extracting(UserSummaryProjection::userId, UserSummaryProjection::teamId, UserSummaryProjection::teamName)
                .containsExactly(tuple(user.getId(), primaryTeam.getId(), "대표팀"));
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("사용자 목록은 대표 팀이 없는 복수 팀 소속 사용자의 팀 필드를 비워 둔다")
    void 사용자_목록은_대표_팀이_없는_복수_팀_소속_사용자의_팀_필드를_비워_둔다() {
        Department department = departmentRepository.save(createDepartment("지원본부"));
        Team firstTeam = teamRepository.save(createTeam("지원1팀"));
        Team secondTeam = teamRepository.save(createTeam("지원2팀"));
        User user = userRepository.save(createUser(department.getId(), "대표없음", "no-primary@example.com", UserRole.MEMBER, EmploymentStatus.ACTIVE, "사원", "팀원", "010-0000-0022"));

        userTeamRepository.save(UserTeam.create(user.getId(), firstTeam.getId(), false, "겸임", "50%", false, UserTeamStatus.ACTIVE));
        userTeamRepository.save(UserTeam.create(user.getId(), secondTeam.getId(), false, "겸임", "50%", false, UserTeamStatus.ACTIVE));

        Page<UserSummaryProjection> result = userRepository.findUsers(new UserListQuery(null, null, null, null, 1, 20));

        assertThat(result.getContent())
                .extracting(UserSummaryProjection::userId, UserSummaryProjection::teamId, UserSummaryProjection::teamName)
                .containsExactly(tuple(user.getId(), null, null));
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("사용자 목록은 부서가 없는 사용자도 조회하고 부서와 팀 필드를 비워 둔다")
    void 사용자_목록은_부서가_없는_사용자도_조회하고_부서와_팀_필드를_비워_둔다() {
        User user = userRepository.save(createUser(null, "무소속", "no-department@example.com", UserRole.MEMBER, EmploymentStatus.ACTIVE, "사원", "팀원", "010-0000-0023"));

        Page<UserSummaryProjection> result = userRepository.findUsers(new UserListQuery(null, null, null, null, 1, 20));

        assertThat(result.getContent())
                .extracting(
                        UserSummaryProjection::userId,
                        UserSummaryProjection::departmentId,
                        UserSummaryProjection::departmentName,
                        UserSummaryProjection::teamId,
                        UserSummaryProjection::teamName
                )
                .containsExactly(tuple(user.getId(), null, null, null, null));
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("부서 후보는 departmentId 가 null 인 DEPT_HEAD 사용자만 id 오름차순으로 조회한다")
    void 부서_후보는_departmentId가_null인_DEPT_HEAD_사용자만_id_오름차순으로_조회한다() {
        Department department = departmentRepository.save(createDepartment("소속부서"));
        User firstCandidate = userRepository.save(createUser(null, "무소속부서장", "candidate-1@example.com", UserRole.DEPT_HEAD, EmploymentStatus.ACTIVE, "부장", "부서장 후보", "010-0000-0011"));
        User assignedDeptHead = userRepository.save(createUser(department.getId(), "소속부서장", "assigned@example.com", UserRole.DEPT_HEAD, EmploymentStatus.ACTIVE, "부장", "부서장", "010-0000-0012"));
        User memberWithoutDepartment = userRepository.save(createUser(null, "무소속팀원", "member-null@example.com", UserRole.MEMBER, EmploymentStatus.ACTIVE, "사원", "팀원", "010-0000-0013"));
        User secondCandidate = userRepository.save(createUser(null, "예비부서장", "candidate-2@example.com", UserRole.DEPT_HEAD, EmploymentStatus.ACTIVE, "차장", "부서장 후보", "010-0000-0014"));

        List<User> result = userRepository.findAllByRoleCodeAndDepartmentIdIsNullOrderByIdAsc(UserRole.DEPT_HEAD);

        assertThat(result)
                .extracting(User::getId, User::getUserName)
                .containsExactly(
                        tuple(firstCandidate.getId(), "무소속부서장"),
                        tuple(secondCandidate.getId(), "예비부서장")
                );
        assertThat(result)
                .extracting(User::getId)
                .doesNotContain(assignedDeptHead.getId(), memberWithoutDepartment.getId());
    }

    /** FK 제약을 피하기 위해 참조 테이블부터 테스트 데이터를 비운다. */
    private void clearDatabase() {
        worklogRepository.deleteAll();
        List<Department> departments = departmentRepository.findAll();
        departments.forEach(department -> department.assignHeadUserId(null));
        departmentRepository.saveAll(departments);
        departmentRepository.flush();

        teamAdminRepository.deleteAll();
        userTeamRepository.deleteAll();
        teamRepository.deleteAll();
        userRepository.deleteAll();
        departmentRepository.deleteAll();
    }

    /** 필터, 정렬, 대표 팀 조회를 함께 검증할 수 있는 사용자 목록 fixture 를 구성한다. */
    private UserListFixture seedUserListFixture() {
        Department logistics = departmentRepository.save(createDepartment("물류본부"));
        Department support = departmentRepository.save(createDepartment("지원본부"));
        Team primaryTeam = teamRepository.save(createTeam("대표팀"));
        Team secondaryTeam = teamRepository.save(createTeam("지원팀"));
        Team nonPrimaryTeam = teamRepository.save(createTeam("겸임팀"));

        User director = userRepository.save(createUser(logistics.getId(), "본부장", "director@example.com", UserRole.DIRECTOR, EmploymentStatus.ACTIVE, "임원", "본부장", "010-0000-0001"));
        User deptHead = userRepository.save(createUser(logistics.getId(), "부서장", "dept-head@example.com", UserRole.DEPT_HEAD, EmploymentStatus.ACTIVE, "부장", "사업부장", "010-0000-0002"));
        User teamLead = userRepository.save(createUser(support.getId(), "팀장", "team-lead@example.com", UserRole.TEAM_LEAD, EmploymentStatus.LEAVE, "과장", "팀장", "010-0000-0003"));
        User member = userRepository.save(createUser(support.getId(), "팀원", "member@example.com", UserRole.MEMBER, EmploymentStatus.ACTIVE, "사원", "팀원", "010-0000-0004"));
        userRepository.save(createUser(support.getId(), "퇴직자", "retired@example.com", UserRole.MEMBER, EmploymentStatus.RETIRED, "사원", "팀원", "010-0000-0005"));

        userTeamRepository.save(UserTeam.create(deptHead.getId(), primaryTeam.getId(), true, "관리", "주담당", true, UserTeamStatus.ACTIVE));
        userTeamRepository.save(UserTeam.create(deptHead.getId(), nonPrimaryTeam.getId(), false, "겸임", "겸임", false, UserTeamStatus.ACTIVE));
        userTeamRepository.save(UserTeam.create(teamLead.getId(), secondaryTeam.getId(), true, "리딩", "주담당", true, UserTeamStatus.ACTIVE));

        return new UserListFixture(
                logistics.getId(),
                support.getId(),
                primaryTeam.getId(),
                secondaryTeam.getId(),
                director.getId(),
                deptHead.getId(),
                teamLead.getId(),
                member.getId()
        );
    }

    /** 테스트용 활성 부서를 생성한다. */
    private Department createDepartment(String departmentName) {
        Department department = Department.create(departmentName, departmentName + " 설명");
        department.changeStatus(DepartmentStatus.ACTIVE);
        return department;
    }

    /** 테스트용 활성 팀을 생성한다. */
    private Team createTeam(String teamName) {
        return Team.create(
                teamName,
                TeamStatus.ACTIVE,
                teamName + " 설명",
                LocalDate.of(2026, 4, 1),
                null
        );
    }

    /** 테스트용 사용자를 생성한다. */
    private User createUser(Long departmentId,
                            String userName,
                            String email,
                            UserRole role,
                            EmploymentStatus employmentStatus,
                            String positionName,
                            String titleName,
                            String phone) {
        return User.create(
                departmentId,
                userName,
                email,
                "$2a$10$abcdefghijklmnopqrstuv",
                role,
                employmentStatus,
                positionName,
                titleName,
                LocalDate.of(2025, 1, 1),
                phone,
                null
        );
    }

    private record UserListFixture(Long logisticsDepartmentId,
                                   Long supportDepartmentId,
                                   Long primaryTeamId,
                                   Long secondaryTeamId,
                                   Long directorId,
                                   Long deptHeadId,
                                   Long teamLeadId,
                                   Long memberId) {
    }
}
