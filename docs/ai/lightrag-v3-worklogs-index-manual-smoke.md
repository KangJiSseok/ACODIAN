# LightRAG v3 업무일지 index manual smoke

## 목적

이 문서는 `POST /ai/light/worklogs-v3/index`가 실제 LightRAG adapter를 통해 KG 생성
파이프라인을 실행하는지 수동으로 확인하는 절차를 기록한다.

자동 테스트는 fake adapter/fake LightRAG를 사용하므로 외부 Gemini API, PostgreSQL,
LightRAG storage에 의존하지 않는다. 실제 KG 생성은 이 문서의 opt-in smoke test로만
확인한다.

## 사전 조건

- `ai/.env`에 `GEMINI_API_KEY`가 설정되어 있다.
- `ai/.env`에 `PGVECTOR_DSN`이 실제 업무일지 데이터가 있는 PostgreSQL을 가리킨다.
- `LIGHTRAG_WORKING_DIR`이 쓰기 가능한 디렉터리를 가리킨다.
- 요청에 사용할 업무일지 ID 2개가 DB에 존재하며 삭제 상태가 아니다.
- 외부 Gemini API 호출 비용과 데이터 전송을 수동으로 승인했다.

예시 환경변수:

```env
GEMINI_API_KEY=...
PGVECTOR_DSN=postgresql+asyncpg://...
LIGHTRAG_WORKING_DIR=./data/lightrag-v3
LIGHTRAG_INDEX_MAX_BATCH_SIZE=10
LIGHTRAG_INSERT_TIMEOUT_SECONDS=120
```

## 실행 절차

```bash
cd ai
python -m uvicorn app.main:app --reload
```

다른 터미널에서 존재하는 업무일지 ID로 요청한다.

```bash
curl -X POST http://localhost:8000/ai/light/worklogs-v3/index \
  -H "Content-Type: application/json" \
  -d '{"worklogIds":[101,102]}'
```

## 기대 결과

정상 성공 시 두 ID 모두 `indexed: true`로 반환된다.

```json
{
  "items": [
    {"worklogId": 101, "indexed": true, "error": null},
    {"worklogId": 102, "indexed": true, "error": null}
  ]
}
```

일부 ID가 존재하지 않으면 해당 ID만 실패한다.

```json
{
  "items": [
    {"worklogId": 101, "indexed": true, "error": null},
    {"worklogId": 999999, "indexed": false, "error": "WORKLOG_NOT_FOUND"}
  ]
}
```

## 실패 판정

- `500 LIGHTRAG_CONFIGURATION_ERROR`: `GEMINI_API_KEY`, LightRAG dependency import,
  model 설정, working dir 권한을 확인한다.
- `LIGHTRAG_INSERT_TIMEOUT`: batch size를 줄이거나
  `LIGHTRAG_INSERT_TIMEOUT_SECONDS`를 운영 timeout보다 낮은 안전 범위에서 조정한다.
- `LIGHTRAG_INSERT_FAILED`: 서버 로그의 LightRAG/Gemini/storage 예외를 확인한다.

## 기록할 증거

수동 smoke를 수행했다면 PR 또는 작업 로그에 아래를 남긴다.

- 실행 일시
- 사용한 `worklogIds`
- `LIGHTRAG_WORKING_DIR`
- HTTP 응답 JSON
- LightRAG storage 디렉터리 생성/갱신 여부
- 실패 시 서버 로그 요약

업무일지 원문은 외부 공유 로그에 남기지 않는다.
