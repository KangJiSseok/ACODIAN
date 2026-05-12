-- iBank team seed SQL
-- Scope: tb_department, tb_user, tb_team(101-115), tb_team_admin, tb_user_team
-- Rules:
--   1. user_id=1 is director/admin-only and is not used for team department ownership.
--   2. tb_team.department_id follows the non-director tb_team_admin user's department_id.
--   3. Each user_id in tb_user_team has exactly one is_primary = true row.

BEGIN;

-- Departments: insert without head first to avoid the circular FK with tb_user.
INSERT INTO tb_department (department_id, department_name, description, department_head_user_id, status_code) VALUES
  (1, '데이터컨설팅사업부', '운영 데이터 품질, 정합성 분석, 리포트 검증을 담당하는 사업부', NULL, 'ACTIVE'),
  (2, '솔루션사업부', '고객 운영, 정산, 권한, 현업 요구사항 대응을 담당하는 사업부', NULL, 'ACTIVE'),
  (3, '솔루션개발사업부', '플랫폼 개발, 배포 안정화, AI 검색 및 인프라 협업을 담당하는 사업부', NULL, 'ACTIVE')
ON CONFLICT (department_id) DO UPDATE SET
  department_name = EXCLUDED.department_name,
  description = EXCLUDED.description,
  status_code = EXCLUDED.status_code;

