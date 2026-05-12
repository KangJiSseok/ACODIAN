"""업무일지 시맨틱 검색 서비스."""

from app.client.gemini_client import GeminiClient, get_gemini_client
from app.model.search import (
    SemanticWorklogSearchItem,
    SemanticWorklogSearchRequest,
    SemanticWorklogSearchResponse,
)
from app.store.embedding_store import EmbeddingStore
from app.store.session import get_session_factory


class SearchService:
    """업무일지 시맨틱 검색 use case.

    query 문장을 Gemini로 임베딩한 뒤, 실제 유사도 검색과 필터링은 store 계층에 맡긴다.
    """

    def __init__(
        self,
        *,
        embedding_store: EmbeddingStore | None = None,
        gemini_client: GeminiClient | None = None,
    ) -> None:
        self.embedding_store = embedding_store or EmbeddingStore()
        self.gemini_client = gemini_client

    async def search_worklogs(
        self,
        request: SemanticWorklogSearchRequest,
    ) -> SemanticWorklogSearchResponse:
        """검색어를 vector로 바꾸고 pgvector 검색 결과를 응답 모델로 변환한다."""
        # 업무일지 본문은 이미 embedding table에 저장되어 있으므로, 검색 시에는 query만 임베딩한다.
        query_embedding = (await self._gemini_client().embed([request.keyword]))[0]
        factory = get_session_factory()
        async with factory() as session:
            page = await self.embedding_store.search_worklogs(
                session,
                request,
                query_embedding,
            )

        return SemanticWorklogSearchResponse(
            # store dataclass를 FastAPI 응답 모델로 변환해 HTTP 계약을 한 곳에서 고정한다.
            items=[
                SemanticWorklogSearchItem(
                    worklogId=item.worklog_id,
                    score=item.score,
                    chunkIndex=item.chunk_index,
                    matchedChunk=item.matched_chunk,
                    predecessorWorklogIds=item.predecessor_worklog_ids,
                )
                for item in page.items
            ],
            page=page.page,
            pageSize=page.page_size,
            totalCount=page.total_count,
            totalPages=page.total_pages,
            isFirst=page.is_first,
            isLast=page.is_last,
            hasNext=page.has_next,
            hasPrevious=page.has_previous,
        )

    def _gemini_client(self) -> GeminiClient:
        """앱 import 시점에 API key 검증이 터지지 않도록 GeminiClient를 지연 생성한다."""
        if self.gemini_client is None:
            self.gemini_client = get_gemini_client()
        return self.gemini_client
