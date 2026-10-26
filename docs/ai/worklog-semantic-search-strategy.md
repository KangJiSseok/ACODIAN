# Worklog Semantic Search Strategy

## 목적

이 문서는 `tb_worklog_embedding`에 저장된 업무일지 vector를 어떻게 검색하는지 기록한다.
현재 검색 경로는 GraphRAG v1 업무일지 query API가 권한 범위를 먼저 적용한 corpus를 만든 뒤
Local/BYOG 문맥을 구성하는 방식으로 확장되었다.

검색 결과 품질은 이후 chunking/embedding 방식 변경 시 이 문서를 기준으로 비교한다.

## 현재 범위

현재 검색 범위는 다음을 다룬다.

- jevos O/X 두 질문으로 관계·유사도 경로를 관찰하고, 불확실하면 두 경로를 실행
- 유사도 경로는 검색어를 Gemini embedding vector로 변환하고 `tb_worklog_embedding`에서 pgvector 후보를 넉넉히 검색
- 허용된 후보 ID를 Neo4j에서 최대 2홉 확장하고 관계 질문은 정확한 제목 식별 후 고정 Cypher로 선행 업무를 조회
- API 서버가 계산한 명시적 GraphRAG scope 계약 적용
- scope 적용 후 허용된 업무일지, 선행 업무, 작성자/팀/태그 메타데이터만 graph context 로 변환
- provenance 가 검증된 node/edge/reference/text unit 만 응답에 포함
- 수동 평가용 질의 fixture와 CLI 추가

아직 다루지 않는 범위:

- shared all-team GraphRAG Global/DRIFT/community report
- 완료일이 마감일보다 늦은 업무 같은 계산형 조건 검색

## 검색 흐름

기존 pgvector 검색 구현:

- `app/router/search.py`
- `app/service/search_service.py`
- `app/store/embedding_store.py`

GraphRAG v1 query 구현:

- `app/graph/v1/router/worklog_query.py`
- `app/graph/v1/service/worklog_query_service.py`
- `app/graph/v1/store/graph_authorized_corpus_store.py`
- `app/graph/v1/store/graph_rag_store.py`
- `app/graph/v1/service/jev_router.py`
- `app/graph/v1/service/graph_scope_context_builder.py`
- `app/graph/v1/service/graph_answer_adapter.py`

흐름:

1. API 서버가 public semantic request 의 `query` 와 서버에서 계산한 내부 scope 를 `/ai/graph/worklogs-v1/query` 로 보낸다.
2. `WorklogGraphQueryRequest`가 `scopeMode=TEAM_IDS|ALL` 계약을 검증한다.
3. jevos가 관계/유사도 필요 여부를 한 요청의 O/X 두 질문으로 판단한다. `shadow`에서는 예측만 기록하고 두 경로를 실행한다.
4. 유사도 경로는 PostgreSQL 권한 필터를 적용한 pgvector 후보를 최종 reference 수보다 넓게 찾는다. 관계 경로는 정확한 제목의 업무일지가 하나인지 확인한다.
5. Neo4j는 후보 ID의 관계와 연결 업무 ID만 반환한다. 원장의 최신 팀 범위·삭제 상태·관계를 재확인한 뒤 `GraphScopeContextBuilder`가 text unit, node, edge, reference를 만든다.
6. context guard 가 `sourceWorklogIds`와 `sourceTeamIds` provenance 를 확인한 뒤 LLM prompt 로 넘긴다.
7. `GraphAnswerAdapter`가 Local/BYOG context 기반으로만 답변을 생성하고, Global/DRIFT/community report 모드는 사용하지 않는다.

## 구조적 필터

시맨틱 검색은 의미 순위를 만들고, 아래 값은 최신 원본 테이블 기준으로 필터링한다.

- `teamId`
- `teamStatus`
- `statusCode`
- `importanceCode`
- `authorId`
- `tagId`
- `createdFrom`
- `scopeMode`
- `allowedTeamIds` (`scopeMode=TEAM_IDS`일 때만)

