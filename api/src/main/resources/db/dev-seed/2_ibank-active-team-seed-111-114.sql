-- iBank active team seed SQL
-- Scope: new active teams 111-114 only
-- Prerequisite: tb_department and tb_user seed data already exists.
BEGIN;

INSERT INTO tb_team (team_id, team_name, status_code, description, start_date, expected_end_date, deleted_at) VALUES
  (111, '현대자동차 부품 수급 데이터 정합성 컨설팅 TF', 'ACTIVE', '현대자동차의 부품 수급, 협력사 납기, 창고 입출고, 생산 라인 투입 데이터를 비교해 결품 위험과 납기 지연 원인을 조기에 찾는 데이터 정합성 개선안을 제시한다.', '2026-04-06', '2026-05-20', NULL),
  (112, 'CJ대한통운 풀필먼트 알림 안정화 TF', 'ACTIVE', 'CJ대한통운 풀필먼트 고객사의 주문 접수, 피킹, 출고, 배송 추적 알림이 채널별로 다르게 발송되는 문제를 줄이고 배송 상태 알림 솔루션 개선안을 제시한다.', '2026-04-13', '2026-06-03', NULL),
  (113, '한국콜마 B2B 수주·정산 예외 대응 TF', 'ACTIVE', '한국콜마의 B2B 고객 주문, 생산 의뢰, 출고 승인, 세금계산서, 정산 보정 과정에서 반복되는 예외를 운영 솔루션 개선안으로 정리한다.', '2026-04-27', '2026-06-17', NULL),
  (114, 'Siemens 스마트팩토리 장애 상담 요약 품질 TF', 'ACTIVE', 'Siemens 스마트팩토리 장비 장애 상담 기록을 AI가 요약할 때 누락되는 설비명, 알람 코드, 현장 조치, 부품 교체 여부, 재발 가능성을 평가하고 품질 개선안을 제시한다.', '2026-05-04', '2026-07-01', NULL)
ON CONFLICT (team_id) DO UPDATE SET
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
  (2, 111),
  (1, 111),
  (4, 112),
  (1, 112),
  (3, 113),
  (1, 113),
  (2, 114),
  (1, 114)
) AS v(user_id, team_id)
WHERE NOT EXISTS (
  SELECT 1
  FROM tb_team_admin ta
  WHERE ta.user_id = v.user_id
    AND ta.team_id = v.team_id
);

-- Team members
INSERT INTO tb_user_team (user_id, team_id, team_role, allocation, is_primary, status_code, is_leader)
SELECT
  v.user_id,
  v.team_id,
  v.team_role,
  v.allocation,
  v.is_primary,
  v.status_code,
  v.is_leader
FROM (VALUES
  (5, 111, '공급망 데이터 정합성 리드 / 팀장', '주담당', true, 'ACTIVE', true),
  (7, 111, '수량 차이 및 예외 케이스 분석', '주담당', true, 'ACTIVE', false),
  (10, 111, '부품/협력사 기준정보 정리', '주담당', true, 'ACTIVE', false),
  (16, 111, '창고 입출고 운영 이슈 분석', '지원', false, 'ACTIVE', false),
  (32, 111, '보정 로직 및 검증 룰 협업', '지원', false, 'ACTIVE', false),
  (2, 111, '사업부 승인 및 고객 제안 범위 확정', '겸임', false, 'ACTIVE', false),
  (27, 112, '물류 알림 안정화 리드 / 팀장', '주담당', true, 'ACTIVE', true),
  (30, 112, '배송 상태 화면/지표 연동', '주담당', true, 'ACTIVE', false),
  (34, 112, '배포 품질 및 회귀 검증', '주담당', true, 'ACTIVE', false),
  (21, 112, '고객사 운영 커뮤니케이션', '지원', false, 'ACTIVE', false),
  (11, 112, '알림 지표 리포트 검증', '지원', false, 'ACTIVE', false),
  (4, 112, '사업부 기술 총괄', '겸임', false, 'ACTIVE', false),
  (24, 113, 'B2B 수주·정산 예외 리드 / 팀장', '주담당', true, 'ACTIVE', true),
  (17, 113, '정산 운영 검증', '주담당', true, 'ACTIVE', false),
  (16, 113, '출고/WMS 운영 원인 분석', '주담당', true, 'ACTIVE', false),
  (7, 113, '정산 데이터 예외 분석', '지원', false, 'ACTIVE', false),
  (28, 113, '외부 주문 API 확인', '지원', false, 'ACTIVE', false),
  (3, 113, '사업부 운영 우선순위 조정', '겸임', false, 'ACTIVE', false),
  (8, 114, 'AI 장애 상담 요약 품질 리드 / 팀장', '주담당', true, 'ACTIVE', true),
  (6, 114, '품질 KPI 및 지표 검증', '주담당', true, 'ACTIVE', false),
  (11, 114, '평가 리포트 검증', '주담당', true, 'ACTIVE', false),
  (18, 114, '설비 장애 API 요구사항 분석', '지원', false, 'ACTIVE', false),
  (31, 114, 'AI 평가셋/임베딩 협업', '지원', false, 'ACTIVE', false),
  (2, 114, '사업부 승인 및 품질 기준 확정', '겸임', false, 'ACTIVE', false)
) AS v(user_id, team_id, team_role, allocation, is_primary, status_code, is_leader)
WHERE NOT EXISTS (
  SELECT 1
  FROM tb_user_team ut
  WHERE ut.user_id = v.user_id
    AND ut.team_id = v.team_id
);

SELECT setval(pg_get_serial_sequence('tb_team', 'team_id'), (SELECT COALESCE(MAX(team_id), 1) FROM tb_team), true);

COMMIT;
