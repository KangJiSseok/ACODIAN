"""파일 단위 AI 요약 Celery 태스크.

api 의 트리거를 받아 S3 → 임시파일 다운로드 → 확장자별 파싱/Gemini 호출 →
``PATCH /internal/files/{fileId}/ai-result`` 콜백까지 한 사이클로 처리한다.
실패해도 콜백을 ``FAILED`` 로 한 번은 보내 api 측 ``ai_processing_status`` 를 정리한다.
"""

import asyncio
import logging
import shutil
from pathlib import Path

from app.client.file_client import FileClient
from app.client.gemini_client import get_gemini_client
from app.client.s3_downloader import S3Downloader
from app.service.file_summary.dispatch import (
    UnsupportedFileExtension,
    resolve_source_type,
    summarize_by_source,
)
from app.task.celery_app import celery_app

logger = logging.getLogger(__name__)


@celery_app.task(name="file.summary")
def generate_file_summary(
    file_id: int,
    worklog_id: int,
    storage_key: str,
    original_name: str,
    file_extension: str,
) -> dict[str, int | str]:
    return asyncio.run(
        _generate_file_summary(
            file_id=file_id,
            worklog_id=worklog_id,
            storage_key=storage_key,
            original_name=original_name,
            file_extension=file_extension,
        )
    )


async def _generate_file_summary(
    file_id: int,
    worklog_id: int,
    storage_key: str,
    original_name: str,
    file_extension: str,
) -> dict[str, int | str]:
    local_path: Path | None = None
    try:
        source_type = resolve_source_type(file_extension)
        local_path = S3Downloader().download_to_tempfile(storage_key, original_name)
        gemini = get_gemini_client()
        summary = await summarize_by_source(gemini, source_type, local_path)

        async with FileClient() as file_client:
            await file_client.update_ai_result(
                file_id=file_id,
                ai_summary=summary,
                ai_processing_status="COMPLETED",
            )

        return {
            "file_id": file_id,
            "worklog_id": worklog_id,
            "status": "COMPLETED",
        }

    except UnsupportedFileExtension:
        logger.warning(
            "지원하지 않는 확장자라 요약을 건너뜀. file_id=%s ext=%s", file_id, file_extension
        )
        await _report_failure(file_id)
        return {"file_id": file_id, "worklog_id": worklog_id, "status": "FAILED"}

    except Exception:
        logger.exception("파일 AI 요약 실패 file_id=%s storage_key=%s", file_id, storage_key)
        await _report_failure(file_id)
        raise

    finally:
        if local_path is not None:
            shutil.rmtree(local_path.parent, ignore_errors=True)


async def _report_failure(file_id: int) -> None:
    """콜백 ``FAILED`` 송신. 콜백 자체가 실패해도 원래 예외 흐름은 그대로 유지한다."""

    try:
        async with FileClient() as file_client:
            await file_client.update_ai_result(
                file_id=file_id,
                ai_summary="",
                ai_processing_status="FAILED",
            )
    except Exception:
        logger.exception("FAILED 콜백 송신 실패 file_id=%s", file_id)
