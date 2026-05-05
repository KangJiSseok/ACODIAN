-- AX-WMS ERD Draft (PostgreSQL 기준)
-- root project: S14P31S209
-- app structure in docs: web / api / ai / nginx
-- backend architecture: NestJS 제거, Spring Boot(api)로 대체
-- note:
--   1) tb_worklog_file는 사용하지 않고 tb_file.worklog_id FK로 일원화
--   2) 팀장 정보는 tb_team.leader_id 대신 tb_user_team.team_leader boolean 으로 관리
--   3) pgvector 사용을 전제로 embedding 테이블을 작성
--   4) 관계 표기는 '관계 이름 + cardinality + 해설' 형태로 정리
--   5) 같은 두 테이블 사이에 관계가 2개 이상 있으면 각각 분리해서 설명
--
-- cardinality 읽는 법
--   - A (1) : B (N) = A 한 건에 대해 B 여러 건이 연결될 수 있음
--   - A (N) : B (1) = A 여러 건이 B 한 건을 참조함
--   - A (1) : B (1) = 1:1 관계
--   - M:N 관계는 연결 테이블로 해소함

CREATE EXTENSION IF NOT EXISTS vector;

-- =====================================================================
-- 1. 부서
-- 관계 요약
--   [사용자 소속 관계]
--   - tb_department (1) : tb_user (N)
--     -> 사용자는 보통 1개의 부서에 소속되며, 부서 배정 전 DEPT_HEAD 후보는 department_id가 NULL일 수 있음.
--
--   [팀 소속 관계]
--   - tb_department (1) : tb_team (N)
--     -> 부서는 여러 팀을 가질 수 있고, 팀은 1개의 부서에 소속됨.
--
--   [부서장 지정 관계]
--   - tb_department (1) : tb_user (1) via department_head_user_id
--     -> 각 부서는 부서장 사용자 1명을 참조함.
--     -> UNIQUE(department_head_user_id)로 사용자 1명은 최대 1개 부서의 부서장만 맡을 수 있음.
--     -> 부서장-부서 간 부서 일치 여부는 DB에서 강제하지 않으며, 애플리케이션 레이어 책임임.
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
-- 2. 사용자
-- 관계 요약
--   [부서 소속 관계]
--   - tb_user (N) : tb_department (1)
--     -> 여러 사용자가 1개의 부서에 소속될 수 있으며, 부서 배정 전 후보 사용자는 예외적으로 소속 부서가 없을 수 있음.
--
--   [팀 소속 관계]
--   - tb_user (1) : tb_user_team (N)
--     -> 사용자는 여러 팀-소속 관계를 가질 수 있음.
--
--   [사용자 스킬 관계]
--   - tb_user (1) : tb_user_skill (N)
--     -> 사용자는 여러 스킬 레코드를 가질 수 있음.
--
--   [평가 대상 관계]
--   - tb_user (1) : tb_user_evaluation (N)
--     -> 사용자는 여러 평가를 받을 수 있음.
--
--   [평가자 관계]
--   - tb_user (1) : tb_user_evaluation (N)
--     -> 사용자는 여러 평가를 작성할 수 있음.
--
--   [업무 작성자 관계]
--   - tb_user (1) : tb_worklog (N)
--     -> 사용자는 여러 업무일지를 작성할 수 있음.
--
--   [파일 업로더 관계]
--   - tb_user (1) : tb_file (N)
--     -> 사용자는 여러 파일을 업로드할 수 있음.
--
--   [알림 수신 관계]
--   - tb_user (1) : tb_notification (N)
--     -> 사용자는 여러 알림을 받을 수 있음.
-- =====================================================================
CREATE TABLE tb_user (
    user_id              BIGSERIAL PRIMARY KEY,                 -- PK, 사용자 식별자
    department_id        BIGINT,                               -- N:1, 소속 부서 ID -> tb_department.department_id, 부서 배정 전 DEPT_HEAD 후보는 NULL 허용
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
    CONSTRAINT fk_user_primary_department
        FOREIGN KEY (department_id) REFERENCES tb_department(department_id),
    CONSTRAINT ck_user_role_code
        CHECK (role_code IN ('DIRECTOR', 'DEPT_HEAD', 'TEAM_LEAD', 'MEMBER')),
    CONSTRAINT ck_user_employment_status
        CHECK (employment_status IN ('ACTIVE', 'LEAVE', 'RETIRED'))
);

