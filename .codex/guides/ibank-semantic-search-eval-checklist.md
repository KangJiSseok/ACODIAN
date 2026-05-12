# iBank Semantic Search Evaluation Checklist

이 문서는 `.codex/guides/ibank-semantic-search-query-set.md`의 질의문 세트를 기준으로  
검색 품질을 점검할 때 사용하는 **자동 평가 체크리스트**다.

---

## 1. 평가 단위

각 질의마다 아래를 기록한다.

- query_id
- query_text
- query_type
  - exact
  - paraphrase
  - recurrence
  - role-based
  - status/deadline
  - cross-team
  - needle
  - persona
- top_k
- returned_worklog_ids
- returned_team_ids
- expected_team_ids
- expected_role_hint
- expected_status_hint

---

## 2. 자동 점검 항목

### A. Team hit accuracy

- top 3 안에 기대 팀이 1개 이상 있는가
- top 5 안에 기대 팀이 2개 이상 있는가
- 기대 팀이 하나뿐인 질의에서 top 3이 모두 그 팀 중심인가

추천 계산:

- `team_hit@3`
- `team_hit@5`
- `primary_team_rank`

---

### B. Cross-team linkage accuracy

후속 TF/선행 TF 질의에 대해:

- 선행 팀과 후속 팀이 모두 top 5 안에 들어오는가
- 후속 팀만 나오고 선행 팀이 빠지지 않는가
- 선행 팀만 나오고 후속 팀이 빠지지 않는가

예:
- `101 -> 106`
- `102 -> 107`
- `103 -> 108`
- `104 -> 109`
- `105 -> 110`

추천 계산:

- `linked_pair_hit@5`
- `linked_pair_hit@10`

---

### C. Semantic paraphrase quality

직접 키워드가 없거나 약한 질의에서:

- 의미가 비슷한 팀이 검색되는가
- exact match보다 paraphrase에서 급격히 무너지지 않는가
- 표면 단어가 다르더라도 같은 이슈 흐름 문서가 잡히는가

추천 계산:

- `paraphrase_hit@5`
- `paraphrase_vs_exact_gap`

---

### D. Role-aware retrieval

역할 기반 질의에서:

- 팀장 검토 질의면 팀장 authored worklog가 상위에 오는가
- 사업부장 검토 질의면 사업부장 authored worklog가 상위에 오는가
- 본부장 보고 질의면 보고/리스크/승인 문맥이 높은가

추천 계산:

- `role_hit@5`
- `lead_hit@5`
- `dept_head_hit@5`
- `director_context_hit@10`

---

### E. Status-aware retrieval

상태 질의에서:

- `ON_HOLD`를 찾으면 실제 보류 worklog가 상위에 오는가
- `CANCELLED`를 찾으면 실제 취소 worklog가 상위에 오는가
- `지연 완료`를 찾으면 `completion_date > due_date` 문서가 상위에 오는가

추천 계산:

- `status_hit@5`
- `overdue_completed_hit@10`

---

### F. Persona differentiation

사람 성향 질의에서:

- 운영형/직설형/분석형/검토형 문맥이 구분되는가
- 같은 팀 안에서도 다른 작성자 스타일이 검색 결과에 반영되는가

추천 계산:

- `persona_hit@5`
- `author_diversity@10`

---

## 3. 권장 자동 평가 기준

아래는 시작점용 권장 기준이다.

### Basic pass

- `team_hit@5 >= 0.8`
- `linked_pair_hit@10 >= 0.7`
- `status_hit@5 >= 0.8`

### Good

- `team_hit@3 >= 0.75`
- `paraphrase_hit@5 >= 0.75`
- `role_hit@5 >= 0.7`

### Strong

- `linked_pair_hit@5 >= 0.75`
- `persona_hit@5 >= 0.65`
- `overdue_completed_hit@10 >= 0.8`

---

## 4. 수동 QA 병행 기준

자동 평가는 통과했더라도 아래는 사람이 같이 본다.

- top 5가 모두 같은 템플릿 문장으로 보이지 않는가
- 결과가 맞더라도 실제 읽었을 때 너무 AI 같지 않은가
- 후속 TF가 검색되더라도 문맥 연결이 자연스러운가
- 역할 기반 질의에서 팀장/사업부장/본부장 문맥이 섞이지 않는가

---

## 5. 추천 평가 순서

1. exact keyword 10개
2. paraphrase 8개
3. linked pair 5개
4. status/deadline 6개
5. role/persona 8개

---

## 6. 결과 기록 템플릿

```json
{
  "query_id": "Q-PARA-03",
  "query_text": "권한은 있는데 실제로는 접근이 막혔던 건",
  "query_type": "paraphrase",
  "top_k": 10,
  "returned_team_ids": [103, 108, 102, 103, 109],
  "expected_team_ids": [103],
  "expected_role_hint": ["TEAM_LEAD", "DEPT_HEAD", "MEMBER"],
  "expected_status_hint": [],
  "team_hit@5": true,
  "role_hit@5": true,
  "notes": "권한 하드닝 TF가 상위에 잘 노출됨"
}
```

---

## 7. 실패 패턴 진단 힌트

### 기대 팀이 안 나옴
- 키워드 분산이 너무 강함
- 팀 고유 도메인 문장이 약함
- 후속 TF가 선행 TF 문맥을 충분히 흡수하지 못함

### 너무 많은 팀이 섞임
- 공통 운영 문장이 지나치게 강함
- 상태/검토 문장이 범용적임
- 팀 고유 표현보다 일반 표현 비중이 큼

### 역할 구분이 안 됨
- 팀장/사업부장/본부장 문체 차이가 약함
- 승인/검토/보고 표현이 비슷함

### persona 차이가 약함
- 후속 문장/관찰 문장이 공통 템플릿으로 남아 있음
- 팀원별 습관 문장이 충분히 드러나지 않음

