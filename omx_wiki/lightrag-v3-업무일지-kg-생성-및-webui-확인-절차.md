---
title: "LightRAG v3 업무일지 KG 생성 및 WebUI 확인 절차"
tags: ["lightrag", "kg", "webui", "ai", "worklog", "smoke-test"]
created: 2026-05-13T15:22:05.517Z
updated: 2026-05-13T15:22:05.517Z
sources: []
links: []
category: session-log
confidence: medium
schemaVersion: 1
---

# LightRAG v3 업무일지 KG 생성 및 WebUI 확인 절차

# LightRAG v3 업무일지 KG 생성 및 WebUI 확인 절차

## 목적

`tb_worklog`의 실제 업무일지 데이터를 LightRAG v3 index endpoint로 색인해 Knowledge Graph(KG)가 생성되는지 확인한 뒤, LightRAG 자체 WebUI 서버로 생성된 KG를 시각적으로 확인한 절차를 기록한다.

## 1. WebUI를 띄우기 전에 호출한 실제 endpoint

### FastAPI 서버

- 실행 서버: `uvicorn app.main:app`
- 호출 대상 base URL: `http://127.0.0.1:8016`
- 기능: AX-WMS AI 서버의 LightRAG v3 업무일지 index endpoint

### 호출 endpoint

```http
POST http://127.0.0.1:8016/ai/light/worklogs-v3/index
Content-Type: application/json
```

### 요청 body

```json
{"worklogIds":[1]}
```

- `worklogIds`에는 실제 DB의 `tb_worklog` ID를 전달했다.
- smoke 대상은 `worklogId=1`이었다.
- 당시 DB에는 `tb_worklog` active 데이터가 686건 있었다.

### 성공 응답

```json
{"items":[{"worklogId":1,"indexed":true,"error":null}]}
```

### 이 endpoint가 만든 LightRAG 산출물

성공한 working directory:

```text
ai/data/lightrag-v3-smoke-20260513T144618Z
```

주요 생성 파일:

```text
graph_chunk_entity_relation.graphml
kv_store_doc_status.json
kv_store_entity_chunks.json
kv_store_full_docs.json
kv_store_full_entities.json
kv_store_full_relations.json
kv_store_llm_response_cache.json
kv_store_relation_chunks.json
kv_store_text_chunks.json
vdb_chunks.json
vdb_entities.json
vdb_relationships.json
```

검증된 KG 결과:

- `graph_chunk_entity_relation.graphml`: 25 nodes, 25 edges
- `vdb_entities.json`: `embedding_dim=768`, `data_len=25`
- `vdb_relationships.json`: `embedding_dim=768`, `data_len=25`
- `vdb_chunks.json`: `embedding_dim=768`, `data_len=1`
- `kv_store_doc_status.json`: `worklog-1`, `processed`, `chunks_count=1`

당시 로그 핵심:

```text
Chunk 1 of 1 extracted 25 Ent + 25 Rel
Writing graph with 25 nodes, 25 edges
```

## 2. WebUI 서버를 띄우기 위해 준비한 것

### LightRAG WebUI/API extra 설치

처음 `lightrag-server.exe --help` 실행 시 `ModuleNotFoundError: No module named 'jwt'`가 발생했다. 원인은 `lightrag-hku` 기본 패키지만 설치되어 있고 WebUI/API extra 의존성(`PyJWT` 등)이 빠져 있었기 때문이다.

해결 명령:

```powershell
.\ai\.venv\Scripts\python.exe -m pip install "lightrag-hku[api]==1.4.10"
```

설치 후 확인된 핵심 패키지:

- `lightrag-hku==1.4.10`
- `PyJWT==2.12.1`
- `fastapi==0.136.0`
- `uvicorn==0.44.0`
- `nano-vectordb==0.0.4.3`

## 3. LightRAG 자체 WebUI 서버 기동 방법

### 사용한 KG working directory

```powershell
$work = "C:\Users\SSAFY\Desktop\wt\feat-ai-lightrag-v3\ai\data\lightrag-v3-smoke-20260513T144618Z"
```

