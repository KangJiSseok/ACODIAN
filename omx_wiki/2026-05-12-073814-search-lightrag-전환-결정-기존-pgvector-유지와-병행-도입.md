---
title: "LightRAG 전환 결정: 기존 pgvector 유지와 endpoint 병행 도입"
tags: ["ai", "lightrag", "pgvector", "semantic-search", "migration", "decision", "authorization", "endpoint-versioning"]
created: 2026-05-12T07:38:14.210Z
updated: 2026-05-13T00:00:00.000Z
sources: []
links: ["2026-05-12-065231-search-lightrag-전환-조사와-구성-선택지.md", "2026-05-12-080727-search-lightrag-병행-도입-구현-계획.md"]
category: decision
confidence: medium
schemaVersion: 1
---

# LightRAG 전환 결정: 기존 pgvector 유지와 endpoint 병행 도입

관련 문서:

- [[2026-05-12-065231-search-lightrag-전환-조사와-구성-선택지]]
- [[2026-05-12-080727-search-lightrag-병행-도입-구현-계획]]

## 결정

AX-WMS `ai`의 기존 pgvector 기반 업무일지 semantic search를 즉시 제거하거나 내부에서 교체하지 않는다. 기존 검색은 v1 compatibility endpoint로 유지하고, LightRAG 검색은 별도 endpoint와 별도 코드 폴더로 병행 도입한다.

선택한 방식은 다음이다.

```text
기존 v1 compatibility endpoint 유지
  POST /ai/search/worklogs
  -> ai/app/router/search.py

LightRAG versioned endpoint 추가
  POST /ai/light/worklogs-v2
  -> ai/app/light/v2/...

후속 version 추가
  POST /ai/light/worklogs-v3
  -> ai/app/light/v3/...
```

즉, 이번 결정의 핵심은 `SearchEngine` 전략 패턴이나 runtime engine switching이 아니라 **endpoint versioning + version별 폴더 격리**다.

## 선택 이유

### 1. 기존 검색 기능을 깨지 않고 LightRAG를 검증할 수 있다

현재 AX-WMS `ai`에는 이미 pgvector 기반 검색 구현이 존재한다.

```text
/ai/search/worklogs
  -> SearchService.search_worklogs
  -> GeminiClient.embed
  -> EmbeddingStore.search_worklogs
  -> WorklogEmbedding.embedding Vector(768)
```

이 경로를 바로 제거하거나 내부 구현을 LightRAG로 교체하면 검색 품질, 응답 latency, 권한 필터링, 결과 정렬, API contract가 동시에 흔들릴 수 있다.

따라서 기존 `/ai/search/worklogs`는 v1 compatibility endpoint로 그대로 두고, LightRAG는 `/ai/light/worklogs-v2` 같은 새 endpoint로 검증한다.

### 2. endpoint로 version을 드러내면 호출 경계가 명확하다

LightRAG version을 환경변수나 내부 registry로 숨기지 않고 endpoint에 드러내면 다음 장점이 있다.

- API 서버 또는 클라이언트가 어떤 검색 version을 호출하는지 URL만 보고 알 수 있다.
- `/ai/search/worklogs`와 `/ai/light/worklogs-v2`의 책임이 섞이지 않는다.
- v2 실험 중 문제가 생겨도 v1 compatibility endpoint를 건드리지 않는다.
- v3가 필요하면 v2를 수정하지 않고 `/ai/light/worklogs-v3`를 추가할 수 있다.

### 3. version별 폴더 격리로 rollback이 쉬워진다

LightRAG 도입 과정에서는 request/response contract, ranking, score mapping, source mapping, 권한 재검증 위치가 version마다 달라질 수 있다.

따라서 LightRAG 관련 코드는 version별 폴더에 둔다.

```text
ai/app/light/
  v2/
    router.py
    model.py
    service.py
    client.py
  v3/
    router.py
    model.py
    service.py
    client.py
```

원칙:

- `ai/app/light/v2`는 `ai/app/light/v3`를 import하지 않는다.
- `ai/app/light/v3`는 `ai/app/light/v2`를 import하지 않는다.
- version 간 공통화보다 독립성과 git revert 용이성을 우선한다.
- 한 version의 실험/수정이 다른 version endpoint를 오염시키지 않아야 한다.

