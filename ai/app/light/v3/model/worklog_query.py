"""LightRAG v3 업무일지 query 요청/응답 모델."""

from __future__ import annotations

from typing import Literal

from pydantic import BaseModel, ConfigDict, Field


class WorklogLightQueryRequest(BaseModel):
    """내부 검증용 LightRAG 업무일지 query 요청."""

    model_config = ConfigDict(extra="forbid")

    query: str = Field(min_length=1)


class WorklogLightReferenceItem(BaseModel):
    """LightRAG reference 1건을 업무일지 기준으로 정규화한 응답 item."""

    model_config = ConfigDict(populate_by_name=True)

    reference_id: str | None = Field(default=None, alias="referenceId")
    file_path: str | None = Field(default=None, alias="filePath")


class WorklogLightQueryResponse(BaseModel):
    """내부 검증용 LightRAG query 응답."""

    model_config = ConfigDict(populate_by_name=True)

    answer: str
    references: list[WorklogLightReferenceItem]
    mode: Literal["mix"] = "mix"
    internal_only: bool = Field(default=True, alias="internalOnly")
