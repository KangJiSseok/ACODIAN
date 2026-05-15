---
title: "LightRAG 변경사항 요약: a2d4d9a 이후"
tags: ["ai", "lightrag", "v3", "worklog", "change-summary"]
created: 2026-05-14T00:10:02.612Z
updated: 2026-05-14T00:10:02.612Z
sources: []
links: []
category: session-log
confidence: medium
schemaVersion: 1
---

# LightRAG 변경사항 요약: a2d4d9a 이후

# LightRAG 변경사항 요약: a2d4d9a 이후

기준 범위: `a2d4d9aca1d738de31faefe93d8bac6c4cc08be7..087b25b`

이 범위의 핵심은 **기존 pgvector 검색은 그대로 두고, LightRAG v3 업무일지 색인 실험 경로를 새로 추가한 것**이다. 아직 사용자 검색 API를 LightRAG로 교체한 단계는 아니며, 먼저 업무일지 ID를 받아 LightRAG Knowledge Graph(KG)에 넣는 index 파이프라인을 만들었다.

## 한 줄 요약

`POST /ai/light/worklogs-v3/index` endpoint가 추가되어, `worklogIds`를 받으면 DB에서 업무일지 원문과 문맥을 조회하고 LightRAG `ainsert`로 KG/vector storage에 색인할 수 있게 되었다.

## 무엇이 새로 생겼나

### 1. LightRAG v3 전용 폴더

새 코드는 `ai/app/light/v3/**` 아래에 격리되었다.

```text
ai/app/light/v3/
  model/worklog_index.py
  router/worklog_index.py
  service/worklog_index_service.py
  service/worklog_source_reader.py
  service/worklog_document_builder.py
  service/lightrag_adapter.py
```

이 구조는 기존 `/ai/search/worklogs` pgvector 검색을 건드리지 않고, LightRAG 실험 경로만 독립적으로 되돌리거나 확장하기 위한 것이다.

### 2. 업무일지 index endpoint

새 endpoint:

```http
POST /ai/light/worklogs-v3/index
```

요청 예시:

```json
{ "worklogIds": [101, 102] }
```

응답 예시:

```json
{
  "items": [
    { "worklogId": 101, "indexed": true, "error": null },
    { "worklogId": 102, "indexed": false, "error": "WORKLOG_NOT_FOUND" }
  ]
}
```

현재 구현된 LightRAG v3 기능은 **index** 중심이다. `search` endpoint는 이 커밋 범위에서 아직 추가되지 않았다.

### 3. 입력 검증

`worklogIds`는 다음 조건을 검증한다.

- 비어 있으면 안 됨
- 양의 정수만 허용
- 중복 ID 거절
- 기본 최대 10건, 운영 설정 `LIGHTRAG_INDEX_MAX_BATCH_SIZE`로 제한
- 허용량 초과 시 422 반환

### 4. DB 업무일지 source 조회

`WorklogLightSourceReader`가 기존 `EmbeddingStore.fetch_worklog_source`를 이용해 업무일지 원문과 주변 문맥을 가져온다.

LightRAG 문서에 포함하는 주요 정보:

- `worklog_id`, `title`
- 작성자 ID/이름/역할
- 팀 ID/팀명/부서 ID
- 선행 업무일지 ID/제목
- 태그 ID
- 요청 내용, 업무 내용

즉 LightRAG가 단순 본문만 보는 것이 아니라, 업무일지 관계와 작성 맥락까지 보고 KG를 만들 수 있게 준비했다.

### 5. LightRAG 문서 변환 규칙

`worklogId`를 나중에 다시 회수할 수 있도록 같은 ID를 여러 위치에 심는다.

```text
document_id = worklog-101
file_path   = worklog://101
text 안 marker = worklog_id: 101
```

이 결정은 후속 검색 단계에서 LightRAG 결과를 최종 LLM 답변이 아니라 **업무일지 ID 후보**로 되돌리기 위한 기반이다.

### 6. LightRAG adapter

`LightRagWorklogIndexAdapter`가 실제 LightRAG runtime과 연결된다.

핵심 동작:

