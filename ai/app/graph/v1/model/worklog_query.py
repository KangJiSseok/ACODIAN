"""GraphRAG v1 업무일지 query 요청/응답 모델."""

from __future__ import annotations

from typing import Any, Literal

from pydantic import BaseModel, ConfigDict, Field, PositiveInt


class WorklogGraphQueryRequest(BaseModel):
    """GraphRAG 업무일지 subgraph query 요청."""

    model_config = ConfigDict(extra="forbid", validate_by_alias=True, validate_by_name=False)

    query: str = Field(min_length=1)
    allowed_team_ids: list[PositiveInt] | None = Field(default=None, alias="allowedTeamIds")
    max_depth: PositiveInt | None = Field(default=None, alias="maxDepth")
    limit: PositiveInt | None = None


class WorklogGraphNodeItem(BaseModel):
    """GraphRAG query 응답 node."""

    model_config = ConfigDict(populate_by_name=True)

    node_id: str = Field(alias="nodeId")
    label: str
    properties: dict[str, Any] = Field(default_factory=dict)


class WorklogGraphEdgeItem(BaseModel):
    """GraphRAG query 응답 edge."""

    model_config = ConfigDict(populate_by_name=True)

    edge_id: str = Field(alias="edgeId")
    source_id: str = Field(alias="sourceId")
    target_id: str = Field(alias="targetId")
    type: str
    properties: dict[str, Any] = Field(default_factory=dict)


class WorklogGraphReferenceItem(BaseModel):
    """GraphRAG query reference item."""

    model_config = ConfigDict(populate_by_name=True)

    reference_id: str = Field(alias="referenceId")
    file_path: str = Field(alias="filePath")


class WorklogGraphQueryResponse(BaseModel):
    """GraphRAG query 응답."""

    model_config = ConfigDict(populate_by_name=True)

    answer: str | None = None
    nodes: list[WorklogGraphNodeItem]
    edges: list[WorklogGraphEdgeItem]
    references: list[WorklogGraphReferenceItem]
    mode: Literal["graph"] = "graph"
    internal_only: bool = Field(default=True, alias="internalOnly")
