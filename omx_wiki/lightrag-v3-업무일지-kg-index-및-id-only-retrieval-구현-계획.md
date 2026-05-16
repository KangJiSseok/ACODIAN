---
title: "LightRAG v3 업무일지 KG index 및 ID-only retrieval 구현 계획"
tags: ["ai", "lightrag", "v3", "kg", "worklog", "id-retrieval", "authorization", "implementation-plan"]
created: 2026-05-13T06:18:30.716Z
updated: 2026-05-13T06:57:27.553Z
sources: [".omx/specs/deep-interview-lightrag-worklog-id-retrieval-v2.md", ".omx/specs/deep-interview-lightrag-v3-index-worklogid-only-doc-update.md", ".omx/specs/deep-interview-lightrag-v3-index-worklogids-list-doc-update.md"]
links: ["2026-05-12-065231-search-lightrag-전환-조사와-구성-선택지.md", "2026-05-12-073814-search-lightrag-전환-결정-기존-pgvector-유지와-병행-도입.md", "2026-05-12-080727-search-lightrag-병행-도입-구현-계획.md", "2026-05-13-031018-V2-lightrag-v2-scoped-kg-query-설계-인터뷰.md"]
category: architecture
confidence: medium
schemaVersion: 1
---

# LightRAG v3 업무일지 KG index 및 ID-only retrieval 구현 계획

관련 문서:

- [[2026-05-12-065231-search-lightrag-전환-조사와-구성-선택지]]
- [[2026-05-12-073814-search-lightrag-전환-결정-기존-pgvector-유지와-병행-도입]]
- [[2026-05-12-080727-search-lightrag-병행-도입-구현-계획]]
- [[2026-05-13-031018-V2-lightrag-v2-scoped-kg-query-설계-인터뷰]]

## 목표

AX-WMS `ai` 모듈에 LightRAG v3 경로를 추가한다. v3의 목적은 Spring/API 통합까지 한 번에 끝내는 것이 아니라, AI 서버 단독으로 다음 두 흐름을 검증하는 것이다.

1. 업무일지 신규 등록 후 전달된 `worklogIds` 목록을 받아, AI 내부에서 각 ID에 해당하는 DB의 업무일지 내용을 조회한 뒤 LightRAG 내장 기능으로 KG를 생성한다.
2. 사용자의 자연어 `query`를 받아 LightRAG 검색을 수행하고, 최종 LLM 답변이 아니라 관련 `worklogId` 후보만 반환한다.

핵심 결정은 다음과 같다.

```text
Indexing:
  POST /ai/light/worklogs-v3/index
    -> worklogIds 수신
    -> AI 내부에서 각 worklogId에 해당하는 DB의 업무일지 내용 조회
    -> LightRAG insert/ainsert
    -> LightRAG 내장 chunking
    -> LLM 기반 entity/relation extraction
    -> LightRAG graph merge / KG 생성
    -> worklogId를 회수 가능한 source/document mapping으로 보존

Search:
  POST /ai/light/worklogs-v3/search
    -> query 수신
    -> LightRAG KG/vector retrieval 사용
    -> 최종 사용자용 LLM answer/context/reference 생성하지 않음
    -> retrieval 결과에서 worklogId 후보 추출
    -> worklogIds 반환
```

## 현재 기준점

기존 안정 검색 endpoint는 계속 유지한다.

```text
POST /ai/search/worklogs
```

현재 v1 흐름은 다음과 같다.

```text
api
  -> /ai/search/worklogs
  -> ai/app/router/search.py
  -> ai/app/service/search_service.py
  -> GeminiClient.embed
  -> ai/app/store/embedding_store.py
  -> WorklogEmbedding.embedding
  -> SemanticWorklogSearchResponse
```

v3는 이 경로를 대체하거나 리팩토링하지 않는다. LightRAG 작업은 `ai/app/light/v3` 아래에 격리한다.

## v2와 v3의 차이

이전 V2 문서에서는 권한 hard filter를 LightRAG query 과정 안에 강하게 적용하는 scoped KG query 방향을 다뤘다.

```text
V2 방향:
  allowedWorklogIds
    -> allowed chunks/entities/relations scope
    -> scoped KG query
    -> 권한 없는 KG 정보가 query context/ranking에 들어가지 않게 차단
```

v3는 의도적으로 더 단순한 실험 경로다.

```text
V3 방향:
  LightRAG 전체 KG에서 후보 worklogIds 검색
    -> AI는 worklogIds만 반환
    -> 후속 Spring 통합에서 권한 있는 worklog만 재조회/노출
```

