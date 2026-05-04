package com.ibank.axwms.domain.organization.skill.repository;

import com.ibank.axwms.domain.organization.skill.entity.UserSkill;
import com.ibank.axwms.domain.organization.skill.repository.jooq.UserSkillJooqRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSkillRepository extends JpaRepository<UserSkill, Long>, UserSkillJooqRepository {

    /** 한 사용자가 같은 스킬명을 중복 등록했는지 확인한다. */
    boolean existsByUserIdAndSkillName(Long userId, String skillName);
}
