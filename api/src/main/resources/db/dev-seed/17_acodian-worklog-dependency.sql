-- ACODIAN worklog dependency dev seed
BEGIN;

DELETE FROM tb_worklog_dependency WHERE worklog_id BETWEEN 2001 AND 2050 OR depends_on_worklog_id BETWEEN 2001 AND 2050;

INSERT INTO tb_worklog_dependency (worklog_id, depends_on_worklog_id, created_at) VALUES
  (2002, 2001, '2026-05-19 09:00:00'),
  (2003, 2002, '2026-05-19 09:00:00'),
  (2004, 2003, '2026-05-19 09:00:00'),
  (2005, 2004, '2026-05-19 09:00:00'),
  (2006, 2004, '2026-05-19 09:00:00'),
  (2011, 2009, '2026-05-19 09:00:00'),
  (2016, 2014, '2026-05-19 09:00:00'),
  (2021, 2019, '2026-05-19 09:00:00'),
  (2026, 2024, '2026-05-19 09:00:00'),
  (2031, 2029, '2026-05-19 09:00:00'),
  (2036, 2034, '2026-05-19 09:00:00'),
  (2041, 2039, '2026-05-19 09:00:00'),
  (2046, 2044, '2026-05-19 09:00:00')
ON CONFLICT (worklog_id, depends_on_worklog_id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('tb_worklog_dependency', 'dependency_id'), GREATEST((SELECT MAX(dependency_id) FROM tb_worklog_dependency), 1), true);

COMMIT;
