"""GraphRAG v1 업무일지 query 요청/응답 모델."""

from __future__ import annotations

from enum import StrEnum
from typing import Any, Literal
from typing import assert_never

from pydantic import BaseModel, ConfigDict, Field, PositiveInt, model_validator
from pydantic_core import PydanticCustomError


class WorklogGraphScopeMode(StrEnum):
    TEAM_IDS = "TEAM_IDS"
    ALL = "ALL"


class WorklogGraphQueryRequest(BaseModel):
    """GraphRAG 업무일지 subgraph query 요청."""

    model_config = ConfigDict(extra="forbid", validate_by_alias=True, validate_by_name=False)

    query: str = Field(min_length=1)
    scope_mode: WorklogGraphScopeMode = Field(alias="scopeMode")
    allowed_team_ids: list[PositiveInt] | None = Field(default=None, alias="allowedTeamIds")
    max_depth: PositiveInt | None = Field(default=None, alias="maxDepth")
    limit: PositiveInt | None = None

    @model_validator(mode="after")
    def validate_scope_contract(self) -> WorklogGraphQueryRequest:
        """scopeMode와 allowedTeamIds 조합의 보안 계약을 요청 경계에서 고정한다."""
        match self.scope_mode:
            case WorklogGraphScopeMode.TEAM_IDS:
                if not self.allowed_team_ids:
                    raise PydanticCustomError(
                        "graphrag_scope_missing_team_ids",
                        "TEAM_IDS scope requires non-empty allowedTeamIds",
                    )
            case WorklogGraphScopeMode.ALL:
                if self.allowed_team_ids is not None:
                    raise PydanticCustomError(
                        "graphrag_scope_all_forbids_team_ids",
                        "ALL scope forbids allowedTeamIds",
                    )
            case unreachable:
                assert_never(unreachable)
        return self


class WorklogGraphNodeItem(BaseModel):
    """GraphRAG query 응답 node."""

    model_config = ConfigDict(populate_by_name=True)

    node_id: str = Field(alias="nodeId")
    label: str
    source_worklog_ids: list[PositiveInt] = Field(min_length=1, alias="sourceWorklogIds")
    source_team_ids: list[PositiveInt] = Field(min_length=1, alias="sourceTeamIds")
    properties: dict[str, Any] = Field(default_factory=dict)


class WorklogGraphEdgeItem(BaseModel):
    """GraphRAG query 응답 edge."""

    model_config = ConfigDict(populate_by_name=True)

    edge_id: str = Field(alias="edgeId")
    source_id: str = Field(alias="sourceId")
    target_id: str = Field(alias="targetId")
    type: str
    source_worklog_ids: list[PositiveInt] = Field(min_length=1, alias="sourceWorklogIds")
    source_team_ids: list[PositiveInt] = Field(min_length=1, alias="sourceTeamIds")
    properties: dict[str, Any] = Field(default_factory=dict)


class WorklogGraphReferenceItem(BaseModel):
    """GraphRAG query reference item."""

    model_config = ConfigDict(populate_by_name=True)

    reference_id: str = Field(alias="referenceId")
    file_path: str = Field(alias="filePath")
    source_worklog_ids: list[PositiveInt] = Field(min_length=1, alias="sourceWorklogIds")
    source_team_ids: list[PositiveInt] = Field(min_length=1, alias="sourceTeamIds")


class WorklogGraphQueryResponse(BaseModel):
    """GraphRAG query 응답."""

    model_config = ConfigDict(populate_by_name=True)

    answer: str | None = None
    nodes: list[WorklogGraphNodeItem]
    edges: list[WorklogGraphEdgeItem]
    references: list[WorklogGraphReferenceItem]
    mode: Literal["graph"] = "graph"
    internal_only: bool = Field(default=True, alias="internalOnly")
