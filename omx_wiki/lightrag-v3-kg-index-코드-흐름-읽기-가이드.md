---
title: "LightRAG v3 KG index 코드 흐름 읽기 가이드"
tags: ["lightrag", "v3", "kg", "index", "worklog", "code-flow", "python-beginner"]
created: 2026-05-14T01:14:27.131Z
updated: 2026-05-14T01:14:27.131Z
sources: []
links: ["lightrag-v3-업무일지-kg-index-및-id-only-retrieval-구현-계획.md", "lightrag-변경사항-요약-a2d4d9a-이후.md", "lightrag-v3-업무일지-kg-생성-및-webui-확인-절차.md"]
category: reference
confidence: medium
schemaVersion: 1
---

# LightRAG v3 KG index 코드 흐름 읽기 가이드

# LightRAG v3 KG index 코드 흐름 읽기 가이드

관련 문서:

- [[lightrag-v3-업무일지-kg-index-및-id-only-retrieval-구현-계획]]
- [[lightrag-변경사항-요약-a2d4d9a-이후]]
- [[lightrag-v3-업무일지-kg-생성-및-webui-확인-절차]]

## 목적

`ai/app/light` 아래의 LightRAG v3 업무일지 KG index 코드를 처음 읽는 사람, 특히 Python/FastAPI 초보자가 어디서부터 따라가야 하는지 정리한다.

이 경로의 핵심은 AX-WMS가 entity/relation을 직접 만들지 않고, DB에서 업무일지 원문과 문맥을 조회해 LightRAG가 ingest할 문서로 변환한 뒤 `LightRAG.ainsert(...)`에 전달하는 것이다. 실제 chunking, entity extraction, relation extraction, graph merge, vector/KG storage 생성은 LightRAG 내부에서 수행된다.

## 전체 흐름

```text
POST /ai/light/worklogs-v3/index
  -> 요청 body의 worklogIds 검증
  -> DB에서 업무일지 원문/작성자/팀/선행업무/태그 조회
  -> LightRAG용 document text 생성
  -> LightRAG instance lazy 초기화
  -> rag.initialize_storages()
  -> rag.ainsert(texts, ids, file_paths)
  -> LightRAG 내부에서 KG/vector storage 생성
  -> ID별 성공/실패 응답 반환
```

## 추천 읽기 순서

### 1. API가 어디에 붙는지 확인

- `ai/app/main.py`
  - `create_app()`에서 `app.include_router(api_router, prefix=settings.api_prefix)`를 호출한다.
  - 기본 prefix는 `/ai`다.
- `ai/app/router/__init__.py`
  - `app.light.v3.router.worklog_index`의 router를 import하고 `api_router.include_router(...)`로 등록한다.

최종 endpoint는 다음 조합이다.

```text
settings.api_prefix: /ai
router prefix:       /light/worklogs-v3
method path:         /index
최종 URL:            /ai/light/worklogs-v3/index
```

### 2. 요청/응답 모델 확인

- `ai/app/light/v3/model/worklog_index.py`

중요 객체:

- `WorklogLightIndexRequest`
  - JSON 필드명은 `worklogIds`다.
  - 1개 이상, 기본 최대 10개까지 허용한다.
  - `PositiveInt`라서 0 또는 음수 ID는 거절한다.
  - 중복 ID도 validator에서 거절한다.
- `WorklogLightIndexItem`
  - `worklogId`, `indexed`, `error`를 가진 ID별 결과다.
- `WorklogLightIndexResponse`
  - `items` 배열로 전체 결과를 반환한다.

예시:

```json
{
  "items": [
    {"worklogId": 101, "indexed": true, "error": null},
    {"worklogId": 102, "indexed": false, "error": "WORKLOG_NOT_FOUND"}
  ]
}
```

### 3. HTTP endpoint 함수 확인

- `ai/app/light/v3/router/worklog_index.py`

핵심 함수:

```python
@router.post("/index", response_model=WorklogLightIndexResponse)
async def index_worklogs(...):
```

역할:

1. 요청받은 `request.worklog_ids`를 `list[int]`로 변환한다.
2. `settings.lightrag_index_max_batch_size`를 초과하면 422를 반환한다.
3. 실제 처리는 `LightWorklogIndexService.index_worklogs(worklog_ids)`에 위임한다.
4. `LightRagConfigurationError`는 endpoint 레벨에서 500 `LIGHTRAG_CONFIGURATION_ERROR`로 변환한다.

초보자 관점에서 이 파일은 "HTTP 입구"다. 비즈니스 로직은 service 파일에 있다.

### 4. 전체 파이프라인 중심 확인

- `ai/app/light/v3/service/worklog_index_service.py`

가장 먼저 볼 함수:

```python
async def index_worklogs(self, worklog_ids: list[int]) -> WorklogLightIndexResponse:
```

내부 순서:

```text
index_worklogs()
  -> prepare_documents()
      -> DB source 조회
      -> LightRAG document 생성
      -> found/missing ID 분리
  -> _index_found_documents()
      -> 존재하는 업무일지만 adapter에 전달
      -> LightRAG timeout/failure를 ID별 오류로 변환
  -> _build_response_item()
      -> 요청받은 ID 순서대로 응답 조립
```

중요한 실패 코드:

- `WORKLOG_NOT_FOUND`: DB에서 해당 업무일지를 찾지 못함
- `LIGHTRAG_INSERT_TIMEOUT`: LightRAG insert가 timeout 됨
- `LIGHTRAG_INSERT_FAILED`: LightRAG insert 또는 storage 초기화가 일반 실패함

### 5. DB source 조회 확인

- `ai/app/light/v3/service/worklog_source_reader.py`
- 연결되는 기존 store: `ai/app/store/embedding_store.py`