따라서 v3에서는 LightRAG ranking 과정에 권한 없는 업무일지의 KG 정보가 간접적으로 영향을 줄 수 있다. 이번 v3의 성공 기준은 **최종 사용자 응답에서 권한 없는 worklog가 제거되는 후속 Spring 권한 필터 전제를 유지하면서, AI 서버가 후보 ID를 잘 반환하는 것**이다.

## 새 구조 결정

### 1. URL은 `/ai/light/worklogs-v3/...`로 분리한다

```text
기존 v1 compatibility endpoint:
  POST /ai/search/worklogs

이전/다른 LightRAG version 후보:
  POST /ai/light/worklogs-v2

이번 v3 endpoint:
  POST /ai/light/worklogs-v3/index
  POST /ai/light/worklogs-v3/search
```

v3 검색은 기존 `/ai/search/worklogs`나 v2 경로를 변경하지 않는다.

### 2. 코드는 `ai/app/light/v3`에 둔다

```text
ai/app/light/
  __init__.py
  v3/
    __init__.py
    router.py
    model.py
    service.py
    lightrag_adapter.py
    source_mapper.py
    phase/
```

원칙:

- `ai/app/light/v3`는 `ai/app/light/v2`를 import하지 않는다.
- 기존 `app/router/search.py`, `app/service/search_service.py`, `app/model/search.py`는 v3 구현을 위해 변경하지 않는다.
- 공통화보다 version별 rollback 용이성을 우선한다.

## Index endpoint 계약

### Request

```http
POST /ai/light/worklogs-v3/index
```

```json
{
  "worklogIds": [101, 102]
}
```

### 처리 원칙

Index 요청은 `worklogIds` 목록만 받는다. AI 내부 로직은 각 `worklogId`로 DB에서 업무일지 내용을 조회한 뒤 LightRAG에 전달한다. 이때 DB에서 어떤 필드와 문맥을 조합할지는 실제 구현 시 별도 deep-interview에서 결정한다. 중복 ID는 호출자가 주지 않는다는 전제로 두며, 이번 문서에서는 중복 처리 정책을 확정하지 않는다. 업무일지 원문에서 entity/edge/relation을 AX-WMS가 직접 만들지 않는다. KG 생성은 LightRAG의 내장 ingestion pipeline에 맡긴다.

```text
worklogIds
  -> 각 worklogId별 DB 업무일지 내용 조회
  -> LightRAG insert/ainsert
  -> LightRAG chunking
  -> LightRAG LLM entity extraction
  -> LightRAG LLM relationship extraction
  -> LightRAG graph merge
  -> LightRAG storage/index update
```

### Minimum Response

```json
{
  "items": [
    { "worklogId": 101, "indexed": true },
    { "worklogId": 102, "indexed": false, "error": "WORKLOG_NOT_FOUND" }
  ]
}
```

실패 시에는 ID별로 DB 조회 실패, LightRAG 호출 실패, LLM 설정 오류, source mapping 실패를 구분할 수 있어야 한다.

## Search endpoint 계약

### Request

```http
POST /ai/light/worklogs-v3/search
```

```json
{
  "query": "인증 오류 원인 분석"
}
```

v3 검색 요청은 `query`만 받는다.

명시적으로 제거하는 필드:

- `teamId`
- `statusCode`
- `importanceCode`
- `authorId`
- `tagId`
- `createdFrom`
- `page`
- `pageSize`
- `allowedTeamIds`

### Minimum Response

```json
{
  "items": [
    { "worklogId": 101 },
    { "worklogId": 87 }
  ]
}
```

LightRAG 결과에서 안정적으로 얻을 수 있을 때만 score를 선택적으로 포함한다.

```json
{
  "items": [
    { "worklogId": 101, "score": 0.91 },
    { "worklogId": 87, "score": 0.84 }
  ]
}
```

## 권한 처리 원칙

v3 AI endpoint는 권한 source of truth가 아니다. 이번 작업은 AI 서버의 ID-only retrieval까지다.

후속 Spring 통합 흐름은 다음을 전제로 한다.

```text
Spring 사용자 검색 요청
  -> AI /ai/light/worklogs-v3/search 호출
  -> AI가 candidate worklogIds 반환
  -> Spring이 WorklogVisibilityScope 기준으로 DB 재조회
  -> 권한 없는 worklogId 제거
  -> 권한 있는 worklog만 최종 응답
```

Spring 권한 필터 후 결과가 줄어들어도 v3에서는 다시 AI에 요청하지 않는다.

예:

```text
AI 반환: [1, 2, 3, 4, 5]
Spring 권한 필터 후: [2, 5]
최종 응답: [2, 5]
```

이 경우도 성공이다. backfill/over-fetch loop는 v3 범위가 아니다.

## 명시적 non-goals

v3에서 제외하는 항목:

- AX-WMS가 직접 entity/edge/relation/KG schema를 생성하는 것
- LightRAG document에 넣을 구체 DB 컬럼/문맥을 이번 문서에서 확정하는 것
- 중복 `worklogIds` 처리 정책을 이번 문서에서 확정하는 것
- 최종 사용자용 LLM answer 생성
- 최종 사용자용 context/reference 반환
- 검색 요청에 query 외 필터를 받는 것
- Spring API가 v3 endpoint를 실제 호출하도록 연결하는 것
- Spring DTO/client/service 수정
- Spring 권한 필터 구현/수정
- query-time KG hard filter
- scoped storage proxy
- allowedWorklogIds 기반 retrieval 제한
- 권한 필터 후 결과 부족 시 backfill/over-fetch
- 업무일지 수정/삭제 시 LightRAG 재색인/삭제 반영
- pgvector 도입
- Neo4j 도입
- 사용자별 LightRAG workspace/storage 분리
- 기존 `/ai/search/worklogs` 리팩토링

## 구현 단계 후보

### Phase 1. v3 폴더와 endpoint skeleton

- `ai/app/light/v3` 생성
- v3 request/response model 정의 (`index` request는 `worklogIds`만 포함하고, response는 ID별 결과를 포함)
- v3 router 등록
- `/ai/light/worklogs-v3/index`
- `/ai/light/worklogs-v3/search`

### Phase 2. LightRAG adapter 경계

- LightRAG 초기화/설정 경계 정의
- index 시 각 `worklogId`로 DB에서 해당 업무일지 내용을 조회한 뒤 `insert`/`ainsert` 호출
- 테스트용 fake adapter 제공
- LLM 설정 누락 시 명확한 오류 반환

### Phase 3. worklogId source mapping

- LightRAG document/source에 `worklogId`를 안정적으로 심는 방식 결정
- 후보 결과에서 `worklogId` 역추출
- 검색 결과 후보의 중복 ID 제거 및 순서 보존

### Phase 4. 검색 결과 ID-only 반환

- `query`만 받는 search endpoint 구현
- LightRAG 검색 결과에서 worklogIds 추출
- LLM answer/context/reference는 응답에서 제외

### Phase 5. 테스트

- index endpoint가 request에서 `worklogIds`만 받고, 각 ID별 내부 DB 조회 후 LightRAG adapter 경계로 전달하는지 검증
- index endpoint가 ID별 `indexed` 결과와 오류 정보를 반환하는지 검증
- AX-WMS가 직접 edge/relation을 만들지 않는 경계를 검증
- search endpoint가 `query`만 받아 worklogIds를 반환하는지 검증
- 기존 `/ai/search/worklogs` 테스트가 깨지지 않는지 검증
- 수정/삭제 재색인이 v3 범위 밖임을 테스트/문서로 고정

## 수용 기준

1. `POST /ai/light/worklogs-v3/index`가 request에서 `worklogIds` 목록만 받는다.
2. AI 내부 로직은 각 `worklogId`로 DB에서 해당 업무일지 내용을 조회한 뒤 LightRAG ingestion 경계로 전달한다.
3. index 응답은 ID별 `indexed` 결과와 오류 정보를 반환한다.
4. KG 생성은 LightRAG 내장 기능에 맡긴다.
5. AX-WMS v3 코드가 entity/edge/relation을 직접 생성하지 않는다.
6. `worklogId`가 LightRAG document/source mapping에 보존된다.
7. `POST /ai/light/worklogs-v3/search`는 `query`만 받는다.
8. search 응답은 `worklogId` 후보 목록을 반환한다.
9. search 응답은 최종 LLM answer/context/reference를 포함하지 않는다.
10. Spring 연결과 권한 필터는 이번 브랜치에서 구현하지 않는다.
11. 권한 필터 후 결과 부족분을 채우는 재검색/backfill은 구현하지 않는다.
12. 기존 v1 `/ai/search/worklogs` contract와 테스트는 유지된다.

## 결정 상태

- 결정일: 2026-05-13
- 상태: 채택 후보 / 구현 계획
- 선택: LightRAG v3 ID-only retrieval + 신규 업무일지 index endpoint
- 핵심: KG는 LightRAG가 만들고, AI는 `worklogIds`만 반환하며, Spring 권한 필터 통합은 후속으로 둔다.


