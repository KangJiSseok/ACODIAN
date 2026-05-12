-- Flyway execution source of truth
-- Purpose: enforce that each user has at most one active primary team membership.
-- Rule: this migration changes schema only and does not backfill or seed data.

CREATE UNIQUE INDEX uq_user_team_active_primary_per_user
    ON tb_user_team (user_id)
    WHERE status_code = 'ACTIVE' AND is_primary = TRUE;
