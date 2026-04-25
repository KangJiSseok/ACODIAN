package com.ibank.axwms.domain.organization.team.repository.jooq;

import com.ibank.axwms.domain.organization.team.UserTeamStatus;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;
import org.jooq.Condition;
import org.jooq.Field;

@Component
public class TeamMembershipConditionSupport {

    /** soft-delete 되지 않은 팀만 남기는 조건을 만든다. */
    public Condition activeTeam(Field<LocalDateTime> deletedAtField) {
        return deletedAtField.isNull();
    }

    /** `/users/me` 와 team 조회 공통으로 재사용하는 ACTIVE membership 조건을 만든다. */
    public Condition activeMembership(Field<String> statusCodeField) {
        return statusCodeField.eq(UserTeamStatus.ACTIVE.name());
    }
}
