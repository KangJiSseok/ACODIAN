"""HWP 텍스트(hwp5txt 서브프로세스) + OLE BinData 이미지 추출."""

from __future__ import annotations

import logging
import subprocess
import zlib
from dataclasses import dataclass, field
from pathlib import Path

import olefile

from app.service.file_summary.office_extractor import ExtractedImage

logger = logging.getLogger(__name__)

MAX_CHARS = 60_000
MAX_IMAGES = 20
MAX_IMAGE_BYTES = 5 * 1024 * 1024


@dataclass
class HwpContent:
    text: str
    images: list[ExtractedImage] = field(default_factory=list)


class HwpExtractionError(RuntimeError):
    pass


def extract_hwp_content(path: Path) -> HwpContent:
    text = _extract_text(path)
    images = _extract_images(path)
    if len(text) > MAX_CHARS:
        logger.info("HWP 텍스트 truncate: %s %d→%d", path.name, len(text), MAX_CHARS)
        text = text[:MAX_CHARS]
    images = _cap_images(path.name, images)
    if not text and not images:
        raise HwpExtractionError(f"읽을 수 있는 내용이 없습니다: {path.name}")
    return HwpContent(text=text, images=images)


def _extract_text(path: Path) -> str:
    try:
        completed = subprocess.run(
            ["hwp5txt", str(path)],
            capture_output=True,
            timeout=60,
        )
    except FileNotFoundError as exc:
        raise HwpExtractionError(
            "hwp5txt 명령을 찾지 못함. pyhwp 설치 필요: pip install pyhwp"
        ) from exc
    except subprocess.TimeoutExpired as exc:
        raise HwpExtractionError(f"hwp5txt timeout: {path.name}") from exc

    if completed.returncode != 0:
        stderr = completed.stderr.decode("utf-8", errors="replace").strip()
        raise HwpExtractionError(f"hwp5txt exit {completed.returncode}: {stderr}")
    return completed.stdout.decode("utf-8", errors="replace").strip()


def _extract_images(path: Path) -> list[ExtractedImage]:
    if not olefile.isOleFile(str(path)):
        logger.warning("HWP %s 가 OLE 컨테이너가 아님 — 이미지 추출 생략", path.name)
        return []

    images: list[ExtractedImage] = []
    ole = olefile.OleFileIO(str(path))
    try:
        for stream_path in ole.listdir(streams=True):
            if len(stream_path) < 2 or stream_path[0] != "BinData":
                continue
            stream_name = stream_path[-1]
            try:
                with ole.openstream(stream_path) as stream:
                    raw = stream.read()
            except OSError:
                logger.warning("HWP BinData stream %s 읽기 실패", stream_name)
                continue

            blob = _maybe_decompress(raw)
            mime = _sniff_image_mime(blob)
            if mime is None:
                continue
            images.append(
                ExtractedImage(
                    data=blob,
                    mime_type=mime,
                    origin=f"BinData/{stream_name}",
                )
            )
    finally:
        ole.close()
    return images


def _maybe_decompress(data: bytes) -> bytes:
    if len(data) < 2:
        return data
    if data[:2] in {b"\x78\x01", b"\x78\x9c", b"\x78\xda"}:
        try:
            return zlib.decompress(data)
        except zlib.error:
            return data
    # HWP 가 raw deflate(zlib header 없음) 로 저장하는 경우가 있다.
    if _sniff_image_mime(data) is None:
        try:
            return zlib.decompress(data, -zlib.MAX_WBITS)
        except zlib.error:
            return data
    return data


def _sniff_image_mime(data: bytes) -> str | None:
    if data.startswith(b"\x89PNG\r\n\x1a\n"):
        return "image/png"
    if data.startswith(b"\xff\xd8\xff"):
        return "image/jpeg"
    if data[:6] in {b"GIF87a", b"GIF89a"}:
        return "image/gif"
    if data.startswith(b"BM"):
        return "image/bmp"
    if data[:4] == b"RIFF" and data[8:12] == b"WEBP":
        return "image/webp"
    return None


def _cap_images(file_name: str, images: list[ExtractedImage]) -> list[ExtractedImage]:
    kept: list[ExtractedImage] = []
    for image in images:
        if len(image.data) > MAX_IMAGE_BYTES:
            logger.info("oversized HWP 이미지 제외 (%d bytes) from %s", len(image.data), file_name)
            continue
        kept.append(image)
        if len(kept) >= MAX_IMAGES:
            logger.info("HWP image cap (%d) 도달 from %s", MAX_IMAGES, file_name)
            break
    return kept