ALTER TABLE tb_department
    ADD CONSTRAINT fk_department_head_member
    FOREIGN KEY (department_head_user_id) REFERENCES tb_user(user_id);

-- =====================================================================
-- 3. 팀
-- 관계 요약
--   [부서 소속 관계]
--   - tb_team (N) : tb_department (1)
--     -> 여러 팀이 1개의 부서에 소속됨.
--
--   [사용자-팀 연결 관계]
--   - tb_team (1) : tb_user_team (N)
--     -> 팀 1개는 여러 사용자-팀 관계 레코드를 가질 수 있음.
--
--   [업무 소속 관계]
--   - tb_team (1) : tb_worklog (N)
--     -> 팀 1개는 여러 업무일지를 가질 수 있음.
--
--   [임베딩 필터 관계]
--   - tb_team (1) : tb_worklog_embedding (N)
--   - tb_team (1) : tb_file_embedding (N)
--     -> 팀 정보는 임베딩 검색 필터용 메타데이터로도 사용됨.
-- =====================================================================
CREATE TABLE tb_team (
    team_id               BIGSERIAL PRIMARY KEY,                -- PK, 팀 식별자
    department_id         BIGINT NOT NULL,                     -- N:1, 소속 부서 ID -> tb_department.department_id
    team_name             VARCHAR(100) NOT NULL,               -- 팀 표시명 (예: 삼성전자 DX 플랫폼 운영 TF, 분기 통합 릴리즈 트레인)
    status_code           VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- 팀 상태 코드
    description           TEXT,                                -- 팀 설명
    start_date            DATE,                                -- 팀/프로젝트 시작일
    expected_end_date     DATE,                                -- 팀/프로젝트 종료 예정일
    created_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 생성 시각
    updated_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 수정 시각
    CONSTRAINT fk_team_department
        FOREIGN KEY (department_id) REFERENCES tb_department(department_id),
    CONSTRAINT uq_team_name_in_department
        UNIQUE (department_id, team_name),
    CONSTRAINT ck_team_status_code
        CHECK (status_code IN ('ACTIVE', 'INACTIVE'))
);

