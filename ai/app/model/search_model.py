from pydantic import BaseModel, Field


class SearchRequest(BaseModel):
    query: str = Field(..., min_length=1)
    limit: int = Field(5, ge=1, le=20)


class SearchResult(BaseModel):
    source_id: str
    score: float
    content: str


class SearchResponse(BaseModel):
    items: list[SearchResult]
