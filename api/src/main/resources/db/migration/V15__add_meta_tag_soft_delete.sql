-- tb_meta_tag 에 소프트 삭제 컬럼을 추가한다.
-- MetaTag 엔티티에 is_deleted 가 정의되어 있으나 DB 스키마에는 누락되어 있어 동기화한다.
-- 기존 행은 모두 미삭제 상태로 보존하기 위해 DEFAULT FALSE 로 채운다.
-- 컨벤션은 tb_worklog 등 다른 테이블과 동일하게 BOOLEAN NOT NULL DEFAULT FALSE.

ALTER TABLE tb_meta_tag
    ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT FALSE;
