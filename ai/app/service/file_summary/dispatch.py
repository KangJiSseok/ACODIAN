"""확장자 → 처리 분기. 콜백용 단일 ``aiSummary`` 문자열을 반환한다.

이미지(png/jpg) 의 OCR 텍스트는 콜백 DTO 가 한 컬럼(`aiSummary`) 이므로
``설명 + \\n[OCR]\\n + ocr_text`` 형태로 합쳐 한 문자열로 반환한다(옵션 A).
"""

from __future__ import annotations

from pathlib import Path
from typing import Literal

from app.client.gemini_client import GeminiClient
from app.service.file_summary.hwp_extractor import extract_hwp_content
from app.service.file_summary.office_extractor import extract_office_content
from app.service.file_summary.summarizer import (
    summarize_hwp,
    summarize_image,
    summarize_office,
    summarize_pdf,
    summarize_svg,
)
from app.service.file_summary.svg_extractor import inspect_svg

SourceType = Literal["pdf", "docx", "pptx", "xlsx", "hwp", "png", "jpg", "svg"]

EXT_TO_SOURCE: dict[str, SourceType] = {
    ".pdf": "pdf",
    ".docx": "docx",
    ".pptx": "pptx",
    ".xlsx": "xlsx",
    ".hwp": "hwp",
    ".png": "png",
    ".jpg": "jpg",
    ".jpeg": "jpg",
    ".svg": "svg",
}

OFFICE_TYPES: set[SourceType] = {"docx", "pptx", "xlsx"}


class UnsupportedFileExtension(ValueError):
    """``EXT_TO_SOURCE`` 가 모르는 확장자."""


def resolve_source_type(extension: str) -> SourceType:
    """확장자(dot 유무, 대소문자 무관) 를 정규화하여 SourceType 으로 변환한다."""

    normalized = extension.lower()
    if not normalized.startswith("."):
        normalized = f".{normalized}"
    source = EXT_TO_SOURCE.get(normalized)
    if source is None:
        raise UnsupportedFileExtension(f"지원하지 않는 파일 확장자: {extension}")
    return source


async def summarize_by_source(
    client: GeminiClient,
    source_type: SourceType,
    path: Path,
) -> str:
    """``source_type`` 에 맞춰 추출 → 요약을 수행하고 단일 요약 문자열을 반환한다."""

    if source_type == "pdf":
        return await summarize_pdf(client, path)
    if source_type in OFFICE_TYPES:
        content = extract_office_content(path)
        return await summarize_office(client, path, content)
    if source_type == "hwp":
        content = extract_hwp_content(path)
        return await summarize_hwp(client, path, content)
    if source_type in {"png", "jpg"}:
        description, ocr_text = await summarize_image(client, path)
        return _merge_image_outputs(description, ocr_text)
    if source_type == "svg":
        inspection = inspect_svg(path)
        return await summarize_svg(client, path, inspection)
    raise UnsupportedFileExtension(f"디스패치되지 않은 source_type: {source_type}")


def _merge_image_outputs(description: str, ocr_text: str) -> str:
    """이미지 설명 + OCR 텍스트를 한 컬럼(`aiSummary`) 으로 합쳐 보낸다.

    OCR 이 비어 있으면 설명만 반환해 ``[OCR]`` 빈 섹션이 붙지 않도록 한다.
    """

    description = (description or "").strip()
    ocr_text = (ocr_text or "").strip()
    if not ocr_text:
        return description
    if not description:
        return f"[OCR]\n{ocr_text}"
    return f"{description}\n\n[OCR]\n{ocr_text}"
