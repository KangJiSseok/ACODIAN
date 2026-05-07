# iBank Semantic Search Expected Hit Set

이 문서는 `.codex/guides/ibank-semantic-search-query-set.md`에 대응하는  
**샘플 expected hit set**이다.

목적:
- 질의마다 어느 팀/역할/상태가 우선적으로 나와야 하는지 기준을 제공
- 자동 평가 시 정답 세트(seed gold set)의 초안으로 사용

주의:
- 아래 expected set은 **엄격한 유일 정답**이 아니라, 상위 검색 결과에서 우선 기대하는 hit 후보 집합이다.

---

## 표기 규칙

- `primary_team_ids`: 가장 먼저 기대하는 팀
- `secondary_team_ids`: 같이 섞여도 자연스러운 팀
- `expected_roles`: 상위에 오면 좋은 작성자 역할
- `expected_status`: 상위에 오면 좋은 상태
- `notes`: 왜 그렇게 기대하는지

---

## A. Exact keyword expected hits

### Q-EX-01
- query: `P0 회귀 대응 이력`
- primary_team_ids: `[101]`
- secondary_team_ids: `[105]`
- expected_roles: `["MEMBER", "DEPT_HEAD"]`
- expected_status: `["COMPLETED", "IN_PROGRESS"]`
- notes: DX 플랫폼 운영 안정화 문맥이 가장 강해야 함

### Q-EX-02
- query: `배치 윈도우 충돌 해결 기록`
- primary_team_ids: `[101]`
- secondary_team_ids: `[105, 106]`
- expected_roles: `["MEMBER", "TEAM_LEAD"]`
- expected_status: `["COMPLETED", "ON_HOLD", "IN_PROGRESS"]`

### Q-EX-03
- query: `매장 주문 취소 정산 누락 업무`
- primary_team_ids: `[102, 107]`
- secondary_team_ids: `[]`
- expected_roles: `["MEMBER", "TEAM_LEAD"]`
- expected_status: `["IN_PROGRESS", "COMPLETED"]`

### Q-EX-04
- query: `JWT 인증 실패 로그 분석`
- primary_team_ids: `[103]`
- secondary_team_ids: `[108]`
- expected_roles: `["MEMBER", "DEPT_HEAD"]`
- expected_status: `["IN_PROGRESS", "COMPLETED"]`

### Q-EX-05
- query: `레거시 모듈 분리 후 장애`
- primary_team_ids: `[101, 104]`
- secondary_team_ids: `[105]`
- expected_roles: `["MEMBER", "TEAM_LEAD"]`
- expected_status: `["IN_PROGRESS", "COMPLETED"]`

### Q-EX-06
- query: `재고 스냅샷 차이 보정 이력`
- primary_team_ids: `[106]`
- secondary_team_ids: `[108]`
- expected_roles: `["MEMBER", "TEAM_LEAD"]`
- expected_status: `["COMPLETED", "ON_HOLD"]`

### Q-EX-07
- query: `중복 데이터 누락 필드 품질 리포트`
- primary_team_ids: `[108]`
- secondary_team_ids: `[103]`
- expected_roles: `["MEMBER", "DEPT_HEAD"]`
- expected_status: `["COMPLETED", "IN_PROGRESS"]`

### Q-EX-08
- query: `임베딩 pgvector 유사 업무 검색`
- primary_team_ids: `[109]`
- secondary_team_ids: `[104, 108]`
- expected_roles: `["MEMBER", "TEAM_LEAD"]`
- expected_status: `["IN_PROGRESS", "COMPLETED"]`

### Q-EX-09
- query: `KPI 산식 수정 대시보드`
- primary_team_ids: `[110]`
- secondary_team_ids: `[108]`
- expected_roles: `["MEMBER", "DEPT_HEAD"]`
- expected_status: `["COMPLETED", "IN_PROGRESS"]`

### Q-EX-10
- query: `릴리즈 노트 운영 이관 체크리스트`
- primary_team_ids: `[105]`
- secondary_team_ids: `[110]`
- expected_roles: `["MEMBER", "DEPT_HEAD"]`
- expected_status: `["COMPLETED", "ON_HOLD"]`

---

## B. Paraphrase expected hits

