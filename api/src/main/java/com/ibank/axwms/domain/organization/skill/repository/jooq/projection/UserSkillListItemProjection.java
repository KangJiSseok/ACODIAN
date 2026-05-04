package com.ibank.axwms.domain.organization.skill.repository.jooq.projection;

import static com.ibank.axwms.global.jooq.Tables.TB_USER_SKILL;

import java.time.LocalDateTime;
import org.jooq.Record;

/** 사용자 스킬 목록 한 행에 필요한 필드만 담는 읽기 전용 projection 이다. */
public record UserSkillListItemProjection(
        Long skillId,
        String skillName,
        Short skillLevel,
        LocalDateTime updatedAt
) {

    /** JOOQ 조회 결과 record 를 사용자 스킬 목록 projection 으로 조립한다. */
    public static UserSkillListItemProjection from(Record record) {
        return new UserSkillListItemProjection(
                record.get(TB_USER_SKILL.USER_SKILL_ID),
                record.get(TB_USER_SKILL.SKILL_NAME),
                record.get(TB_USER_SKILL.SKILL_LEVEL),
                record.get(TB_USER_SKILL.UPDATED_AT)
        );
    }
}
