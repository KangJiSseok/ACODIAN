package com.ibank.axwms.domain.organization.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.ibank.axwms.domain.organization.department.entity.Department;
import com.ibank.axwms.domain.organization.department.repository.DepartmentRepository;
import com.ibank.axwms.domain.organization.team.TeamStatus;
import com.ibank.axwms.domain.organization.team.UserTeamStatus;
import com.ibank.axwms.domain.organization.team.entity.Team;
import com.ibank.axwms.domain.organization.team.entity.UserTeam;
import com.ibank.axwms.domain.organization.team.repository.TeamRepository;
import com.ibank.axwms.domain.organization.team.repository.UserTeamRepository;
import com.ibank.axwms.domain.organization.user.EmploymentStatus;
import com.ibank.axwms.domain.organization.user.UserRole;
import com.ibank.axwms.domain.organization.user.dto.GetMyProfileApiDto;
import com.ibank.axwms.domain.organization.user.entity.User;
import com.ibank.axwms.domain.organization.user.repository.UserRepository;
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private UserTeamRepository userTeamRepository;

    @Mock
    private TeamRepository teamRepository;

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
        UserTeam primaryUserTeam = createUserTeam(101L, 21L, true, UserTeamStatus.ACTIVE);
        UserTeam secondaryUserTeam = createUserTeam(101L, 22L, false, UserTeamStatus.ACTIVE);
        Team primaryTeam = createTeam(21L, 10L, "물류혁신TF", null);
        Team secondaryTeam = createTeam(22L, 10L, "SCM분석팀", null);

        given(userRepository.findById(101L)).willReturn(Optional.of(user));
        given(departmentRepository.findById(10L)).willReturn(Optional.of(department));
        given(userTeamRepository.findAllByUserIdAndStatusCodeOrderByIsPrimaryDesc(101L, UserTeamStatus.ACTIVE))
                .willReturn(List.of(primaryUserTeam, secondaryUserTeam));
        given(teamRepository.findAllById(List.of(21L, 22L))).willReturn(List.of(secondaryTeam, primaryTeam));

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
        given(userTeamRepository.findAllByUserIdAndStatusCodeOrderByIsPrimaryDesc(101L, UserTeamStatus.ACTIVE))
                .willReturn(List.of());

        GetMyProfileApiDto.Response result = userService.getMyProfile(principal);

        assertThat(result.email()).isEqualTo("user@ibank.com");
        assertThat(result.phone()).isNull();
        assertThat(result.joinDate()).isNull();
        assertThat(result.employmentStatus()).isEqualTo(EmploymentStatus.ACTIVE);
        assertThat(result.teams()).isEmpty();
    }

    @Test
    @DisplayName("LEFT membership 은 현재 사용자 팀 목록에서 제외한다")
    void LEFT_membership_은_현재_사용자_팀_목록에서_제외한다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(101L, "user@ibank.com", "MEMBER");
        User user = createUser(101L, 10L, "홍길동", "과장", "팀장", null, null, LocalDate.of(2025, 1, 1));
        Department department = createDepartment(10L, "물류본부");
        UserTeam activeUserTeam = createUserTeam(101L, 21L, true, UserTeamStatus.ACTIVE);
        Team activeTeam = createTeam(21L, 10L, "물류혁신TF", null);

        given(userRepository.findById(101L)).willReturn(Optional.of(user));
        given(departmentRepository.findById(10L)).willReturn(Optional.of(department));
        given(userTeamRepository.findAllByUserIdAndStatusCodeOrderByIsPrimaryDesc(101L, UserTeamStatus.ACTIVE))
                .willReturn(List.of(activeUserTeam));
        given(teamRepository.findAllById(List.of(21L))).willReturn(List.of(activeTeam));

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
        UserTeam userTeam = createUserTeam(101L, 21L, true, UserTeamStatus.ACTIVE);
        Team deletedTeam = createTeam(21L, 10L, "물류혁신TF", LocalDateTime.of(2026, 4, 25, 0, 0));

        given(userRepository.findById(101L)).willReturn(Optional.of(user));
        given(departmentRepository.findById(10L)).willReturn(Optional.of(department));
        given(userTeamRepository.findAllByUserIdAndStatusCodeOrderByIsPrimaryDesc(101L, UserTeamStatus.ACTIVE))
                .willReturn(List.of(userTeam));
        given(teamRepository.findAllById(List.of(21L))).willReturn(List.of(deletedTeam));

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

    private User createUser(Long id,
                            Long departmentId,
                            String userName,
                            String positionName,
                            String titleName,
                            String profileImageUrl,
                            String phone,
                            LocalDate joinDate) {
        User user = User.create(
                departmentId,
                userName,
                "user@ibank.com",
                "$2a$10$fake-hashed",
                UserRole.MEMBER,
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

    private Team createTeam(Long id, Long departmentId, String teamName, LocalDateTime deletedAt) {
        Team team = Team.create(
                teamName,
                TeamStatus.ACTIVE,
                "테스트 팀",
                LocalDate.of(2025, 1, 1),
                null
        );
        ReflectionTestUtils.setField(team, "id", id);
        ReflectionTestUtils.setField(team, "deletedAt", deletedAt);
        return team;
    }

    private UserTeam createUserTeam(Long userId, Long teamId, boolean isPrimary, UserTeamStatus statusCode) {
        return UserTeam.create(
                userId,
                teamId,
                isPrimary,
                isPrimary ? "플랫폼 총괄" : "SCM 분석",
                isPrimary ? "주담당" : "겸임",
                isPrimary,
                statusCode
        );
    }
}
