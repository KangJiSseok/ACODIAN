"""LightRAG v3 업무일지 index 요청/응답 모델."""

from pydantic import BaseModel, ConfigDict, Field, PositiveInt, field_validator

DEFAULT_LIGHTRAG_INDEX_MAX_BATCH_SIZE = 10


class WorklogLightIndexRequest(BaseModel):
    """업무일지 LightRAG index 요청.

    Phase 1에서는 HTTP 계약을 고정한다. 실제 source 조회와 LightRAG insert는
    후속 phase에서 service/adapter 구현으로 연결한다.
    """

    model_config = ConfigDict(extra="forbid", validate_by_alias=True, validate_by_name=False)

    worklog_ids: list[PositiveInt] = Field(
        alias="worklogIds",
        min_length=1,
        max_length=DEFAULT_LIGHTRAG_INDEX_MAX_BATCH_SIZE,
    )

    @field_validator("worklog_ids")
    @classmethod
    def reject_duplicate_worklog_ids(
        cls,
        worklog_ids: list[PositiveInt],
    ) -> list[PositiveInt]:
        """중복 업무일지 ID를 422 validation error로 거절한다."""
        if len(set(worklog_ids)) != len(worklog_ids):
            raise ValueError("worklogIds must not contain duplicates")
        return worklog_ids


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
