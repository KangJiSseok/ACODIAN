-- Flyway execution source of truth
-- Purpose: create all baseline tables for AX-WMS without foreign keys.
-- Rule: this file must not contain REFERENCES clauses or FOREIGN KEY constraints.

-- =====================================================================
-- Table 1. tb_department
-- =====================================================================
CREATE TABLE tb_department (
    department_id        BIGSERIAL PRIMARY KEY,                 -- PK, 부서 식별자
    department_name      VARCHAR(100) NOT NULL UNIQUE,         -- 부서명, 전사/본부 내 유니크
    description          TEXT,                                 -- 부서 설명
    department_head_user_id BIGINT UNIQUE,                     -- 1:1, 부서장 사용자 ID -> tb_user.user_id
    created_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 생성 시각
    updated_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP  -- 수정 시각
);

-- =====================================================================
-- Table 2. tb_user
-- =====================================================================
CREATE TABLE tb_user (
    user_id              BIGSERIAL PRIMARY KEY,                 -- PK, 사용자 식별자
    department_id        BIGINT NOT NULL,                      -- N:1, 소속 부서 ID -> tb_department.department_id
    user_name            VARCHAR(50) NOT NULL,                 -- 사용자 이름
    email                VARCHAR(100) NOT NULL UNIQUE,         -- 로그인 이메일, 유니크
    password_hash        VARCHAR(255) NOT NULL,                -- 비밀번호 해시
    position_name        VARCHAR(50),                          -- 직급명 (사원/대리/과장 등)
    title_name           VARCHAR(50),                          -- 직책명 (팀원/팀장/사업부장/본부장 등)
    join_date            DATE,                                 -- 입사일
    role_code            VARCHAR(20) NOT NULL,                 -- 시스템 권한 코드
    profile_image_url    VARCHAR(500),                         -- 프로필 이미지 URL
    phone                VARCHAR(20),                          -- 연락처
    employment_status    VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',-- 재직 상태
    created_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 생성 시각
    updated_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 수정 시각
    CONSTRAINT ck_user_role_code
        CHECK (role_code IN ('DIRECTOR', 'DEPT_HEAD', 'TEAM_LEAD', 'MEMBER')),
    CONSTRAINT ck_user_employment_status
        CHECK (employment_status IN ('ACTIVE', 'LEAVE', 'RETIRED'))
);

-- =====================================================================
-- Table 3. tb_team
-- =====================================================================
CREATE TABLE tb_team (
    team_id               BIGSERIAL PRIMARY KEY,                -- PK, 팀 식별자
    department_id         BIGINT NOT NULL,                     -- N:1, 소속 부서 ID -> tb_department.department_id
    team_name             VARCHAR(100) NOT NULL,               -- 팀명
    status_code           VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- 팀 상태 코드
    description           TEXT,                                -- 팀 설명
    start_date            DATE,                                -- 팀/프로젝트 시작일
    expected_end_date     DATE,                                -- 팀/프로젝트 종료 예정일
    created_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 생성 시각
    updated_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 수정 시각
    CONSTRAINT uq_team_name_in_department
        UNIQUE (department_id, team_name),
    CONSTRAINT ck_team_status_code
        CHECK (status_code IN ('ACTIVE', 'INACTIVE'))
);

-- =====================================================================
-- Table 4. tb_user_team
-- =====================================================================
CREATE TABLE tb_user_team (
    user_team_id          BIGSERIAL PRIMARY KEY,                -- PK, 사용자-팀 관계 식별자
    user_id               BIGINT NOT NULL,                     -- N:1, 사용자 ID -> tb_user.user_id
    team_id               BIGINT NOT NULL,                     -- N:1, 팀 ID -> tb_team.team_id
    team_leader           BOOLEAN NOT NULL DEFAULT FALSE,      -- 팀장 여부
    team_role             VARCHAR(50) NOT NULL,                -- 팀 내 업무 역할명
    allocation            VARCHAR(50),                         -- 참여/배치 성격
    is_primary            BOOLEAN NOT NULL DEFAULT FALSE,      -- 주 소속 팀 여부 (대시보드/알림 기본 기준)
    joined_at             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 팀 합류 시각
    created_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 관계 생성 시각
    updated_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 관계 수정 시각
    CONSTRAINT uq_user_team_pair
        UNIQUE (user_id, team_id)
);

