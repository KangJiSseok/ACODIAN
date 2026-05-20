---
title: "AI 로컬 실행: FastAPI와 LightRAG WebUI"
tags: ["ai", "fastapi", "lightrag", "webui", "local-dev"]
created: 2026-05-17T04:10:59.834Z
updated: 2026-05-17T04:34:00.000Z
sources: []
links: []
category: environment
confidence: medium
schemaVersion: 1
---

# AI 로컬 실행: FastAPI와 LightRAG WebUI


## FastAPI 서버

```bash
cd ai
.venv/bin/uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

- 확인: `http://localhost:8000/ai/health`
- 문서: `http://localhost:8000/ai/docs`
- 배포/컨테이너 기준 포트는 `8200`이다.

## LightRAG WebUI

```bash
cd ai
set -a; source .env; set +a
export QDRANT_URL="${LIGHTRAG_QDRANT_URL:-http://localhost:6333}"
export QDRANT_API_KEY="${LIGHTRAG_QDRANT_API_KEY:-}"
export LLM_MODEL="${LIGHTRAG_LLM_MODEL:-gemini-2.5-flash}"
export EMBEDDING_MODEL="${LIGHTRAG_EMBEDDING_MODEL:-gemini-embedding-001}"

.venv/bin/lightrag-server \
  --host 0.0.0.0 \
  --port 9621 \
  --working-dir "${LIGHTRAG_WORKING_DIR:-./data/lightrag-v3}" \
  --llm-binding gemini \
  --embedding-binding gemini
```

- 접속: `http://localhost:9621/webui`