-- Users
INSERT INTO tb_user (user_id, department_id, user_name, email, password_hash, position_name, title_name, join_date, role_code, profile_image_url, phone, employment_status) VALUES
  (1, NULL, '윤태훈', 'director@ibank.local', '$2b$12$tzatvdD02DDLmrqIXLtCa.ucFmjdnydHO73TWemkBYQSHVAUnYNXW', '이사', '본부장', '2018-03-05', 'DIRECTOR', 'https://d1sif143wcm5pd.cloudfront.net/local/default/profile/01.png', '02-6281-0001', 'ACTIVE'),
  (2, 1, '박서진', 'u002@ibank.local', '$2b$12$tzatvdD02DDLmrqIXLtCa.ucFmjdnydHO73TWemkBYQSHVAUnYNXW', '상무', '사업부장', '2019-01-14', 'DEPT_HEAD', 'https://d1sif143wcm5pd.cloudfront.net/local/default/profile/02.png', '010-2000-0002', 'ACTIVE'),
  (3, 2, '최민석', 'u003@ibank.local', '$2b$12$tzatvdD02DDLmrqIXLtCa.ucFmjdnydHO73TWemkBYQSHVAUnYNXW', '상무', '사업부장', '2019-03-11', 'DEPT_HEAD', 'https://d1sif143wcm5pd.cloudfront.net/local/default/profile/03.png', '010-2000-0003', 'ACTIVE'),
  (4, 3, '한지훈', 'u004@ibank.local', '$2b$12$tzatvdD02DDLmrqIXLtCa.ucFmjdnydHO73TWemkBYQSHVAUnYNXW', '상무', '사업부장', '2019-05-20', 'DEPT_HEAD', 'https://d1sif143wcm5pd.cloudfront.net/local/default/profile/04.png', '010-2000-0004', 'ACTIVE'),
  (5, 1, '김도윤', 'u005@ibank.local', '$2b$12$tzatvdD02DDLmrqIXLtCa.ucFmjdnydHO73TWemkBYQSHVAUnYNXW', '과장', '팀원', '2021-02-01', 'MEMBER', 'https://d1sif143wcm5pd.cloudfront.net/local/default/profile/05.png', '010-2000-0005', 'ACTIVE'),
  (6, 1, '이서연', 'u006@ibank.local', '$2b$12$tzatvdD02DDLmrqIXLtCa.ucFmjdnydHO73TWemkBYQSHVAUnYNXW', '대리', '팀원', '2021-03-08', 'MEMBER', 'https://d1sif143wcm5pd.cloudfront.net/local/default/profile/06.png', '010-2000-0006', 'ACTIVE'),
  (7, 1, '정유진', 'u007@ibank.local', '$2b$12$tzatvdD02DDLmrqIXLtCa.ucFmjdnydHO73TWemkBYQSHVAUnYNXW', '대리', '팀원', '2021-04-12', 'MEMBER', 'https://d1sif143wcm5pd.cloudfront.net/local/default/profile/07.png', '010-2000-0007', 'ACTIVE'),
  (8, 1, '오현우', 'u008@ibank.local', '$2b$12$tzatvdD02DDLmrqIXLtCa.ucFmjdnydHO73TWemkBYQSHVAUnYNXW', '대리', '팀원', '2021-05-17', 'MEMBER', 'https://d1sif143wcm5pd.cloudfront.net/local/default/profile/08.png', '010-2000-0008', 'ACTIVE'),
  (9, 1, '임지민', 'u009@ibank.local', '$2b$12$tzatvdD02DDLmrqIXLtCa.ucFmjdnydHO73TWemkBYQSHVAUnYNXW', '대리', '팀원', '2021-06-21', 'MEMBER', 'https://d1sif143wcm5pd.cloudfront.net/local/default/profile/09.png', '010-2000-0009', 'ACTIVE'),
  (10, 1, '윤가은', 'u010@ibank.local', '$2b$12$tzatvdD02DDLmrqIXLtCa.ucFmjdnydHO73TWemkBYQSHVAUnYNXW', '과장', '팀원', '2021-07-05', 'MEMBER', 'https://d1sif143wcm5pd.cloudfront.net/local/default/profile/10.png', '010-2000-0010', 'ACTIVE'),
  (11, 1, '송태성', 'u011@ibank.local', '$2b$12$tzatvdD02DDLmrqIXLtCa.ucFmjdnydHO73TWemkBYQSHVAUnYNXW', '과장', '팀원', '2021-08-09', 'MEMBER', 'https://d1sif143wcm5pd.cloudfront.net/local/default/profile/11.png', '010-2000-0011', 'ACTIVE'),
  (12, 1, '조하늘', 'u012@ibank.local', '$2b$12$tzatvdD02DDLmrqIXLtCa.ucFmjdnydHO73TWemkBYQSHVAUnYNXW', '대리', '팀원', '2021-09-13', 'MEMBER', 'https://d1sif143wcm5pd.cloudfront.net/local/default/profile/12.png', '010-2000-0012', 'ACTIVE'),
  (13, 1, '백민지', 'u013@ibank.local', '$2b$12$tzatvdD02DDLmrqIXLtCa.ucFmjdnydHO73TWemkBYQSHVAUnYNXW', '과장', '팀원', '2021-10-18', 'MEMBER', 'https://d1sif143wcm5pd.cloudfront.net/local/default/profile/13.png', '010-2000-0013', 'ACTIVE'),
  (14, 1, '장도현', 'u014@ibank.local', '$2b$12$tzatvdD02DDLmrqIXLtCa.ucFmjdnydHO73TWemkBYQSHVAUnYNXW', '사원', '팀원', '2021-11-22', 'MEMBER', 'https://d1sif143wcm5pd.cloudfront.net/local/default/profile/14.png', '010-2000-0014', 'ACTIVE'),
  (15, 1, '신예린', 'u015@ibank.local', '$2b$12$tzatvdD02DDLmrqIXLtCa.ucFmjdnydHO73TWemkBYQSHVAUnYNXW', '대리', '팀원', '2022-01-10', 'MEMBER', 'https://d1sif143wcm5pd.cloudfront.net/local/default/profile/15.png', '010-2000-0015', 'ACTIVE'),
  (16, 2, '강민호', 'u016@ibank.local', '$2b$12$tzatvdD02DDLmrqIXLtCa.ucFmjdnydHO73TWemkBYQSHVAUnYNXW', '과장', '팀원', '2021-02-15', 'MEMBER', 'https://d1sif143wcm5pd.cloudfront.net/local/default/profile/16.png', '010-2000-0016', 'ACTIVE'),
  (17, 2, '노수진', 'u017@ibank.local', '$2b$12$tzatvdD02DDLmrqIXLtCa.ucFmjdnydHO73TWemkBYQSHVAUnYNXW', '과장', '팀원', '2021-03-22', 'MEMBER', 'https://d1sif143wcm5pd.cloudfront.net/local/default/profile/17.png', '010-2000-0017', 'ACTIVE'),
  (18, 2, '문지후', 'u018@ibank.local', '$2b$12$tzatvdD02DDLmrqIXLtCa.ucFmjdnydHO73TWemkBYQSHVAUnYNXW', '대리', '팀원', '2021-04-26', 'MEMBER', 'https://d1sif143wcm5pd.cloudfront.net/local/default/profile/18.png', '010-2000-0018', 'ACTIVE'),
  (19, 2, '배서윤', 'u019@ibank.local', '$2b$12$tzatvdD02DDLmrqIXLtCa.ucFmjdnydHO73TWemkBYQSHVAUnYNXW', '사원', '팀원', '2021-05-31', 'MEMBER', 'https://d1sif143wcm5pd.cloudfront.net/local/default/profile/19.png', '010-2000-0019', 'ACTIVE'),
  (20, 2, '서진우', 'u020@ibank.local', '$2b$12$tzatvdD02DDLmrqIXLtCa.ucFmjdnydHO73TWemkBYQSHVAUnYNXW', '대리', '팀원', '2021-07-12', 'MEMBER', 'https://d1sif143wcm5pd.cloudfront.net/local/default/profile/20.png', '010-2000-0020', 'ACTIVE'),
  (21, 2, '손나래', 'u021@ibank.local', '$2b$12$tzatvdD02DDLmrqIXLtCa.ucFmjdnydHO73TWemkBYQSHVAUnYNXW', '사원', '팀원', '2021-08-16', 'MEMBER', 'https://d1sif143wcm5pd.cloudfront.net/local/default/profile/21.png', '010-2000-0021', 'ACTIVE'),
  (22, 2, '안태영', 'u022@ibank.local', '$2b$12$tzatvdD02DDLmrqIXLtCa.ucFmjdnydHO73TWemkBYQSHVAUnYNXW', '대리', '팀원', '2021-09-27', 'MEMBER', 'https://d1sif143wcm5pd.cloudfront.net/local/default/profile/22.png', '010-2000-0022', 'ACTIVE'),
  (23, 2, '유정민', 'u023@ibank.local', '$2b$12$tzatvdD02DDLmrqIXLtCa.ucFmjdnydHO73TWemkBYQSHVAUnYNXW', '대리', '팀원', '2021-10-25', 'MEMBER', 'https://d1sif143wcm5pd.cloudfront.net/local/default/profile/23.png', '010-2000-1023', 'ACTIVE'),
  (24, 2, '전하람', 'u024@ibank.local', '$2b$12$tzatvdD02DDLmrqIXLtCa.ucFmjdnydHO73TWemkBYQSHVAUnYNXW', '차장', '팀원', '2021-11-29', 'MEMBER', 'https://d1sif143wcm5pd.cloudfront.net/local/default/profile/24.png', '010-2000-0024', 'ACTIVE'),
  (25, 2, '차은성', 'u025@ibank.local', '$2b$12$tzatvdD02DDLmrqIXLtCa.ucFmjdnydHO73TWemkBYQSHVAUnYNXW', '사원', '팀원', '2022-01-17', 'MEMBER', 'https://d1sif143wcm5pd.cloudfront.net/local/default/profile/25.png', '010-2000-0025', 'ACTIVE'),
  (26, 3, '채도윤', 'u026@ibank.local', '$2b$12$tzatvdD02DDLmrqIXLtCa.ucFmjdnydHO73TWemkBYQSHVAUnYNXW', '차장', '팀원', '2021-02-08', 'MEMBER', 'https://d1sif143wcm5pd.cloudfront.net/local/default/profile/26.png', '010-2000-0026', 'ACTIVE'),
  (27, 3, '천예준', 'u027@ibank.local', '$2b$12$tzatvdD02DDLmrqIXLtCa.ucFmjdnydHO73TWemkBYQSHVAUnYNXW', '과장', '팀원', '2021-03-15', 'MEMBER', 'https://d1sif143wcm5pd.cloudfront.net/local/default/profile/27.png', '010-2000-0027', 'ACTIVE'),
  (28, 3, '최서아', 'u028@ibank.local', '$2b$12$tzatvdD02DDLmrqIXLtCa.ucFmjdnydHO73TWemkBYQSHVAUnYNXW', '대리', '팀원', '2021-04-19', 'MEMBER', 'https://d1sif143wcm5pd.cloudfront.net/local/default/profile/28.png', '010-2000-0028', 'ACTIVE'),
  (29, 3, '하민재', 'u029@ibank.local', '$2b$12$tzatvdD02DDLmrqIXLtCa.ucFmjdnydHO73TWemkBYQSHVAUnYNXW', '사원', '팀원', '2021-05-24', 'MEMBER', 'https://d1sif143wcm5pd.cloudfront.net/local/default/profile/29.png', '010-2000-0029', 'ACTIVE'),
  (30, 3, '허유나', 'u030@ibank.local', '$2b$12$tzatvdD02DDLmrqIXLtCa.ucFmjdnydHO73TWemkBYQSHVAUnYNXW', '과장', '팀원', '2021-06-28', 'MEMBER', 'https://d1sif143wcm5pd.cloudfront.net/local/default/profile/30.png', '010-2000-0030', 'ACTIVE'),
  (31, 3, '황지후', 'u031@ibank.local', '$2b$12$tzatvdD02DDLmrqIXLtCa.ucFmjdnydHO73TWemkBYQSHVAUnYNXW', '과장', '팀원', '2021-08-02', 'MEMBER', 'https://d1sif143wcm5pd.cloudfront.net/local/default/profile/31.png', '010-2000-0031', 'ACTIVE'),
  (32, 3, '김하린', 'u032@ibank.local', '$2b$12$tzatvdD02DDLmrqIXLtCa.ucFmjdnydHO73TWemkBYQSHVAUnYNXW', '과장', '팀원', '2021-09-06', 'MEMBER', 'https://d1sif143wcm5pd.cloudfront.net/local/default/profile/32.png', '010-2000-0032', 'ACTIVE'),
  (33, 3, '이도겸', 'u033@ibank.local', '$2b$12$tzatvdD02DDLmrqIXLtCa.ucFmjdnydHO73TWemkBYQSHVAUnYNXW', '대리', '팀원', '2021-10-11', 'MEMBER', 'https://d1sif143wcm5pd.cloudfront.net/local/default/profile/33.png', '010-2000-0033', 'ACTIVE'),
  (34, 3, '박연우', 'u034@ibank.local', '$2b$12$tzatvdD02DDLmrqIXLtCa.ucFmjdnydHO73TWemkBYQSHVAUnYNXW', '과장', '팀원', '2021-11-15', 'MEMBER', 'https://d1sif143wcm5pd.cloudfront.net/local/default/profile/34.png', '010-2000-0034', 'ACTIVE'),
  (35, 3, '오세린', 'u035@ibank.local', '$2b$12$tzatvdD02DDLmrqIXLtCa.ucFmjdnydHO73TWemkBYQSHVAUnYNXW', '대리', '팀원', '2022-01-24', 'MEMBER', 'https://d1sif143wcm5pd.cloudfront.net/local/default/profile/35.png', '010-2000-0035', 'ACTIVE')
