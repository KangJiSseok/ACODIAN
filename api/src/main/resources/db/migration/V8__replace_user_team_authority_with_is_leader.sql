ALTER TABLE tb_user_team
    ADD COLUMN is_leader BOOLEAN;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM tb_user_team
        WHERE team_authority = 'ADMIN'
    ) THEN
        RAISE EXCEPTION 'ADMIN team_authority rows must be migrated before replacing team_authority with is_leader';
    END IF;
END $$;

UPDATE tb_user_team
SET is_leader = team_authority = 'LEADER'
WHERE is_leader IS NULL;

ALTER TABLE tb_user_team
    ALTER COLUMN is_leader SET DEFAULT FALSE;

ALTER TABLE tb_user_team
    ALTER COLUMN is_leader SET NOT NULL;

DROP INDEX IF EXISTS uq_user_team_active_leader_per_team;

ALTER TABLE tb_user_team
    DROP CONSTRAINT IF EXISTS ck_user_team_authority;

ALTER TABLE tb_user_team
    DROP COLUMN team_authority;

CREATE UNIQUE INDEX uq_user_team_active_leader_per_team
    ON tb_user_team (team_id)
    WHERE status_code = 'ACTIVE' AND is_leader = TRUE;
