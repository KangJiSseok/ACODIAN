# Worklog Semantic Search Strategy

## 목적

이 문서는 `tb_worklog_embedding`에 저장된 업무일지 vector를 어떻게 검색하는지 기록한다.
현재 목표는 GraphRAG 전 단계인 **시맨틱 검색 API와 수동 평가 기반**을 만드는 것이다.

검색 결과 품질은 이후 chunking/embedding 방식 변경 시 이 문서를 기준으로 비교한다.

## 현재 범위

이번 검색 MR은 다음만 다룬다.

- 검색어를 Gemini embedding vector로 변환
- `tb_worklog_embedding`에서 pgvector cosine distance로 유사 chunk 검색
- worklog별 가장 가까운 chunk 1개만 결과로 반환
- API 서버 DTO에서 받을 수 있는 구조적 필터 적용
- 수동 평가용 질의 fixture와 CLI 추가

아직 다루지 않는 범위:

- API 서버의 기존 `SearchService`와 실제 end-to-end 연동
- GraphRAG / Neo4j 기반 관계 확장
- 완료일이 마감일보다 늦은 업무 같은 계산형 조건 검색
- 검색 결과 설명문 생성

## 검색 흐름

현재 구현:

- `app/router/search.py`
- `app/service/search_service.py`
- `app/store/embedding_store.py`

흐름:

1. API 서버가 `/ai/search/worklogs`로 검색 요청을 보낸다.
2. `SearchService`가 `keyword`만 Gemini로 보내 query embedding을 만든다.
3. `EmbeddingStore`가 `tb_worklog_embedding`과 최신 `tb_worklog`, `tb_team`을 join한다.
4. pgvector cosine distance로 chunk 유사도를 계산한다.
5. `row_number()`로 worklog별 가장 가까운 chunk만 남긴다.
6. 페이지 정보와 함께 ranked `worklogId`, `score`, `matchedChunk`, `predecessorWorklogIds`를 반환한다.

## 구조적 필터

시맨틱 검색은 의미 순위를 만들고, 아래 값은 최신 원본 테이블 기준으로 필터링한다.

- `teamId`
- `teamStatus`
- `statusCode`
- `importanceCode`
- `authorId`
- `tagId`
- `createdFrom`
- `allowedTeamIds`

`allowedTeamIds`는 public API DTO에는 없지만, API 서버가 권한 계산 후 AI 서버에 넘겨줄 내부 필드다.
빈 배열이 오면 접근 가능한 팀이 없다는 뜻이므로 결과를 0건으로 처리한다.

상태/중요도는 임베딩 텍스트에 넣지 않고 검색 시점에 `tb_worklog`에서 읽는다.
값이 자주 바뀌어도 재임베딩 없이 최신 상태로 필터링하기 위해서다.

## 응답 형태

AI 서버는 화면 표시용 상세 DTO를 만들지 않는다.
대신 API 서버가 기존 projection으로 다시 조회할 수 있도록 랭킹 근거만 반환한다.

```json
{
  "items": [
    {
      "worklogId": 7,
      "score": 0.91,
      "chunkIndex": 0,
      "matchedChunk": "...",
      "predecessorWorklogIds": [1]
    }
  ],
  "page": 1,
  "pageSize": 20,
  "totalCount": 1,
  "totalPages": 1,
  "isFirst": true,
  "isLast": true,
  "hasNext": false,
  "hasPrevious": false
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
- 방식 F: GraphRAG 단계에서 `tb_worklog_dependency` 관계를 확장 검색에 사용