-- =====================================================================
-- 4. 사용자-팀 관계
-- 관계 요약
--   [사용자 참조 관계]
--   - tb_user_team (N) : tb_user (1)
--     -> 여러 사용자-팀 관계 레코드가 1명의 사용자를 참조함.
--
--   [팀 참조 관계]
--   - tb_user_team (N) : tb_team (1)
--     -> 여러 사용자-팀 관계 레코드가 1개의 팀을 참조함.
--
--   [M:N 해소 관계]
--   - tb_user 와 tb_team 사이의 M:N 관계를 해소하는 연결 테이블임.
--
--   [팀장/업무 역할 정책]
--   - team_leader = 팀 내 팀장 여부(boolean)임.
--   - team_role = 실제 업무 역할명(예: 운영총괄, 고객커뮤니케이션, 장애분석)임.
--   - 애플리케이션 단에서 팀당 팀장 1명 정책을 관리함.
--
--   [참여 성격 정책]
--   - allocation = 참여/배치 성격(예: 주담당, 겸임)이며 DB enum/check 로 제한하지 않음.
--   - is_primary = 대표 소속 팀 여부이며 allocation 과 별도 의미를 가짐.
--
--   [부서 경계 정책]
--   - 사용자는 자신의 주 소속 부서와 다른 부서의 팀에도 참여할 수 있음.
--   - 즉, tb_user.department_id 와 tb_team.department_id 일치 여부는 강제하지 않음.
-- =====================================================================
CREATE TABLE tb_user_team (
    user_team_id          BIGSERIAL PRIMARY KEY,                -- PK, 사용자-팀 관계 식별자
    user_id               BIGINT NOT NULL,                     -- N:1, 사용자 ID -> tb_user.user_id
    team_id               BIGINT NOT NULL,                     -- N:1, 팀 ID -> tb_team.team_id
    team_leader           BOOLEAN NOT NULL DEFAULT FALSE,      -- 팀장 여부
    team_role             VARCHAR(50) NOT NULL,                -- 팀 내 업무 역할명 (예: 운영총괄, 릴리즈/변경관리, 인프라/배포)
    allocation            VARCHAR(50),                         -- 참여/배치 성격 (예: 주담당, 겸임)
    is_primary            BOOLEAN NOT NULL DEFAULT FALSE,      -- 주 소속 팀 여부 (대시보드/알림 기본 기준)
    joined_at             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 팀 합류 시각
    created_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 관계 생성 시각
    updated_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 관계 수정 시각
    CONSTRAINT fk_user_team_user
        FOREIGN KEY (user_id) REFERENCES tb_user(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_user_team_team
        FOREIGN KEY (team_id) REFERENCES tb_team(team_id),
    CONSTRAINT uq_user_team_pair
        UNIQUE (user_id, team_id)
);

-- =====================================================================
-- 5. 사용자 스킬
-- 관계 요약
--   [사용자-스킬 관계]
--   - tb_user_skill (N) : tb_user (1)
--     -> 사용자는 여러 스킬 레코드를 가질 수 있음.
-- =====================================================================
CREATE TABLE tb_user_skill (
    user_skill_id         BIGSERIAL PRIMARY KEY,                -- PK, 사용자 스킬 식별자
    user_id               BIGINT NOT NULL,                     -- N:1, 사용자 ID -> tb_user.user_id
    skill_name            VARCHAR(100) NOT NULL,               -- 스킬명
    skill_level           SMALLINT NOT NULL,                   -- 스킬 수준 (1~5)
    created_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 생성 시각
    updated_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 수정 시각
    CONSTRAINT fk_user_skill_user
        FOREIGN KEY (user_id) REFERENCES tb_user(user_id) ON DELETE CASCADE,
    CONSTRAINT uq_user_skill_name
        UNIQUE (user_id, skill_name),
    CONSTRAINT ck_user_skill_level
        CHECK (skill_level BETWEEN 1 AND 5)
);

-- =====================================================================
-- 6. 관리자 평가
-- 관계 요약
--   [피평가자 관계]
--   - tb_user_evaluation (N) : tb_user (1)
--     -> 사용자는 여러 평가를 받을 수 있음.
--
--   [평가자 관계]
--   - tb_user_evaluation (N) : tb_user (1)
--     -> 사용자는 여러 평가를 작성할 수 있음.
-- =====================================================================
CREATE TABLE tb_user_evaluation (
    evaluation_id         BIGSERIAL PRIMARY KEY,                -- PK, 평가 식별자
    evaluatee_user_id     BIGINT NOT NULL,                     -- N:1, 피평가자 사용자 ID -> tb_user.user_id
    evaluator_user_id     BIGINT NOT NULL,                     -- N:1, 평가자 사용자 ID -> tb_user.user_id
    content               TEXT NOT NULL,                       -- 평가 내용
    created_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 작성 시각
    CONSTRAINT fk_user_evaluation_evaluatee
        FOREIGN KEY (evaluatee_user_id) REFERENCES tb_user(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_user_evaluation_evaluator
        FOREIGN KEY (evaluator_user_id) REFERENCES tb_user(user_id)
);

-- =====================================================================
-- 7. 메타 태그 풀
-- 관계 요약
--   [업무일지-태그 연결 관계]
--   - tb_meta_tag (1) : tb_worklog_tag (N)
--     -> 태그 1개는 여러 업무일지 연결 레코드에서 사용될 수 있음.
--
--   [M:N 해소 관계]
--   - tb_worklog 와 tb_meta_tag 사이의 M:N 관계는 tb_worklog_tag가 해소함.
-- =====================================================================
CREATE TABLE tb_meta_tag (
    tag_id                BIGSERIAL PRIMARY KEY,                -- PK, 태그 식별자
    tag_name              VARCHAR(100) NOT NULL UNIQUE,        -- 태그명
    usage_count           INTEGER NOT NULL DEFAULT 0,          -- 사용 횟수 캐시
    created_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP -- 생성 시각
);

-- =====================================================================
-- 8. 업무일지
-- 관계 요약
--   [작성자 관계]
--   - tb_worklog (N) : tb_user (1)
--     -> 여러 업무일지가 1명의 작성자를 참조함.
--
--   [팀 소속 관계]
--   - tb_worklog (N) : tb_team (1)
--     -> 여러 업무일지가 1개의 팀에 소속됨.
--
--   [상태 이력 관계]
--   - tb_worklog (1) : tb_worklog_status_history (N)
--     -> 업무일지 1건은 여러 상태 변경 이력을 가질 수 있음.
--
--   [의존성 관계]
--   - tb_worklog (1) : tb_worklog_dependency (N)
--     -> 업무일지 1건은 여러 선행 업무 연결 레코드를 가질 수 있음.
--
--   [태그 관계]
--   - tb_worklog (1) : tb_worklog_tag (N)
--     -> 업무일지 1건은 여러 태그 연결 레코드를 가질 수 있음.
--
--   [첨부 파일 관계]
--   - tb_worklog (1) : tb_file (N)
--     -> 업무일지 1건은 여러 첨부 파일을 가질 수 있음.
--
--   [임베딩 관계]
--   - tb_worklog (1) : tb_worklog_embedding (N)
--   - tb_worklog (1) : tb_file_embedding (N)
--     -> 업무일지 본문/첨부파일 분석 결과가 여러 청크 임베딩으로 확장됨.
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
    CONSTRAINT fk_worklog_author
        FOREIGN KEY (author_id) REFERENCES tb_user(user_id),
    CONSTRAINT fk_worklog_team
        FOREIGN KEY (team_id) REFERENCES tb_team(team_id),
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
-- 9. 업무 상태 변경 이력
-- 관계 요약
--   [업무일지 참조 관계]
--   - tb_worklog_status_history (N) : tb_worklog (1)
--     -> 여러 상태 이력 레코드가 1개의 업무일지를 참조함.
--
--   [변경자 참조 관계]
--   - tb_worklog_status_history (N) : tb_user (1)
--     -> 여러 상태 이력 레코드가 1명의 변경자를 참조할 수 있음.
-- =====================================================================
CREATE TABLE tb_worklog_status_history (
    history_id            BIGSERIAL PRIMARY KEY,                -- PK, 상태 변경 이력 식별자
    worklog_id            BIGINT NOT NULL,                     -- N:1, 업무일지 ID -> tb_worklog.worklog_id
    previous_status_code  VARCHAR(20),                         -- 변경 전 상태 코드
    new_status_code       VARCHAR(20) NOT NULL,                -- 변경 후 상태 코드
    reason                TEXT,                                -- 변경 사유
    changed_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 변경 시각
    changed_by            BIGINT NOT NULL,                     -- N:1, 변경자 사용자 ID -> tb_user.user_id
    CONSTRAINT fk_status_history_worklog
        FOREIGN KEY (worklog_id) REFERENCES tb_worklog(worklog_id) ON DELETE CASCADE,
    CONSTRAINT fk_status_history_changed_by
        FOREIGN KEY (changed_by) REFERENCES tb_user(user_id),
    CONSTRAINT ck_status_history_previous_status
        CHECK (previous_status_code IS NULL OR previous_status_code IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'ON_HOLD', 'CANCELLED')),
    CONSTRAINT ck_status_history_new_status
        CHECK (new_status_code IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'ON_HOLD', 'CANCELLED'))
);

