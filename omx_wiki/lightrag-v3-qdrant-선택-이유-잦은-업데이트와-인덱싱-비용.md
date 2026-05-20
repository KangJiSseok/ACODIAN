---
title: "LightRAG v3 Qdrant 선택 이유: 잦은 업데이트와 인덱싱 비용"
tags: ["ai", "lightrag", "qdrant", "pgvector", "vector-store", "indexing-cost", "non-developer", "decision"]
created: 2026-05-16T06:56:10.383Z
updated: 2026-05-16T07:07:10.613Z
sources: []
links: ["2026-05-12-065231-search-lightrag-전환-조사와-구성-선택지.md", "2026-05-12-073814-search-lightrag-전환-결정-기존-pgvector-유지와-병행-도입.md", "2026-05-13-050000-v2-lightrag-graphrag-권한-scoped-kg-검색-대안-조사.md", "lightrag-v3-업무일지-kg-index-및-id-only-retrieval-구현-계획.md"]
category: decision
confidence: medium
schemaVersion: 1
---

# LightRAG v3 Qdrant 선택 이유: 잦은 업데이트와 인덱싱 비용

관련 문서:

- [[2026-05-12-065231-search-lightrag-전환-조사와-구성-선택지]]
- [[2026-05-12-073814-search-lightrag-전환-결정-기존-pgvector-유지와-병행-도입]]
- [[2026-05-13-050000-V2-lightrag-graphrag-권한-scoped-kg-검색-대안-조사]]
- [[lightrag-v3-업무일지-kg-index-및-id-only-retrieval-구현-계획]]

## 한 줄 결론

AX-WMS에서 LightRAG v3를 운영형으로 키운다면 `QdrantVectorDBStorage`가 `PGVectorStorage`보다 유리하다.

가장 큰 이유는 **업무일지 수정이 잦을 때 생기는 벡터 인덱싱/재색인 비용을 PostgreSQL에서 분리할 수 있기 때문**이다.

```text
PGVector 선택:
  업무 원장 DB인 PostgreSQL이 업무 데이터 + 권한 + 기존 검색 + LightRAG 벡터 인덱싱까지 함께 부담한다.

Qdrant 선택:
  PostgreSQL은 업무 원장/권한을 맡고,
  Qdrant는 LightRAG 벡터 검색과 재색인 비용을 전담한다.
```

단, Qdrant가 권한 문제를 자동으로 해결하는 것은 아니다. 최종 권한 검증은 계속 PostgreSQL/API 기준으로 해야 한다.

---

## 먼저 알아야 할 쉬운 용어 정리

### 벡터(vector)

문장을 숫자 배열로 바꾼 것이다.

예를 들어 다음 문장이 있다고 하자.

```text
"물류센터 재고 현황을 점검했다"
```

AI는 이 문장을 사람이 읽는 글자 그대로 비교하지 않고, 의미를 나타내는 숫자 배열로 바꾼다.

```text
[0.12, -0.03, 0.88, ...]
```

이 숫자 배열을 벡터라고 한다.

### 벡터 검색

사용자가 검색어를 입력하면 검색어도 벡터로 바꾼 뒤, 기존 업무일지 벡터와 가까운 것을 찾는다.

```text
검색어 → 벡터로 변환 → 업무일지 벡터들과 거리 비교 → 가까운 업무일지 반환
```

### 벡터 스토어(vector store)

벡터를 저장하고 빠르게 찾는 저장소다.

이번 비교 대상은 다음이다.

```text
PGVector = PostgreSQL 안에 벡터를 저장하는 방식
Qdrant = 벡터 검색 전용 DB에 벡터를 저장하는 방식
```

### 인덱스(index)

검색을 빠르게 하기 위한 목차다.

책에서 원하는 내용을 찾을 때 전체 페이지를 처음부터 끝까지 읽지 않고 목차를 보듯이, DB도 검색을 빠르게 하려고 인덱스를 만든다.

벡터 검색의 인덱스는 일반 목차보다 훨씬 무겁다. 숫자 배열끼리 가까운 것을 빠르게 찾기 위한 특수한 구조이기 때문이다.