ON CONFLICT (user_id) DO UPDATE SET
  department_id = EXCLUDED.department_id,
  user_name = EXCLUDED.user_name,
  email = EXCLUDED.email,
  password_hash = EXCLUDED.password_hash,
  position_name = EXCLUDED.position_name,
  title_name = EXCLUDED.title_name,
  join_date = EXCLUDED.join_date,
  role_code = EXCLUDED.role_code,
  profile_image_url = EXCLUDED.profile_image_url,
  phone = EXCLUDED.phone,
  employment_status = EXCLUDED.employment_status;

-- Department heads after users exist.
UPDATE tb_department SET department_head_user_id = 2 WHERE department_id = 1;
UPDATE tb_department SET department_head_user_id = 3 WHERE department_id = 2;
UPDATE tb_department SET department_head_user_id = 4 WHERE department_id = 3;

-- Teams
INSERT INTO tb_team (team_id, department_id, team_name, status_code, description, start_date, expected_end_date, deleted_at) VALUES
  (101, 1, '삼성전자 DX 플랫폼 운영 TF', 'INACTIVE', '삼성전자 DX 플랫폼의 운영 안정성을 확보하고, 레거시 모듈 분리 과정에서 발생하는 P0 회귀, 배치 윈도우 충돌, WMS/API 로그 이상을 추적 가능한 운영 체계로 정리한다.', '2024-01-01', '2024-06-30', NULL),
  (102, 2, '스타벅스 매장 시스템 운영 TF', 'INACTIVE', '매장 주문, 정산, 화면 상태, 에러 토스트, 권한 매트릭스 관련 운영 이슈를 묶어 고객 요청 대응과 P0 회귀 검증이 가능한 매장 운영 기준을 만든다.', '2024-04-01', '2024-10-24', NULL),
  (103, 3, '계정·권한 하드닝 TF', 'INACTIVE', 'JWT, 접근 제어, 권한 매트릭스, 인증 실패 로그를 기준으로 계정·권한 체계를 점검하고, 보안 예외와 UAT 검증까지 포함한 권한 하드닝 기준을 수립한다.', '2025-01-01', '2025-06-30', NULL),
  (104, 1, 'AX 그룹웨어 개발 TF', 'INACTIVE', 'AX 그룹웨어 개발 과정에서 레거시 모듈을 분리하고, API 개발, UX 상태 처리, 스테이징 회귀, 결함 등록, 우선순위 조정을 개발 의사결정 로그로 남긴다.', '2025-07-01', '2025-12-31', NULL),
  (105, 2, '분기 통합 릴리즈 트레인', 'INACTIVE', '여러 TF 산출물을 분기 릴리즈 단위로 묶어 배포 안정화, 파이프라인 캐시, 아티팩트 정리, UAT, 릴리즈 노트, 운영 이관 체크리스트까지 닫는다.', '2026-01-01', '2026-05-05', NULL),
  (106, 3, 'WMS 재고 정합성 개선 TF', 'INACTIVE', '101번 DX 플랫폼 운영 중 확인된 WMS 재고 이슈를 이어받아 입출고 수량 불일치, 재고 스냅샷, 창고별 차이, 보정 이력을 실제 운영 문의 대응 수준까지 정리한다.', '2024-03-01', '2024-08-31', NULL),
  (107, 1, '매장 정산 고도화 TF', 'INACTIVE', '102번 스타벅스 운영 TF의 정산·주문 취소 이슈를 후속 고도화로 전환하고, 일마감 정산, 포인트 누락, 보정 승인, 회계 검증 흐름을 안정화한다.', '2024-09-01', '2025-02-28', NULL),
  (108, 2, '운영 데이터 품질 진단 TF', 'INACTIVE', '103번 권한 하드닝 과정에서 드러난 로그·권한·기준정보 품질 문제를 확장해 중복 데이터, 누락 필드, 데이터 프로파일링, 품질 리포트 기준을 마련한다.', '2025-03-01', '2025-08-31', NULL),
  (109, 3, 'AI 업무일지 검색 PoC TF', 'INACTIVE', '104번 AX 그룹웨어 개발 산출물과 업무일지를 활용해 임베딩, pgvector, 유사 업무 검색, RAG 응답 품질, 자연어 질의 기반 과거 업무 검색 가능성을 검증한다.', '2025-10-01', '2026-03-31', NULL),
  (110, 1, '통합 운영 대시보드 TF', 'INACTIVE', '105번 릴리즈 트레인의 현황과 운영 지표를 받아 KPI, 장애 건수, 업무 지연, 팀별 처리량, 마감 임박 업무를 사업부장·본부장 보고용 화면으로 통합한다.', '2026-02-01', '2026-05-05', NULL),
  (111, 1, '현대자동차 부품 수급 데이터 정합성 컨설팅 TF', 'ACTIVE', '현대자동차의 부품 수급, 협력사 납기, 창고 입출고, 생산 라인 투입 데이터를 비교해 결품 위험과 납기 지연 원인을 조기에 찾는 데이터 정합성 개선안을 제시한다.', '2026-04-06', '2026-05-20', NULL),
  (112, 2, 'CJ대한통운 고객 알림 안정화 TF', 'ACTIVE', 'CJ대한통운 고객사의 주문 접수, 피킹, 출고, 배송 추적 알림을 채널별로 빠르게 발송하는 문제를 줄이고 배송 상태 알림 흐름을 개선한다.', '2026-04-13', '2026-06-03', NULL),
  (113, 3, '한국콜마 B2B 수주·정산 예외 대응 TF', 'ACTIVE', '한국콜마의 B2B 고객 주문, 생산 의뢰, 출고 승인, 월말 정산 과정에서 반복되는 예외를 운영 흐름 개선안으로 정리한다.', '2026-04-27', '2026-06-17', NULL),
  (114, 1, 'Siemens 스마트팩토리 장애 상담 요약 품질 TF', 'ACTIVE', 'Siemens 스마트팩토리 설비 장애 상담 기록을 AI가 요약할 때 누락되는 설비명, 알람 코드, 현장 조치, 부품 교체 여부, 재가동 가능성을 평가하고 품질 기준을 개선한다.', '2026-05-04', '2026-07-01', NULL),
  (115, 2, '쿠팡 반품 입고 예외 안정화 TF', 'ACTIVE', '쿠팡 반품 입고 과정에서 반복되는 회수 송장, 검수 보류, 재고 복귀, 정산 보정 예외를 묶어 운영 문의와 마감 검증이 가능한 안정화 기준을 만든다.', '2026-05-11', '2026-07-08', NULL)
