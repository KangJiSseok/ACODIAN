from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession

from app.model.tag_merge_model import (
    GenerateTagMergeCandidatesRequest,
    GenerateTagMergeCandidatesResponse,
)
from app.service.tag_merge_service import TagMergeService
from app.store.session import get_session

router = APIRouter(prefix="/tags", tags=["tags"])
tag_merge_service = TagMergeService()


@router.post("/merge-candidates", response_model=GenerateTagMergeCandidatesResponse)
async def generate_tag_merge_candidates(
    request: GenerateTagMergeCandidatesRequest,
    session: AsyncSession = Depends(get_session),
) -> GenerateTagMergeCandidatesResponse:
    return await tag_merge_service.generate_candidates(session, request)
