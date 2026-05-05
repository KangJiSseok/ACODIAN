package com.ibank.axwms.domain.organization.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.ibank.axwms.domain.organization.department.entity.Department;
import com.ibank.axwms.domain.organization.department.repository.DepartmentRepository;
import com.ibank.axwms.domain.organization.team.UserTeamStatus;
import com.ibank.axwms.domain.organization.team.entity.UserTeam;
import com.ibank.axwms.domain.organization.team.repository.TeamRepository;
import com.ibank.axwms.domain.organization.team.repository.UserTeamRepository;
import com.ibank.axwms.domain.organization.team.repository.jooq.projection.UserTeamSummaryProjection;
import com.ibank.axwms.domain.organization.user.EmploymentStatus;
import com.ibank.axwms.domain.organization.user.UserRole;
import com.ibank.axwms.domain.organization.user.dto.GetAdminCandidatesApiDto;
import com.ibank.axwms.domain.organization.user.dto.GetDepartmentCandidatesApiDto;
import com.ibank.axwms.domain.organization.user.dto.GetMyProfileApiDto;
import com.ibank.axwms.domain.organization.user.dto.GetUserApiDto;
import com.ibank.axwms.domain.organization.user.dto.GetUsersApiDto;
import com.ibank.axwms.domain.organization.user.dto.UpdateUserApiDto;
import com.ibank.axwms.domain.organization.user.entity.User;
import com.ibank.axwms.domain.organization.user.repository.UserRepository;
import com.ibank.axwms.domain.organization.user.repository.jooq.projection.UserSummaryProjection;
import com.ibank.axwms.domain.organization.user.repository.jooq.query.UserListQuery;
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private UserTeamRepository userTeamRepository;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("현재 사용자와 부서 및 ACTIVE 팀이 존재하면 프로필 문맥을 반환한다")
    void 현재_사용자와_부서_및_ACTIVE_팀이_존재하면_프로필_문맥을_반환한다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(101L, "user@ibank.com", "MEMBER");
        User user = createUser(
                101L,
                10L,
                "홍길동",
                "과장",
                "팀장",
                "https://cdn.axwms.com/profile/101.png",
                "010-1234-5678",
                LocalDate.of(2025, 1, 1)
        );
        Department department = createDepartment(10L, "물류본부");

        given(userRepository.findById(101L)).willReturn(Optional.of(user));
        given(departmentRepository.findById(10L)).willReturn(Optional.of(department));
        given(userTeamRepository.findUserTeamSummaries(101L))
                .willReturn(List.of(
                        new UserTeamSummaryProjection(true, 21L, "물류혁신TF", true, "플랫폼 총괄", "주담당"),
                        new UserTeamSummaryProjection(false, 22L, "SCM분석팀", false, "SCM 분석", "겸임")
                ));

        GetMyProfileApiDto.Response result = userService.getMyProfile(principal);

        assertThat(result.teams())
                .extracting(GetMyProfileApiDto.Response.TeamSummary::isPrimary,
                        GetMyProfileApiDto.Response.TeamSummary::teamId,
                        GetMyProfileApiDto.Response.TeamSummary::teamName,
                        GetMyProfileApiDto.Response.TeamSummary::isLeader,
                        GetMyProfileApiDto.Response.TeamSummary::teamRole,
                        GetMyProfileApiDto.Response.TeamSummary::allocation)
                .containsExactly(
                        Tuple.tuple(true, 21L, "물류혁신TF", true, "플랫폼 총괄", "주담당"),
                        Tuple.tuple(false, 22L, "SCM분석팀", false, "SCM 분석", "겸임")
                );
    }

    @Test
    @DisplayName("phone 과 joinDate 가 비어 있어도 null 로 그대로 반환한다")
    void phone_과_joinDate_가_비어_있어도_null_로_그대로_반환한다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(101L, "user@ibank.com", "MEMBER");
        User user = createUser(101L, 10L, "홍길동", "과장", "팀장", null, null, null);
        Department department = createDepartment(10L, "물류본부");

        given(userRepository.findById(101L)).willReturn(Optional.of(user));
        given(departmentRepository.findById(10L)).willReturn(Optional.of(department));
        given(userTeamRepository.findUserTeamSummaries(101L)).willReturn(List.of());

        GetMyProfileApiDto.Response result = userService.getMyProfile(principal);

        assertThat(result.email()).isEqualTo("user@ibank.com");
        assertThat(result.phone()).isNull();
        assertThat(result.joinDate()).isNull();
        assertThat(result.employmentStatus()).isEqualTo(EmploymentStatus.ACTIVE);
        assertThat(result.teams()).isEmpty();
    }

    @Test
    @DisplayName("ACTIVE membership 만 현재 사용자 팀 목록에 포함한다")
    void ACTIVE_membership_만_현재_사용자_팀_목록에_포함한다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(101L, "user@ibank.com", "MEMBER");
        User user = createUser(101L, 10L, "홍길동", "과장", "팀장", null, null, LocalDate.of(2025, 1, 1));
        Department department = createDepartment(10L, "물류본부");

        given(userRepository.findById(101L)).willReturn(Optional.of(user));
        given(departmentRepository.findById(10L)).willReturn(Optional.of(department));
        given(userTeamRepository.findUserTeamSummaries(101L))
                .willReturn(List.of(
                        new UserTeamSummaryProjection(true, 21L, "물류혁신TF", true, "플랫폼 총괄", "주담당")
                ));

        GetMyProfileApiDto.Response result = userService.getMyProfile(principal);

        assertThat(result.teams())
                .extracting(GetMyProfileApiDto.Response.TeamSummary::teamId)
                .containsExactly(21L);
    }

    @Test
    @DisplayName("soft-delete 된 팀은 현재 사용자 팀 목록에서 제외한다")
    void soft_delete_된_팀은_현재_사용자_팀_목록에서_제외한다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(101L, "user@ibank.com", "MEMBER");
        User user = createUser(101L, 10L, "홍길동", "과장", "팀장", null, null, LocalDate.of(2025, 1, 1));
        Department department = createDepartment(10L, "물류본부");

        given(userRepository.findById(101L)).willReturn(Optional.of(user));
        given(departmentRepository.findById(10L)).willReturn(Optional.of(department));
        // soft-delete 된 팀은 jOOQ 쿼리에서 이미 제외되어 빈 목록이 반환된다
        given(userTeamRepository.findUserTeamSummaries(101L)).willReturn(List.of());

        GetMyProfileApiDto.Response result = userService.getMyProfile(principal);

        assertThat(result.teams()).isEmpty();
    }

    @Test
    @DisplayName("현재 사용자가 없으면 USER_NOT_FOUND 예외를 던진다")
    void 현재_사용자가_없으면_USER_NOT_FOUND_예외를_던진다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(404L, "missing@ibank.com", "MEMBER");
        given(userRepository.findById(404L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getMyProfile(principal))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("사용자 부서가 없으면 USER_NOT_FOUND 예외를 던진다")
    void 사용자_부서가_없으면_USER_NOT_FOUND_예외를_던진다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(101L, "user@ibank.com", "MEMBER");
        User user = createUser(101L, 10L, "홍길동", "과장", "팀장", null, null, LocalDate.of(2025, 1, 1));

        given(userRepository.findById(101L)).willReturn(Optional.of(user));
        given(departmentRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getMyProfile(principal))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("사용자 상세 조회 대상이 존재하면 기본 정보와 전체 팀 문맥을 반환한다")
    void 사용자_상세_조회_대상이_존재하면_기본_정보와_전체_팀_문맥을_반환한다() {
        User user = createUser(
                101L,
                10L,
                "홍길동",
                "과장",
                "팀장",
                "https://cdn.axwms.com/profile/101.png",
                "010-1234-5678",
                LocalDate.of(2024, 3, 1),
                UserRole.MEMBER,
                "hong@axwms.com"
        );
        Department department = createDepartment(10L, "물류본부");

        given(userRepository.findById(101L)).willReturn(Optional.of(user));
        given(departmentRepository.findById(10L)).willReturn(Optional.of(department));
        // jOOQ 쿼리가 isPrimary DESC, isLeader DESC 순서로 반환한다
        given(userTeamRepository.findUserTeamSummaries(101L))
                .willReturn(List.of(
                        new UserTeamSummaryProjection(true, 21L, "웹서비스 개발", false, "대표 역할", "주담당"),
                        new UserTeamSummaryProjection(false, 23L, "SCM 분석", true, "리더 역할", "겸임"),
                        new UserTeamSummaryProjection(false, 22L, "AWS 개발", false, "일반 역할", "겸임")
                ));

        GetUserApiDto.Response result = userService.getUser(101L);

        assertThat(result.userId()).isEqualTo(101L);
        assertThat(result.userName()).isEqualTo("홍길동");
        assertThat(result.email()).isEqualTo("hong@axwms.com");
        assertThat(result.departmentId()).isEqualTo(10L);
        assertThat(result.departmentName()).isEqualTo("물류본부");
        assertThat(result.joinDate()).isEqualTo(LocalDate.of(2024, 3, 1));
        assertThat(result.phone()).isEqualTo("010-1234-5678");
        assertThat(result.employmentStatus()).isEqualTo(EmploymentStatus.ACTIVE);
        assertThat(result.teams())
                .extracting(GetUserApiDto.Response.TeamSummary::isPrimary,
                        GetUserApiDto.Response.TeamSummary::teamId,
                        GetUserApiDto.Response.TeamSummary::teamName,
                        GetUserApiDto.Response.TeamSummary::isLeader,
                        GetUserApiDto.Response.TeamSummary::teamRole)
                .containsExactly(
                        Tuple.tuple(true, 21L, "웹서비스 개발", false, "대표 역할"),
                        Tuple.tuple(false, 23L, "SCM 분석", true, "리더 역할"),
                        Tuple.tuple(false, 22L, "AWS 개발", false, "일반 역할")
                );
    }

    @Test
    @DisplayName("사용자 상세 조회 대상이 없으면 USER_NOT_FOUND 예외를 던진다")
    void 사용자_상세_조회_대상이_없으면_USER_NOT_FOUND_예외를_던진다() {
        given(userRepository.findById(404L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUser(404L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("사용자 상세 조회에서 soft-delete 된 팀은 제외한다")
    void 사용자_상세_조회에서_soft_delete_된_팀은_제외한다() {
        User user = createUser(101L, 10L, "홍길동", "과장", "팀장", null, null, LocalDate.of(2024, 3, 1));
        Department department = createDepartment(10L, "물류본부");

        given(userRepository.findById(101L)).willReturn(Optional.of(user));
        given(departmentRepository.findById(10L)).willReturn(Optional.of(department));
        // soft-delete 된 팀은 jOOQ 쿼리에서 이미 제외되어 빈 목록이 반환된다
        given(userTeamRepository.findUserTeamSummaries(101L)).willReturn(List.of());

        GetUserApiDto.Response result = userService.getUser(101L);

        assertThat(result.teams()).isEmpty();
    }

    @Test
    @DisplayName("DIRECTOR 는 DEPT_HEAD 사용자 전체를 관리자 후보로 조회한다")
    void DIRECTOR는_DEPT_HEAD_사용자_전체를_관리자_후보로_조회한다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(1L, "director@ibank.com", "DIRECTOR");
        User firstDepartmentHead = createUser(
                201L,
                10L,
                "김부서",
                "부장",
                "부서장",
                null,
                null,
                LocalDate.of(2025, 1, 1),
                UserRole.DEPT_HEAD,
                "dept-head-1@ibank.com"
        );
        User secondDepartmentHead = createUser(
                202L,
                11L,
                "이본부",
                null,
                "부서장",
                null,
                null,
                LocalDate.of(2025, 1, 2),
                UserRole.DEPT_HEAD,
                "dept-head-2@ibank.com"
        );
        given(userRepository.findAllByRoleCodeOrderByIdAsc(UserRole.DEPT_HEAD))
                .willReturn(List.of(firstDepartmentHead, secondDepartmentHead));

        List<GetAdminCandidatesApiDto.Response> result = userService.getAdminCandidates(principal);

        assertThat(result)
                .extracting(GetAdminCandidatesApiDto.Response::userId,
                        GetAdminCandidatesApiDto.Response::userName,
                        GetAdminCandidatesApiDto.Response::titleName,
                        GetAdminCandidatesApiDto.Response::positionName)
                .containsExactly(
                        Tuple.tuple(201L, "김부서", "부서장", "부장"),
                        Tuple.tuple(202L, "이본부", "부서장", null)
                );
    }

    @Test
    @DisplayName("DEPT_HEAD 는 자기 자신만 관리자 후보로 조회한다")
    void DEPT_HEAD는_자기_자신만_관리자_후보로_조회한다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(201L, "dept-head@ibank.com", "DEPT_HEAD");
        User departmentHead = createUser(
                201L,
                10L,
                "김부서",
                "부장",
                "부서장",
                null,
                null,
                LocalDate.of(2025, 1, 1),
                UserRole.DEPT_HEAD,
                "dept-head@ibank.com"
        );
        given(userRepository.findById(201L)).willReturn(Optional.of(departmentHead));

        List<GetAdminCandidatesApiDto.Response> result = userService.getAdminCandidates(principal);

        assertThat(result)
                .extracting(GetAdminCandidatesApiDto.Response::userId,
                        GetAdminCandidatesApiDto.Response::userName,
                        GetAdminCandidatesApiDto.Response::titleName,
                        GetAdminCandidatesApiDto.Response::positionName)
                .containsExactly(Tuple.tuple(201L, "김부서", "부서장", "부장"));
    }

    @Test
    @DisplayName("DEPT_HEAD principal 사용자 id 가 없으면 USER_NOT_FOUND 예외를 던진다")
    void DEPT_HEAD_principal_사용자_id가_없으면_USER_NOT_FOUND_예외를_던진다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(404L, "missing@ibank.com", "DEPT_HEAD");
        given(userRepository.findById(404L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getAdminCandidates(principal))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("부서 후보는 departmentId 가 없는 DEPT_HEAD 사용자만 조회한다")
    void 부서_후보는_departmentId가_없는_DEPT_HEAD_사용자만_조회한다() {
        User firstCandidate = createUser(
                301L,
                null,
                "무소속부서장",
                "부장",
                "부서장 후보",
                null,
                null,
                LocalDate.of(2025, 1, 1),
                UserRole.DEPT_HEAD,
                "candidate-1@ibank.com"
        );
        User secondCandidate = createUser(
                302L,
                null,
                "예비부서장",
                "차장",
                "부서장 후보",
                null,
                null,
                LocalDate.of(2025, 1, 2),
                UserRole.DEPT_HEAD,
                "candidate-2@ibank.com"
        );
        given(userRepository.findAllByRoleCodeAndDepartmentIdIsNullOrderByIdAsc(UserRole.DEPT_HEAD))
                .willReturn(List.of(firstCandidate, secondCandidate));

        List<GetDepartmentCandidatesApiDto.Response> result = userService.getDepartmentCandidates();

        assertThat(result)
                .extracting(GetDepartmentCandidatesApiDto.Response::userId,
                        GetDepartmentCandidatesApiDto.Response::userName)
                .containsExactly(
                        Tuple.tuple(301L, "무소속부서장"),
                        Tuple.tuple(302L, "예비부서장")
                );
    }

    @Test
    @DisplayName("사용자 목록 조회 요청을 repository query 로 정규화하고 응답 배열을 반환한다")
    void 사용자_목록_조회_요청을_repository_query로_정규화하고_응답_배열을_반환한다() {
        GetUsersApiDto.Request request = new GetUsersApiDto.Request("홍길동", 10L, "과장", EmploymentStatus.ACTIVE);
        UserSummaryProjection projection = new UserSummaryProjection(
                101L,
                "홍길동",
                "hong@axwms.com",
                "010-1234-1234",
                10L,
                "물류본부",
                "https://cdn.axwms.com/profile/101.png",
                21L,
                "물류혁신TF",
                "과장",
                "팀장",
                EmploymentStatus.ACTIVE
        );
        given(userRepository.findUsers(UserListQuery.from(request))).willReturn(List.of(projection));

        List<GetUsersApiDto.Response> result = userService.getUsers(request);

        assertThat(result)
                .extracting(GetUsersApiDto.Response::userId,
                        GetUsersApiDto.Response::userName,
                        GetUsersApiDto.Response::email,
                        GetUsersApiDto.Response::phone,
                        GetUsersApiDto.Response::departmentId,
                        GetUsersApiDto.Response::departmentName,
                        GetUsersApiDto.Response::teamId,
                        GetUsersApiDto.Response::teamName,
                        GetUsersApiDto.Response::employmentStatus)
                .containsExactly(Tuple.tuple(
                        101L,
                        "홍길동",
                        "hong@axwms.com",
                        "010-1234-1234",
                        10L,
                        "물류본부",
                        21L,
                        "물류혁신TF",
                        EmploymentStatus.ACTIVE
                ));
    }

    @Test
    @DisplayName("사용자 부분 수정은 null 이 아닌 기본 필드와 대표 팀만 반영한다")
    void 사용자_부분_수정은_null이_아닌_기본_필드와_대표_팀만_반영한다() {
        User user = createUser(101L, 10L, "홍길동", "과장", "팀장", "https://old.example/profile.png", "010-0000-0000", LocalDate.of(2024, 1, 1));
        UserTeam oldPrimary = UserTeam.create(101L, 21L, false, "기존 역할", "주담당", true, UserTeamStatus.ACTIVE);
        UserTeam newPrimary = UserTeam.create(101L, 22L, false, "신규 역할", "겸임", false, UserTeamStatus.ACTIVE);
        UpdateUserApiDto.Request request = new UpdateUserApiDto.Request(
                "김수정",
                "updated@axwms.com",
                "https://new.example/profile.png",
                "차장",
                "파트장",
                20L,
                "010-1111-2222",
                EmploymentStatus.LEAVE,
                LocalDate.of(2025, 2, 3),
                22L
        );
        given(userRepository.findById(101L)).willReturn(Optional.of(user));
        given(departmentRepository.existsById(20L)).willReturn(true);
        given(teamRepository.existsByIdAndDeletedAtIsNull(22L)).willReturn(true);
        given(userTeamRepository.findByUserIdAndTeamId(101L, 22L)).willReturn(Optional.of(newPrimary));
        given(userTeamRepository.findAllByUserId(101L)).willReturn(List.of(oldPrimary, newPrimary));

        userService.updateUser(directorPrincipal(), 101L, request);

        assertThat(user)
                .extracting(User::getUserName,
                        User::getEmail,
                        User::getProfileImageUrl,
                        User::getPositionName,
                        User::getTitleName,
                        User::getDepartmentId,
                        User::getPhone,
                        User::getEmploymentStatus,
                        User::getJoinDate)
                .containsExactly(
                        "김수정",
                        "updated@axwms.com",
                        "https://new.example/profile.png",
                        "차장",
                        "파트장",
                        20L,
                        "010-1111-2222",
                        EmploymentStatus.LEAVE,
                        LocalDate.of(2025, 2, 3)
                );
        assertThat(oldPrimary.getIsPrimary()).isFalse();
        assertThat(newPrimary.getIsPrimary()).isTrue();
    }

    @Test
    @DisplayName("사용자 부분 수정 요청 필드가 null 이면 기존 값을 유지한다")
    void 사용자_부분_수정_요청_필드가_null이면_기존_값을_유지한다() {
        User user = createUser(101L, 10L, "홍길동", "과장", "팀장", "https://old.example/profile.png", "010-0000-0000", LocalDate.of(2024, 1, 1));
        UpdateUserApiDto.Request request = new UpdateUserApiDto.Request(null, null, null, null, null, null, null, null, null, null);
        given(userRepository.findById(101L)).willReturn(Optional.of(user));

        userService.updateUser(directorPrincipal(), 101L, request);

        assertThat(user)
                .extracting(User::getUserName,
                        User::getEmail,
                        User::getProfileImageUrl,
                        User::getPositionName,
                        User::getTitleName,
                        User::getDepartmentId,
                        User::getPhone,
                        User::getEmploymentStatus,
                        User::getJoinDate)
                .containsExactly(
                        "홍길동",
                        "user@ibank.com",
                        "https://old.example/profile.png",
                        "과장",
                        "팀장",
                        10L,
                        "010-0000-0000",
                        EmploymentStatus.ACTIVE,
                        LocalDate.of(2024, 1, 1)
                );
    }

    @Test
    @DisplayName("사용자 부분 수정 대상이 없으면 USER_NOT_FOUND 예외를 던진다")
    void 사용자_부분_수정_대상이_없으면_USER_NOT_FOUND_예외를_던진다() {
        UpdateUserApiDto.Request request = new UpdateUserApiDto.Request(null, null, null, null, null, null, null, null, null, null);
        given(userRepository.findById(404L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(directorPrincipal(), 404L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("사용자 부분 수정 요청 부서가 없으면 DEPARTMENT_NOT_FOUND 예외를 던진다")
    void 사용자_부분_수정_요청_부서가_없으면_DEPARTMENT_NOT_FOUND_예외를_던진다() {
        User user = createUser(101L, 10L, "홍길동", "과장", "팀장", null, null, LocalDate.of(2024, 1, 1));
        UpdateUserApiDto.Request request = new UpdateUserApiDto.Request(null, null, null, null, null, 999L, null, null, null, null);
        given(userRepository.findById(101L)).willReturn(Optional.of(user));
        given(departmentRepository.existsById(999L)).willReturn(false);

        assertThatThrownBy(() -> userService.updateUser(directorPrincipal(), 101L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.DEPARTMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("사용자 부분 수정 요청 팀이 없으면 TEAM_NOT_FOUND 예외를 던진다")
    void 사용자_부분_수정_요청_팀이_없으면_TEAM_NOT_FOUND_예외를_던진다() {
        User user = createUser(101L, 10L, "홍길동", "과장", "팀장", null, null, LocalDate.of(2024, 1, 1));
        UpdateUserApiDto.Request request = new UpdateUserApiDto.Request(null, null, null, null, null, null, null, null, null, 999L);
        given(userRepository.findById(101L)).willReturn(Optional.of(user));
        given(teamRepository.existsByIdAndDeletedAtIsNull(999L)).willReturn(false);

        assertThatThrownBy(() -> userService.updateUser(directorPrincipal(), 101L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.TEAM_NOT_FOUND);
    }

    @Test
    @DisplayName("사용자 부분 수정 요청 팀 membership 이 없으면 TEAM_NOT_FOUND 예외를 던진다")
    void 사용자_부분_수정_요청_팀_membership이_없으면_TEAM_NOT_FOUND_예외를_던진다() {
        User user = createUser(101L, 10L, "홍길동", "과장", "팀장", null, null, LocalDate.of(2024, 1, 1));
        UpdateUserApiDto.Request request = new UpdateUserApiDto.Request(null, null, null, null, null, null, null, null, null, 22L);
        given(userRepository.findById(101L)).willReturn(Optional.of(user));
        given(teamRepository.existsByIdAndDeletedAtIsNull(22L)).willReturn(true);
        given(userTeamRepository.findByUserIdAndTeamId(101L, 22L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(directorPrincipal(), 101L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.TEAM_NOT_FOUND);
    }

    @Test
    @DisplayName("기존 대표 팀이 없어도 요청 팀을 대표 팀으로 지정한다")
    void 기존_대표_팀이_없어도_요청_팀을_대표_팀으로_지정한다() {
        User user = createUser(101L, 10L, "홍길동", "과장", "팀장", null, null, LocalDate.of(2024, 1, 1));
        UserTeam firstMembership = UserTeam.create(101L, 21L, false, "일반 역할", "겸임", false, UserTeamStatus.ACTIVE);
        UserTeam requestedMembership = UserTeam.create(101L, 22L, false, "대표 역할", "주담당", false, UserTeamStatus.ACTIVE);
        UpdateUserApiDto.Request request = new UpdateUserApiDto.Request(null, null, null, null, null, null, null, null, null, 22L);
        given(userRepository.findById(101L)).willReturn(Optional.of(user));
        given(teamRepository.existsByIdAndDeletedAtIsNull(22L)).willReturn(true);
        given(userTeamRepository.findByUserIdAndTeamId(101L, 22L)).willReturn(Optional.of(requestedMembership));
        given(userTeamRepository.findAllByUserId(101L)).willReturn(List.of(firstMembership, requestedMembership));

        userService.updateUser(directorPrincipal(), 101L, request);

        assertThat(firstMembership.getIsPrimary()).isFalse();
        assertThat(requestedMembership.getIsPrimary()).isTrue();
    }

    @Test
    @DisplayName("DEPT_HEAD 는 같은 부서 TEAM_LEAD 사용자를 부분 수정할 수 있다")
    void DEPT_HEAD는_같은_부서_TEAM_LEAD_사용자를_부분_수정할_수_있다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(201L, "dept-head@ibank.com", "DEPT_HEAD");
        User departmentHead = createUser(201L, 10L, "김부서", "부장", "사업부장", null, null, LocalDate.of(2023, 1, 1), UserRole.DEPT_HEAD, "dept-head@ibank.com");
        User teamLead = createUser(101L, 10L, "홍팀장", "과장", "팀장", null, null, LocalDate.of(2024, 1, 1), UserRole.TEAM_LEAD, "team-lead@ibank.com");
        UpdateUserApiDto.Request request = new UpdateUserApiDto.Request("홍수정", null, null, null, null, null, null, null, null, null);
        given(userRepository.findById(101L)).willReturn(Optional.of(teamLead));
        given(userRepository.findById(201L)).willReturn(Optional.of(departmentHead));

        userService.updateUser(principal, 101L, request);

        assertThat(teamLead.getUserName()).isEqualTo("홍수정");
    }

    @Test
    @DisplayName("DEPT_HEAD 는 같은 부서 MEMBER 사용자를 부분 수정할 수 있다")
    void DEPT_HEAD는_같은_부서_MEMBER_사용자를_부분_수정할_수_있다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(201L, "dept-head@ibank.com", "DEPT_HEAD");
        User departmentHead = createUser(201L, 10L, "김부서", "부장", "사업부장", null, null, LocalDate.of(2023, 1, 1), UserRole.DEPT_HEAD, "dept-head@ibank.com");
        User member = createUser(101L, 10L, "홍팀원", "대리", "팀원", null, null, LocalDate.of(2024, 1, 1), UserRole.MEMBER, "member@ibank.com");
        UpdateUserApiDto.Request request = new UpdateUserApiDto.Request("홍수정", null, null, null, null, null, null, null, null, null);
        given(userRepository.findById(101L)).willReturn(Optional.of(member));
        given(userRepository.findById(201L)).willReturn(Optional.of(departmentHead));

        userService.updateUser(principal, 101L, request);

        assertThat(member.getUserName()).isEqualTo("홍수정");
    }

    @Test
    @DisplayName("DEPT_HEAD 는 DIRECTOR 사용자를 부분 수정할 수 없다")
    void DEPT_HEAD는_DIRECTOR_사용자를_부분_수정할_수_없다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(201L, "dept-head@ibank.com", "DEPT_HEAD");
        User departmentHead = createUser(201L, 10L, "김부서", "부장", "사업부장", null, null, LocalDate.of(2023, 1, 1), UserRole.DEPT_HEAD, "dept-head@ibank.com");
        User director = createUser(1L, 10L, "박본부", "본부장", "본부장", null, null, LocalDate.of(2022, 1, 1), UserRole.DIRECTOR, "director@ibank.com");
        UpdateUserApiDto.Request request = new UpdateUserApiDto.Request("박수정", null, null, null, null, null, null, null, null, null);
        given(userRepository.findById(1L)).willReturn(Optional.of(director));
        given(userRepository.findById(201L)).willReturn(Optional.of(departmentHead));

        assertThatThrownBy(() -> userService.updateUser(principal, 1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_ACCESS_DENIED);
    }

    @Test
    @DisplayName("DEPT_HEAD 는 다른 DEPT_HEAD 사용자를 부분 수정할 수 없다")
    void DEPT_HEAD는_다른_DEPT_HEAD_사용자를_부분_수정할_수_없다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(201L, "dept-head@ibank.com", "DEPT_HEAD");
        User actor = createUser(201L, 10L, "김부서", "부장", "사업부장", null, null, LocalDate.of(2023, 1, 1), UserRole.DEPT_HEAD, "dept-head@ibank.com");
        User otherDepartmentHead = createUser(202L, 10L, "이부서", "부장", "사업부장", null, null, LocalDate.of(2023, 2, 1), UserRole.DEPT_HEAD, "other-head@ibank.com");
        UpdateUserApiDto.Request request = new UpdateUserApiDto.Request("이수정", null, null, null, null, null, null, null, null, null);
        given(userRepository.findById(202L)).willReturn(Optional.of(otherDepartmentHead));
        given(userRepository.findById(201L)).willReturn(Optional.of(actor));

        assertThatThrownBy(() -> userService.updateUser(principal, 202L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_ACCESS_DENIED);
    }

    @Test
    @DisplayName("DEPT_HEAD 는 다른 부서 TEAM_LEAD 사용자를 부분 수정할 수 없다")
    void DEPT_HEAD는_다른_부서_TEAM_LEAD_사용자를_부분_수정할_수_없다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(201L, "dept-head@ibank.com", "DEPT_HEAD");
        User departmentHead = createUser(201L, 10L, "김부서", "부장", "사업부장", null, null, LocalDate.of(2023, 1, 1), UserRole.DEPT_HEAD, "dept-head@ibank.com");
        User otherDepartmentTeamLead = createUser(101L, 20L, "홍팀장", "과장", "팀장", null, null, LocalDate.of(2024, 1, 1), UserRole.TEAM_LEAD, "team-lead@ibank.com");
        UpdateUserApiDto.Request request = new UpdateUserApiDto.Request("홍수정", null, null, null, null, null, null, null, null, null);
        given(userRepository.findById(101L)).willReturn(Optional.of(otherDepartmentTeamLead));
        given(userRepository.findById(201L)).willReturn(Optional.of(departmentHead));

        assertThatThrownBy(() -> userService.updateUser(principal, 101L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_ACCESS_DENIED);
    }

    @Test
    @DisplayName("DEPT_HEAD 는 같은 부서 사용자를 다른 부서로 이동시킬 수 없다")
    void DEPT_HEAD는_같은_부서_사용자를_다른_부서로_이동시킬_수_없다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(201L, "dept-head@ibank.com", "DEPT_HEAD");
        User departmentHead = createUser(201L, 10L, "김부서", "부장", "사업부장", null, null, LocalDate.of(2023, 1, 1), UserRole.DEPT_HEAD, "dept-head@ibank.com");
        User member = createUser(101L, 10L, "홍팀원", "대리", "팀원", null, null, LocalDate.of(2024, 1, 1), UserRole.MEMBER, "member@ibank.com");
        UpdateUserApiDto.Request request = new UpdateUserApiDto.Request(null, null, null, null, null, 20L, null, null, null, null);
        given(userRepository.findById(101L)).willReturn(Optional.of(member));
        given(userRepository.findById(201L)).willReturn(Optional.of(departmentHead));

        assertThatThrownBy(() -> userService.updateUser(principal, 101L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_ACCESS_DENIED);
    }

    private CustomUserPrincipal directorPrincipal() {
        return new CustomUserPrincipal(1L, "director@ibank.com", "DIRECTOR");
    }

    private User createUser(Long id,
                            Long departmentId,
                            String userName,
                            String positionName,
                            String titleName,
                            String profileImageUrl,
                            String phone,
                            LocalDate joinDate) {
        return createUser(
                id,
                departmentId,
                userName,
                positionName,
                titleName,
                profileImageUrl,
                phone,
                joinDate,
                UserRole.MEMBER,
                "user@ibank.com"
        );
    }

    private User createUser(Long id,
                            Long departmentId,
                            String userName,
                            String positionName,
                            String titleName,
                            String profileImageUrl,
                            String phone,
                            LocalDate joinDate,
                            UserRole roleCode,
                            String email) {
        User user = User.create(
                departmentId,
                userName,
                email,
                "$2a$10$fake-hashed",
                roleCode,
                EmploymentStatus.ACTIVE,
                positionName,
                titleName,
                joinDate,
                phone,
                profileImageUrl
        );
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Department createDepartment(Long id, String departmentName) {
        Department department = Department.create(departmentName, "테스트 부서");
        ReflectionTestUtils.setField(department, "id", id);
        return department;
    }
}
