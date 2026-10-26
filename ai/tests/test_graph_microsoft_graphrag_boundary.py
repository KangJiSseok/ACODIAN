from __future__ import annotations

import sys
from pathlib import Path

from app.graph.v1.service.microsoft_graphrag_boundary import (
    MICROSOFT_GRAPHRAG_DEFERRED_VERSION,
    MICROSOFT_GRAPHRAG_PACKAGE,
    get_microsoft_graphrag_boundary_decision,
)


def test_microsoft_graphrag_boundary_defers_runtime_import() -> None:
    had_runtime_import = MICROSOFT_GRAPHRAG_PACKAGE in sys.modules

    decision = get_microsoft_graphrag_boundary_decision()

    assert decision.package_name == "graphrag"
    assert decision.deferred_version == "3.1.0"
    assert decision.runtime_import_required is False
    assert decision.local_byog_only is True
    assert (MICROSOFT_GRAPHRAG_PACKAGE in sys.modules) is had_runtime_import


def test_microsoft_graphrag_dependency_is_not_pinned_until_direct_import_is_used() -> None:
    requirements = Path("requirements.txt").read_text(encoding="utf-8")

    assert f"{MICROSOFT_GRAPHRAG_PACKAGE}==" not in requirements
    assert MICROSOFT_GRAPHRAG_DEFERRED_VERSION == "3.1.0"