### Q-PARA-01
- query: `야간에 돌던 작업이 겹치면서 운영이 흔들린 사례`
- primary_team_ids: `[101]`
- secondary_team_ids: `[105, 106]`
- expected_roles: `["MEMBER"]`
- expected_status: `["COMPLETED", "IN_PROGRESS"]`

### Q-PARA-02
- query: `매장에서 화면은 이상 없는데 정산 숫자가 안 맞았던 일`
- primary_team_ids: `[102, 107]`
- secondary_team_ids: `[]`
- expected_roles: `["MEMBER"]`
- expected_status: `["COMPLETED", "IN_PROGRESS"]`

### Q-PARA-03
- query: `권한은 있는데 실제로는 접근이 막혔던 건`
- primary_team_ids: `[103]`
- secondary_team_ids: `[102, 108]`
- expected_roles: `["MEMBER", "DEPT_HEAD"]`
- expected_status: `["IN_PROGRESS", "COMPLETED"]`

### Q-PARA-04
- query: `로그는 남았는데 기준 정보 품질이 안 좋아서 후속 조치가 생긴 업무`
- primary_team_ids: `[108]`
- secondary_team_ids: `[103]`
- expected_roles: `["MEMBER"]`
- expected_status: `["ON_HOLD", "COMPLETED", "IN_PROGRESS"]`

### Q-PARA-05
- query: `비슷한 과거 업무를 찾기 위한 검색 실험`
- primary_team_ids: `[109]`
- secondary_team_ids: `[104]`
- expected_roles: `["MEMBER", "TEAM_LEAD"]`
- expected_status: `["IN_PROGRESS", "COMPLETED"]`

### Q-PARA-06
- query: `운영 지표를 경영 보고용으로 다시 묶은 작업`
- primary_team_ids: `[110]`
- secondary_team_ids: `[105]`
- expected_roles: `["MEMBER", "DEPT_HEAD"]`
- expected_status: `["COMPLETED"]`

### Q-PARA-07
- query: `기존 시스템을 쪼개다가 생긴 후폭풍 정리`
- primary_team_ids: `[101, 104]`
- secondary_team_ids: `[105]`
- expected_roles: `["MEMBER", "TEAM_LEAD"]`
- expected_status: `["IN_PROGRESS", "COMPLETED"]`

### Q-PARA-08
- query: `같은 문제가 며칠 뒤 다시 올라와 개선안까지 정리한 사례`
- primary_team_ids: `[101, 102, 106, 109]`
- secondary_team_ids: `[103, 107]`
- expected_roles: `["MEMBER", "TEAM_LEAD"]`
- expected_status: `["COMPLETED", "IN_PROGRESS"]`

---

## C. Recurrence / linkage expected hits

### Q-LINK-01
- query: `DX 플랫폼 운영 이슈가 WMS 정합성 개선으로 이어진 흐름`
- primary_team_ids: `[101, 106]`
- secondary_team_ids: `[]`
- expected_roles: `["MEMBER", "DEPT_HEAD"]`
- expected_status: `["COMPLETED", "IN_PROGRESS"]`

### Q-LINK-02
- query: `스타벅스 운영 이슈가 정산 고도화로 이어진 사례`
- primary_team_ids: `[102, 107]`
- secondary_team_ids: `[]`

### Q-LINK-03
- query: `권한 하드닝이 데이터 품질 진단으로 확장된 흐름`
- primary_team_ids: `[103, 108]`
- secondary_team_ids: `[]`

### Q-LINK-04
- query: `그룹웨어 개발 로그를 검색 PoC에서 다시 활용한 흐름`
- primary_team_ids: `[104, 109]`
- secondary_team_ids: `[]`

### Q-LINK-05
- query: `릴리즈 트레인 결과가 통합 대시보드로 이어진 흐름`
- primary_team_ids: `[105, 110]`
- secondary_team_ids: `[]`

---

## D. Role-based expected hits

### Q-ROLE-01
- query: `팀장이 후속 조치나 승인 여부를 검토한 기록`
- primary_team_ids: `[101, 104, 109]`
- secondary_team_ids: `[103, 110]`
- expected_roles: `["TEAM_LEAD"]`