ON CONFLICT (team_id) DO UPDATE SET
  department_id = EXCLUDED.department_id,
  team_name = EXCLUDED.team_name,
  status_code = EXCLUDED.status_code,
  description = EXCLUDED.description,
  start_date = EXCLUDED.start_date,
  expected_end_date = EXCLUDED.expected_end_date,
  deleted_at = EXCLUDED.deleted_at;

-- Team admins: owning department head + director(user_id=1)
INSERT INTO tb_team_admin (user_id, team_id)
SELECT v.user_id, v.team_id
FROM (VALUES
  (2, 101), (1, 101),
  (3, 102), (1, 102),
  (4, 103), (1, 103),
  (2, 104), (1, 104),
  (3, 105), (1, 105),
  (4, 106), (1, 106),
  (2, 107), (1, 107),
  (3, 108), (1, 108),
  (4, 109), (1, 109),
  (2, 110), (1, 110),
  (2, 111), (1, 111),
  (3, 112), (1, 112),
  (4, 113), (1, 113),
  (2, 114), (1, 114),
  (3, 115), (1, 115)
) AS v(user_id, team_id)
ON CONFLICT (user_id, team_id) DO NOTHING;

-- 팀 소유 부서는 director가 아닌 관리자 사용자의 부서를 따른다.
UPDATE tb_team t
SET department_id = u.department_id
FROM tb_team_admin ta
JOIN tb_user u ON u.user_id = ta.user_id
WHERE ta.team_id = t.team_id
  AND ta.user_id <> 1
  AND t.team_id BETWEEN 101 AND 115
  AND t.department_id IS DISTINCT FROM u.department_id;