### 4. 권한 필터링은 PostgreSQL/API 기준으로 계속 보장한다

AX-WMS 업무일지 접근권한은 PostgreSQL의 팀/멤버십/관리자 grant 기준으로 계산된다.

대표 조건:

```java
/** team.deleted_at IS NULL AND team_id IN (admin grant ∪ ACTIVE membership). */
private Condition visibleTeamCondition(Long userId) {
    return TB_TEAM.DELETED_AT.isNull()
            .and(TB_TEAM.TEAM_ID.in(visibleTeamIds(userId)));
}

/**
 * 파일 목록과 업무 요약 조회가 같은 팀 가시성 집합을 사용하도록 grant 와 ACTIVE membership 을 합친다.
 */
private Select<Record1<Long>> visibleTeamIds(Long userId) {
    return DSL.select(TB_TEAM_ADMIN.TEAM_ID)
            .from(TB_TEAM_ADMIN)
            .where(TB_TEAM_ADMIN.USER_ID.eq(userId))
            .union(DSL.select(TB_USER_TEAM.TEAM_ID)
                    .from(TB_USER_TEAM)
                    .where(TB_USER_TEAM.USER_ID.eq(userId))
                    .and(TB_USER_TEAM.STATUS_CODE.eq(ACTIVE_USER_TEAM_STATUS)));
}
```

LightRAG는 검색 엔진이지 AX-WMS의 권한 source of truth가 아니다. 따라서 LightRAG를 도입하더라도 최종 권한 검증은 PostgreSQL/API 기준으로 유지해야 한다.

후속 구현의 기본 원칙은 다음과 같다.

```text
LightRAG candidate result
  -> worklog_id/source_id 추출
  -> PostgreSQL/API 기준 접근 가능 여부 확인
  -> 허용된 업무일지만 최종 응답
```

### 5. 검색 품질 비교는 가능하지만 runtime mode로 섞지 않는다

LightRAG는 기존 vector top-k 검색과 다르게 entity / relationship extraction, knowledge graph, vector retrieval, reranking을 결합한다.

따라서 기존 pgvector보다 항상 좋다고 가정하지 않고 다음 기준으로 비교한다.

- 기존 pgvector top-k와 LightRAG 결과 비교
- LightRAG `naive`, `hybrid`, `mix` 같은 LightRAG 내부 query mode 비교
- 권한 필터 적용 후 실제 반환 가능한 결과 수 비교
- 검색 latency 비교
- 업무일지 검색 fixture/gold query 기준 recall, precision, 사용자 만족도 비교

다만 이 비교는 별도 평가 스크립트, 테스트, shadow 성격의 로그 수집으로 수행한다. 사용자 요청 처리 중 자동 fallback, 자동 hybrid merge, 요청별 동적 engine switching을 기본 전략으로 두지 않는다.

## 비채택한 이전 방향

V2 초안에서 검토했던 다음 방향은 V3 기준 구현 방향으로 채택하지 않는다.

```text
SearchService
  -> WorklogSearchEngine interface
      -> PgvectorWorklogSearchEngine
      -> LightRagWorklogSearchEngine
      -> ShadowWorklogSearchEngine
      -> FallbackWorklogSearchEngine
      -> HybridWorklogSearchEngine
```

```text
WORKLOG_SEARCH_ENGINE=pgvector
WORKLOG_SEARCH_ENGINE=lightrag
WORKLOG_SEARCH_ENGINE=shadow
WORKLOG_SEARCH_ENGINE=fallback
WORKLOG_SEARCH_ENGINE=hybrid
```

비채택 이유:

- LightRAG 실험이 기존 `/ai/search/worklogs` 내부 구현과 강하게 결합된다.
- registry/전략 패턴이 version별 실험과 git revert 경계를 흐린다.
- fallback/hybrid 같은 runtime mode는 장애와 품질 문제를 숨길 수 있다.
- 사용자는 endpoint 단위 version 관리를 원하며, LightRAG 작업을 `ai/app/light/vN` 아래에 격리하려는 목적이 명확하다.

## 선택한 방식의 설계 방향

### 기본 구조