### 인덱싱(indexing)

데이터를 검색 가능한 상태로 만드는 작업이다.

업무일지를 LightRAG에 넣으면 대략 다음 일이 일어난다.

```text
업무일지 원문 읽기
→ 문장을 벡터로 변환
→ chunk/entity/relation 벡터 저장
→ 빠르게 찾기 위한 인덱스 생성 또는 갱신
→ KG 관련 저장소 갱신
```

### 재색인(re-indexing)

이미 저장된 데이터를 최신 내용으로 다시 인덱싱하는 작업이다.

업무일지가 수정되면 예전 벡터가 더 이상 맞지 않을 수 있다. 그래서 이전 벡터를 지우고 새 벡터를 다시 넣어야 한다.

```text
기존 업무일지 벡터 삭제
→ 최신 업무일지 내용으로 새 벡터 생성
→ 새 벡터 저장
→ 인덱스 다시 갱신
```

### KG(Knowledge Graph)

업무일지 안에서 중요한 대상과 관계를 뽑아 연결한 그래프다.

예:

```text
"재고 현황" — 관련됨 → "물류센터"
"입고 지연" — 원인 → "공급사 납기 변경"
```

LightRAG는 단순 벡터 검색뿐 아니라 이런 관계 정보를 같이 활용하려고 한다.

---

## AX-WMS 프로젝트 상황

AX-WMS의 PostgreSQL은 이미 중요한 일을 많이 하고 있다.

```text
PostgreSQL이 현재 담당하는 것:
  - 업무일지 원본
  - 사용자/팀/권한
  - 기존 pgvector 기반 semantic search
  - API가 최종 권한 필터에 사용하는 데이터
```

기존 검색은 다음 테이블을 사용한다.

```text
지금 있는 기존 검색:
  tb_worklog_embedding.embedding Vector(768)
```

LightRAG v3를 붙이면 여기에 새로운 벡터 저장소가 추가된다.

LightRAG는 업무일지를 단순히 하나의 벡터로만 저장하지 않는다. 보통 다음처럼 여러 종류의 벡터와 상태를 만든다.

```text
LightRAG가 만드는 것:
  - 문서 chunk 벡터
  - entity 벡터
  - relationship 벡터
  - full document 저장소
  - text chunk 저장소
  - graph 저장소
  - doc status 저장소
```

따라서 업무일지 하나가 수정되어도 내부적으로는 여러 저장소가 함께 바뀔 수 있다.

---

## 왜 업데이트가 잦으면 문제가 되는가

업무일지가 한 번 저장되고 거의 바뀌지 않는다면 PGVector도 충분히 단순하고 좋다.

하지만 AX-WMS에서는 업무일지 업데이트가 자주 발생할 수 있다.

예:

```text
업무 내용 수정
요청 내용 수정
선행 업무 관계 변경
태그 변경
팀 문맥 변경
AI 요약/가공 결과 변경
```

이런 변경이 생기면 LightRAG 입장에서는 기존 인덱스가 낡은 정보가 된다.

그래서 다음 작업이 반복된다.

```text
1. 기존 LightRAG 문서 삭제
2. 최신 업무일지 원문 다시 읽기
3. embedding 다시 생성
4. chunk/entity/relation vector 다시 저장
5. vector index 다시 갱신
6. KG 관련 정보 다시 정리
```

핵심은 5번이다. **벡터 인덱스 갱신 비용**이 작지 않다.

---

## PGVector에서는 이 비용이 어떻게 생기는가

### 1. PostgreSQL의 update/delete는 실제로 바로 사라지는 것이 아니다

PostgreSQL은 데이터를 안전하게 관리하기 위해 MVCC라는 방식을 쓴다.

비개발자 관점에서는 이렇게 이해하면 된다.

```text
PostgreSQL은 기존 줄을 지우개로 바로 지우지 않는다.
기존 줄에 "삭제됨" 표시를 해두고,
새 줄을 다시 추가한다.
나중에 청소 작업이 와서 삭제된 줄을 정리한다.
```

즉 업무일지를 다시 색인하면 PostgreSQL 안에서는 이런 일이 생긴다.

