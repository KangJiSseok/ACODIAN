CREATE TABLE tb_tag_merge_candidate (
    merge_candidate_id BIGSERIAL PRIMARY KEY,
    target_tag_id BIGINT NOT NULL REFERENCES tb_meta_tag(tag_id),
    target_tag_name VARCHAR(100) NOT NULL,
    result_description VARCHAR(150),
    status_code VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_tag_merge_candidate_status
        CHECK (status_code IN ('PENDING', 'APPLIED', 'REJECTED'))
);

CREATE TABLE tb_tag_merge_candidate_item (
    merge_candidate_item_id BIGSERIAL PRIMARY KEY,
    merge_candidate_id BIGINT NOT NULL REFERENCES tb_tag_merge_candidate(merge_candidate_id) ON DELETE CASCADE,
    source_tag_id BIGINT NOT NULL REFERENCES tb_meta_tag(tag_id),
    source_tag_name VARCHAR(100) NOT NULL,

    CONSTRAINT uq_tag_merge_candidate_item UNIQUE (merge_candidate_id, source_tag_id)
);

CREATE INDEX idx_tag_merge_candidate_status
ON tb_tag_merge_candidate(status_code);

CREATE INDEX idx_tag_merge_candidate_item_source_tag
ON tb_tag_merge_candidate_item(source_tag_id);
