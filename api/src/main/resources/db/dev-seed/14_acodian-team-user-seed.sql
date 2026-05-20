-- ACODIAN team/user dev seed generated from git log
-- Scope: tb_user, tb_team, tb_team_admin, tb_user_team
BEGIN;

INSERT INTO tb_user (user_id, department_id, user_name, email, password_hash, position_name, title_name, join_date, role_code, profile_image_url, phone, employment_status) VALUES
  (201, 3, '강지석', 'acodian01@dev.local', '$2b$12$tzatvdD02DDLmrqIXLtCa.ucFmjdnydHO73TWemkBYQSHVAUnYNXW', '수석', '팀장', '2026-05-01', 'TEAM_LEAD', 'https://d1sif143wcm5pd.cloudfront.net/local/default/profile/01.png', '010-9201-0001', 'ACTIVE'),
  (202, 3, '이성훈', 'acodian02@dev.local', '$2b$12$tzatvdD02DDLmrqIXLtCa.ucFmjdnydHO73TWemkBYQSHVAUnYNXW', '책임', '팀원', '2026-05-01', 'MEMBER', 'https://d1sif143wcm5pd.cloudfront.net/local/default/profile/02.png', '010-9201-0002', 'ACTIVE'),
  (203, 3, '진경석', 'acodian03@dev.local', '$2b$12$tzatvdD02DDLmrqIXLtCa.ucFmjdnydHO73TWemkBYQSHVAUnYNXW', '선임', '팀원', '2026-05-01', 'MEMBER', 'https://d1sif143wcm5pd.cloudfront.net/local/default/profile/03.png', '010-9201-0003', 'ACTIVE'),
  (204, 3, '안성훈', 'acodian04@dev.local', '$2b$12$tzatvdD02DDLmrqIXLtCa.ucFmjdnydHO73TWemkBYQSHVAUnYNXW', '선임', '팀원', '2026-05-01', 'MEMBER', 'https://d1sif143wcm5pd.cloudfront.net/local/default/profile/04.png', '010-9201-0004', 'ACTIVE'),
  (205, 3, '정인호', 'acodian05@dev.local', '$2b$12$tzatvdD02DDLmrqIXLtCa.ucFmjdnydHO73TWemkBYQSHVAUnYNXW', '선임', '팀원', '2026-05-01', 'MEMBER', 'https://d1sif143wcm5pd.cloudfront.net/local/default/profile/05.png', '010-9201-0005', 'ACTIVE'),
  (206, 3, '서백균', 'acodian06@dev.local', '$2b$12$tzatvdD02DDLmrqIXLtCa.ucFmjdnydHO73TWemkBYQSHVAUnYNXW', '전임', '팀원', '2026-05-01', 'MEMBER', 'https://d1sif143wcm5pd.cloudfront.net/local/default/profile/06.png', '010-9201-0006', 'ACTIVE')
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

INSERT INTO tb_team (team_id, team_name, status_code, description, start_date, expected_end_date, deleted_at) VALUES
  (201, 'ACODIAN', 'ACTIVE', 'AX-WMS 실제 개발 이력을 바탕으로 API, WEB, AI 업무를 추적하는 ACODIAN 개발용 팀', '2026-05-01', '2026-06-30', NULL)
ON CONFLICT (team_id) DO UPDATE SET
  team_name = EXCLUDED.team_name,
  status_code = EXCLUDED.status_code,
  description = EXCLUDED.description,
  start_date = EXCLUDED.start_date,
  expected_end_date = EXCLUDED.expected_end_date,
  deleted_at = EXCLUDED.deleted_at;

INSERT INTO tb_team_admin (user_id, team_id)
SELECT v.user_id, v.team_id
FROM (VALUES (201, 201)) AS v(user_id, team_id)
ON CONFLICT (user_id, team_id) DO NOTHING;

INSERT INTO tb_user_team (user_id, team_id, team_role, allocation, is_primary, status_code, is_leader) VALUES
  (201, 201, '기술 리드 / 팀장', '주담당', true, 'ACTIVE', true),
  (202, 201, '백엔드 API 안정화', '주담당', true, 'ACTIVE', false),
  (203, 201, '프론트엔드 UX 개선', '주담당', true, 'ACTIVE', false),
  (204, 201, 'AI 검색/요약 연동', '주담당', true, 'ACTIVE', false),
  (205, 201, '인증·알림 품질 점검', '주담당', true, 'ACTIVE', false),
  (206, 201, '인프라·문서 검증', '주담당', true, 'ACTIVE', false)
ON CONFLICT (user_id, team_id) DO UPDATE SET
  team_role = EXCLUDED.team_role,
  allocation = EXCLUDED.allocation,
  is_primary = EXCLUDED.is_primary,
  status_code = EXCLUDED.status_code,
  is_leader = EXCLUDED.is_leader;

SELECT setval(pg_get_serial_sequence('tb_user', 'user_id'), GREATEST((SELECT MAX(user_id) FROM tb_user), 1), true);
SELECT setval(pg_get_serial_sequence('tb_team', 'team_id'), GREATEST((SELECT MAX(team_id) FROM tb_team), 1), true);
SELECT setval(pg_get_serial_sequence('tb_team_admin', 'team_admin_id'), GREATEST((SELECT MAX(team_admin_id) FROM tb_team_admin), 1), true);
SELECT setval(pg_get_serial_sequence('tb_user_team', 'user_team_id'), GREATEST((SELECT MAX(user_team_id) FROM tb_user_team), 1), true);

COMMIT;
