"""LightRAG custom KG 업무일지 index 요청/응답 모델."""

from pydantic import BaseModel, ConfigDict, Field, PositiveInt

from app.light.v3.model.worklog_index import DEFAULT_LIGHTRAG_INDEX_MAX_BATCH_SIZE


class WorklogCustomKgIndexRequest(BaseModel):
    """POST /ai/light/worklogs-v3/custom-kg/index 요청."""

    model_config = ConfigDict(extra="forbid", validate_by_alias=True, validate_by_name=False)

    worklog_ids: list[PositiveInt] = Field(
        alias="worklogIds",
        min_length=1,
        max_length=DEFAULT_LIGHTRAG_INDEX_MAX_BATCH_SIZE,
    )


class WorklogCustomKgIndexItem(BaseModel):
    """업무일지 1건의 LightRAG custom KG index 결과."""

    model_config = ConfigDict(populate_by_name=True)

    worklog_id: int = Field(alias="worklogId")
    indexed: bool
    error: str | None = None


class WorklogCustomKgIndexResponse(BaseModel):
    """업무일지 LightRAG custom KG index 응답."""

    model_config = ConfigDict(populate_by_name=True)

    items: list[WorklogCustomKgIndexItem]
