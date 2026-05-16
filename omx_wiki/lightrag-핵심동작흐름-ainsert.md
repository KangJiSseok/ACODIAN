---
title: "LightRAG 핵심동작흐름-ainsert"
tags: ["ai", "lightrag", "v3", "ainsert", "embedding", "kg", "worklog"]
created: 2026-05-16T10:39:19.108Z
updated: 2026-05-16T10:40:00.000Z
sources: []
links: []
category: reference
confidence: medium
schemaVersion: 1
---

# LightRAG 핵심동작흐름 ainsert 흐름

## 핵심 답

`rag.ainsert(...)`는 단순히 문서를 저장하는 함수가 아니다. LightRAG에서 문서를 받아 검색 가능한 형태로 만드는 전체 ingest pipeline의 진입점이다.

우리 코드에서는 `ainsert()`에 문서 본문, 문서 ID, file path만 넘기는 것처럼 보이지만, 그 전에 LightRAG 인스턴스를 만들 때 이미 두 가지 핵심 함수가 주입되어 있다.

- `llm_model_func`: KG 생성을 위해 entity/relation을 뽑을 때 호출할 LLM 함수
- `embedding_func`: chunk, entity, relation을 vector DB에 넣을 때 사용할 embedding 함수

그래서 `ainsert()` 내부에서 chunking, embedding, entity/relation 추출, graph merge, vector 저장, storage persist가 이어서 실행된다.

## 우리 코드에서 LightRAG에 넘기는 것

진입점은 `ai/app/light/v3/service/lightrag_adapter.py`의 `LightRagWorklogIndexAdapter.index_documents()`다.

```python
rag = await self._get_initialized_rag()
await rag.ainsert(
    [document.text for document in documents],
    ids=[document.document_id for document in documents],
    file_paths=[document.file_path for document in documents],
)
```

여기서 넘기는 값은 다음과 같다.

| 값 | 예시 | 역할 |
|---|---|---|
| `document.text` | `source_type: WORKLOG ...` | LightRAG가 읽고 chunking / KG 추출할 원문 |
| `document.document_id` | `worklog-101` | LightRAG 내부 문서 ID |
| `document.file_path` | `worklog://101` | 나중에 출처와 업무일지 ID를 회수하기 위한 경로 |

문서 객체는 `ai/app/light/v3/service/worklog_document_builder.py`에서 만들어진다. `worklog_id`, `title`, `author_name`, `team_name`, `predecessors`, `tags`, `request_content`, `work_content`가 하나의 텍스트로 합쳐진다.

## LightRAG 인스턴스가 만들어질 때 이미 설정되는 것

`LightRagWorklogIndexAdapter._build_lightrag()`에서 LightRAG를 생성한다.

```python
LightRAG(
    working_dir=settings.lightrag_working_dir,
    llm_model_func=llm_model_func,
    llm_model_name=settings.lightrag_llm_model,
    embedding_func=embedding_func,
    addon_params={"language": settings.lightrag_kg_language},
)
```

현재 기본 설정은 `ai/app/config/settings.py`에 있다.

| 설정 | 기본값 | 의미 |
|---|---|---|
| `lightrag_working_dir` | `./data/lightrag-v3` | LightRAG storage 저장 위치 |
| `lightrag_llm_model` | `gemini-2.5-flash` | entity/relation 추출에 사용할 LLM |
| `lightrag_embedding_model` | `gemini-embedding-001` | vector embedding 모델 |
| `lightrag_embedding_max_token_size` | `2048` | embedding 입력 token 제한 |
| `lightrag_kg_language` | `Korean` | KG 추출 prompt 언어 지시 |

`_get_initialized_rag()`는 LightRAG 인스턴스를 lazy singleton으로 만들고 `initialize_storages()`를 한 번 호출한다. 이때 LightRAG의 여러 storage가 준비된다.

## ainsert 내부 큰 흐름

