"""S3 객체 다운로더.

api 가 업로드한 storageKey 를 받아 로컬 임시 파일로 받아온다.
api 의 ``S3StorageProperties`` 와 동일한 자격증명/버킷/basePrefix 를 공유한다.
basePrefix 가 설정돼 있으면 prefix 를 붙여 ``{basePrefix}/{key}`` 로 GET 한다.
"""

from __future__ import annotations

import logging
import tempfile
from pathlib import Path

import boto3
from botocore.config import Config

from app.config.settings import settings

logger = logging.getLogger(__name__)


class S3Downloader:
    def __init__(self) -> None:
        self._client = boto3.client(
            "s3",
            region_name=settings.aws_region,
            aws_access_key_id=settings.aws_access_key_id,
            aws_secret_access_key=settings.aws_secret_access_key,
            config=Config(retries={"max_attempts": 3, "mode": "standard"}),
        )
        self._bucket = settings.aws_s3_bucket
        self._base_prefix = settings.aws_s3_base_prefix.strip("/")

    def download_to_tempfile(self, storage_key: str, original_name: str) -> Path:
        """``storage_key`` 를 ``original_name`` 의 확장자를 보존한 임시 파일로 받아 경로를 반환한다.

        호출자는 처리 후 반드시 파일과 부모 디렉토리를 정리해야 한다.
        확장자는 파싱 디스패치(``EXT_TO_SOURCE``)가 의존하므로 원본 이름의 suffix 를 그대로 사용한다.
        """

        qualified_key = self._qualify(storage_key)
        suffix = Path(original_name).suffix.lower()
        tmp_dir = Path(tempfile.mkdtemp(prefix="ai_file_"))
        local_path = tmp_dir / f"object{suffix}"
        try:
            self._client.download_file(self._bucket, qualified_key, str(local_path))
        except Exception:
            logger.exception(
                "S3 다운로드 실패. bucket=%s key=%s", self._bucket, qualified_key
            )
            raise
        return local_path

    def _qualify(self, key: str) -> str:
        if not self._base_prefix:
            return key
        return f"{self._base_prefix}/{key}"
