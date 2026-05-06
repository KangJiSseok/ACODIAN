-- Flyway execution source of truth
-- Purpose: restore nullable department ownership on teams for department detail/list/delete read semantics.
-- Rule: this migration changes schema only and does not backfill or seed data.

ALTER TABLE tb_team
    ADD COLUMN department_id BIGINT NULL;

ALTER TABLE tb_team
    ADD CONSTRAINT fk_team_department
    FOREIGN KEY (department_id) REFERENCES tb_department(department_id);

CREATE INDEX ix_team_department_id
    ON tb_team (department_id);

CREATE INDEX ix_team_department_active_not_deleted
    ON tb_team (department_id, status_code, team_id)
    WHERE deleted_at IS NULL;
