---
title: "LightRAG v3 Retrieval No relevant context 원인과 해결"
tags: ["lightrag", "retrieval", "webui", "debugging", "kg"]
created: 2026-05-14T04:41:08.123Z
updated: 2026-05-14T04:41:08.123Z
sources: []
links: []
category: debugging
confidence: medium
schemaVersion: 1
---

# LightRAG v3 Retrieval No relevant context 원인과 해결

## 상황

LightRAG v3 업무일지 KG를 `ai/data/lightrag-v3`에 재생성한 뒤 WebUI Retrieval에서 `No relevant context found for the query.`가 표시됐다.

## 결론

KG 파일 자체가 비어 있거나 깨진 문제가 아니었다. 재생성된 KG는 정상적으로 존재했고, 직접 `LightRAG.aquery(..., only_need_context=True)` 및 HTTP `/query`, `/query/data`, `/query/stream` 호출에서는 context와 references가 정상 반환됐다.

문제는 WebUI/Retrieval 서버 프로세스가 KG 생성 시 사용한 설정과 맞지 않는 상태로 떠 있었던 것이다.

## 확인 증거

- `ai/data/lightrag-v3/kv_store_doc_status.json` 기준 `worklog-1`, `worklog-2`, `worklog-4`, `worklog-7`, `worklog-8` 모두 `processed`였다.
- graph는 `73 nodes / 105 edges`로 로드됐다.
- 직접 query 검증:
  - `P0 회귀`
  - `배치 윈도우 충돌`
  - `유정민이 작성한 P0 회귀 업무`
  - `삼성전자 DX 플랫폼 운영 TF`
  모두 context가 반환됐다.
- `/query/stream` 재검증에서 `references`가 반환됐고 `No relevant context found`는 `false`였다.

## 실제 문제 지점

LightRAG WebUI 서버 health에서 처음에는 다음처럼 보였다.

- `working_directory`: `ai/data/lightrag-v3`
- `summary_language`: `English`

반면 KG 재생성 코드는 `addon_params={"language": "Korean"}`를 사용하도록 수정되어 있었다. WebUI 서버는 별도 `lightrag-server.exe` 프로세스라, 앱 설정 `LIGHTRAG_KG_LANGUAGE`만으로는 자동 동기화되지 않는다. WebUI 서버에는 `SUMMARY_LANGUAGE=Korean`도 별도로 주입해야 한다.

## 조치

WebUI 서버를 다음 조건으로 재기동했다.

- `--working-dir ./data/lightrag-v3`
- `LLM_BINDING=gemini`
- `EMBEDDING_BINDING=gemini`
- `LLM_MODEL=gemini-2.5-flash`
- `EMBEDDING_MODEL=gemini-embedding-001`
- `EMBEDDING_DIM=768`
- `SUMMARY_LANGUAGE=Korean`

그리고 로컬/env 예시에 다음을 맞췄다.

```env
LIGHTRAG_KG_LANGUAGE=Korean
SUMMARY_LANGUAGE=Korean
```

## 재발 방지 체크리스트

1. KG 재생성 후 `kv_store_doc_status.json`에서 대상 worklog가 모두 `processed`인지 확인한다.
2. `GET /health`에서 WebUI 서버의 `working_directory`가 실제 KG 디렉터리인지 확인한다.
3. `GET /health`에서 `summary_language`가 `Korean`인지 확인한다.
4. Retrieval 테스트는 `bypass`가 아니라 `mix`, `global`, `local`, `naive` 중 하나로 한다.
5. WebUI가 오래 떠 있었다면 브라우저 새로고침 및 서버 재기동을 먼저 의심한다.
6. `Rerank is enabled but no rerank model is configured` 경고는 이번 `No relevant context found`의 직접 원인은 아니었다.

## 관련 파일/설정

- `ai/app/light/v3/service/lightrag_adapter.py`
- `ai/app/config/settings.py`
- `ai/.env.example`
- `ai/.env` 로컬 설정
- `ai/data/lightrag-v3/`
