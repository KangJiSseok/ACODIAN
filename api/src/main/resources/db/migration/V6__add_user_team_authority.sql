ALTER TABLE tb_user_team
    ADD COLUMN team_authority VARCHAR(20);

UPDATE tb_user_team
SET team_authority = CASE
    WHEN team_leader THEN 'LEADER'
    ELSE 'MEMBER'
END
WHERE team_authority IS NULL;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM tb_user_team
        WHERE status_code = 'ACTIVE'
          AND team_authority = 'LEADER'
        GROUP BY team_id
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'duplicate ACTIVE LEADER memberships exist in tb_user_team';
    END IF;
END $$;

ALTER TABLE tb_user_team
    ALTER COLUMN team_authority SET DEFAULT 'MEMBER';

ALTER TABLE tb_user_team
    ALTER COLUMN team_authority SET NOT NULL;

ALTER TABLE tb_user_team
    ADD CONSTRAINT ck_user_team_authority
        CHECK (team_authority IN ('MEMBER', 'LEADER', 'ADMIN'));

CREATE UNIQUE INDEX uq_user_team_active_leader_per_team
    ON tb_user_team (team_id)
    WHERE status_code = 'ACTIVE' AND team_authority = 'LEADER';