-- =====================================================================
-- Table 5. tb_user_skill
-- =====================================================================
CREATE TABLE tb_user_skill (
    user_skill_id         BIGSERIAL PRIMARY KEY,                -- PK, 사용자 스킬 식별자
    user_id               BIGINT NOT NULL,                     -- N:1, 사용자 ID -> tb_user.user_id
    skill_name            VARCHAR(100) NOT NULL,               -- 스킬명
    skill_level           SMALLINT NOT NULL,                   -- 스킬 수준 (1~5)
    created_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 생성 시각
    updated_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 수정 시각
    CONSTRAINT uq_user_skill_name
        UNIQUE (user_id, skill_name),
    CONSTRAINT ck_user_skill_level
        CHECK (skill_level BETWEEN 1 AND 5)
);

-- =====================================================================
-- Table 6. tb_user_evaluation
-- =====================================================================
CREATE TABLE tb_user_evaluation (
    evaluation_id         BIGSERIAL PRIMARY KEY,                -- PK, 평가 식별자
    evaluatee_user_id     BIGINT NOT NULL,                     -- N:1, 피평가자 사용자 ID -> tb_user.user_id
    evaluator_user_id     BIGINT NOT NULL,                     -- N:1, 평가자 사용자 ID -> tb_user.user_id
    content               TEXT NOT NULL,                       -- 평가 내용
    created_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP -- 작성 시각
);

-- =====================================================================
-- Table 7. tb_meta_tag
-- =====================================================================
CREATE TABLE tb_meta_tag (
    tag_id                BIGSERIAL PRIMARY KEY,                -- PK, 태그 식별자
    tag_name              VARCHAR(100) NOT NULL UNIQUE,        -- 태그명
    usage_count           INTEGER NOT NULL DEFAULT 0,          -- 사용 횟수 캐시
    created_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 생성 시각
    updated_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP  -- 수정 시각
);

-- =====================================================================
-- Table 8. tb_worklog
-- =====================================================================
CREATE TABLE tb_worklog (
    worklog_id            BIGSERIAL PRIMARY KEY,                -- PK, 업무일지 식별자
    author_id             BIGINT NOT NULL,                     -- N:1, 작성자 사용자 ID -> tb_user.user_id
    team_id               BIGINT NOT NULL,                     -- N:1, 실제 수행 팀 ID -> tb_team.team_id
    title                 VARCHAR(200) NOT NULL,               -- 업무 제목
    request_content       TEXT,                                -- 요청/지시 내용 원문
    work_content          TEXT NOT NULL,                       -- 실제 업무 상세 내용
    status_code           VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- 업무 상태 코드
    importance_code       VARCHAR(20) NOT NULL DEFAULT 'NORMAL', -- 중요도 코드
    actual_hours          NUMERIC(5,1),                        -- 실제 투입 시간
    instruction_date      DATE,                                -- 지시일
    due_date              DATE,                                -- 마감일
    completion_date       DATE,                                -- 완료일
    ai_summary            TEXT,                                -- AI 생성 요약
    ai_summary_edited     BOOLEAN NOT NULL DEFAULT FALSE,      -- AI 요약 수동 편집 여부
    ai_processing_status  VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- AI 처리 상태
    is_deleted            BOOLEAN NOT NULL DEFAULT FALSE,      -- 소프트 삭제 여부
    created_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 생성 시각
    updated_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 수정 시각
    CONSTRAINT ck_worklog_status_code
        CHECK (status_code IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'ON_HOLD', 'CANCELLED')),
    CONSTRAINT ck_worklog_importance_code
        CHECK (importance_code IN ('URGENT', 'HIGH', 'NORMAL', 'LOW')),
    CONSTRAINT ck_worklog_ai_processing_status
        CHECK (ai_processing_status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
    CONSTRAINT ck_worklog_actual_hours
        CHECK (actual_hours IS NULL OR actual_hours >= 0)
);