```text
기존 vector row 삭제 표시
새 vector row 추가
삭제 표시된 row는 당장 물리적으로 사라지지 않음
```

삭제 표시된 row를 `dead tuple`이라고 부른다.

### 2. vacuum은 PostgreSQL의 청소 작업이다

삭제 표시된 row가 쌓이면 PostgreSQL은 나중에 청소해야 한다. 이 청소 작업을 `vacuum`이라고 한다.

업데이트가 적으면 괜찮다.

하지만 LightRAG 재색인이 자주 일어나면 다음 일이 반복된다.

```text
vector 삭제/추가 반복
→ dead tuple 증가
→ vacuum 더 자주 필요
→ CPU, 디스크 I/O 사용
→ 일반 업무 쿼리와 리소스 경쟁
```

즉 `vacuum 비용`은 “삭제된 벡터 row를 PostgreSQL이 뒤늦게 청소하는 비용”이다.

### 3. bloat는 죽은 공간이 쌓여 저장소가 부풀어 오르는 현상이다

`bloat`는 실제로 살아있는 데이터보다 테이블이나 인덱스가 더 커지는 현상이다.

예를 들어 실제 살아있는 벡터가 100만 개라고 하자.

하지만 잦은 update/delete 때문에 내부에는 다음처럼 남을 수 있다.

```text
살아있는 vector row: 100만 개
삭제 표시되었지만 아직 공간을 차지하는 row: 50만 개
DB가 관리해야 하는 전체 흔적: 150만 개 수준
```

이렇게 되면 다음 문제가 생긴다.

```text
디스크 사용량 증가
메모리 캐시 효율 감소
인덱스 크기 증가
검색/정리 작업이 느려짐
reindex 필요성 증가
```

### 4. vector index 유지 비용이 일반 인덱스보다 무겁다

PGVector는 벡터 검색을 빠르게 하기 위해 HNSW나 IVFFLAT 같은 특수 인덱스를 쓴다.

이 인덱스는 일반 숫자/문자 인덱스보다 무겁다.

업무일지 벡터가 자주 바뀌면 PostgreSQL은 단순히 row만 바꾸는 것이 아니라, 벡터 검색용 인덱스도 계속 갱신해야 한다.

```text
새 vector insert
→ vector index에 추가

기존 vector delete
→ index에서 제외되어야 함
→ 실제 정리는 vacuum/reindex와 연결
```

LightRAG는 업무일지 하나당 chunk/entity/relation 벡터를 만들 수 있으므로, 업무일지 1건 수정이 여러 vector row와 index 갱신으로 번질 수 있다.

### 5. I/O 경합이 생긴다

I/O 경합은 쉽게 말하면 “같은 DB가 너무 많은 일을 동시에 하려고 줄 서는 현상”이다.

PostgreSQL은 이미 AX-WMS에서 다음 일을 한다.

```text
업무일지 저장/조회
팀/사용자/권한 조회
기존 semantic search
검색 필터링
트랜잭션 처리
```

여기에 LightRAG 재색인까지 PGVector로 얹으면 같은 PostgreSQL에서 다음이 동시에 일어난다.

```text
업무 API query
권한 query
기존 검색 query
LightRAG vector insert/delete
LightRAG vector index maintenance
vacuum
```

이러면 PostgreSQL의 CPU, 메모리, 디스크 I/O가 서로 경쟁한다.

---

## Qdrant에서는 같은 문제가 어떻게 처리되는가

Qdrant도 update/delete 비용이 없는 것은 아니다.

다만 Qdrant는 처음부터 벡터 검색을 위해 만든 DB라서, 벡터 업데이트와 인덱스 정리를 PostgreSQL과 다른 방식으로 처리한다.

특히 아래 내용을 명확히 구분해야 한다.

```text
틀린 이해:
  Qdrant는 인덱스가 없어서 update 비용이 싸다.

정확한 이해:
  Qdrant도 HNSW 같은 vector index가 있다.
  다만 update/delete/re-indexing 비용을 PostgreSQL의 row, MVCC, vacuum, table/index bloat 방식이 아니라
  Qdrant의 point, segment, optimizer 방식으로 처리한다.
```

