---
title: "LightRAG v3 코드 읽기 순서"
tags: ["ai", "lightrag", "v3", "worklog", "code-flow", "python-beginner"]
created: 2026-05-15T15:38:34.530Z
updated: 2026-05-15T15:38:34.530Z
sources: []
links: []
category: reference
confidence: medium
schemaVersion: 1
---

# LightRAG v3 코드 읽기 순서


## 1. 전체 코드 흐름 한 장 요약

```text
POST /ai/light/worklogs-v3/index
        |
        v
ai/app/light/v3/router/worklog_index.py
index_worklogs()
        |
        v
ai/app/light/v3/service/worklog_index_service.py
LightWorklogIndexService.index_worklogs()
        |
        v
prepare_documents()
        |
        +--> worklog_source_reader.py
        |    DB에서 업무일지 source 조회
        |
        +--> worklog_document_builder.py
        |    LightRAG용 document 생성
        |
        v
lightrag_adapter.py
LightRagWorklogIndexAdapter.index_documents()
        |
        v
LightRAG.ainsert()
        |
        v
LightRAG 내부에서 KG/vector storage 생성
```

## 2. 파일별 역할 요약

| 파일 | 역할 |
|---|---|
| `ai/app/main.py` | FastAPI 앱을 만든다 |
| `ai/app/router/__init__.py` | 전체 router를 모은다 |
| `ai/app/light/v3/model/worklog_index.py` | 요청/응답 JSON 모양을 정의한다 |
| `ai/app/light/v3/router/worklog_index.py` | HTTP 요청을 받는 입구다 |
| `ai/app/light/v3/service/worklog_index_service.py` | 전체 index 순서를 지휘한다 |
| `ai/app/light/v3/service/worklog_source_reader.py` | DB에서 업무일지 정보를 가져온다 |
| `ai/app/light/v3/service/worklog_document_builder.py` | 업무일지를 LightRAG 문서로 바꾼다 |
| `ai/app/light/v3/service/lightrag_adapter.py` | 실제 LightRAG를 호출한다 |
| `ai/app/config/settings.py` | LightRAG/Gemini 설정값을 관리한다 |
| `ai/.env.example` | 필요한 환경변수 예시를 보여준다 |
| `.gitignore` | LightRAG 로컬 산출물이 Git에 올라가지 않게 한다 |
