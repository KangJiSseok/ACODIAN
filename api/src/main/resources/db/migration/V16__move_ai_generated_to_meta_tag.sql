-- description 은 후속 정책에서 채우는 nullable 저장 슬롯으로만 추가한다.
-- is_ai_generated 는 태그 생성 출처를 future-only 로 기록하므로 기존 태그/연결 값은 승격하지 않는다.
ALTER TABLE tb_meta_tag
    ADD COLUMN description TEXT,
    ADD COLUMN is_ai_generated BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE tb_worklog_tag
    DROP COLUMN is_ai_generated;