DROP TABLE IF EXISTS tmp_ibank_user_team_seed;
CREATE TEMP TABLE tmp_ibank_user_team_seed (
  user_id BIGINT NOT NULL,
  team_id BIGINT NOT NULL,
  team_role VARCHAR(50) NOT NULL,
  allocation VARCHAR(50),
  is_primary BOOLEAN NOT NULL,
  status_code VARCHAR(20) NOT NULL,
  is_leader BOOLEAN NOT NULL
) ON COMMIT DROP;

INSERT INTO tmp_ibank_user_team_seed (user_id, team_id, team_role, allocation, is_primary, status_code, is_leader) VALUES
  (35, 101, '플랫폼 운영 총괄 / 팀장', '주담당', true, 'ACTIVE', true),
  (33, 101, '인프라 지원', '주담당', true, 'ACTIVE', false),
  (27, 101, '배치 윈도우 점검', '주담당', false, 'ACTIVE', false),
  (26, 101, '레거시 모듈 분리 및 백엔드 수정', '겸임', false, 'ACTIVE', false),
  (23, 101, '운영 환경 점검', '지원', true, 'ACTIVE', false),
  (18, 101, 'API 요구사항/로그 해석', '지원', false, 'ACTIVE', false),
  (4, 101, '사업부 총괄 검토', '겸임', false, 'ACTIVE', false),
  (16, 102, '매장 운영 총괄 / 팀장', '주담당', false, 'ACTIVE', true),
  (17, 102, '정산 운영 검증', '주담당', false, 'ACTIVE', false),
  (19, 102, '권한 매트릭스 검토', '주담당', true, 'ACTIVE', false),
  (25, 102, '운영 품질 확인', '겸임', false, 'ACTIVE', false),
  (24, 102, '현업 요구사항 정리', '지원', false, 'ACTIVE', false),
  (30, 102, '화면 상태/토스트 UX 협업', '지원', false, 'ACTIVE', false),
  (3, 102, '사업부 총괄 검토', '겸임', false, 'ACTIVE', false),
  (15, 103, '권한 기준 수립 / 팀장', '주담당', true, 'ACTIVE', true),
  (13, 103, '품질 감사 및 UAT 점검', '주담당', false, 'ACTIVE', false),
  (12, 103, '외부 시스템 연계 영향 점검', '주담당', true, 'ACTIVE', false),
  (19, 103, '권한 정책 검토', '지원', false, 'ACTIVE', false),
  (29, 103, 'JWT 인증/인가 처리 협업', '지원', false, 'ACTIVE', false),
  (2, 103, '사업부 승인 및 기준 확정', '겸임', false, 'ACTIVE', false),
  (26, 104, '백엔드 개발 리드 / 팀장', '주담당', true, 'ACTIVE', true),
  (28, 104, '외부 연동 API 개발', '주담당', false, 'ACTIVE', false),
  (30, 104, 'UX 상태 처리 및 화면 연동', '주담당', false, 'ACTIVE', false),
  (34, 104, '스테이징 회귀 및 배포 점검', '겸임', false, 'ACTIVE', false),
  (24, 104, '현업 요구사항 정리', '지원', false, 'ACTIVE', false),
  (22, 104, '배포 일정 조율', '지원', false, 'ACTIVE', false),
  (4, 104, '사업부 기술 총괄', '겸임', false, 'ACTIVE', false),
  (34, 105, '릴리즈 품질 총괄 / 팀장', '주담당', false, 'ACTIVE', true),
  (33, 105, '파이프라인/환경 관리', '주담당', false, 'ACTIVE', false),
  (26, 105, '통합 수정 및 릴리즈 이슈 대응', '주담당', false, 'ACTIVE', false),
  (22, 105, '배포 일정 및 체크리스트', '지원', false, 'ACTIVE', false),
  (23, 105, '환경 점검 및 운영 이관 협업', '지원', false, 'ACTIVE', false),
  (4, 105, '릴리즈 게이트 승인', '겸임', false, 'ACTIVE', false),
  (32, 106, '정합성 보정 리드 / 팀장', '주담당', false, 'ACTIVE', true),
  (27, 106, '배치 및 재처리 로직 점검', '주담당', false, 'ACTIVE', false),
  (33, 106, '스냅샷/환경 점검', '주담당', false, 'ACTIVE', false),
  (28, 106, '보정 API 수정 지원', '겸임', false, 'ACTIVE', false),
  (9, 106, '재고 스냅샷 적재 검증', '지원', true, 'ACTIVE', false),
  (5, 106, '기준 데이터 정합성 확인', '지원', false, 'ACTIVE', false),
  (4, 106, '사업부 총괄 검토', '겸임', false, 'ACTIVE', false),
  (17, 107, '정산 고도화 리드 / 팀장', '주담당', false, 'ACTIVE', true),
  (16, 107, '주문 취소/운영 영향 점검', '주담당', false, 'ACTIVE', false),
  (25, 107, '운영 품질 및 마감 검증', '주담당', true, 'ACTIVE', false),
  (7, 107, '정산 데이터 예외 분석', '지원', false, 'ACTIVE', false),
  (21, 107, '고객사 운영 커뮤니케이션', '지원', false, 'ACTIVE', false),
  (18, 107, '정산 인터페이스 요구사항 분석', '겸임', false, 'ACTIVE', false),
  (3, 107, '사업부 검토 및 승인', '겸임', false, 'ACTIVE', false),
  (13, 108, '품질 진단 리드 / 팀장', '주담당', true, 'ACTIVE', true),
  (5, 108, '마스터데이터 정합성 분석', '주담당', false, 'ACTIVE', false),
  (10, 108, '기준정보/메타데이터 정리', '주담당', false, 'ACTIVE', false),
  (11, 108, '품질 리포트 검증', '겸임', false, 'ACTIVE', false),
  (12, 108, '연계 데이터 점검', '지원', false, 'ACTIVE', false),
  (14, 108, '고객/회원 데이터 품질 해석', '지원', true, 'ACTIVE', false),
  (32, 108, '보정 로직 협업', '지원', false, 'ACTIVE', false),
  (2, 108, '사업부 승인 및 기준 확정', '겸임', false, 'ACTIVE', false),
  (31, 109, 'AI 검색 PoC 리드 / 팀장', '주담당', false, 'ACTIVE', true),
  (30, 109, '검색 결과 화면/UX 검증', '주담당', false, 'ACTIVE', false),
  (26, 109, '검색 API 및 백엔드 구현', '주담당', false, 'ACTIVE', false),
  (8, 109, '검색 품질 및 태그 검증', '지원', false, 'ACTIVE', false),
  (10, 109, '메타데이터/태그 기준 정리', '지원', false, 'ACTIVE', false),
  (4, 109, '사업부 기술 총괄', '겸임', false, 'ACTIVE', false),
  (20, 110, '대시보드 요구사항 총괄 / 팀장', '주담당', true, 'ACTIVE', true),
  (22, 110, '릴리즈 현황/일정 반영', '주담당', true, 'ACTIVE', false),
  (6, 110, 'KPI/지표 검증', '지원', false, 'ACTIVE', false),
  (11, 110, '정기 리포트 검증', '지원', false, 'ACTIVE', false),
  (30, 110, '대시보드 화면 연동 협업', '지원', false, 'ACTIVE', false),
  (3, 110, '사업부 경영 관점 검토', '겸임', false, 'ACTIVE', false),
  (5, 111, '공급망 데이터 정합성 리드 / 팀장', '주담당', true, 'ACTIVE', true),
  (7, 111, '수량 차이 및 예외 케이스 분석', '주담당', true, 'ACTIVE', false),
  (10, 111, '부품/협력사 기준정보 정리', '주담당', true, 'ACTIVE', false),
  (16, 111, '창고 입출고 운영 이슈 분석', '지원', false, 'ACTIVE', false),
  (32, 111, '보정 로직 및 검증 룰 협업', '지원', true, 'ACTIVE', false),
  (2, 111, '사업부 승인 및 고객 제안 범위 확정', '겸임', true, 'ACTIVE', false),
  (27, 112, '물류 알림 안정화 리드 / 팀장', '주담당', true, 'ACTIVE', true),
  (30, 112, '배송 상태 화면/지표 연동', '주담당', true, 'ACTIVE', false),
  (34, 112, '배포 품질 및 회귀 검증', '주담당', true, 'ACTIVE', false),
  (21, 112, '고객사 운영 커뮤니케이션', '지원', true, 'ACTIVE', false),
  (11, 112, '알림 지표 리포트 검증', '지원', false, 'ACTIVE', false),
  (4, 112, '사업부 기술 총괄', '겸임', true, 'ACTIVE', false),
  (24, 113, 'B2B 수주·정산 예외 리드 / 팀장', '주담당', true, 'ACTIVE', true),
  (17, 113, '정산 운영 검증', '주담당', true, 'ACTIVE', false),
  (16, 113, '출고/WMS 운영 원인 분석', '주담당', true, 'ACTIVE', false),
  (7, 113, '정산 데이터 예외 분석', '지원', false, 'ACTIVE', false),
  (28, 113, '외부 주문 API 확인', '지원', true, 'ACTIVE', false),
  (3, 113, '사업부 운영 우선순위 조정', '겸임', true, 'ACTIVE', false),
  (8, 114, 'AI 장애 상담 요약 품질 리드 / 팀장', '주담당', true, 'ACTIVE', true),
  (6, 114, '품질 KPI 및 지표 검증', '주담당', true, 'ACTIVE', false),
  (11, 114, '평가 리포트 검증', '주담당', true, 'ACTIVE', false),
  (18, 114, '설비 장애 API 요구사항 분석', '지원', true, 'ACTIVE', false),
  (31, 114, 'AI 평가셋/임베딩 협업', '지원', true, 'ACTIVE', false),
  (2, 114, '사업부 승인 및 품질 기준 확정', '겸임', false, 'ACTIVE', false),
  (29, 115, '반품 입고 예외 리드 / 팀장', '주담당', true, 'ACTIVE', true),
  (18, 115, 'WMS 운영 영향 분석', '지원', false, 'ACTIVE', false),
  (23, 115, '고객사 반품 문의 정리', '지원', false, 'ACTIVE', false),
  (25, 115, '정산/마감 영향 검증', '지원', false, 'ACTIVE', false),
  (32, 115, '보정 로직 협업', '지원', false, 'ACTIVE', false),
  (3, 115, '사업부 운영 우선순위 조정', '겸임', false, 'ACTIVE', false);