-- =====================================================================
-- Table 9. tb_worklog_status_history
-- =====================================================================
CREATE TABLE tb_worklog_status_history (
    history_id            BIGSERIAL PRIMARY KEY,                -- PK, 상태 변경 이력 식별자
    worklog_id            BIGINT NOT NULL,                     -- N:1, 업무일지 ID -> tb_worklog.worklog_id
    previous_status_code  VARCHAR(20),                         -- 변경 전 상태 코드
    new_status_code       VARCHAR(20) NOT NULL,                -- 변경 후 상태 코드
    reason                TEXT,                                -- 변경 사유
    changed_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 변경 시각
    changed_by            BIGINT NOT NULL,                     -- N:1, 변경자 사용자 ID -> tb_user.user_id
    CONSTRAINT ck_status_history_previous_status
        CHECK (previous_status_code IS NULL OR previous_status_code IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'ON_HOLD', 'CANCELLED')),
    CONSTRAINT ck_status_history_new_status
        CHECK (new_status_code IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'ON_HOLD', 'CANCELLED'))
);

-- =====================================================================
-- Table 10. tb_worklog_dependency
-- =====================================================================
CREATE TABLE tb_worklog_dependency (
    dependency_id         BIGSERIAL PRIMARY KEY,                -- PK, 의존성 식별자
    worklog_id            BIGINT NOT NULL,                     -- N:1, 현재 업무 ID -> tb_worklog.worklog_id
    depends_on_worklog_id BIGINT NOT NULL,                     -- N:1, 선행 업무 ID -> tb_worklog.worklog_id
    created_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 등록 시각
    CONSTRAINT uq_worklog_dependency_pair
        UNIQUE (worklog_id, depends_on_worklog_id),
    CONSTRAINT ck_worklog_dependency_not_self
        CHECK (worklog_id <> depends_on_worklog_id)
);

-- =====================================================================
-- Table 11. tb_worklog_tag
-- =====================================================================
CREATE TABLE tb_worklog_tag (
    worklog_tag_id        BIGSERIAL PRIMARY KEY,                -- PK, 업무일지-태그 관계 식별자
    worklog_id            BIGINT NOT NULL,                     -- N:1, 업무일지 ID -> tb_worklog.worklog_id
    tag_id                BIGINT NOT NULL,                     -- N:1, 태그 ID -> tb_meta_tag.tag_id
    is_ai_generated       BOOLEAN NOT NULL DEFAULT TRUE,       -- AI 생성 태그 여부
    created_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 생성 시각
    CONSTRAINT uq_worklog_tag_pair
        UNIQUE (worklog_id, tag_id)
);