LightRAG 패키지 기준 흐름은 다음과 같다.

```text
LightRAG.ainsert()
  ├─ apipeline_enqueue_documents()
  │   ├─ 입력 정규화
  │   ├─ ids / file_paths 검증
  │   ├─ 문서 내용 sanitize
  │   ├─ 중복 문서 필터링
  │   ├─ full_docs에 원문 저장
  │   └─ doc_status에 PENDING 기록
  │
  └─ apipeline_process_enqueue_documents()
      ├─ PENDING 문서 조회
      ├─ full_docs에서 원문 조회
      ├─ chunking_func로 chunk 분리
      ├─ chunks_vdb.upsert(chunks)
      ├─ text_chunks.upsert(chunks)
      ├─ extract_entities(chunks)
      ├─ merge_nodes_and_edges(...)
      ├─ doc_status를 PROCESSED로 변경
      └─ _insert_done()으로 storage persist
```

즉 `ainsert()`는 문서를 queue에 넣고, 그 queue를 바로 처리한다.

## embedding은 어디서 실행되는가

`ainsert()` 안에서 직접 `embedding_func(...)`가 보이지 않을 수 있다. 실제 호출은 vector storage의 `upsert()` 안에서 일어난다.

예를 들어 chunk를 저장할 때 LightRAG는 다음 작업을 한다.

```text
chunks_vdb.upsert(chunks)
  └─ NanoVectorDBStorage.upsert()
      ├─ chunk content 목록 생성
      ├─ embedding_func(batch) 호출
      ├─ embedding 결과를 vector로 붙임
      └─ vector DB에 upsert
```

entity와 relation도 비슷하다. `merge_nodes_and_edges()`에서 entity vector DB와 relationship vector DB에 upsert하면, 그 storage가 `embedding_func`를 호출한다.

정리하면 embedding은 세 군데에서 만들어질 수 있다.

| 대상 | 저장소 | embedding 대상 content |
|---|---|---|
| 문서 chunk | `chunks_vdb` | chunk 본문 |
| entity | `entities_vdb` | entity 이름 + 설명 |
| relation | `relationships_vdb` | relation keyword + 양 끝 entity + 설명 |

## KG는 어디서 생성되는가

KG 생성은 `apipeline_process_enqueue_documents()` 안의 `_process_extract_entities(chunks)`에서 시작된다.

이 함수는 LightRAG의 `extract_entities(...)`를 호출한다. `extract_entities(...)`는 각 chunk에 대해 다음을 수행한다.

1. entity/relation 추출 prompt를 만든다.
2. `llm_model_func`를 호출한다.
3. LLM 응답을 entity record와 relation record로 파싱한다.
4. `maybe_nodes`, `maybe_edges`를 반환한다.

그 다음 `merge_nodes_and_edges(...)`가 실행된다. 이 함수는 추출된 entity와 relation을 실제 graph와 vector storage에 반영한다.

```text
extract_entities(chunks)
  ├─ LLM으로 entity/relation 추출
  └─ maybe_nodes, maybe_edges 반환

merge_nodes_and_edges(...)
  ├─ entity merge
  │   ├─ graph node upsert
  │   └─ entities_vdb upsert
  ├─ relation merge
  │   ├─ graph edge upsert
  │   └─ relationships_vdb upsert
  └─ full_entities / full_relations 갱신
```

## storage별 역할

LightRAG가 초기화되면 여러 storage가 준비된다. `ainsert()`는 이 storage들을 함께 갱신한다.

