from app.client.gemini_client import GeminiClient, get_gemini_client
from app.model.embedding import WorklogEmbeddingResult
from app.service.chunking_service import ChunkingService
from app.store.embedding_store import (
    EmbeddingStore,
    WorklogEmbeddingChunk,
)
from app.store.session import get_session_factory


class EmbeddingService:
    """업무일지 임베딩 생성 use case.

    Service는 "원본 조회 → chunk 생성 → Gemini 임베딩 → DB 저장" 흐름만 조립한다.
    SQL 세부사항은 `EmbeddingStore`, 텍스트 분할 규칙은 `ChunkingService`가 담당한다.
    """

    def __init__(
        self,
        *,
        chunking_service: ChunkingService | None = None,
        embedding_store: EmbeddingStore | None = None,
        gemini_client: GeminiClient | None = None,
    ) -> None:
        self.chunking_service = chunking_service or ChunkingService()
        self.embedding_store = embedding_store or EmbeddingStore()
        self.gemini_client = gemini_client or get_gemini_client()

    async def embed_worklog(self, worklog_id: int) -> WorklogEmbeddingResult:
        """단일 업무일지를 임베딩하고 기존 chunk를 새 결과로 교체한다."""
        factory = get_session_factory()
        async with factory() as session:
            source = await self.embedding_store.fetch_worklog_source(session, worklog_id)
            if source is None:
                raise ValueError(f"worklog not found or deleted: {worklog_id}")

            chunk_contents = self.chunking_service.create_worklog_chunks(source)
            # Gemini는 입력 순서대로 vector를 반환하므로, index를 보존해 chunk와 다시 묶는다.
            vectors = await self.gemini_client.embed(chunk_contents)
            chunks = [
                WorklogEmbeddingChunk(
                    chunk_index=index,
                    chunk_content=chunk_content,
                    embedding=vectors[index],
                )
                for index, chunk_content in enumerate(chunk_contents)
            ]
            await self.embedding_store.replace_worklog_embeddings(session, source, chunks)
            await session.commit()

        return WorklogEmbeddingResult(worklogId=worklog_id, chunkCount=len(chunks))
