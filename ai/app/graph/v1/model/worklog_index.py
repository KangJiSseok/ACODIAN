"""GraphRAG v1 업무일지 index 요청/응답 모델."""

from pydantic import BaseModel, ConfigDict, Field, PositiveInt

DEFAULT_GRAPHRAG_INDEX_MAX_BATCH_SIZE = 10


class WorklogGraphIndexRequest(BaseModel):
    """POST /ai/graph/worklogs-v1/index 요청."""

    model_config = ConfigDict(extra="forbid", validate_by_alias=True, validate_by_name=False)

    worklog_ids: list[PositiveInt] = Field(
        alias="worklogIds",
        min_length=1,
        max_length=DEFAULT_GRAPHRAG_INDEX_MAX_BATCH_SIZE,
    )


class WorklogGraphIndexItem(BaseModel):
    """업무일지 1건의 GraphRAG index 결과."""

    model_config = ConfigDict(populate_by_name=True)

    worklog_id: int = Field(alias="worklogId")
    indexed: bool
    error: str | None = None


class WorklogGraphIndexResponse(BaseModel):
    """업무일지 GraphRAG index 응답."""

    model_config = ConfigDict(populate_by_name=True)

    items: list[WorklogGraphIndexItem]
