-- Flyway execution source of truth
-- Purpose: add nullable organization scope references to notifications for future read/authorization filtering.
-- Rule: this migration changes schema only and does not backfill or seed data.

ALTER TABLE tb_notification
    ADD COLUMN department_id BIGINT NULL,
    ADD COLUMN team_id BIGINT NULL;

ALTER TABLE tb_notification
    ADD CONSTRAINT fk_notification_department
    FOREIGN KEY (department_id) REFERENCES tb_department(department_id);

ALTER TABLE tb_notification
    ADD CONSTRAINT fk_notification_team
    FOREIGN KEY (team_id) REFERENCES tb_team(team_id);
