# iBank pgvector / SQL Query Examples

이 문서는 iBank 더미 데이터 기반 시멘틱 검색 실험을 위한 **SQL / pgvector 예시 쿼리** 모음이다.

전제:
- `tb_worklog`는 현재 생성되어 있음
- `tb_worklog_embedding`은 나중에 별도로 적재한다고 가정
- `embedding vector(768)` 컬럼에 query embedding과 cosine/l2/inner product 비교를 수행

---

## 1. 가장 단순한 키워드 검색

```sql
select
  worklog_id,
  team_id,
  author_id,
  title,
  status_code,
  importance_code,
  instruction_date
from tb_worklog
where work_content ilike '%P0 회귀%'
   or request_content ilike '%P0 회귀%'
   or title ilike '%P0 회귀%'
order by instruction_date desc
limit 20;
```

---

## 2. 팀 필터 + 키워드 검색

```sql
select
  worklog_id,
  title,
  status_code,
  due_date,
  completion_date
from tb_worklog
where team_id in (101, 106)
  and (
    work_content ilike '%WMS%'
    or work_content ilike '%재고%'
    or title ilike '%정합성%'
  )
order by instruction_date desc
limit 20;
```

---

## 3. 지연 완료 업무 찾기

```sql
select
  worklog_id,
  team_id,
  author_id,
  title,
  due_date,
  completion_date
from tb_worklog
where status_code = 'COMPLETED'
  and completion_date is not null
  and completion_date > due_date
order by completion_date - due_date desc, due_date desc
limit 50;
```

---

## 4. 보류/취소 상태 업무 찾기

```sql
select
  worklog_id,
  team_id,
  title,
  status_code,
  request_content
from tb_worklog
where status_code in ('ON_HOLD', 'CANCELLED')
order by instruction_date desc
limit 50;
```

---

## 5. 역할 기반 검색용 join 예시

```sql
select
  w.worklog_id,
  w.team_id,
  w.title,
  u.user_name,
  u.role_code,
  ut.team_role,
  ut.is_leader
from tb_worklog w
join tb_user u
  on u.user_id = w.author_id
left join tb_user_team ut
  on ut.user_id = w.author_id
 and ut.team_id = w.team_id
where ut.is_leader = true
order by w.instruction_date desc
limit 50;
```

---

## 6. 상태 이력 포함 조회

```sql
select
  w.worklog_id,
  w.team_id,
  w.title,
  h.previous_status_code,
  h.new_status_code,
  h.changed_at,
  h.changed_by
from tb_worklog w
join tb_worklog_status_history h
  on h.worklog_id = w.worklog_id
where w.team_id = 105
order by w.worklog_id, h.changed_at;
```

---

## 7. pgvector cosine similarity 예시

아래는 앱/스크립트에서 query embedding을 구한 뒤, SQL에 바인딩하는 형태다.

```sql
-- :query_embedding 은 vector(768) 바인딩 변수라고 가정
select
  e.worklog_id,
  w.team_id,
  w.author_id,
  w.title,
  w.status_code,
  1 - (e.embedding <=> :query_embedding) as cosine_similarity
from tb_worklog_embedding e
join tb_worklog w
  on w.worklog_id = e.worklog_id
order by e.embedding <=> :query_embedding
limit 20;
```

---

## 8. 팀 제한 + semantic search

```sql
select
  e.worklog_id,
  w.team_id,
  w.title,
  w.instruction_date,
  1 - (e.embedding <=> :query_embedding) as cosine_similarity
from tb_worklog_embedding e
join tb_worklog w
  on w.worklog_id = e.worklog_id
where w.team_id in (101, 106)
order by e.embedding <=> :query_embedding
limit 20;
```

---

## 9. 상태 제한 + semantic search

```sql
select
  e.worklog_id,
  w.team_id,
  w.title,
  w.status_code,
  w.due_date,
  w.completion_date,
  1 - (e.embedding <=> :query_embedding) as cosine_similarity
from tb_worklog_embedding e
join tb_worklog w
  on w.worklog_id = e.worklog_id
where w.status_code in ('ON_HOLD', 'CANCELLED', 'IN_PROGRESS')
order by e.embedding <=> :query_embedding
limit 20;
```

---

## 10. hybrid search 예시

키워드 필터 + vector similarity를 같이 쓴다.

```sql
select
  e.worklog_id,
  w.team_id,
  w.title,
  w.work_content,
  1 - (e.embedding <=> :query_embedding) as cosine_similarity
from tb_worklog_embedding e
join tb_worklog w
  on w.worklog_id = e.worklog_id
where (
  w.title ilike '%권한%'
  or w.request_content ilike '%권한%'
  or w.work_content ilike '%권한%'
)
order by e.embedding <=> :query_embedding
limit 20;
```

---

## 11. linked TF 흐름 검색용 query

예: `101 -> 106` 연결 검증

```sql
select
  w.worklog_id,
  w.team_id,
  w.title,
  w.instruction_date,
  w.status_code
from tb_worklog w
where w.team_id in (101, 106)
  and (
    w.work_content ilike '%WMS%'
    or w.work_content ilike '%재고%'
    or w.work_content ilike '%정합성%'
    or w.title ilike '%재고%'
  )
order by w.team_id, w.instruction_date;
```

---

## 12. sample query embedding set

아래 자연어 질의를 embedding 후 7~10번 쿼리에 넣는 것을 추천한다.

- `야간에 돌던 작업이 겹치면서 운영이 흔들린 사례`
- `매장에서 화면은 이상 없는데 정산 숫자가 안 맞았던 일`
- `권한은 있는데 실제로는 접근이 막혔던 건`
- `비슷한 과거 업무를 찾기 위한 검색 실험`
- `마감일을 넘겨서 완료된 업무`
- `팀장이 후속 조치까지 같이 검토한 기록`

---

## 13. 결과 기록 권장 컬럼

검색 결과를 저장할 때 아래 정도를 같이 남기면 좋다.

- query_id
- query_text
- rank
- worklog_id
- team_id
- author_id
- similarity_score
- title
- status_code
- expected_hit(boolean)