-- Re-seeding must keep exactly one primary membership per user.
UPDATE tb_user_team ut
SET is_primary = false,
    updated_at = CURRENT_TIMESTAMP
FROM (
  SELECT DISTINCT user_id
  FROM tmp_ibank_user_team_seed
) seeded_user
WHERE ut.user_id = seeded_user.user_id
  AND ut.is_primary = true;

INSERT INTO tb_user_team (user_id, team_id, team_role, allocation, is_primary, status_code, is_leader)
SELECT user_id, team_id, team_role, allocation, is_primary, status_code, is_leader
FROM tmp_ibank_user_team_seed
ON CONFLICT (user_id, team_id) DO UPDATE SET
  team_role = EXCLUDED.team_role,
  allocation = EXCLUDED.allocation,
  is_primary = EXCLUDED.is_primary,
  status_code = EXCLUDED.status_code,
  is_leader = EXCLUDED.is_leader,
  updated_at = CURRENT_TIMESTAMP;

-- Sequence alignment for explicit IDs.
SELECT setval(pg_get_serial_sequence('tb_department', 'department_id'), (SELECT COALESCE(MAX(department_id), 1) FROM tb_department), true);
SELECT setval(pg_get_serial_sequence('tb_user', 'user_id'), (SELECT COALESCE(MAX(user_id), 1) FROM tb_user), true);
SELECT setval(pg_get_serial_sequence('tb_team', 'team_id'), (SELECT COALESCE(MAX(team_id), 1) FROM tb_team), true);
SELECT setval(pg_get_serial_sequence('tb_team_admin', 'team_admin_id'), (SELECT COALESCE(MAX(team_admin_id), 1) FROM tb_team_admin), true);
SELECT setval(pg_get_serial_sequence('tb_user_team', 'user_team_id'), (SELECT COALESCE(MAX(user_team_id), 1) FROM tb_user_team), true);

COMMIT;
