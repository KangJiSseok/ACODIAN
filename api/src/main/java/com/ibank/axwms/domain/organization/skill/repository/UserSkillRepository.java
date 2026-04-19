package com.ibank.axwms.domain.organization.skill.repository;

import com.ibank.axwms.domain.organization.skill.entity.UserSkill;
import com.ibank.axwms.domain.organization.skill.repository.jooq.UserSkillJooqRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSkillRepository extends JpaRepository<UserSkill, Long>, UserSkillJooqRepository {
}