```text
API 또는 caller
  -> 기존 검색 필요 시
      POST /ai/search/worklogs
      -> 기존 pgvector semantic search 흐름

  -> LightRAG v2 검증/사용 필요 시
      POST /ai/light/worklogs-v2
      -> ai/app/light/v2/router.py
      -> ai/app/light/v2/service.py
      -> LightRAG 후보 검색 및 후속 권한 검증 정책 적용
```

`/ai/search/worklogs`는 기존 안정 경로다. `/ai/light/worklogs-v2`는 LightRAG v2 경로다. 두 경로는 하나의 engine selector 뒤에서 섞이지 않는다.

### LightRAG 결과 처리 원칙

LightRAG에 업무일지를 ingestion할 때, 추적 가능한 source identifier를 포함해야 한다.

예:

```text
source_id = "worklog:123"
file_path = "worklog/123"
metadata = {
  "worklog_id": 123,
  "team_id": 10,
  "writer_id": 5
}
```

LightRAG 검색 결과에서는 reference/source/chunk 정보에서 `worklog_id`를 추출하고, PostgreSQL/API 기준으로 다시 권한을 검증한다.

## 운영/검증 방식 후보

### 1. v1 compatibility 유지

```text
POST /ai/search/worklogs
```

용도:

- 기존 안정 검색 경로
- LightRAG 실험과 무관하게 유지해야 하는 compatibility boundary

### 2. v2 endpoint 검증

```text
POST /ai/light/worklogs-v2
```

용도:

- LightRAG v2 검색 품질 검증
- v2 request/response contract 검증
- v2 내부 ranking/score/source mapping 정책 검증

### 3. v3 endpoint 추가

```text
POST /ai/light/worklogs-v3
```

용도:

- v2와 다른 contract 또는 ranking 정책 실험
- v2 파일을 수정하지 않는 새 version 검증
- version별 rollback 경계 유지

### 4. offline/shadow 평가

```text
기존 fixture/gold query
  -> /ai/search/worklogs 결과 수집
  -> /ai/light/worklogs-v2 결과 수집
  -> 별도 평가 로그/리포트에서 비교
```

용도:

- 사용자 응답 경로를 자동으로 섞지 않고 품질 비교
- latency, recall, precision, 권한 검증 후 유효 결과 수 측정

## 다른 선택지와 비교

### 선택지 1. PostgreSQL + pgvector를 LightRAG vector storage로 사용하며 기존 검색 교체

```text
SearchService
  -> LightRAG adapter
  -> LightRAG Server
  -> PGVectorStorage
```

장점:

- PostgreSQL/pgvector 인프라를 계속 활용 가능
- 외부 vector DB를 추가하지 않아도 됨
- LightRAG를 메인 검색 엔진으로 단순화 가능

단점:

- 기존 `EmbeddingStore.search_worklogs` 중심 코드가 대폭 수정/축소/삭제됨
- 기존 `tb_worklog_embedding`을 그대로 재사용하는 것이 아니라 LightRAG 자체 storage로 재색인해야 함
- 검색 품질 검증 전 교체하면 rollback이 어렵다
- 권한 필터, score, 정렬, API 응답 호환성을 동시에 맞춰야 한다

이번에 선택하지 않은 이유:

- 현재는 LightRAG 품질과 운영 특성을 아직 충분히 검증하지 않았다.
- 기존 검색 기능을 보존하며 점진 이전하는 편이 안전하다.

### 선택지 2. LightRAG 기본 local storage만 사용

```text
LightRAG local/default storage
  - JSON/local KV
  - NanoVectorDB
  - NetworkX
```

장점:

- 가장 빠른 PoC 가능
- 외부 인프라 구성 없이 LightRAG 동작 확인 가능

단점:

- 운영 부적합 가능성이 큼
- 동시성, 백업, 복구, 멀티 인스턴스, 장애 대응이 약함
- 추후 production storage로 바꾸면 재색인이 필요하다

이번에 선택하지 않은 이유:

- PoC 보조 수단으로는 가능하지만, AX-WMS 검색 마이그레이션 전략 자체로 삼기에는 부족하다.

### 선택지 3. PostgreSQL pgvector + Neo4j 조합

```text
LightRAG Server
  -> PostgreSQL: KV / Vector / Doc status
  -> Neo4j: Graph
```

장점:

- graph retrieval과 graph visualization에 강함
- 팀/사용자/업무/태그 관계를 지식 그래프로 확장하기 좋음
- LightRAG의 graph 기반 장점을 더 적극적으로 활용 가능

단점:

- Neo4j 운영 컴포넌트 추가
- compose/deploy/backup/monitoring/security 복잡도 증가
- 현재 단계에서는 graph 활용도가 검증되지 않았다

이번에 선택하지 않은 이유:

- 장기 확장 후보로는 좋지만, 초기 마이그레이션 단계에서는 운영 복잡도 대비 이점이 아직 불확실하다.

### 선택지 4. 기존 pgvector 검색 유지 + LightRAG endpoint 병행

```text
기존 /ai/search/worklogs 유지
  + 신규 /ai/light/worklogs-v2 추가
  + 후속 /ai/light/worklogs-v3 추가 가능
```

장점:

- 기존 기능 보존
- 검색 품질 비교 가능
- endpoint 단위 호출 경계 명확
- version별 폴더 격리로 rollback 쉬움
- 장기적으로 LightRAG version 승격 가능

단점:

- 검색 endpoint가 일시적으로 여러 개가 된다
- caller/API 서버가 어떤 endpoint를 호출할지 결정해야 한다
- version별 중복 코드가 생길 수 있다

이번 선택 이유:

- 현재 프로젝트 상황에서 가장 안전하고 점진적인 방식이다.
- 기존 pgvector 구현을 폐기하지 않고 LightRAG를 검증할 수 있다.
- 사용자가 원하는 endpoint versioning과 `ai/app/light/vN` 격리 전략에 부합한다.

### 선택지 5. Qdrant/Milvus + Neo4j 전문 검색 인프라

```text
Vector: Qdrant 또는 Milvus
Graph: Neo4j 또는 Memgraph
KV/status: PostgreSQL/Redis/MongoDB
```

장점:

- 대규모 vector search와 graph search에 유리
- 검색 플랫폼으로 장기 확장 가능

단점:

- 현재 단계에서는 과할 가능성이 높음
- 운영 컴포넌트와 장애 지점 증가
- 개발/배포 복잡도 증가

이번에 선택하지 않은 이유:

- 현재 목표는 안전한 LightRAG 도입과 검증이지, 대규모 검색 플랫폼 구축이 아니다.

## 구현 시 주의사항

1. `/ai/search/worklogs`는 v1 compatibility endpoint로 유지한다.
2. LightRAG 관련 작업은 `/ai/light/worklogs-vN` endpoint와 `ai/app/light/vN` 폴더로 격리한다.
3. `v2`, `v3` 등 version 폴더는 서로 import/참조하지 않는다.
4. LightRAG를 권한 source of truth로 사용하지 않는다.
5. 최종 응답 전 PostgreSQL/API 기준 권한 검증을 반드시 고려한다.
6. LightRAG ingestion 시 `worklog_id`, `team_id`, `writer_id` 등 추적 가능한 metadata/source를 포함한다.
7. 검색 결과 비교를 위해 기존 fixture와 gold query set을 유지/확장한다.
8. runtime automatic fallback, 자동 hybrid merge, 요청별 동적 engine switching은 기본 전략으로 두지 않는다.
9. 기존 pgvector 코드 삭제는 LightRAG 품질/운영 검증 이후로 미룬다.

## 후속 작업 후보

- `ai/app/light/v2` endpoint skeleton 설계
- `/ai/light/worklogs-v2` request/response contract 정의
- v2 router/model/service/test 파일 경계 정의
- LightRAG ingestion source format 정의
- LightRAG 결과에서 `worklog_id` 추출 규칙 정의
- PostgreSQL/API 기준 최종 권한 검증 단계 명시
- 검색 품질 비교 fixture 확장
- LightRAG Server compose 구성 PoC
- v3 추가 시 `ai/app/light/v3` 독립성 검증

## 결정 상태

- 결정일: 2026-05-12
- 갱신일: 2026-05-13
- 상태: 채택
- 선택: 기존 pgvector 검색 endpoint 유지 + LightRAG versioned endpoint 병행
- 이유: 안정성, endpoint 단위 호출 경계, version별 rollback 용이성, 품질 비교 가능성, 권한 검증 보존, 점진 전환 가능성