-- =====================================================================
-- 10. 업무 의존성
-- 관계 요약
--   [현재 업무 참조 관계]
--   - tb_worklog_dependency (N) : tb_worklog (1)
--     -> 여러 의존성 레코드가 1개의 현재 업무를 참조함.
--
--   [선행 업무 참조 관계]
--   - tb_worklog_dependency (N) : tb_worklog (1)
--     -> 여러 의존성 레코드가 1개의 선행 업무를 참조함.
--
--   [자기참조 M:N 해소 관계]
--   - tb_worklog 와 tb_worklog 사이의 자기참조 M:N 관계를 해소함.
-- =====================================================================
CREATE TABLE tb_worklog_dependency (
    dependency_id         BIGSERIAL PRIMARY KEY,                -- PK, 의존성 식별자
    worklog_id            BIGINT NOT NULL,                     -- N:1, 현재 업무 ID -> tb_worklog.worklog_id
    depends_on_worklog_id BIGINT NOT NULL,                     -- N:1, 선행 업무 ID -> tb_worklog.worklog_id
    created_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 등록 시각
    CONSTRAINT fk_worklog_dependency_current_worklog
        FOREIGN KEY (worklog_id) REFERENCES tb_worklog(worklog_id) ON DELETE CASCADE,
    CONSTRAINT fk_worklog_dependency_predecessor_worklog
        FOREIGN KEY (depends_on_worklog_id) REFERENCES tb_worklog(worklog_id) ON DELETE CASCADE,
    CONSTRAINT uq_worklog_dependency_pair
        UNIQUE (worklog_id, depends_on_worklog_id),
    CONSTRAINT ck_worklog_dependency_not_self
        CHECK (worklog_id <> depends_on_worklog_id)
);