`WorklogLightSourceReader.fetch_sources(worklog_ids)`는 각 업무일지 ID를 순회하면서 `EmbeddingStore.fetch_worklog_source(session, worklog_id)`를 호출한다.

`EmbeddingStore.fetch_worklog_source()`가 가져오는 주요 정보:

- 업무일지 ID, 제목, 요청 내용, 업무 내용
- 작성자 ID, 이름, 역할
- 팀 ID, 팀명, 부서 ID
- 선행 업무일지 ID/제목
- 태그 ID

그 후 `to_light_worklog_source(...)`로 기존 `WorklogEmbeddingSource`를 LightRAG 전용 `LightRagWorklogSource`로 변환한다.

이 변환 계층을 둔 이유는 LightRAG index 코드가 기존 embedding DTO에 직접 강하게 묶이지 않도록 하기 위함이다.

### 6. LightRAG 문서 생성 확인

- `ai/app/light/v3/service/worklog_document_builder.py`

핵심 함수:

```python
def build_worklog_light_document(source: LightRagWorklogSource) -> LightRagWorklogDocument:
```

`worklog_id=101`이면 다음 값이 만들어진다.

```text
document_id = worklog-101
file_path   = worklog://101
text 안 marker = worklog_id: 101
```

`text`에는 다음 정보가 들어간다.

```text
source_type: WORKLOG
worklog_id: ...
title: ...
author_id: ...
author_name: ...
author_role: ...
team_id: ...
team_name: ...
department_id: ...
predecessor_worklog_ids: ...
predecessor_titles: ...
tag_ids: ...

request_content:
...

work_content:
...
```

같은 업무일지 ID를 `document_id`, `file_path`, 본문 marker에 반복 보존하는 이유는 후속 search 단계에서 LightRAG 결과로부터 `worklogId`만 안정적으로 회수하기 위해서다.

### 7. LightRAG 실제 호출 확인

- `ai/app/light/v3/service/lightrag_adapter.py`

가장 먼저 볼 함수:

```python
async def index_documents(self, documents: list[LightRagWorklogDocument]) -> None:
```

핵심 호출:

```python
rag = await self._get_initialized_rag()
await asyncio.wait_for(
    rag.ainsert(
        [document.text for document in documents],
        ids=[document.document_id for document in documents],
        file_paths=[document.file_path for document in documents],
    ),
    timeout=self._settings.lightrag_insert_timeout_seconds,
)
```

이 지점 이후부터는 LightRAG 내부 동작이다.

초기화 순서:

```text
index_documents()
  -> _get_initialized_rag()
      -> _build_lightrag()
          -> _ensure_required_config()
          -> _load_lightrag_dependencies()
          -> _build_gemini_embedding_func()
          -> _build_gemini_llm_model_func()
      -> rag.initialize_storages()
  -> rag.ainsert(...)
```

`GEMINI_API_KEY`가 없으면 `LightRagConfigurationError`가 발생한다. router는 이를 500 `LIGHTRAG_CONFIGURATION_ERROR`로 변환한다.

## 설정값

- `ai/app/config/settings.py`

관련 설정:

```python
lightrag_working_dir = "./data/lightrag-v3"
lightrag_llm_model = "gemini-2.5-flash"
lightrag_embedding_model = "gemini-embedding-001"
lightrag_embedding_max_token_size = 2048
lightrag_index_max_batch_size = 10
lightrag_insert_timeout_seconds = 120
```

LightRAG 결과 파일은 `lightrag_working_dir` 아래에 생성된다. smoke 기록 기준 주요 산출물은 다음과 같다.

```text
graph_chunk_entity_relation.graphml
kv_store_doc_status.json
kv_store_full_docs.json
kv_store_full_entities.json
kv_store_full_relations.json
vdb_chunks.json
vdb_entities.json
vdb_relationships.json
```

## 테스트로 흐름 확인하기

초보자가 흐름을 확인하기 좋은 테스트 파일:

- `ai/tests/test_light_worklogs_v3_router.py`
  - 실제 endpoint 요청/응답 계약 확인
- `ai/tests/test_light_worklog_index_service.py`
  - fake source reader와 fake adapter로 service 흐름 확인
- `ai/tests/test_light_worklog_document_builder.py`
  - 업무일지 ID가 document 세 위치에 보존되는지 확인
- `ai/tests/test_lightrag_v3_adapter.py`
  - fake LightRAG로 `ainsert(texts, ids, file_paths)` 호출 형태 확인

특히 `test_index_worklogs_indexes_found_documents_and_preserves_response_order`와 `test_lightrag_adapter_initializes_inserts_ids_and_finalizes`를 보면 전체 동작을 가장 빠르게 이해할 수 있다.

## 현재 구현 범위

현재 코드 기준 구현된 것은 index 경로다.

```text
구현됨:
POST /ai/light/worklogs-v3/index

아직 아님:
POST /ai/light/worklogs-v3/search
기존 /ai/search/worklogs 대체
Spring API/web 호출부 통합
권한 필터링과 ID-only retrieval 완성
재색인/삭제 반영
```

## 읽을 때 기억할 핵심

1. `router`는 HTTP 입구다.
2. `model`은 요청/응답 JSON 계약이다.
3. `service`는 전체 순서를 조립하는 지휘자다.
4. `source_reader`는 DB source를 가져오는 계층이다.
5. `document_builder`는 DB source를 LightRAG 입력 text로 바꾼다.
6. `lightrag_adapter`는 실제 `LightRAG.ainsert()` 호출 경계다.
7. 실제 KG 생성은 AX-WMS 코드가 직접 하는 것이 아니라 LightRAG 내부에서 수행된다.