따라서 Qdrant의 장점은 “인덱스가 없어서 공짜”가 아니라, **AI 검색용 인덱스 변경 비용을 PostgreSQL 원장 DB에서 분리한다는 점**이다.

### 1. row가 아니라 point 단위로 저장한다

Qdrant는 벡터 하나를 `point`로 저장한다.

```text
point = id + vector + payload
```

예:

```text
id: chunk-123
vector: [0.12, -0.03, 0.88, ...]
payload: { full_doc_id: "worklog-10", file_path: "worklog://10" }
```

같은 id로 다시 저장하면 기존 point를 새 값으로 바꾼다.

```text
upsert(point_id, vector, payload)
→ 같은 point_id가 있으면 새 값으로 대체
```

이 문장은 조금 더 정확히 풀어서 이해해야 한다.

`upsert`는 **Qdrant가 업무일지 원문을 다시 읽고 embedding을 만들어준다는 뜻이 아니다.**
수정된 업무일지 내용으로 새 embedding을 만드는 일은 여전히 AX-WMS/LightRAG/embedding model이 해야 한다.
Qdrant는 이미 만들어진 새 `vector`와 `payload`를 받아 저장소에 반영한다.

```text
업무일지 수정
→ AX-WMS/LightRAG가 최신 원문으로 embedding 재생성
→ 새 vector와 payload 준비
→ Qdrant에 같은 point_id로 upsert
→ 검색 기준은 새 point 값으로 교체
```

또 하나 중요한 점은, `upsert`가 내부 비용 없이 기존 데이터를 마법처럼 제자리 수정한다는 뜻도 아니라는 것이다.
실제로는 **기존 point를 더 이상 최신 값으로 쓰지 않게 만들고, 새 point를 넣는 동작**에 가깝다.

```text
기존 point
  id: worklog-10-chunk-0
  vector: 예전 업무일지 embedding
  상태: 더 이상 최신 값 아님 / 삭제 표시 대상

새 point
  id: worklog-10-chunk-0
  vector: 수정된 업무일지 embedding
  상태: 최신 검색 대상
```

삭제할 때도 point id 기준으로 삭제한다.

```text
delete(point_id)
→ 검색 결과에서 제외
→ 물리 정리는 Qdrant optimizer가 나중에 처리
```

정리하면 다음과 같다.

```text
Qdrant upsert가 해주는 것:
  - 같은 point_id의 검색 기준을 새 vector/payload로 바꾼다.
  - 기존 point는 검색에서 제외되도록 처리한다.
  - 삭제된 흔적과 segment/index 정리는 optimizer가 나중에 처리한다.

Qdrant upsert가 해주지 않는 것:
  - 업무일지 원문 재해석
  - embedding 재생성
  - LightRAG KG 재구축
  - PostgreSQL 권한/원장 데이터 갱신
```

### 2. vacuum 대신 optimizer/compaction이 있다

Qdrant에는 PostgreSQL의 `vacuum`과 같은 이름의 기능은 없다.

대신 `optimizer`와 `compaction`에 가까운 정리 작업이 있다.

Qdrant 내부는 데이터를 segment라는 단위로 나눠 관리한다.

```text
Collection
  ├─ Segment A
  ├─ Segment B
  └─ Segment C
```

삭제나 업데이트가 많아지면 어떤 segment에는 삭제된 point 흔적이 많아진다.

그러면 Qdrant optimizer가 백그라운드에서 다음 일을 한다.

```text
삭제된 point가 많은 segment 정리
작은 segment 병합
살아있는 point만 새 segment로 재작성
vector index 재구성 또는 최적화
```

역할은 PostgreSQL vacuum과 비슷하게 “정리”지만, 목적이 다르다.

```text
PostgreSQL vacuum:
  범용 관계형 DB 테이블 청소

Qdrant optimizer:
  벡터 검색 segment와 vector index 최적화
```

### 3. Qdrant에도 공간 낭비는 생기지만 위치가 분리된다

Qdrant도 삭제가 많으면 일시적으로 segment 안에 낭비 공간이 생길 수 있다.

하지만 그 낭비는 Qdrant 안에서 발생한다.

