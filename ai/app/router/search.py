from fastapi import APIRouter

from app.model.search_model import SearchRequest, SearchResponse
from app.service.search_service import SearchService

router = APIRouter(prefix="/search", tags=["search"])
search_service = SearchService()


@router.post("/semantic", response_model=SearchResponse)
def semantic_search(payload: SearchRequest) -> SearchResponse:
    return search_service.search(payload)