| storage | 역할 |
|---|---|
| `full_docs` | 원문 문서 저장 |
| `doc_status` | 문서 처리 상태 저장: `PENDING`, `PROCESSING`, `PROCESSED`, `FAILED` |
| `text_chunks` | chunk 텍스트와 chunk metadata 저장 |
| `chunks_vdb` | chunk embedding vector 저장 |
| `chunk_entity_relation_graph` | entity node와 relation edge를 담는 graph storage |
| `entities_vdb` | entity 검색용 vector 저장 |
| `relationships_vdb` | relation 검색용 vector 저장 |
| `full_entities` | 문서별 entity 목록 저장 |
| `full_relations` | 문서별 relation 목록 저장 |
| `entity_chunks` | entity가 어떤 chunk에서 나왔는지 추적 |
| `relation_chunks` | relation이 어떤 chunk에서 나왔는지 추적 |
| `llm_response_cache` | LLM 추출/요약 응답 cache |

## `ids`와 `file_paths`를 넘기는 이유

`ids`에는 `worklog-101` 같은 안정적인 문서 ID를 넣는다. LightRAG가 자체 hash ID를 만들게 두지 않고, 우리 업무일지 ID와 연결된 문서 ID를 유지하기 위해서다.

`file_paths`에는 `worklog://101` 같은 값을 넣는다. LightRAG의 chunk, entity, relation metadata에 file path가 따라다니기 때문에, 검색 결과나 KG 결과에서 원래 업무일지 ID를 다시 찾기 쉬워진다.

## 헷갈리기 쉬운 지점

### `ainsert()` 한 줄만 있으니 embedding을 안 하는 것처럼 보인다

embedding은 adapter 코드에 직접 드러나지 않는다. LightRAG vector storage의 `upsert()`가 내부에서 `embedding_func`를 호출한다.

### KG 추출 코드가 adapter에 없으니 KG가 안 만들어지는 것처럼 보인다

KG 추출은 LightRAG 내부의 `extract_entities()`와 `merge_nodes_and_edges()`에서 수행된다. adapter는 LightRAG에 LLM 함수와 입력 문서를 넘기는 경계 역할만 한다.

### `initialize_storages()`는 KG를 만드는 함수가 아니다

`initialize_storages()`는 저장소를 준비하는 단계다. 실제 문서 처리, embedding, KG 생성은 `ainsert()` 이후 pipeline에서 일어난다.

### 자동 테스트에서 실제 KG 생성이 보이지 않는다

`ai/tests/test_lightrag_v3_adapter.py`는 fake LightRAG를 사용한다. 그래서 외부 Gemini API나 실제 LightRAG storage에 의존하지 않고 adapter가 올바르게 `ainsert()`를 호출하는지만 검증한다. 실제 KG 생성 확인은 `docs/ai/lightrag-v3-worklogs-index-manual-smoke.md`의 opt-in smoke 절차로 확인한다.

## 코드 확인 순서

1. `ai/app/light/v3/service/worklog_index_service.py`
   - 업무일지 ID 요청이 document 목록으로 바뀌는 흐름을 본다.
2. `ai/app/light/v3/service/worklog_document_builder.py`
   - LightRAG에 들어가는 문서 본문이 어떻게 생기는지 본다.
3. `ai/app/light/v3/service/lightrag_adapter.py`
   - LightRAG 인스턴스 생성, Gemini 함수 주입, `ainsert()` 호출을 본다.
4. `ai/.venv/lib/python3.12/site-packages/lightrag/lightrag.py`
   - `ainsert()`, `apipeline_enqueue_documents()`, `apipeline_process_enqueue_documents()`를 본다.
5. `ai/.venv/lib/python3.12/site-packages/lightrag/operate.py`
   - `extract_entities()`, `merge_nodes_and_edges()`를 본다.
6. `ai/.venv/lib/python3.12/site-packages/lightrag/kg/nano_vector_db_impl.py`
   - vector storage가 `embedding_func`를 호출하는 위치를 본다.

## 한 문장으로 기억하기

우리 코드는 LightRAG에 입력 문서와 Gemini 함수들을 연결하고, LightRAG의 `ainsert()`가 문서 저장부터 chunk embedding, KG 추출, graph/vector 저장까지 ingest pipeline을 실행한다.

