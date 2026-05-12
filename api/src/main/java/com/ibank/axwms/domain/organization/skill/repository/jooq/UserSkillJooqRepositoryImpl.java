package com.ibank.axwms.domain.organization.skill.repository.jooq;

import static com.ibank.axwms.global.jooq.Tables.TB_USER_SKILL;

import com.ibank.axwms.domain.organization.skill.repository.jooq.projection.UserSkillListItemProjection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserSkillJooqRepositoryImpl implements UserSkillJooqRepository {

    private final DSLContext dsl;

    /** 높은 레벨 순서로 사용자 스킬 목록을 조회한다. */
    @Override
    public List<UserSkillListItemProjection> findUserSkillsByUserId(Long userId) {
        return dsl.select(
                        TB_USER_SKILL.USER_SKILL_ID,
                        TB_USER_SKILL.SKILL_NAME,
                        TB_USER_SKILL.SKILL_LEVEL,
                        TB_USER_SKILL.UPDATED_AT
                )
                .from(TB_USER_SKILL)
                .where(TB_USER_SKILL.USER_ID.eq(userId))
                .orderBy(TB_USER_SKILL.SKILL_LEVEL.desc(), TB_USER_SKILL.SKILL_NAME.asc())
                .fetch(UserSkillListItemProjection::from);
    }
}