-- =====================================================================
-- 11. 업무일지-태그 연결
-- 관계 요약
--   [업무일지 참조 관계]
--   - tb_worklog_tag (N) : tb_worklog (1)
--     -> 여러 업무일지-태그 레코드가 1개의 업무일지를 참조함.
--
--   [태그 참조 관계]
--   - tb_worklog_tag (N) : tb_meta_tag (1)
--     -> 여러 업무일지-태그 레코드가 1개의 태그를 참조함.
--
--   [M:N 해소 관계]
--   - tb_worklog 와 tb_meta_tag 사이의 M:N 관계를 해소함.
-- =====================================================================
CREATE TABLE tb_worklog_tag (
    worklog_tag_id        BIGSERIAL PRIMARY KEY,                -- PK, 업무일지-태그 관계 식별자
    worklog_id            BIGINT NOT NULL,                     -- N:1, 업무일지 ID -> tb_worklog.worklog_id
    tag_id                BIGINT NOT NULL,                     -- N:1, 태그 ID -> tb_meta_tag.tag_id
    is_ai_generated       BOOLEAN NOT NULL DEFAULT TRUE,       -- AI 생성 태그 여부
    created_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 생성 시각
    CONSTRAINT fk_worklog_tag_worklog
        FOREIGN KEY (worklog_id) REFERENCES tb_worklog(worklog_id) ON DELETE CASCADE,
    CONSTRAINT fk_worklog_tag_tag
        FOREIGN KEY (tag_id) REFERENCES tb_meta_tag(tag_id),
    CONSTRAINT uq_worklog_tag_pair
        UNIQUE (worklog_id, tag_id)
);

-- =====================================================================
-- 12. 첨부 파일
-- 관계 요약
--   [업무일지 소속 관계]
--   - tb_file (N) : tb_worklog (1)
--     -> 여러 파일이 1개의 업무일지에 소속될 수 있음.
--
--   [업로더 관계]
--   - tb_file (N) : tb_user (1)
--     -> 여러 파일이 1명의 업로더를 참조할 수 있음.
--
--   [첨부 정책]
--   - tb_worklog_file 별도 연결 테이블 없이 tb_file.worklog_id로 관리함.
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
    CONSTRAINT fk_file_worklog
        FOREIGN KEY (worklog_id) REFERENCES tb_worklog(worklog_id) ON DELETE CASCADE,
    CONSTRAINT fk_file_uploaded_by
        FOREIGN KEY (uploaded_by) REFERENCES tb_user(user_id),
    CONSTRAINT ck_file_size_bytes
        CHECK (file_size_bytes >= 0),
    CONSTRAINT ck_file_ai_processing_status
        CHECK (ai_processing_status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'))
);