### 기동 시 사용한 핵심 환경변수

> 보안상 실제 `GEMINI_API_KEY` 값은 문서화하지 않는다. 서버 기동 스크립트는 `ai/.env`에서 값을 읽어 `LLM_BINDING_API_KEY`, `EMBEDDING_BINDING_API_KEY`에 주입했다.

```powershell
$env:PYTHONIOENCODING = 'utf-8'
$env:PYTHONUTF8 = '1'
$env:LLM_BINDING = 'gemini'
$env:EMBEDDING_BINDING = 'gemini'
$env:LLM_MODEL = 'gemini-2.5-flash'
$env:EMBEDDING_MODEL = 'gemini-embedding-001'
$env:EMBEDDING_DIM = '768'
$env:TOKENIZERS_PARALLELISM = 'false'
```

`PYTHONIOENCODING=utf-8`과 `PYTHONUTF8=1`은 Windows PowerShell의 `cp949` 인코딩에서 LightRAG 배너 출력이 `UnicodeEncodeError`로 죽는 문제를 피하기 위해 필요했다.

### 실제 서버 실행 명령

```powershell
.\.venv\Scripts\lightrag-server.exe `
  --host 127.0.0.1 `
  --port 9621 `
  --working-dir ".\data\lightrag-v3" `
  --llm-binding gemini `
  --embedding-binding gemini
```

실제 작업에서는 위 명령을 `Start-Process`로 hidden background PowerShell 프로세스에서 실행했다.

- WebUI URL: `http://127.0.0.1:9621/webui`
- API docs: `http://127.0.0.1:9621/docs`
- 실행 PID: `31992`
- 종료 명령:

```powershell
Stop-Process -Id 31992
```

## 4. WebUI 기동 후 확인한 endpoint

### Health 확인

```http
GET http://127.0.0.1:9621/health
```

확인 결과:

- `status`: `healthy`
- `webui_available`: `true`
- `working_directory`: `.../ai/data/lightrag-v3-smoke-20260513T144618Z`
- `graph_storage`: `NetworkXStorage`
- `vector_storage`: `NanoVectorDBStorage`
- `embedding_model`: `gemini-embedding-001`

### KG label 확인

```http
GET http://127.0.0.1:9621/graph/label/popular?limit=10
```

대표 응답 label:

```text
유정민
P0 회귀 현상 재현 요청
P0 회귀 이슈
삼성전자 DX 플랫폼 운영 TF
확인 포인트
재현 조건 정리
체감되는 불편
다음 사람
출발점
WMS
```

### 특정 label 중심 graph 확인

```http
GET http://127.0.0.1:9621/graphs?label=유정민&max_depth=2&max_nodes=50
```

확인 결과:

- `nodes`: 21
- `edges`: 22
- sample node: `유정민`
- sample node property의 `file_path`: `worklog://1`

## 5. WebUI 화면에서 보는 방법

1. 브라우저에서 `http://127.0.0.1:9621/webui` 접속
2. 상단 탭에서 `Knowledge Graph` 클릭
3. 왼쪽 label selector가 `*`이면 전체 KG가 보인다.
4. 화면에서 `유정민`, `P0 회귀 현상 재현 요청`, `P0 회귀 이슈`, `삼성전자 DX 플랫폼 운영 TF` 등의 node와 edge가 표시된다.

확인 스크린샷:

```text
C:\Users\SSAFY\Downloads\lightrag-webui-kg-visible-2026-05-13T15-15-46-187Z.png
```

## 6. 저장소 상태와 주의사항

- LightRAG WebUI는 기존 `ai/data/lightrag-v3-smoke-20260513T144618Z` 산출물을 읽어서 띄웠다.
- `PGVECTOR_DSN`은 업무일지 원본을 읽을 때 쓰였고, LightRAG vector storage는 `NanoVectorDBStorage`였다.
- WebUI 서버는 인증 없이 `127.0.0.1`에만 바인딩했다.
- API key, DSN 등 민감정보는 로그/문서에 남기지 않았다.
- 작업 후 `git status --short`는 clean이었다.
