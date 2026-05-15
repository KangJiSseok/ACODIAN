-- tb_worklog.title 길이 제약을 VARCHAR(200) -> VARCHAR(50) 으로 축소한다.
-- 도메인 정책상 업무 제목은 짧게 유지하며, 본문은 work_content/request_content 에 적는다.
-- 운영/시드 데이터의 현재 최대 길이는 50자 미만이라 데이터 손실 없이 안전하게 축소 가능하다.

ALTER TABLE tb_worklog
    ALTER COLUMN title TYPE VARCHAR(50);
