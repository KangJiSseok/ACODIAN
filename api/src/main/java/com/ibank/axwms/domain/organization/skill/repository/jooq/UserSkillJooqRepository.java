package com.ibank.axwms.domain.organization.skill.repository.jooq;

import com.ibank.axwms.domain.organization.skill.repository.jooq.projection.UserSkillListItemProjection;

import java.util.List;

public interface UserSkillJooqRepository {

    /** 특정 사용자의 스킬 목록을 최근 수정일 기준으로 조회한다. */
    List<UserSkillListItemProjection> findUserSkillsByUserId(Long userId);
}
