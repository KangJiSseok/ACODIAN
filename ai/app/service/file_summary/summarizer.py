"""파일 단위 Gemini 요약/OCR/이미지 설명 루틴.

각 함수는 호출 측이 이미 추출한 본문/이미지를 받아 LLM 호출만 수행한다.
모든 출력은 한국어로 강제된다.
"""

from __future__ import annotations

import logging
import mimetypes
from pathlib import Path

from google.genai import types
from pydantic import BaseModel, Field

from app.client.gemini_client import GeminiClient
from app.service.file_summary.hwp_extractor import HwpContent
from app.service.file_summary.office_extractor import OfficeContent
from app.service.file_summary.svg_extractor import SvgInspection

logger = logging.getLogger(__name__)


class SummaryPayload(BaseModel):
    summary: str = Field(description="1 to 2 sentence summary in Korean.")


class ImagePayload(BaseModel):
    ocr_text: str = Field(default="", description="Text extracted from the image.")
    description: str = Field(description="Short Korean description of the image.")


class SvgPayload(BaseModel):
    summary: str = Field(description="Korean description of what the SVG depicts.")


DOCUMENT_INSTRUCTION = """\
You are a document summarizer. Read the supplied document content and produce
a faithful summary.

Rules:
- The summary MUST be written in Korean (한국어), regardless of the source language.
- Length: 1 to 2 sentences.
- Capture the core topic, key facts, and any conclusions.
- Do not add information that is not present in the source.
- Keep proper nouns, technical terms, and acronyms in their original form.
- Output must match the response schema exactly.
"""

OFFICE_INSTRUCTION = """\
You are a document summarizer. The user gives you the text body of an Office
document and any images that were embedded inside it. Use BOTH the text and
the images together to produce one combined summary.

Rules:
- The summary MUST be written in Korean (한국어), regardless of the source language.
- Length: 1 to 2 sentences.
- Integrate information from embedded images (charts, screenshots, diagrams,
  pictures) when relevant. If an image is decorative, you may ignore it.
- Do not list the images separately; weave their content into the summary.
- Keep proper nouns, technical terms, and acronyms in their original form.
- Do not add information that is not present in either the text or the images.
- Output must match the response schema exactly.
"""

IMAGE_INSTRUCTION = """\
You are an image analyst. Look at the supplied image and produce two outputs:

1. ocr_text: Any text visible in the image, transcribed verbatim in its
   original language. If there is no readable text, return an empty string.
2. description: A short Korean (한국어) description (1-2 sentences) of what
   the image depicts. The description MUST be in Korean even if the source
   image is in another language. Keep proper nouns and technical terms in
   their original form.

Do not invent content that is not visible.
"""

SVG_INSTRUCTION = """\
You are an SVG analyst. The user will paste the raw SVG XML source.
Analyze the structure and produce a 1-2 sentence Korean (한국어) description
of what the graphic depicts (shapes, layout, likely subject). The summary
MUST be written in Korean. If the source is too abstract or unreadable, say
so briefly in Korean. Do not invent content.
"""


async def summarize_pdf(client: GeminiClient, path: Path) -> str:
    uploaded = await client.upload_file(path, mime_type="application/pdf")
    try:
        payload = await client.generate_structured(
            contents=[uploaded, "Summarize the attached document."],
            schema=SummaryPayload,
            instruction=DOCUMENT_INSTRUCTION,
        )
    finally:
        await client.delete_file(uploaded)
    return payload.summary


async def summarize_office(
    client: GeminiClient,
    path: Path,
    content: OfficeContent,
) -> str:
    text_prompt = (
        f"Filename: {path.name}\n"
        f"Embedded image count: {len(content.images)}\n"
        "Document text follows between <<< and >>>.\n"
        "<<<\n"
        f"{content.text}\n"
        ">>>\n"
        "Use the text together with the attached images to produce one summary."
    )
    contents: list[object] = [text_prompt]
    for index, image in enumerate(content.images, start=1):
        contents.append(types.Part.from_bytes(data=image.data, mime_type=image.mime_type))
        contents.append(f"[image {index} from {image.origin}]")
    payload = await client.generate_structured(
        contents=contents,
        schema=SummaryPayload,
        instruction=OFFICE_INSTRUCTION,
    )
    return payload.summary


async def summarize_hwp(
    client: GeminiClient,
    path: Path,
    content: HwpContent,
) -> str:
    text_prompt = (
        f"Filename: {path.name}\n"
        f"Embedded image count: {len(content.images)}\n"
        "Document text follows between <<< and >>>.\n"
        "<<<\n"
        f"{content.text}\n"
        ">>>\n"
        "Use the text together with the attached images to produce one summary."
    )
    contents: list[object] = [text_prompt]
    for index, image in enumerate(content.images, start=1):
        contents.append(types.Part.from_bytes(data=image.data, mime_type=image.mime_type))
        contents.append(f"[image {index} from {image.origin}]")
    payload = await client.generate_structured(
        contents=contents,
        schema=SummaryPayload,
        instruction=OFFICE_INSTRUCTION,
    )
    return payload.summary


async def summarize_image(client: GeminiClient, path: Path) -> tuple[str, str]:
    """이미지 OCR + 설명. 콜백 단계에서 한 컬럼으로 합쳐쓴다."""

    mime = _image_mime(path)
    uploaded = await client.upload_file(path, mime_type=mime)
    try:
        payload = await client.generate_structured(
            contents=[uploaded, "Analyze the attached image."],
            schema=ImagePayload,
            instruction=IMAGE_INSTRUCTION,
        )
    finally:
        await client.delete_file(uploaded)
    return payload.description, payload.ocr_text


async def summarize_svg(
    client: GeminiClient,
    path: Path,
    inspection: SvgInspection,
) -> str:
    if inspection.text is None:
        return ""

    prompt = (
        f"SVG filename: {path.name}\n"
        "Raw SVG source between <<< and >>>.\n"
        "<<<\n"
        f"{inspection.text}\n"
        ">>>\n"
        "Describe what this SVG depicts."
    )
    payload = await client.generate_structured(
        contents=[prompt],
        schema=SvgPayload,
        instruction=SVG_INSTRUCTION,
    )
    return payload.summary


def _image_mime(path: Path) -> str:
    suffix = path.suffix.lower()
    if suffix in {".jpg", ".jpeg"}:
        return "image/jpeg"
    if suffix == ".png":
        return "image/png"
    if suffix == ".webp":
        return "image/webp"
    guessed, _ = mimetypes.guess_type(str(path))
    if guessed and guessed.startswith("image/"):
        return guessed
    raise ValueError(f"Unsupported image type: {suffix}")