-- =====================================================================
-- 13. 알림
-- 관계 요약
--   [수신자 관계]
--   - tb_notification (N) : tb_user (1)
--     -> 여러 알림이 1명의 사용자에게 귀속될 수 있음.
--
--   [다형 참조 관계]
--   - reference_type / reference_id는 WORKLOG, FILE 등 다양한 엔티티를 느슨하게 참조하기 위한 용도임.
--   - 다형 참조이므로 DB 레벨 FK는 강제하지 않음.
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
    updated_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 수정 시각
    CONSTRAINT fk_notification_user
        FOREIGN KEY (user_id) REFERENCES tb_user(user_id) ON DELETE CASCADE
);

-- =====================================================================
-- 14. 업무일지 임베딩
-- 관계 요약
--   [원본 업무일지 관계]
--   - tb_worklog_embedding (N) : tb_worklog (1)
--     -> 여러 임베딩 청크가 1개의 업무일지에 연결될 수 있음.
--
--   [작성자 메타데이터 관계]
--   - tb_worklog_embedding (N) : tb_user (1)
--
--   [팀 메타데이터 관계]
--   - tb_worklog_embedding (N) : tb_team (1)
--
--   [부서 메타데이터 관계]
--   - tb_worklog_embedding (N) : tb_department (1)
--
--   [청크 분할 관계]
--   - 업무일지 1건은 여러 청크 임베딩으로 분할될 수 있음 (1:N).
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
    CONSTRAINT fk_worklog_embedding_worklog
        FOREIGN KEY (worklog_id) REFERENCES tb_worklog(worklog_id) ON DELETE CASCADE,
    CONSTRAINT fk_worklog_embedding_author
        FOREIGN KEY (author_id) REFERENCES tb_user(user_id),
    CONSTRAINT fk_worklog_embedding_team
        FOREIGN KEY (team_id) REFERENCES tb_team(team_id),
    CONSTRAINT fk_worklog_embedding_department
        FOREIGN KEY (department_id) REFERENCES tb_department(department_id),
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
-- 15. 파일 임베딩
-- 관계 요약
--   [원본 파일 관계]
--   - tb_file_embedding (N) : tb_file (1)
--     -> 여러 임베딩 청크가 1개의 파일에 연결될 수 있음.
--
--   [원본 업무일지 관계]
--   - tb_file_embedding (N) : tb_worklog (1)
--     -> 파일 임베딩도 어떤 업무일지에 소속된 파일인지 추적함.
--
--   [작성자/업로더 메타데이터 관계]
--   - tb_file_embedding (N) : tb_user (1)
--
--   [팀 메타데이터 관계]
--   - tb_file_embedding (N) : tb_team (1)
--
--   [부서 메타데이터 관계]
--   - tb_file_embedding (N) : tb_department (1)
--
--   [청크 분할 관계]
--   - 파일 1건은 여러 청크 임베딩으로 분할될 수 있음 (1:N).
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
    CONSTRAINT fk_file_embedding_file
        FOREIGN KEY (file_id) REFERENCES tb_file(file_id) ON DELETE CASCADE,
    CONSTRAINT fk_file_embedding_worklog
        FOREIGN KEY (worklog_id) REFERENCES tb_worklog(worklog_id) ON DELETE CASCADE,
    CONSTRAINT fk_file_embedding_author
        FOREIGN KEY (author_id) REFERENCES tb_user(user_id),
    CONSTRAINT fk_file_embedding_team
        FOREIGN KEY (team_id) REFERENCES tb_team(team_id),
    CONSTRAINT fk_file_embedding_department
        FOREIGN KEY (department_id) REFERENCES tb_department(department_id),
    CONSTRAINT uq_file_embedding_chunk
        UNIQUE (file_id, chunk_index),
    CONSTRAINT ck_file_embedding_total_chunks
        CHECK (total_chunks > 0),
    CONSTRAINT ck_file_embedding_source_type
        CHECK (source_type IN ('FILE'))
);

-- =====================================================================
-- 16. 조회 성능용 인덱스
-- =====================================================================
-- 가이드 정렬 요청에 따라 별도 조회 성능용 인덱스는 정의하지 않음.
