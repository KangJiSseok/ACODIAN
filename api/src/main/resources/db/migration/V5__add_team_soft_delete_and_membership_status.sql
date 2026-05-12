ALTER TABLE tb_team
    ADD COLUMN deleted_at TIMESTAMP;

ALTER TABLE tb_team
    DROP CONSTRAINT uq_team_name_in_department;

CREATE UNIQUE INDEX uq_team_active_name_in_department
    ON tb_team (department_id, team_name)
    WHERE deleted_at IS NULL;

ALTER TABLE tb_user_team
    ADD COLUMN status_code VARCHAR(20);

UPDATE tb_user_team
SET status_code = 'ACTIVE'
WHERE status_code IS NULL;

ALTER TABLE tb_user_team
    ALTER COLUMN status_code SET DEFAULT 'ACTIVE';

ALTER TABLE tb_user_team
    ALTER COLUMN status_code SET NOT NULL;

ALTER TABLE tb_user_team
    ADD CONSTRAINT ck_user_team_status_code
        CHECK (status_code IN ('ACTIVE', 'LEFT'));
