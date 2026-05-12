-- iBank notification seed SQL for every dummy user with an actual worklog author row.
-- Sources: 1_ibank-team-seed.sql, 3_ibank-worklog-seed-v6.sql
-- Includes: tb_notification
-- Contract: user_id is derived from and join-verified against tb_worklog.author_id; user_id 1 is excluded because no worklog author row exists.
BEGIN;

WITH seed(author_id, worklog_id, is_read, created_offset, read_delay) AS (
  VALUES
    (2, 489, false, INTERVAL '08 hours 50 minutes', NULL::interval),
    (3, 425, true, INTERVAL '09 hours 10 minutes', INTERVAL '2 hours 05 minutes'),
    (4, 23, false, INTERVAL '09 hours 30 minutes', NULL::interval),
    (5, 334, false, INTERVAL '09 hours 50 minutes', NULL::interval),
    (6, 572, true, INTERVAL '10 hours 10 minutes', INTERVAL '5 hours 30 minutes'),
    (7, 400, false, INTERVAL '10 hours 30 minutes', NULL::interval),
    (8, 533, false, INTERVAL '10 hours 50 minutes', NULL::interval),
    (9, 328, true, INTERVAL '11 hours 10 minutes', INTERVAL '2 hours 05 minutes'),
    (10, 463, false, INTERVAL '13 hours 20 minutes', NULL::interval),
    (11, 469, false, INTERVAL '14 hours 00 minutes', NULL::interval),
    (12, 141, true, INTERVAL '14 hours 40 minutes', INTERVAL '5 hours 30 minutes'),
    (13, 142, false, INTERVAL '15 hours 10 minutes', NULL::interval),
    (14, 481, false, INTERVAL '08 hours 50 minutes', NULL::interval),
    (15, 145, true, INTERVAL '09 hours 10 minutes', INTERVAL '2 hours 05 minutes'),
    (16, 92, false, INTERVAL '09 hours 30 minutes', NULL::interval),
    (17, 74, false, INTERVAL '09 hours 50 minutes', NULL::interval),
    (18, 32, true, INTERVAL '10 hours 10 minutes', INTERVAL '5 hours 30 minutes'),
    (19, 95, false, INTERVAL '10 hours 30 minutes', NULL::interval),
    (20, 590, false, INTERVAL '10 hours 50 minutes', NULL::interval),
    (21, 399, true, INTERVAL '11 hours 10 minutes', INTERVAL '2 hours 05 minutes'),
    (22, 224, false, INTERVAL '13 hours 20 minutes', NULL::interval),
    (23, 1, false, INTERVAL '14 hours 00 minutes', NULL::interval),
    (24, 73, true, INTERVAL '14 hours 40 minutes', INTERVAL '5 hours 30 minutes'),
    (25, 80, false, INTERVAL '15 hours 10 minutes', NULL::interval),
    (26, 15, false, INTERVAL '08 hours 50 minutes', NULL::interval),
    (27, 14, true, INTERVAL '09 hours 10 minutes', INTERVAL '2 hours 05 minutes'),
    (28, 201, false, INTERVAL '09 hours 30 minutes', NULL::interval),
    (29, 147, false, INTERVAL '09 hours 50 minutes', NULL::interval),
    (30, 93, true, INTERVAL '10 hours 10 minutes', INTERVAL '5 hours 30 minutes'),
    (31, 519, false, INTERVAL '10 hours 30 minutes', NULL::interval),
    (32, 331, false, INTERVAL '10 hours 50 minutes', NULL::interval),
    (33, 3, true, INTERVAL '11 hours 10 minutes', INTERVAL '2 hours 05 minutes'),
    (34, 257, false, INTERVAL '13 hours 20 minutes', NULL::interval),
    (35, 7, false, INTERVAL '14 hours 00 minutes', NULL::interval)
), notification_rows AS (
  SELECT
    wl.author_id AS user_id,
    'WORKLOG_DUE_SOON' AS notification_type,
    '업무 마감 3일 전 알림' AS title,
    t.team_name || '의 ' || wl.title || ' 마감일이 3일 남았습니다. 마감일 : ' || wl.due_date AS content,
    'WORKLOG' AS reference_type,
    wl.worklog_id AS reference_id,
    seed.is_read AS is_read,
    ((wl.due_date::timestamp - INTERVAL '3 days') + seed.created_offset) AS created_at,
    CASE
      WHEN seed.is_read THEN ((wl.due_date::timestamp - INTERVAL '3 days') + seed.created_offset + seed.read_delay)
      ELSE NULL
    END AS read_at
  FROM seed
  JOIN tb_worklog wl ON wl.worklog_id = seed.worklog_id AND wl.author_id = seed.author_id
  JOIN tb_user u ON u.user_id = wl.author_id
  JOIN tb_team t ON t.team_id = wl.team_id
  WHERE wl.status_code IN ('PENDING', 'IN_PROGRESS', 'ON_HOLD')
    AND wl.is_deleted = false
)
INSERT INTO tb_notification (
  user_id,
  notification_type,
  title,
  content,
  reference_type,
  reference_id,
  is_read,
  read_at,
  created_at,
  updated_at
)
SELECT
  nr.user_id,
  nr.notification_type,
  nr.title,
  nr.content,
  nr.reference_type,
  nr.reference_id,
  nr.is_read,
  nr.read_at,
  nr.created_at,
  CASE
    WHEN nr.is_read THEN nr.read_at
    ELSE nr.created_at
  END AS updated_at
FROM notification_rows nr
WHERE NOT EXISTS (
  SELECT 1
  FROM tb_notification existing
  WHERE existing.user_id = nr.user_id
    AND existing.notification_type = nr.notification_type
    AND existing.reference_type = nr.reference_type
    AND existing.reference_id = nr.reference_id
);

SELECT setval(
  pg_get_serial_sequence('tb_notification', 'notification_id'),
  (SELECT COALESCE(MAX(notification_id), 1) FROM tb_notification),
  true
);

COMMIT;
