package com.ibank.axwms.domain.organization.team.repository.jooq;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.jooq.Condition;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TeamMembershipConditionSupportTest {

    private final TeamMembershipConditionSupport support = new TeamMembershipConditionSupport();

    @Test
    @DisplayName("activeTeam 은 deleted_at is null 조건을 만든다")
    void activeTeam_은_deleted_at_is_null_조건을_만든다() {
        Condition condition = support.activeTeam(DSL.field(DSL.name("deleted_at"), LocalDateTime.class));

        String rendered = DSL.using(SQLDialect.POSTGRES).renderInlined(condition);

        assertThat(rendered).containsIgnoringCase("deleted_at");
        assertThat(rendered).containsIgnoringCase("is null");
    }

    @Test
    @DisplayName("activeMembership 은 status_code = ACTIVE 조건을 만든다")
    void activeMembership_은_status_code_ACTIVE_조건을_만든다() {
        Condition condition = support.activeMembership(DSL.field(DSL.name("status_code"), String.class));

        String rendered = DSL.using(SQLDialect.POSTGRES).renderInlined(condition);

        assertThat(rendered).containsIgnoringCase("status_code");
        assertThat(rendered).contains("ACTIVE");
    }
}
