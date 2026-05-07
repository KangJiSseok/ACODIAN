# iBank Semantic Search Worklog Gold Set v6 (Draft)

- source worklog: `.codex/sql/ibank-worklog-seed-v6.sql`
- source expected-hit spec: `.codex/guides/ibank-semantic-search-expected-hits.md`
- coverage: 34 query ids currently defined in expected-hits
- note: this is a draft gold set at `worklog_id` level and should be manually refined over time

## Q-EX-01
- query: `P0 회귀 대응 이력`
- primary_team_ids: [101]
- secondary_team_ids: [105]
- relevant_worklog_ids: [1, 2, 3, 4, 5, 6, 41, 42, 43, 44, 45, 46]

## Q-EX-02
- query: `배치 윈도우 충돌 해결 기록`
- primary_team_ids: [101]
- secondary_team_ids: [105, 106]
- relevant_worklog_ids: [7, 8, 9, 10, 11, 12, 48, 49, 50, 51, 52, 355]

## Q-EX-03
- query: `매장 주문 취소 정산 누락 업무`
- primary_team_ids: [102, 107]
- secondary_team_ids: []
- relevant_worklog_ids: [73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84]

## Q-EX-04
- query: `JWT 인증 실패 로그 분석`
- primary_team_ids: [103]
- secondary_team_ids: [108]
- relevant_worklog_ids: [141, 142, 143, 144, 145, 146, 160, 161, 162, 163, 164, 175]

## Q-EX-05
- query: `레거시 모듈 분리 후 장애`
- primary_team_ids: [101, 104]
- secondary_team_ids: [105]
- relevant_worklog_ids: [25, 26, 27, 28, 63, 64, 65, 66, 67, 68, 199, 200]

## Q-EX-06
- query: `재고 스냅샷 차이 보정 이력`
- primary_team_ids: [106]
- secondary_team_ids: [108]
- relevant_worklog_ids: [333, 334, 335, 336, 337, 338, 346, 347, 348, 349, 350, 367]

## Q-EX-07
- query: `중복 데이터 누락 필드 품질 리포트`
- primary_team_ids: [108]
- secondary_team_ids: [103]
- relevant_worklog_ids: [457, 458, 459, 460, 461, 462, 463, 464, 465, 466, 467, 468]

## Q-EX-08
- query: `임베딩 pgvector 유사 업무 검색`
- primary_team_ids: [109]
- secondary_team_ids: [104, 108]
- relevant_worklog_ids: [513, 514, 515, 516, 517, 518, 519, 520, 521, 522, 523, 524]

## Q-EX-09
- query: `KPI 산식 수정 대시보드`
- primary_team_ids: [110]
- secondary_team_ids: [108]
- relevant_worklog_ids: [571, 572, 573, 574, 575, 576, 583, 584, 585, 586, 587, 588]

## Q-EX-10
- query: `릴리즈 노트 운영 이관 체크리스트`
- primary_team_ids: [105]
- secondary_team_ids: [110]
- relevant_worklog_ids: [273, 274, 275, 276, 277, 278, 292, 293, 294, 295, 296, 307]

## Q-PARA-01
- query: `야간에 돌던 작업이 겹치면서 운영이 흔들린 사례`
- primary_team_ids: [101]
- secondary_team_ids: [105, 106]
- relevant_worklog_ids: [7, 8, 9, 10, 11, 12, 35, 36, 37, 38, 39, 40]

## Q-PARA-02
- query: `매장에서 화면은 이상 없는데 정산 숫자가 안 맞았던 일`
- primary_team_ids: [102, 107]
- secondary_team_ids: []
- relevant_worklog_ids: [79, 80, 81, 82, 83, 84, 97, 98, 99, 100, 120, 121]

## Q-PARA-03
- query: `권한은 있는데 실제로는 접근이 막혔던 건`
- primary_team_ids: [103]
- secondary_team_ids: [102, 108]
- relevant_worklog_ids: [141, 142, 143, 144, 145, 146, 147, 148, 149, 150, 151, 152]

## Q-PARA-04
- query: `로그는 남았는데 기준 정보 품질이 안 좋아서 후속 조치가 생긴 업무`
- primary_team_ids: [108]
- secondary_team_ids: [103]
- relevant_worklog_ids: [463, 464, 465, 466, 467, 468, 469, 470, 471, 472, 473, 474]

## Q-PARA-05
- query: `비슷한 과거 업무를 찾기 위한 검색 실험`
- primary_team_ids: [109]
- secondary_team_ids: [104]
- relevant_worklog_ids: [513, 514, 515, 516, 517, 518, 519, 520, 521, 522, 523, 524]

## Q-PARA-06
- query: `운영 지표를 경영 보고용으로 다시 묶은 작업`
- primary_team_ids: [110]
- secondary_team_ids: [105]
- relevant_worklog_ids: [571, 572, 573, 574, 575, 576, 583, 584, 585, 586, 587, 588]

## Q-PARA-07
- query: `기존 시스템을 쪼개다가 생긴 후폭풍 정리`
- primary_team_ids: [101, 104]
- secondary_team_ids: [105]
- relevant_worklog_ids: [25, 26, 27, 28, 63, 64, 65, 66, 67, 68, 199, 200]

