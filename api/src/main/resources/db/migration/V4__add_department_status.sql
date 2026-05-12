-- 부서 soft-delete 수명주기를 위해 상태 컬럼을 추가한다.
-- 기존 UNIQUE/FK 제약은 유지하므로 INACTIVE row 도 이름/부서장 재사용 대상에서 제외된다.
ALTER TABLE tb_department
    ADD COLUMN status_code VARCHAR(20);

UPDATE tb_department
SET status_code = 'ACTIVE'
WHERE status_code IS NULL;

ALTER TABLE tb_department
    ALTER COLUMN status_code SET DEFAULT 'ACTIVE';

ALTER TABLE tb_department
    ALTER COLUMN status_code SET NOT NULL;

ALTER TABLE tb_department
    ADD CONSTRAINT ck_department_status_code
        CHECK (status_code IN ('ACTIVE', 'INACTIVE'));
