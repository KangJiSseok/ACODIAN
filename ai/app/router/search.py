"""업무일지 시맨틱 검색 라우터."""

from fastapi import APIRouter

from app.model.search import SemanticWorklogSearchRequest, SemanticWorklogSearchResponse
from app.service.search_service import SearchService

router = APIRouter(prefix="/search", tags=["search"])
search_service = SearchService()


@router.post("/worklogs", response_model=SemanticWorklogSearchResponse)
async def search_worklogs(
    request: SemanticWorklogSearchRequest,
) -> SemanticWorklogSearchResponse:
    """업무일지 시맨틱 검색을 수행한다.

    이 라우터는 내부 연동용이다. 최종 사용자 응답 조립은 API 서버가 맡고,
    AI 서버는 query embedding + pgvector 랭킹 결과만 반환한다.
    """
    return await search_service.search_worklogs(request)