## Q-PARA-08
- query: `같은 문제가 며칠 뒤 다시 올라와 개선안까지 정리한 사례`
- primary_team_ids: [101, 102, 106, 109]
- secondary_team_ids: [103, 107]
- relevant_worklog_ids: [4, 6, 10, 11, 17, 32, 38, 40, 45, 47, 60, 62]

## Q-LINK-01
- query: `DX 플랫폼 운영 이슈가 WMS 정합성 개선으로 이어진 흐름`
- primary_team_ids: [101, 106]
- secondary_team_ids: []
- relevant_worklog_ids: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]

## Q-LINK-02
- query: `스타벅스 운영 이슈가 정산 고도화로 이어진 사례`
- primary_team_ids: [102, 107]
- secondary_team_ids: []
- relevant_worklog_ids: [73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84]

## Q-LINK-03
- query: `권한 하드닝이 데이터 품질 진단으로 확장된 흐름`
- primary_team_ids: [103, 108]
- secondary_team_ids: []
- relevant_worklog_ids: [141, 142, 143, 144, 145, 146, 147, 148, 149, 150, 151, 152]

## Q-LINK-04
- query: `그룹웨어 개발 로그를 검색 PoC에서 다시 활용한 흐름`
- primary_team_ids: [104, 109]
- secondary_team_ids: []
- relevant_worklog_ids: [199, 200, 201, 202, 203, 204, 211, 212, 213, 214, 215, 216]

## Q-LINK-05
- query: `릴리즈 트레인 결과가 통합 대시보드로 이어진 흐름`
- primary_team_ids: [105, 110]
- secondary_team_ids: []
- relevant_worklog_ids: [273, 274, 275, 276, 277, 278, 279, 280, 281, 282, 283, 284]

## Q-ROLE-01
- query: `팀장이 후속 조치나 승인 여부를 검토한 기록`
- primary_team_ids: [101, 104, 109]
- secondary_team_ids: [103, 110]
- relevant_worklog_ids: [5, 10, 18, 47, 60, 61, 62, 77, 82, 84, 90, 110]

## Q-ROLE-02
- query: `사업부장이 리스크 보고나 승인 기준을 남긴 업무`
- primary_team_ids: [101, 103, 105, 108, 110]
- secondary_team_ids: [102, 104, 107]
- relevant_worklog_ids: [23, 46, 221, 231, 295, 305, 318, 359, 372, 425, 438, 535]

## Q-ROLE-03
- query: `본부장 보고까지 올라갈 정도로 큰 리스크가 있었던 업무`
- primary_team_ids: [101, 103, 105, 109, 110]
- secondary_team_ids: [104]
- relevant_worklog_ids: [5, 18, 23, 33, 46, 51, 61, 145, 158, 173, 186, 191]

## Q-STATUS-01
- query: `마감일을 넘겨서 완료된 업무`
- primary_team_ids: [101, 102, 105, 110]
- secondary_team_ids: [106, 109]
- relevant_worklog_ids: [5, 6, 10, 11, 12, 17, 18, 22, 24, 26, 30, 31]

## Q-STATUS-02
- query: `진행하다가 외부 확인 때문에 멈춘 일`
- primary_team_ids: [101, 103, 108]
- secondary_team_ids: [105]
- relevant_worklog_ids: [9, 27, 37, 46, 55, 65, 145, 149, 158, 167, 177, 186]

## Q-STATUS-03
- query: `우선순위가 바뀌어서 취소된 업무`
- primary_team_ids: [102, 104, 105]
- secondary_team_ids: [109]
- relevant_worklog_ids: [100, 128, 226, 254, 300, 540, 568]

## Q-STATUS-04
- query: `지금은 미완료인데 나중에 다시 봐야 하는 작업`
- primary_team_ids: [101, 103, 108, 109]
- secondary_team_ids: [105]
- relevant_worklog_ids: [1, 2, 3, 4, 7, 8, 9, 13, 14, 15, 16, 20]

## Q-PER-01
- query: `운영 영향부터 직설적으로 적은 매장/WMS 업무`
- primary_team_ids: [102, 107]
- secondary_team_ids: [101, 106]
- relevant_worklog_ids: [77, 90, 92, 96, 99, 100, 113, 123, 124, 133, 135, 393]

## Q-PER-02
- query: `수치 비교와 원인 후보를 신중하게 적은 데이터 품질 업무`
- primary_team_ids: [108, 106]
- secondary_team_ids: [103]
- relevant_worklog_ids: [142, 143, 151, 153, 155, 159, 161, 162, 171, 175, 176, 178]

## Q-PER-03
- query: `검색 결과 체감과 기술 조치를 같이 적은 AI 검색 업무`
- primary_team_ids: [109]
- secondary_team_ids: []
- relevant_worklog_ids: [517, 519, 522, 524, 530, 532, 539, 540, 541, 549, 550, 552]

## Q-PER-04
- query: `리스크와 승인 기준을 짧게 적은 검토 메모`
- primary_team_ids: [103, 105, 110]
- secondary_team_ids: [101]
- relevant_worklog_ids: []