```text
PostgreSQL:
  업무 원장 / 권한 / 기존 검색 유지

Qdrant:
  LightRAG vector segment / vector index / compaction 처리
```

즉 LightRAG 재색인으로 생기는 정리 비용이 PostgreSQL 원장 DB에 직접 쌓이지 않는다.

### 4. vector index 유지가 vector DB 내부에서 처리된다

Qdrant는 vector index를 segment 단위로 관리한다.

```text
새 point upsert
→ segment에 저장
→ 조건에 따라 vector index 생성/갱신
→ 검색 시 여러 segment 결과를 합쳐 top-k 반환
```

삭제 시에는 다음처럼 동작한다.

```text
delete point
→ 검색에서는 제외
→ segment에는 삭제 표시
→ optimizer가 나중에 segment compact/rebuild
```

즉 Qdrant도 정리 작업은 필요하지만, 그 정리 작업은 vector DB 내부의 역할로 분리된다.

### 5. 그래서 “Qdrant가 더 싸다”의 정확한 의미

Qdrant가 더 유리하다는 말은 “update/delete/index 비용이 없다”는 뜻이 아니다.

더 정확한 의미는 다음이다.

```text
공통점:
  PGVector도 Qdrant도 vector index가 있다.
  둘 다 기존 embedding을 바꾸려면 새 embedding을 다시 넣어야 한다.
  둘 다 삭제/삽입/인덱스 정리 비용이 발생한다.

차이점:
  PGVector는 그 비용이 PostgreSQL 내부의 table, index, vacuum, bloat, I/O 경합으로 나타난다.
  Qdrant는 그 비용이 Qdrant 내부의 point, segment, optimizer, HNSW index 관리로 분리된다.
```

AX-WMS에서 중요한 것은 비용의 절대값만이 아니다.
업무일지 원본, 권한, 팀/사용자 정보를 담당하는 PostgreSQL이 LightRAG 재색인 작업까지 함께 떠안지 않게 만드는 것이 중요하다.

```text
PGVector 사용 시:
  업무 DB PostgreSQL이 업무 원장 + 권한 + 기존 검색 + LightRAG vector 재색인을 함께 부담

Qdrant 사용 시:
  PostgreSQL은 업무 원장/권한 중심으로 유지
  Qdrant가 LightRAG vector 저장/검색/재색인 부담을 전담
```

따라서 Qdrant 선택 이유는 다음처럼 표현하는 것이 가장 정확하다.

> Qdrant는 인덱스가 없어서 싼 것이 아니라,
> vector update/delete/index 최적화를 전용 DB 내부에서 처리하고,
> 그 비용을 PostgreSQL 원장 DB와 분리할 수 있어서 AX-WMS에 유리하다.

---

## PGVector와 Qdrant 비교 요약

| 항목 | PGVector | Qdrant |
|---|---|---|
| 저장 위치 | PostgreSQL 테이블 | Qdrant collection |
| 저장 단위 | row | point |
| 삭제 처리 | dead tuple 발생 | deleted point 표시 |
| 청소 방식 | vacuum/autovacuum | optimizer/compaction |
| 공간 낭비 | PostgreSQL table/index bloat | Qdrant segment 내부 낭비 |
| vector index 관리 | PostgreSQL index maintenance | Qdrant vector index/segment optimizer |
| 같은 ID update | row update/delete + 새 row insert처럼 누적 | 기존 point 삭제 표시 + 새 point 반영, optimizer가 segment 정리 |
| 원장 DB 영향 | 직접 영향 있음 | PostgreSQL과 분리 |
| 초기 인프라 비용 | 낮음. 이미 PostgreSQL이 있으면 유리 | 별도 Qdrant 운영 필요 |
| 잦은 재색인 비용 | PostgreSQL에 누적 | Qdrant가 전담 |
| AX-WMS 적합성 | update 적으면 단순함 | update 많으면 운영 분리에 유리 |

---

## 왜 AX-WMS에는 Qdrant가 더 유리한가

AX-WMS에서 PostgreSQL은 단순 저장소가 아니다.

PostgreSQL은 다음의 기준 시스템이다.

