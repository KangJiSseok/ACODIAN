package com.ibank.axwms.domain.organization.skill.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.ibank.axwms.domain.organization.skill.dto.CreateSkillApiDto;
import com.ibank.axwms.domain.organization.skill.dto.GetSkillsApiDto;
import com.ibank.axwms.domain.organization.skill.dto.UpdateSkillApiDto;
import com.ibank.axwms.domain.organization.skill.entity.UserSkill;
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
import org.mockito.ArgumentCaptor;
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
    @DisplayName("사업부장이 다른 부서 사용자 스킬 목록을 조회하면 접근 거부 예외를 던진다")
    void 사업부장이_다른_부서_사용자_스킬_목록을_조회하면_접근_거부_예외를_던진다() {
        // given
        given(userRepository.findById(OTHER_USER_ID)).willReturn(Optional.of(user(OTHER_USER_ID, UserRole.MEMBER, 20L)));
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user(USER_ID, UserRole.DEPT_HEAD, 10L)));

        // when & then
        assertThatThrownBy(() -> userSkillService.getSkills(
                principal(USER_ID, UserRole.DEPT_HEAD),
                OTHER_USER_ID
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_ACCESS_DENIED);

        verifyNoInteractions(userSkillRepository);
    }

    @Test
    @DisplayName("본부장이 다른 사용자 스킬을 등록하면 저장한다")
    void 본부장이_다른_사용자_스킬을_등록하면_저장한다() {
        // given
        given(userRepository.findById(OTHER_USER_ID)).willReturn(Optional.of(user(OTHER_USER_ID, UserRole.MEMBER)));
        given(userSkillRepository.existsByUserIdAndSkillName(OTHER_USER_ID, "WMS")).willReturn(false);

        // when
        userSkillService.createSkill(
                principal(USER_ID, UserRole.DIRECTOR),
                OTHER_USER_ID,
                request("WMS", (short) 5)
        );

        // then
        ArgumentCaptor<UserSkill> captor = ArgumentCaptor.forClass(UserSkill.class);
        verify(userSkillRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(OTHER_USER_ID);
        assertThat(captor.getValue().getSkillName()).isEqualTo("WMS");
        assertThat(captor.getValue().getSkillLevel()).isEqualTo((short) 5);
    }

    @Test
    @DisplayName("사업부장이 같은 부서 사용자 스킬을 등록하면 저장한다")
    void 사업부장이_같은_부서_사용자_스킬을_등록하면_저장한다() {
        // given
        given(userRepository.findById(OTHER_USER_ID)).willReturn(Optional.of(user(OTHER_USER_ID, UserRole.MEMBER, 10L)));
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user(USER_ID, UserRole.DEPT_HEAD, 10L)));
        given(userSkillRepository.existsByUserIdAndSkillName(OTHER_USER_ID, "WMS")).willReturn(false);

        // when
        userSkillService.createSkill(
                principal(USER_ID, UserRole.DEPT_HEAD),
                OTHER_USER_ID,
                request("WMS", (short) 5)
        );

        // then
        ArgumentCaptor<UserSkill> captor = ArgumentCaptor.forClass(UserSkill.class);
        verify(userSkillRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(OTHER_USER_ID);
    }

    @Test
    @DisplayName("스킬명 양끝 공백을 제거한 뒤 등록한다")
    void create_skill_trims_skill_name() {
        // given
        given(userRepository.findById(OTHER_USER_ID)).willReturn(Optional.of(user(OTHER_USER_ID, UserRole.MEMBER)));
        given(userSkillRepository.existsByUserIdAndSkillName(OTHER_USER_ID, "WMS")).willReturn(false);

        // when
        userSkillService.createSkill(
                principal(USER_ID, UserRole.DIRECTOR),
                OTHER_USER_ID,
                request("  WMS  ", (short) 5)
        );

        // then
        ArgumentCaptor<UserSkill> captor = ArgumentCaptor.forClass(UserSkill.class);
        verify(userSkillRepository).save(captor.capture());
        assertThat(captor.getValue().getSkillName()).isEqualTo("WMS");
    }

    @Test
    @DisplayName("사업부장이 다른 부서 사용자 스킬을 등록하면 접근 거부 예외를 던진다")
    void 사업부장이_다른_부서_사용자_스킬을_등록하면_접근_거부_예외를_던진다() {
        // given
        given(userRepository.findById(OTHER_USER_ID)).willReturn(Optional.of(user(OTHER_USER_ID, UserRole.MEMBER, 20L)));
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user(USER_ID, UserRole.DEPT_HEAD, 10L)));

        // when & then
        assertThatThrownBy(() -> userSkillService.createSkill(
                principal(USER_ID, UserRole.DEPT_HEAD),
                OTHER_USER_ID,
                request("WMS", (short) 5)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_ACCESS_DENIED);

        verifyNoInteractions(userSkillRepository);
    }

    @Test
    @DisplayName("사업부장이 본부장 스킬을 등록하면 접근 거부 예외를 던진다")
    void 사업부장이_본부장_스킬을_등록하면_접근_거부_예외를_던진다() {
        // given
        given(userRepository.findById(OTHER_USER_ID)).willReturn(Optional.of(user(OTHER_USER_ID, UserRole.DIRECTOR, 10L)));

        // when & then
        assertThatThrownBy(() -> userSkillService.createSkill(
                principal(USER_ID, UserRole.DEPT_HEAD),
                OTHER_USER_ID,
                request("WMS", (short) 5)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_ACCESS_DENIED);

        verifyNoInteractions(userSkillRepository);
    }

    @Test
    @DisplayName("조직 관리자가 자기 스킬을 등록하면 접근 거부 예외를 던진다")
    void 조직_관리자가_자기_스킬을_등록하면_접근_거부_예외를_던진다() {
        // given
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user(USER_ID, UserRole.DEPT_HEAD, 10L)));

        // when & then
        assertThatThrownBy(() -> userSkillService.createSkill(
                principal(USER_ID, UserRole.DEPT_HEAD),
                USER_ID,
                request("WMS", (short) 5)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_ACCESS_DENIED);

        verifyNoInteractions(userSkillRepository);
    }

    @Test
    @DisplayName("일반 사용자가 스킬을 등록하면 접근 거부 예외를 던진다")
    void 일반_사용자가_스킬을_등록하면_접근_거부_예외를_던진다() {
        assertThatThrownBy(() -> userSkillService.createSkill(
                principal(USER_ID, UserRole.MEMBER),
                OTHER_USER_ID,
                request("WMS", (short) 5)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_ACCESS_DENIED);

        verifyNoInteractions(userRepository, userSkillRepository);
    }

    @Test
    @DisplayName("같은 사용자에게 같은 스킬명이 있으면 중복 예외를 던진다")
    void 같은_사용자에게_같은_스킬명이_있으면_중복_예외를_던진다() {
        // given
        given(userRepository.findById(OTHER_USER_ID)).willReturn(Optional.of(user(OTHER_USER_ID, UserRole.MEMBER)));
        given(userSkillRepository.existsByUserIdAndSkillName(OTHER_USER_ID, "WMS")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> userSkillService.createSkill(
                principal(USER_ID, UserRole.DIRECTOR),
                OTHER_USER_ID,
                request("WMS", (short) 5)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_SKILL_DUPLICATE_NAME);
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

    @Test
    @DisplayName("본부장이 다른 사용자 스킬을 수정하면 요청 값만 반영한다")
    void director_updates_other_user_skill() {
        // given
        UserSkill userSkill = userSkill(1L, OTHER_USER_ID, "SQL", (short) 3);
        given(userRepository.findById(OTHER_USER_ID)).willReturn(Optional.of(user(OTHER_USER_ID, UserRole.MEMBER)));
        given(userSkillRepository.findByIdAndUserId(1L, OTHER_USER_ID)).willReturn(Optional.of(userSkill));
        given(userSkillRepository.existsByUserIdAndSkillNameAndIdNot(OTHER_USER_ID, "WMS", 1L)).willReturn(false);

        // when
        userSkillService.updateSkill(
                principal(USER_ID, UserRole.DIRECTOR),
                OTHER_USER_ID,
                1L,
                updateRequest("WMS", (short) 5)
        );

        // then
        assertThat(userSkill.getSkillName()).isEqualTo("WMS");
        assertThat(userSkill.getSkillLevel()).isEqualTo((short) 5);
    }

    @Test
    @DisplayName("스킬명 양끝 공백을 제거한 뒤 수정한다")
    void update_skill_trims_skill_name() {
        // given
        UserSkill userSkill = userSkill(1L, OTHER_USER_ID, "SQL", (short) 3);
        given(userRepository.findById(OTHER_USER_ID)).willReturn(Optional.of(user(OTHER_USER_ID, UserRole.MEMBER)));
        given(userSkillRepository.findByIdAndUserId(1L, OTHER_USER_ID)).willReturn(Optional.of(userSkill));
        given(userSkillRepository.existsByUserIdAndSkillNameAndIdNot(OTHER_USER_ID, "WMS", 1L)).willReturn(false);

        // when
        userSkillService.updateSkill(
                principal(USER_ID, UserRole.DIRECTOR),
                OTHER_USER_ID,
                1L,
                updateRequest("  WMS  ", null)
        );

        // then
        assertThat(userSkill.getSkillName()).isEqualTo("WMS");
        assertThat(userSkill.getSkillLevel()).isEqualTo((short) 3);
    }

    @Test
    @DisplayName("스킬명 수정값이 blank 이면 기존 스킬명을 유지한다")
    void update_skill_blank_name_keeps_existing_name() {
        // given
        UserSkill userSkill = userSkill(1L, OTHER_USER_ID, "SQL", (short) 3);
        given(userRepository.findById(OTHER_USER_ID)).willReturn(Optional.of(user(OTHER_USER_ID, UserRole.MEMBER)));
        given(userSkillRepository.findByIdAndUserId(1L, OTHER_USER_ID)).willReturn(Optional.of(userSkill));

        // when
        userSkillService.updateSkill(
                principal(USER_ID, UserRole.DIRECTOR),
                OTHER_USER_ID,
                1L,
                updateRequest("   ", (short) 4)
        );

        // then
        assertThat(userSkill.getSkillName()).isEqualTo("SQL");
        assertThat(userSkill.getSkillLevel()).isEqualTo((short) 4);
        verify(userSkillRepository, never()).existsByUserIdAndSkillNameAndIdNot(any(), any(), any());
    }

    @Test
    @DisplayName("스킬명 수정 시 같은 사용자의 다른 스킬명과 중복되면 예외를 던진다")
    void update_skill_duplicate_name_throws_exception() {
        // given
        UserSkill userSkill = userSkill(1L, OTHER_USER_ID, "SQL", (short) 3);
        given(userRepository.findById(OTHER_USER_ID)).willReturn(Optional.of(user(OTHER_USER_ID, UserRole.MEMBER)));
        given(userSkillRepository.findByIdAndUserId(1L, OTHER_USER_ID)).willReturn(Optional.of(userSkill));
        given(userSkillRepository.existsByUserIdAndSkillNameAndIdNot(OTHER_USER_ID, "WMS", 1L)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> userSkillService.updateSkill(
                principal(USER_ID, UserRole.DIRECTOR),
                OTHER_USER_ID,
                1L,
                updateRequest("WMS", null)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_SKILL_DUPLICATE_NAME);
    }

    @Test
    @DisplayName("path 사용자에게 속한 스킬이 없으면 예외를 던진다")
    void update_skill_not_found_throws_exception() {
        // given
        given(userRepository.findById(OTHER_USER_ID)).willReturn(Optional.of(user(OTHER_USER_ID, UserRole.MEMBER)));
        given(userSkillRepository.findByIdAndUserId(1L, OTHER_USER_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userSkillService.updateSkill(
                principal(USER_ID, UserRole.DIRECTOR),
                OTHER_USER_ID,
                1L,
                updateRequest("WMS", null)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_SKILL_NOT_FOUND);
    }

    @Test
    @DisplayName("조직 관리자가 자기 스킬을 수정하면 접근 거부 예외를 던진다")
    void organization_manager_updates_own_skill_throws_exception() {
        // given
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user(USER_ID, UserRole.DEPT_HEAD, 10L)));

        // when & then
        assertThatThrownBy(() -> userSkillService.updateSkill(
                principal(USER_ID, UserRole.DEPT_HEAD),
                USER_ID,
                1L,
                updateRequest("WMS", (short) 5)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_ACCESS_DENIED);

        verifyNoInteractions(userSkillRepository);
    }

    @Test
    @DisplayName("일반 사용자가 스킬을 수정하면 접근 거부 예외를 던진다")
    void member_updates_skill_throws_exception() {
        assertThatThrownBy(() -> userSkillService.updateSkill(
                principal(USER_ID, UserRole.MEMBER),
                OTHER_USER_ID,
                1L,
                updateRequest("WMS", (short) 5)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_ACCESS_DENIED);

        verifyNoInteractions(userRepository, userSkillRepository);
    }

    @Test
    @DisplayName("본부장이 다른 사용자 스킬을 삭제하면 삭제한다")
    void director_deletes_other_user_skill() {
        // given
        UserSkill userSkill = userSkill(1L, OTHER_USER_ID, "SQL", (short) 3);
        given(userRepository.findById(OTHER_USER_ID)).willReturn(Optional.of(user(OTHER_USER_ID, UserRole.MEMBER)));
        given(userSkillRepository.findByIdAndUserId(1L, OTHER_USER_ID)).willReturn(Optional.of(userSkill));

        // when
        userSkillService.deleteSkill(
                principal(USER_ID, UserRole.DIRECTOR),
                OTHER_USER_ID,
                1L
        );

        // then
        verify(userSkillRepository).delete(userSkill);
    }

    @Test
    @DisplayName("삭제 대상 스킬이 path 사용자에게 속하지 않으면 예외를 던진다")
    void delete_skill_not_found_throws_exception() {
        // given
        given(userRepository.findById(OTHER_USER_ID)).willReturn(Optional.of(user(OTHER_USER_ID, UserRole.MEMBER)));
        given(userSkillRepository.findByIdAndUserId(1L, OTHER_USER_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userSkillService.deleteSkill(
                principal(USER_ID, UserRole.DIRECTOR),
                OTHER_USER_ID,
                1L
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_SKILL_NOT_FOUND);
    }

    @Test
    @DisplayName("조직 관리자가 자기 스킬을 삭제하면 접근 거부 예외를 던진다")
    void organization_manager_deletes_own_skill_throws_exception() {
        // given
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user(USER_ID, UserRole.DEPT_HEAD, 10L)));

        // when & then
        assertThatThrownBy(() -> userSkillService.deleteSkill(
                principal(USER_ID, UserRole.DEPT_HEAD),
                USER_ID,
                1L
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_ACCESS_DENIED);

        verifyNoInteractions(userSkillRepository);
    }

    @Test
    @DisplayName("일반 사용자가 스킬을 삭제하면 접근 거부 예외를 던진다")
    void member_deletes_skill_throws_exception() {
        assertThatThrownBy(() -> userSkillService.deleteSkill(
                principal(USER_ID, UserRole.MEMBER),
                OTHER_USER_ID,
                1L
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_ACCESS_DENIED);

        verifyNoInteractions(userRepository, userSkillRepository);
    }

    private static CustomUserPrincipal principal(Long userId, UserRole role) {
        return new CustomUserPrincipal(userId, "user@test.com", role.name());
    }

    private static User user(Long userId, UserRole role) {
        return user(userId, role, 10L);
    }

    private static User user(Long userId, UserRole role, Long departmentId) {
        User user = User.create(
                departmentId,
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

    private static CreateSkillApiDto.Request request(String skillName, Short skillLevel) {
        return new CreateSkillApiDto.Request(skillName, skillLevel);
    }

    private static UpdateSkillApiDto.Request updateRequest(String skillName, Short skillLevel) {
        return new UpdateSkillApiDto.Request(skillName, skillLevel);
    }

    private static UserSkill userSkill(Long id, Long userId, String skillName, Short skillLevel) {
        UserSkill userSkill = UserSkill.create(userId, skillName, skillLevel);
        ReflectionTestUtils.setField(userSkill, "id", id);
        return userSkill;
    }
}