`scopeMode`와 `allowedTeamIds`는 public API DTO에는 없고, API 서버가 권한 계산 후 AI 서버에 넘겨주는 내부 필드다.
`scopeMode=TEAM_IDS`는 비어 있지 않은 `allowedTeamIds`가 필요하다.
`scopeMode=ALL`만 전체 범위를 뜻하며, 이때 `allowedTeamIds`는 없어야 한다.
`allowedTeamIds=null`만으로 전체 범위를 표현하지 않는다.
접근 가능한 팀이 없으면 API 서버가 AI를 호출하지 않고 빈 semantic response를 반환한다.

상태/중요도는 임베딩 텍스트에 넣지 않고 검색 시점에 `tb_worklog`에서 읽는다.
값이 자주 바뀌어도 재임베딩 없이 최신 상태로 필터링하기 위해서다.

## 응답 형태

GraphRAG v1 query 응답은 API 서버가 public semantic response 로 변환할 수 있는 answer/reference 와 provenance 정보를 포함한다.
API 서버는 반환된 `worklog://<id>` reference 를 같은 `WorklogVisibilityScope`로 재조회해 보이는 ID만 public response 로 내보낸다.

```json
{
  "answer": "결산 보고서 작성 업무가 있습니다.",
  "references": [
    {
      "referenceId": "worklog-7",
      "filePath": "worklog://7",
      "sourceWorklogIds": [7],
      "sourceTeamIds": [3]
    }
  ],
  "mode": "graph",
  "internalOnly": true
}
```

## 평가 방법

수동 평가 CLI:

```bash
cd /c/ssafy/S14P31S209/ai

./.venv/Scripts/python.exe -m app.util.semantic_search_eval \
  --confirm-external-query-embedding \
  --fixture
```

단일 질의:

```bash
./.venv/Scripts/python.exe -m app.util.semantic_search_eval \
  --confirm-external-query-embedding \
  --query "야간 배치가 겹치면서 운영이 흔들린 사례"
```

`--confirm-external-query-embedding`는 검색 질의문을 외부 Gemini API로 보내는 것을 승인하는 플래그다.
업무일지 본문은 이 평가 CLI에서 다시 전송하지 않는다.

## 현재 평가 세트

자동 fixture:

- `ai/tests/fixtures/semantic_search_queries.yaml`
- `ai/tests/fixtures/semantic_search_query_set.md`
- `ai/app/graph/v1/fixtures/graph_routing_questions.json` (`python -m app.graph.v1.evaluate_router`)
- `docs/ai/graph-routing-local-evaluation.md` (로컬 jevos-v5 점검 결과와 shadow 유지 근거)

우선 점검 질의는 다음 관점을 포함한다.

- exact keyword 검색
- paraphrase 검색
- 과거 유사 업무 검색
- 팀장/사업부장 역할 문맥
- 상태/일정 문맥
- 선행 TF / 후속 TF 연결

## 알려진 제약

- 현재 더미 데이터는 대부분 `1 worklog = 1 chunk`라서 chunk 간 경쟁 효과는 아직 작다.
- `tagId` 필터는 구현되어 있지만 현재 `tb_worklog_tag` 더미 데이터가 없어 실제 hit 검증이 어렵다.
- `teamStatus=ACTIVE`는 현재 DB 상태에 따라 결과가 없을 수 있다.
- `마감일을 넘겨서 완료된 업무` 같은 계산형 조건은 현재 검색 DTO만으로 직접 표현하지 못한다.

## 후속 비교 후보

- 방식 B: 선행 업무 제목 없이 검색 품질 비교
- 방식 C: 후속 업무 제목도 임베딩 문맥에 포함
- 방식 D: 역할 문맥 제거 후 role-based query 영향 비교
- 방식 E: 상태성 문맥을 vector가 아니라 별도 reranking hint로 반영