- `lightrag-hku==1.4.10` 의존성 추가
- LightRAG와 Gemini wrapper를 lazy import
- `GEMINI_API_KEY` 없으면 `LIGHTRAG_CONFIGURATION_ERROR`
- LightRAG instance를 lazy singleton으로 초기화
- `initialize_storages()` 후 `ainsert(texts, ids, file_paths)` 호출
- timeout은 `LIGHTRAG_INSERT_TIMEOUT_SECONDS`로 제한
- shutdown/finalize용 `close_lightrag_worklog_index_adapter()` 제공

### 7. 설정 추가

`ai/app/config/settings.py`와 `ai/.env.example`에 LightRAG v3 설정이 추가되었다.

```env
LIGHTRAG_WORKING_DIR=./data/lightrag-v3
LIGHTRAG_LLM_MODEL=gemini-2.5-flash
LIGHTRAG_EMBEDDING_MODEL=gemini-embedding-001
LIGHTRAG_EMBEDDING_MAX_TOKEN_SIZE=2048
LIGHTRAG_INDEX_MAX_BATCH_SIZE=10
LIGHTRAG_INSERT_TIMEOUT_SECONDS=120
```

마지막 수정 커밋에서 Gemini embedding 호출에 `embedding_dim=settings.embedding_dim`을 넘기도록 고쳐, 프로젝트의 768차원 설정과 LightRAG embedding wrapper 차원이 어긋나지 않게 했다.

### 8. LightRAG 산출물 git 제외

`.gitignore`에 `/ai/data/lightrag-v3*/`가 추가되어, 로컬 KG/vector/index 산출물이 저장소에 올라가지 않게 했다.

### 9. 수동 smoke 문서

`docs/ai/lightrag-v3-worklogs-index-manual-smoke.md`가 추가되었다.

이 문서는 실제 Gemini API, PostgreSQL, LightRAG storage를 사용하는 opt-in 수동 검증 절차를 설명한다. 자동 테스트는 fake adapter를 사용하므로 외부 API 비용이나 실제 DB에 의존하지 않는다.

## 커밋별 흐름

1. `f5562b2` — v3 index HTTP 계약과 router skeleton 추가
2. `7337a95` — 업무일지 source 조회와 LightRAG 문서 변환 계약 추가
3. `3519f00` — LightRAG adapter, 설정, 의존성 추가
4. `ded199b` — router/service를 실제 source reader + adapter 흐름에 연결
5. `e346099` — index service/router/adapter 테스트 보강 및 수동 smoke 문서 추가
6. `087b25b` — Gemini embedding 차원 전달 수정, LightRAG data 산출물 gitignore 추가

## 현재 가능한 것

- AI 서버에서 `/ai/light/worklogs-v3/index` 호출 가능
- 업무일지 ID 목록을 받아 존재하는 ID만 LightRAG에 색인
- 존재하지 않는 ID는 ID별 `WORKLOG_NOT_FOUND`로 응답
- LightRAG/Gemini 설정 누락은 500 `LIGHTRAG_CONFIGURATION_ERROR`로 노출
- insert timeout/failure는 ID별 오류로 변환
- 테스트에서는 fake dependency로 외부 API 없이 동작 검증 가능

## 아직 아닌 것

- 기존 `/ai/search/worklogs`를 LightRAG로 교체하지 않았다.
- LightRAG v3 `search` endpoint는 아직 구현되지 않았다.
- Spring API/web 호출부는 아직 LightRAG v3를 사용하지 않는다.
- 권한 필터링, 검색 결과 ID-only 반환, 재색인/삭제 반영은 후속 범위다.
- 실제 KG 생성은 수동 smoke 절차로만 검증한다.

## 검증 증거

실행한 테스트:

```text
.\ai\.venv\Scripts\python.exe -m pytest \
  ai/tests/test_light_worklog_document_builder.py \
  ai/tests/test_light_worklog_index_service.py \
  ai/tests/test_light_worklogs_v3_router.py \
  ai/tests/test_lightrag_v3_adapter.py
```

결과:

```text
26 passed, 1 warning in 6.73s
```

경고는 FastAPI의 `HTTP_422_UNPROCESSABLE_ENTITY` deprecation warning이며, 이번 LightRAG 동작 실패는 아니다.
