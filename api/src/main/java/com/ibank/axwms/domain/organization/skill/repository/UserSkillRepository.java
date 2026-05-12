package com.ibank.axwms.domain.organization.skill.repository;

import com.ibank.axwms.domain.organization.skill.entity.UserSkill;
import com.ibank.axwms.domain.organization.skill.repository.jooq.UserSkillJooqRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSkillRepository extends JpaRepository<UserSkill, Long>, UserSkillJooqRepository {

    /** 한 사용자가 같은 스킬명을 중복 등록했는지 확인한다. */
    boolean existsByUserIdAndSkillName(Long userId, String skillName);

    /** 현재 스킬 레코드를 제외하고 같은 사용자의 동일 스킬명이 이미 있는지 확인한다. */
    boolean existsByUserIdAndSkillNameAndIdNot(Long userId, String skillName, Long id);

    /** path 의 사용자와 스킬 레코드 소유자가 일치하는 스킬만 조회한다. */
    Optional<UserSkill> findByIdAndUserId(Long id, Long userId);
}
