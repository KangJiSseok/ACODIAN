package com.ibank.axwms.domain.organization.skill.service;

import com.ibank.axwms.domain.organization.skill.dto.CreateSkillApiDto;
import com.ibank.axwms.domain.organization.skill.dto.GetSkillsApiDto;
import com.ibank.axwms.domain.organization.skill.entity.UserSkill;
import com.ibank.axwms.domain.organization.skill.repository.UserSkillRepository;
import com.ibank.axwms.domain.organization.skill.repository.jooq.projection.UserSkillListItemProjection;
import com.ibank.axwms.domain.organization.user.UserRole;
import com.ibank.axwms.domain.organization.user.entity.User;
import com.ibank.axwms.domain.organization.user.repository.UserRepository;
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserSkillService {

    private final UserSkillRepository userSkillRepository;
    private final UserRepository userRepository;

    /**
     * 특정 사용자의 보유 스킬 목록을 조회한다.
     * 자기 자신 또는 DEPT_HEAD 가 DIRECTOR 를 조회하면 스킬을 노출하지 않고 빈 목록을 반환한다.
     */
    public GetSkillsApiDto.Response getSkills(CustomUserPrincipal principal, Long userId) {
        validateOrganizationManagerRole(principal);

        User targetUser = getUserOrThrow(userId);
        if (shouldHideSkills(principal, targetUser)) {
            return GetSkillsApiDto.Response.of(userId, List.of());
        }
        validateReadableScope(principal, targetUser);

        List<UserSkillListItemProjection> skills = userSkillRepository.findUserSkillsByUserId(userId);
        return GetSkillsApiDto.Response.of(userId, skills);
    }

    /** 특정 사용자에게 스킬을 등록한다. 조직 관리자만 자기 자신을 제외한 허용 범위 사용자에게 등록할 수 있다. */
    @Transactional
    public void createSkill(CustomUserPrincipal principal, Long userId, CreateSkillApiDto.Request request) {
        validateOrganizationManagerRole(principal);

        User targetUser = getUserOrThrow(userId);
        validateWritable(principal, targetUser);
        ensureSkillNameAvailable(userId, request.skillName());

        userSkillRepository.save(UserSkill.create(userId, request.skillName(), request.skillLevel()));
    }

    /** 스킬 API 는 조직 관리자에게만 열어 둔다. */
    private void validateOrganizationManagerRole(CustomUserPrincipal principal) {
        if (isOrganizationManager(principal)) {
            return;
        }
        throw new BusinessException(ErrorCode.AUTH_ACCESS_DENIED);
    }

    /** DEPT_HEAD 는 자기 부서 사용자 스킬만 조회할 수 있다. */
    private void validateReadableScope(CustomUserPrincipal principal, User targetUser) {
        if (UserRole.DIRECTOR.name().equals(principal.roleCode())) {
            return;
        }

        User actor = getUserOrThrow(principal.userId());
        if (!actor.getDepartmentId().equals(targetUser.getDepartmentId())) {
            throw new BusinessException(ErrorCode.AUTH_ACCESS_DENIED);
        }
    }

    /** 스킬 등록 권한과 DEPT_HEAD 의 부서 범위를 함께 검증한다. */
    private void validateWritable(CustomUserPrincipal principal, User targetUser) {
        if (!isOrganizationManager(principal) || principal.userId().equals(targetUser.getId())) {
            throw new BusinessException(ErrorCode.AUTH_ACCESS_DENIED);
        }
        if (UserRole.DIRECTOR.name().equals(principal.roleCode())) {
            return;
        }
        if (UserRole.DIRECTOR.equals(targetUser.getRoleCode())) {
            throw new BusinessException(ErrorCode.AUTH_ACCESS_DENIED);
        }

        User actor = getUserOrThrow(principal.userId());
        if (!actor.getDepartmentId().equals(targetUser.getDepartmentId())) {
            throw new BusinessException(ErrorCode.AUTH_ACCESS_DENIED);
        }
    }

    /** 사용자별 스킬명 UNIQUE 충돌을 사전에 확인한다. */
    private void ensureSkillNameAvailable(Long userId, String skillName) {
        if (userSkillRepository.existsByUserIdAndSkillName(userId, skillName)) {
            throw new BusinessException(ErrorCode.USER_SKILL_DUPLICATE_NAME);
        }
    }

    /** 조회는 성공시키되 목록을 비워야 하는 비노출 정책을 판별한다. */
    private boolean shouldHideSkills(CustomUserPrincipal principal, User targetUser) {
        return principal.userId().equals(targetUser.getId())
                || isDeptHeadReadingDirector(principal, targetUser);
    }

    /** DEPT_HEAD 가 상위 역할인 DIRECTOR 의 스킬을 보지 못하게 한다. */
    private boolean isDeptHeadReadingDirector(CustomUserPrincipal principal, User targetUser) {
        return UserRole.DEPT_HEAD.name().equals(principal.roleCode())
                && UserRole.DIRECTOR.equals(targetUser.getRoleCode());
    }

    /** 조직 관리자 역할인지 확인한다. */
    private boolean isOrganizationManager(CustomUserPrincipal principal) {
        return UserRole.DIRECTOR.name().equals(principal.roleCode())
                || UserRole.DEPT_HEAD.name().equals(principal.roleCode());
    }

    /** 대상 사용자가 존재하지 않으면 USER_NOT_FOUND 로 변환한다. */
    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
