-- Flyway execution source of truth
-- Purpose: introduce explicit team admin grants and remove department ownership from teams.
-- Rule: this migration changes schema only and does not add seed data.

-- =====================================================================
-- Table. tb_team_admin
-- =====================================================================
CREATE TABLE tb_team_admin (
    team_admin_id         BIGSERIAL PRIMARY KEY,
    user_id               BIGINT NOT NULL,
    team_id               BIGINT NOT NULL,
    granted_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_team_admin_user_team
        UNIQUE (user_id, team_id),
    CONSTRAINT fk_team_admin_user
        FOREIGN KEY (user_id) REFERENCES tb_user(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_team_admin_team
        FOREIGN KEY (team_id) REFERENCES tb_team(team_id) ON DELETE CASCADE
);

CREATE INDEX ix_team_admin_team_id
    ON tb_team_admin (team_id);

-- =====================================================================
-- Table. tb_team
-- =====================================================================
DROP INDEX IF EXISTS uq_team_active_name_in_department;

ALTER TABLE tb_team
    DROP CONSTRAINT IF EXISTS fk_team_department;

ALTER TABLE tb_team
    DROP COLUMN department_id;