```text
업무일지 원본
팀 정보
사용자 정보
권한 정보
기존 검색 fallback
```

이 시스템은 안정적이어야 한다.

그런데 LightRAG는 실험과 변경 가능성이 큰 검색/AI 계층이다.

```text
embedding 모델 변경 가능
chunk 전략 변경 가능
KG 재구축 가능
업무일지 update에 따른 재색인 가능
vector storage 변경 가능
```

이 실험적이고 write-heavy한 비용을 PostgreSQL 안에 넣으면, 업무 원장 DB가 같이 흔들릴 수 있다.

Qdrant를 쓰면 역할을 나눌 수 있다.

```text
PostgreSQL:
  정답 데이터와 권한을 안정적으로 관리한다.

Qdrant:
  LightRAG 벡터 검색과 재색인 비용을 처리한다.
```

이것이 AX-WMS에서 Qdrant를 우선 후보로 두는 이유다.

---

## 중요한 한계

### Qdrant가 권한 필터를 자동으로 해결하지는 않는다

Qdrant 자체는 payload filter를 지원한다.

하지만 LightRAG stock 구현은 query 시 Qdrant에 임의의 `team_id`, `author_id`, `department_id` 필터를 넘기지 않는다. 현재 기본 구현은 workspace filter 중심이다.

따라서 최종 권한 검증은 계속 PostgreSQL/API에서 해야 한다.

```text
LightRAG/Qdrant 후보 검색
→ worklog_id 추출
→ PostgreSQL/API에서 권한 재검증
→ 허용된 업무일지만 최종 응답
```

### Qdrant를 써도 LightRAG update 정책은 따로 필요하다

LightRAG의 `ainsert`는 같은 `doc_id`가 이미 있으면 자동 update하지 않는다.

따라서 업무일지가 수정되면 다음 정책이 필요하다.

```text
adelete_by_doc_id("worklog-{id}")
→ 최신 원문으로 다시 ainsert(..., ids=["worklog-{id}"])
```

Qdrant 선택은 vector storage 비용을 분리하는 선택이지, LightRAG update workflow를 자동으로 완성하는 선택은 아니다.

### vector storage만 Qdrant로 바꿔도 전체 운영 설계가 끝나지는 않는다

LightRAG는 vector storage 외에도 여러 저장소를 쓴다.

```text
KV_STORAGE
VECTOR_STORAGE
GRAPH_STORAGE
DOC_STATUS_STORAGE
```

Qdrant는 이 중 `VECTOR_STORAGE`만 담당한다.

업데이트와 KG 재구축이 정말 잦아지면 다음도 별도로 정해야 한다.

```text
doc_status를 어디에 둘 것인가?
graph storage를 파일로 둘 것인가, DB로 둘 것인가?
full_docs/text_chunks/cache를 어떻게 백업/복구할 것인가?
```

---

## 최종 권고

### 단기

```text
NanoVectorDBStorage 유지
기존 pgvector semantic search 병행
LightRAG v3는 실험/검증 경로로 사용
```

### 중기

```text
QdrantVectorDBStorage 도입 검증
업무일지 update 시 delete 후 reinsert 정책 구현
PostgreSQL 권한 재필터 유지
```

### 장기

```text
Qdrant + 운영형 doc_status/graph/KV storage 전략 수립
LightRAG 재색인/복구/rollback 절차 문서화
기존 pgvector fallback은 충분한 검증 전까지 유지
```

## 최종 결론

PGVector가 나쁘다는 뜻은 아니다.

```text
업데이트가 적고 규모가 작다
→ PGVector가 단순하고 인프라 비용이 싸다.

업데이트가 잦고 LightRAG 재색인이 많다
→ PGVector는 PostgreSQL 내부 인덱싱 비용이 커진다.
→ Qdrant는 그 비용을 vector DB로 분리할 수 있다.
```

AX-WMS는 PostgreSQL이 업무 원장과 권한을 책임지는 시스템이다. 따라서 LightRAG의 잦은 벡터 재색인 비용은 PostgreSQL 안에 얹기보다 Qdrant로 분리하는 편이 운영적으로 더 안전하다.