-- =====================================================================
-- Table 12. tb_file
-- =====================================================================
CREATE TABLE tb_file (
    file_id               BIGSERIAL PRIMARY KEY,                -- PK, 파일 식별자
    worklog_id            BIGINT NOT NULL,                     -- N:1, 소속 업무일지 ID -> tb_worklog.worklog_id
    uploaded_by           BIGINT NOT NULL,                     -- N:1, 업로드 사용자 ID -> tb_user.user_id
    original_name         VARCHAR(500) NOT NULL,               -- 원본 파일명
    stored_path           VARCHAR(1000) NOT NULL,              -- 오브젝트 스토리지 저장 경로
    file_extension        VARCHAR(20) NOT NULL,                -- 파일 확장자
    file_size_bytes       BIGINT NOT NULL,                     -- 파일 크기(바이트)
    ai_summary            TEXT,                                -- AI 파일 요약
    ai_processing_status  VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- AI 처리 상태
    is_deleted            BOOLEAN NOT NULL DEFAULT FALSE,      -- 소프트 삭제 여부
    created_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 생성 시각
    updated_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 수정 시각
    CONSTRAINT ck_file_size_bytes
        CHECK (file_size_bytes >= 0),
    CONSTRAINT ck_file_ai_processing_status
        CHECK (ai_processing_status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'))
);

-- =====================================================================
-- Table 13. tb_notification
-- =====================================================================
CREATE TABLE tb_notification (
    notification_id       BIGSERIAL PRIMARY KEY,                -- PK, 알림 식별자
    user_id               BIGINT NOT NULL,                     -- N:1, 수신 사용자 ID -> tb_user.user_id
    notification_type     VARCHAR(50) NOT NULL,                -- 알림 유형 코드
    title                 VARCHAR(200) NOT NULL,               -- 알림 제목
    content               TEXT,                                -- 알림 본문
    reference_type        VARCHAR(50),                         -- 참조 대상 타입 (WORKLOG, FILE 등)
    reference_id          BIGINT,                              -- 참조 대상 ID
    is_read               BOOLEAN NOT NULL DEFAULT FALSE,      -- 읽음 여부
    read_at               TIMESTAMP,                           -- 읽은 시각
    created_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 생성 시각
    updated_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP -- 수정 시각
);

-- =====================================================================
-- Table 14. tb_worklog_embedding
-- =====================================================================
CREATE TABLE tb_worklog_embedding (
    embedding_id          BIGSERIAL PRIMARY KEY,                -- PK, 업무일지 임베딩 식별자
    worklog_id            BIGINT NOT NULL,                     -- N:1, 원본 업무일지 ID -> tb_worklog.worklog_id
    chunk_index           INTEGER NOT NULL,                    -- 청크 순번 (0부터)
    total_chunks          INTEGER NOT NULL,                    -- 총 청크 수
    chunk_content         TEXT NOT NULL,                       -- 청크 텍스트 본문
    embedding             VECTOR(768) NOT NULL,                -- pgvector 임베딩 벡터
    author_id             BIGINT NOT NULL,                     -- N:1, 작성자 사용자 ID -> tb_user.user_id
    team_id               BIGINT NOT NULL,                     -- N:1, 팀 ID -> tb_team.team_id
    department_id         BIGINT NOT NULL,                     -- N:1, 부서 ID -> tb_department.department_id
    status_code           VARCHAR(20),                         -- 검색 필터용 업무 상태 코드
    importance_code       VARCHAR(20),                         -- 검색 필터용 중요도 코드
    tag_ids               BIGINT[],                            -- 검색 필터용 태그 ID 배열
    source_type           VARCHAR(20) NOT NULL DEFAULT 'WORKLOG', -- 출처 구분
    created_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 생성 시각
    CONSTRAINT uq_worklog_embedding_chunk
        UNIQUE (worklog_id, chunk_index),
    CONSTRAINT ck_worklog_embedding_total_chunks
        CHECK (total_chunks > 0),
    CONSTRAINT ck_worklog_embedding_status_code
        CHECK (status_code IS NULL OR status_code IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'ON_HOLD', 'CANCELLED')),
    CONSTRAINT ck_worklog_embedding_importance_code
        CHECK (importance_code IS NULL OR importance_code IN ('URGENT', 'HIGH', 'NORMAL', 'LOW')),
    CONSTRAINT ck_worklog_embedding_source_type
        CHECK (source_type IN ('WORKLOG'))
);

-- =====================================================================
-- Table 15. tb_file_embedding
-- =====================================================================
CREATE TABLE tb_file_embedding (
    embedding_id          BIGSERIAL PRIMARY KEY,                -- PK, 파일 임베딩 식별자
    file_id               BIGINT NOT NULL,                     -- N:1, 원본 파일 ID -> tb_file.file_id
    worklog_id            BIGINT NOT NULL,                     -- N:1, 원본 업무일지 ID -> tb_worklog.worklog_id
    chunk_index           INTEGER NOT NULL,                    -- 청크 순번 (0부터)
    total_chunks          INTEGER NOT NULL,                    -- 총 청크 수
    chunk_content         TEXT NOT NULL,                       -- 청크 텍스트 본문
    embedding             VECTOR(768) NOT NULL,                -- pgvector 임베딩 벡터
    source_location       VARCHAR(200),                        -- 원본 문서 내 위치(페이지/섹션)
    author_id             BIGINT NOT NULL,                     -- N:1, 업로드/작성 사용자 ID -> tb_user.user_id
    team_id               BIGINT NOT NULL,                     -- N:1, 팀 ID -> tb_team.team_id
    department_id         BIGINT NOT NULL,                     -- N:1, 부서 ID -> tb_department.department_id
    source_type           VARCHAR(20) NOT NULL DEFAULT 'FILE', -- 출처 구분
    created_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 생성 시각
    CONSTRAINT uq_file_embedding_chunk
        UNIQUE (file_id, chunk_index),
    CONSTRAINT ck_file_embedding_total_chunks
        CHECK (total_chunks > 0),
    CONSTRAINT ck_file_embedding_source_type
        CHECK (source_type IN ('FILE'))
);
