"""LightRAG v3 업무일지 index 요청/응답 모델."""

from pydantic import BaseModel, ConfigDict, Field, PositiveInt

DEFAULT_LIGHTRAG_INDEX_MAX_BATCH_SIZE = 10


"""
POST /ai/light/worklogs-v3/index 요청.
BaseModel : Dto 이면서 Valid 함께 들어있음.
"""
class WorklogLightIndexRequest(BaseModel):

    model_config = ConfigDict(extra="forbid", validate_by_alias=True, validate_by_name=False)

    worklog_ids: list[PositiveInt] = Field(
        alias="worklogIds",
        min_length=1,
        max_length=DEFAULT_LIGHTRAG_INDEX_MAX_BATCH_SIZE,
    )


class WorklogLightIndexItem(BaseModel):
    """업무일지 1건의 LightRAG index 결과."""

    model_config = ConfigDict(populate_by_name=True)

    worklog_id: int = Field(alias="worklogId")
    indexed: bool
    error: str | None = None


class WorklogLightIndexResponse(BaseModel):
    """업무일지 LightRAG index 응답."""

    model_config = ConfigDict(populate_by_name=True)

    items: list[WorklogLightIndexItem]
