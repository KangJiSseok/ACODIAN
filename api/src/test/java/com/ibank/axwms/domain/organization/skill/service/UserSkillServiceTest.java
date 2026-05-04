package com.ibank.axwms.domain.organization.skill.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import com.ibank.axwms.domain.organization.skill.dto.GetSkillsApiDto;
import com.ibank.axwms.domain.organization.skill.repository.UserSkillRepository;
import com.ibank.axwms.domain.organization.skill.repository.jooq.projection.UserSkillListItemProjection;
import com.ibank.axwms.domain.organization.user.EmploymentStatus;
import com.ibank.axwms.domain.organization.user.UserRole;
import com.ibank.axwms.domain.organization.user.entity.User;
import com.ibank.axwms.domain.organization.user.repository.UserRepository;
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserSkillServiceTest {

    private static final Long USER_ID = 101L;
    private static final Long OTHER_USER_ID = 202L;

    @Mock private UserSkillRepository userSkillRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private UserSkillService userSkillService;

    @Test
    @DisplayName("본부장이 다른 사용자 스킬 목록을 조회하면 사용자 ID와 스킬 목록을 반환한다")
    void 본부장이_다른_사용자_스킬_목록을_조회하면_사용자_ID와_스킬_목록을_반환한다() {
        // given
        LocalDateTime updatedAt = LocalDateTime.of(2026, 4, 20, 9, 0);

        given(userRepository.findById(OTHER_USER_ID)).willReturn(Optional.of(user(OTHER_USER_ID, UserRole.MEMBER)));
        given(userSkillRepository.findUserSkillsByUserId(OTHER_USER_ID))
                .willReturn(List.of(new UserSkillListItemProjection(1L, "WMS", (short) 5, updatedAt)));

        // when
        GetSkillsApiDto.Response response = userSkillService.getSkills(
                principal(USER_ID, UserRole.DIRECTOR),
                OTHER_USER_ID
        );

        // then
        assertThat(response.userId()).isEqualTo(OTHER_USER_ID);
        assertThat(response.skills()).hasSize(1);
        assertThat(response.skills().get(0).skillId()).isEqualTo(1L);
        assertThat(response.skills().get(0).skillName()).isEqualTo("WMS");
        assertThat(response.skills().get(0).skillLevel()).isEqualTo((short) 5);
        assertThat(response.skills().get(0).updatedAt()).isEqualTo(updatedAt);
    }

    @Test
    @DisplayName("조직 관리자가 자기 스킬 목록을 조회하면 빈 목록을 반환한다")
    void 조직_관리자가_자기_스킬_목록을_조회하면_빈_목록을_반환한다() {
        // given
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user(USER_ID, UserRole.DEPT_HEAD)));

        // when
        GetSkillsApiDto.Response response = userSkillService.getSkills(
                principal(USER_ID, UserRole.DEPT_HEAD),
                USER_ID
        );

        // then
        assertThat(response.userId()).isEqualTo(USER_ID);
        assertThat(response.skills()).isEmpty();
        verifyNoInteractions(userSkillRepository);
    }

    @Test
    @DisplayName("사업부장이 본부장 스킬 목록을 조회하면 빈 목록을 반환한다")
    void 사업부장이_본부장_스킬_목록을_조회하면_빈_목록을_반환한다() {
        // given
        given(userRepository.findById(OTHER_USER_ID)).willReturn(Optional.of(user(OTHER_USER_ID, UserRole.DIRECTOR)));

        // when
        GetSkillsApiDto.Response response = userSkillService.getSkills(
                principal(USER_ID, UserRole.DEPT_HEAD),
                OTHER_USER_ID
        );

        // then
        assertThat(response.userId()).isEqualTo(OTHER_USER_ID);
        assertThat(response.skills()).isEmpty();
        verifyNoInteractions(userSkillRepository);
    }

    @Test
    @DisplayName("일반 사용자가 다른 사용자 스킬 목록을 조회하면 접근 거부 예외를 던진다")
    void 일반_사용자가_다른_사용자_스킬_목록을_조회하면_접근_거부_예외를_던진다() {
        assertThatThrownBy(() -> userSkillService.getSkills(
                principal(USER_ID, UserRole.MEMBER),
                OTHER_USER_ID
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_ACCESS_DENIED);

        verifyNoInteractions(userRepository, userSkillRepository);
    }

    @Test
    @DisplayName("대상 사용자가 없으면 예외를 던진다")
    void 대상_사용자가_없으면_예외를_던진다() {
        // given
        given(userRepository.findById(OTHER_USER_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userSkillService.getSkills(
                principal(USER_ID, UserRole.DIRECTOR),
                OTHER_USER_ID
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);

        verifyNoInteractions(userSkillRepository);
    }

    private static CustomUserPrincipal principal(Long userId, UserRole role) {
        return new CustomUserPrincipal(userId, "user@test.com", role.name());
    }

    private static User user(Long userId, UserRole role) {
        User user = User.create(
                10L,
                "사용자" + userId,
                "user" + userId + "@test.com",
                "encoded",
                role,
                EmploymentStatus.ACTIVE,
                "사원",
                "팀원",
                LocalDate.of(2026, 1, 1),
                "010-0000-0000",
                null
        );
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }
}