### Q-ROLE-02
- query: `사업부장이 리스크 보고나 승인 기준을 남긴 업무`
- primary_team_ids: `[101, 103, 105, 108, 110]`
- secondary_team_ids: `[102, 104, 107]`
- expected_roles: `["DEPT_HEAD"]`

### Q-ROLE-03
- query: `본부장 보고까지 올라갈 정도로 큰 리스크가 있었던 업무`
- primary_team_ids: `[101, 103, 105, 109, 110]`
- secondary_team_ids: `[104]`
- expected_roles: `["DEPT_HEAD", "TEAM_LEAD", "MEMBER"]`
- notes: 본부장은 worklog author는 아니므로 보고 문맥 중심으로 본다

---

## E. Status / deadline expected hits

### Q-STATUS-01
- query: `마감일을 넘겨서 완료된 업무`
- primary_team_ids: `[101, 102, 105, 110]`
- secondary_team_ids: `[106, 109]`
- expected_status: `["COMPLETED"]`

### Q-STATUS-02
- query: `진행하다가 외부 확인 때문에 멈춘 일`
- primary_team_ids: `[101, 103, 108]`
- secondary_team_ids: `[105]`
- expected_status: `["ON_HOLD"]`

### Q-STATUS-03
- query: `우선순위가 바뀌어서 취소된 업무`
- primary_team_ids: `[102, 104, 105]`
- secondary_team_ids: `[109]`
- expected_status: `["CANCELLED"]`

### Q-STATUS-04
- query: `지금은 미완료인데 나중에 다시 봐야 하는 작업`
- primary_team_ids: `[101, 103, 108, 109]`
- secondary_team_ids: `[105]`
- expected_status: `["PENDING", "ON_HOLD", "IN_PROGRESS"]`

---

## F. Persona expected hits

### Q-PER-01
- query: `운영 영향부터 직설적으로 적은 매장/WMS 업무`
- primary_team_ids: `[102, 107]`
- secondary_team_ids: `[101, 106]`
- expected_roles: `["MEMBER"]`
- notes: user 16 계열

### Q-PER-02
- query: `수치 비교와 원인 후보를 신중하게 적은 데이터 품질 업무`
- primary_team_ids: `[108, 106]`
- secondary_team_ids: `[103]`
- notes: user 5, 13, 32 계열

### Q-PER-03
- query: `검색 결과 체감과 기술 조치를 같이 적은 AI 검색 업무`
- primary_team_ids: `[109]`
- secondary_team_ids: `[]`
- notes: user 31 계열

### Q-PER-04
- query: `리스크와 승인 기준을 짧게 적은 검토 메모`
- primary_team_ids: `[103, 105, 110]`
- secondary_team_ids: `[101]`
- expected_roles: `["DEPT_HEAD"]`

---

## G. Quick gold set for smoke test

처음 자동 평가를 돌릴 때는 아래 10개만 먼저 gold set으로 쓰는 것을 추천한다.

| query_id | query | primary_team_ids |
| --- | --- | --- |
| Q-EX-01 | P0 회귀 대응 이력 | [101] |
| Q-EX-03 | 매장 주문 취소 정산 누락 업무 | [102, 107] |
| Q-PARA-03 | 권한은 있는데 실제로는 접근이 막혔던 건 | [103] |
| Q-PARA-05 | 비슷한 과거 업무를 찾기 위한 검색 실험 | [109] |
| Q-LINK-01 | DX 플랫폼 운영 이슈가 WMS 정합성 개선으로 이어진 흐름 | [101, 106] |
| Q-LINK-04 | 그룹웨어 개발 로그를 검색 PoC에서 다시 활용한 흐름 | [104, 109] |
| Q-ROLE-02 | 사업부장이 리스크 보고나 승인 기준을 남긴 업무 | [101, 103, 105, 108, 110] |
| Q-STATUS-01 | 마감일을 넘겨서 완료된 업무 | [101, 102, 105, 110] |
| Q-STATUS-02 | 진행하다가 외부 확인 때문에 멈춘 일 | [101, 103, 108] |
| Q-PER-03 | 검색 결과 체감과 기술 조치를 같이 적은 AI 검색 업무 | [109] |

