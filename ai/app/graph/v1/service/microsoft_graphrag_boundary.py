"""Microsoft GraphRAG package 직접 import를 한 파일 경계로 제한하는 결정."""

from __future__ import annotations

from dataclasses import dataclass
from typing import Final

MICROSOFT_GRAPHRAG_PACKAGE: Final = "graphrag"
MICROSOFT_GRAPHRAG_DEFERRED_VERSION: Final = "3.1.0"


@dataclass(frozen=True, slots=True)
class MicrosoftGraphRagBoundaryDecision:
    """Microsoft GraphRAG 직접 import 도입 전까지의 runtime boundary 계약."""

    package_name: str
    deferred_version: str
    runtime_import_required: bool
    local_byog_only: bool
    rationale: tuple[str, ...]


def get_microsoft_graphrag_boundary_decision() -> MicrosoftGraphRagBoundaryDecision:
    """현재 query path가 package import 없이 authorized BYOG context를 조립한다는 결정을 반환한다."""
    return MicrosoftGraphRagBoundaryDecision(
        package_name=MICROSOFT_GRAPHRAG_PACKAGE,
        deferred_version=MICROSOFT_GRAPHRAG_DEFERRED_VERSION,
        runtime_import_required=False,
        local_byog_only=True,
        rationale=(
            "GraphRAG v1 query path must build authorization-scoped context before any LLM prompt.",
            "Microsoft GraphRAG direct imports stay deferred until Local/BYOG API compatibility is pinned.",
            "When direct imports are introduced, graphrag==3.1.0 is the pinned package boundary.",
        ),
    )
