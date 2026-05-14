"""SVG 원본 inspect — 실패 시 메타데이터만 반환."""

from __future__ import annotations

import logging
from dataclasses import dataclass
from pathlib import Path

logger = logging.getLogger(__name__)

MAX_CHARS = 30_000


@dataclass
class SvgInspection:
    text: str | None
    metadata: dict[str, str]


def inspect_svg(path: Path) -> SvgInspection:
    metadata = {
        "file_name": path.name,
        "size_bytes": str(path.stat().st_size),
    }
    try:
        raw = path.read_text(encoding="utf-8", errors="strict")
    except (UnicodeDecodeError, OSError) as exc:
        logger.warning("SVG 원본 읽기 실패 %s: %s", path.name, exc)
        return SvgInspection(text=None, metadata=metadata)

    stripped = raw.strip()
    if not stripped or "<svg" not in stripped.lower():
        logger.warning("SVG 내용 없음 %s", path.name)
        return SvgInspection(text=None, metadata=metadata)

    if len(stripped) > MAX_CHARS:
        stripped = stripped[:MAX_CHARS]
    return SvgInspection(text=stripped, metadata=metadata)
