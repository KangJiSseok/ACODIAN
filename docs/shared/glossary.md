# AX-WMS 용어 사전

프로젝트 전 영역(api/web/ai/nginx)이 공유하는 도메인 용어의 단일 정의.
팀 간 용어 불일치를 방지하기 위한 **Single Source of Truth**.

## 편집 규칙

- 용어 추가 시 가나다순 유지
- 각 용어: **한글 용어 (영문)** — 정의 1~2줄, 관련 ERD 테이블/컬럼, 사용 맥락
- 동의어/유사어가 있으면 표시

---

## 조직 (Organization)

### 부서 (Department)

- **정의**: 조직 내 최상위 구분 단위. 여러 팀을 포함하며 부서장 1명을 갖는다.
- **ERD 매핑**: `tb_department` — `department_id`, `department_name`, `department_head_user_id`
- **사용 맥락**: 사용자 소속 기준, 임베딩 메타데이터 필터, 조직 관리 화면

### 사용자-팀 연결 (UserTeam)

- **정의**: 사용자와 팀 사이의 M:N 관계를 해소하는 연결 엔티티. 팀 내 역할(팀장/팀원)을 포함한다.
- **ERD 매핑**: `tb_user_team` — `user_id`, `team_id`, `team_role`, `is_primary`
- **사용 맥락**: 팀장 지정(`team_role = 'LEADER'`), 주 소속 팀 판단(`is_primary`), 타 부서 팀 참여 허용

### 팀 (Team)

- **정의**: 부서 하위의 업무 수행 단위. 프로젝트 그룹으로도 운영된다.
- **ERD 매핑**: `tb_team` — `team_id`, `department_id`, `team_name`, `status_code`
- **사용 맥락**: 업무일지 귀속 기준, 임베딩 메타데이터 필터, 조직 관리 화면

---

## 사용자 (User)

### 사용자 (User)

- **정의**: 시스템에 로그인하여 업무를 수행하는 구성원. 시스템 권한 코드(`role_code`)로 역할이 구분된다.
- **ERD 매핑**: `tb_user` — `user_id`, `department_id`, `email`, `role_code`, `employment_status`
- **사용 맥락**: 인증/인가 주체, 업무일지 작성자, 파일 업로더, 알림 수신자
- **권한 코드 값**: `DIRECTOR`, `DEPT_HEAD`, `TEAM_LEAD`, `MEMBER`

### 재직 상태 (EmploymentStatus)

- **정의**: 사용자의 현재 재직 상태.
- **ERD 매핑**: `tb_user.employment_status`
- **값**: `ACTIVE`(재직), `LEAVE`(휴직), `RETIRED`(퇴직)

---

## 업무일지 (Worklog)

### 메타 태그 (MetaTag)

- **정의**: 업무일지에 부착되는 키워드 태그. AI가 자동 생성하거나 수동으로 추가한다.
- **ERD 매핑**: `tb_meta_tag` — `tag_id`, `tag_name`, `usage_count`
- **관련 테이블**: `tb_worklog_tag` (업무일지-태그 M:N 연결)
- **사용 맥락**: 시맨틱 검색 필터, 업무 분류

### 업무 의존성 (WorklogDependency)

- **정의**: 특정 업무가 다른 업무를 선행 조건으로 참조하는 관계.
- **ERD 매핑**: `tb_worklog_dependency` — `worklog_id`, `depends_on_worklog_id`
- **사용 맥락**: 업무 순서 관리, 대시보드 진행 현황

### 업무일지 (Worklog)

- **정의**: 팀 구성원이 작성하는 업무 기록. 요청 내용, 실제 업무 내용, 상태, 중요도, AI 요약을 포함한다.
- **ERD 매핑**: `tb_worklog` — `worklog_id`, `author_id`, `team_id`, `status_code`, `importance_code`, `ai_summary`
- **사용 맥락**: 시스템 핵심 데이터, AI 후처리 트리거, 시맨틱 검색 대상
- **상태 코드 값**: `PENDING`, `IN_PROGRESS`, `COMPLETED`, `ON_HOLD`, `CANCELLED`
- **중요도 코드 값**: `URGENT`, `HIGH`, `NORMAL`, `LOW`

### 첨부 파일 (File)

- **정의**: 업무일지에 직접 소속된 첨부파일. 파일 원본은 Object Storage에, 메타데이터는 `api`가 관리한다.
- **ERD 매핑**: `tb_file` — `file_id`, `worklog_id`, `uploaded_by`, `stored_path`, `ai_processing_status`
- **사용 맥락**: 파일 분석 AI 파이프라인 트리거, 파일 임베딩 생성

---

## AI / 검색 (AI / Search)

### 임베딩 (Embedding)

- **정의**: 업무일지 또는 파일 텍스트를 청킹한 뒤 벡터로 변환한 결과. pgvector에 저장되어 시맨틱 검색에 사용한다.
- **ERD 매핑**: `tb_worklog_embedding`, `tb_file_embedding`
- **사용 맥락**: 시맨틱 검색, AI 파이프라인 산출물
- **참고**: `api`는 현재 embedding 테이블에 직접 접근하지 않는다. 자세한 내용은 `docs/api/jooq-codegen-policy.md` 참조.

### 청킹 (Chunking)

- **정의**: 긴 텍스트를 검색에 적합한 단위로 분할하는 작업. 분할된 각 조각을 청크(chunk)라 한다.
- **ERD 매핑**: `tb_worklog_embedding.chunk_index`, `tb_worklog_embedding.total_chunks`
- **사용 맥락**: 임베딩 생성 전처리, ai 서비스 담당

---

## 예시 항목 (추후 추가 예정)

### SKU (Stock Keeping Unit)

- 정의: 재고 관리 단위. 동일 상품의 색/사이즈 구분 단위.
- ERD 매핑: (미정)
- 사용 맥락: 입출고 추적, 재고 카운팅

### 로케이션 (Location)

- 정의: 창고 내 물리 위치. 랙-열-층 계층.
- ERD 매핑: (미정)
- 사용 맥락: 피킹 경로 계산, 재고 배치

### 피킹 (Picking)

- 정의: 출고 주문에 따라 로케이션에서 상품을 집어오는 작업.
- ERD 매핑: (미정)
- 사용 맥락: 업무일지 주요 활동
