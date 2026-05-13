"""공유 Gemini 클라이언트 (ADR-009).

ai 모듈의 *모든* Gemini API 호출은 이 단일 클라이언트를 거친다.

왜 단일화했는가?
- 인증(API 키), 타임아웃, 재시도 정책을 한 곳에서 관리하기 위함이다.
- 여러 기능(요약/태그/지능형 청킹/임베딩) 이 각자 클라이언트를 만들면
  키 관리, 호출 로그, 정책 변경이 흩어진다.

제공하는 메서드:
- `generate_text(prompt)` : 텍스트 생성. 요약/태그/지능형 청킹 등에서 사용.
- `embed(texts)`          : 임베딩 벡터 생성. 시맨틱 검색에서 사용.

호출 측은 모델명을 override 할 수 있으나 기본값은 settings 를 따른다:
- 텍스트 생성 기본 모델: `settings.gemini_model`
- 임베딩 기본 모델     : `settings.embedding_model`

사용 예:

    from app.client.gemini_client import get_gemini_client

    client = get_gemini_client()
    summary = await client.generate_text("다음 텍스트를 요약하라: ...")
    vectors = await client.embed(["chunk 1", "chunk 2"])

주의:
- 메서드 시그니처 변경은 사전 합의 후 단독 PR 로만 처리한다 (AGENTS.md).
- google-genai SDK 의 호출은 동기 함수라서 `asyncio.to_thread` 로 감싸
  이벤트 루프를 막지 않게 한다.
"""

from __future__ import annotations

import asyncio
import hashlib
import shutil
import tempfile
import time
from pathlib import Path
from typing import Any, TypeVar

from google import genai
from google.genai import types
from pydantic import BaseModel

from app.config.settings import settings

SchemaT = TypeVar("SchemaT", bound=BaseModel)


class GeminiClient:
    """Gemini API 호출 래퍼.

    싱글톤으로 사용하기를 권장한다 (`get_gemini_client()` 사용).
    직접 인스턴스화는 테스트나 키를 명시적으로 주입해야 할 때만 한다.
    """

    def __init__(self, api_key: str | None = None) -> None:
        """클라이언트 초기화.

        Args:
            api_key: 명시적으로 키를 주입할 때 사용.
                미지정 시 ``settings.gemini_api_key`` 를 사용한다.

        Raises:
            RuntimeError: 키가 비어 있을 때.
        """
        key = api_key or settings.gemini_api_key
        if not key:
            raise RuntimeError("GEMINI_API_KEY is not configured")
        self._client = genai.Client(api_key=key)

    async def generate_text(self, prompt: str, *, model: str | None = None) -> str:
        """단일 프롬프트로 텍스트를 생성한다.

        Args:
            prompt: 모델에 보낼 프롬프트.
            model: 모델명 override. 미지정 시 ``settings.gemini_model``.

        Returns:
            생성된 텍스트. 응답이 비어 있으면 빈 문자열.
        """
        target_model = model or settings.gemini_model
        response = await asyncio.to_thread(
            self._client.models.generate_content,
            model=target_model,
            contents=prompt,
        )
        return getattr(response, "text", "") or ""

    async def embed(
        self,
        texts: list[str],
        *,
        model: str | None = None,
    ) -> list[list[float]]:
        """텍스트 리스트를 임베딩 벡터 리스트로 변환한다.

        Args:
            texts: 임베딩할 텍스트들.
            model: 모델명 override. 미지정 시 ``settings.embedding_model``
                (기본 ``gemini-embedding-001``, 768 차원 — ERD ``VECTOR(768)`` 와 일치).

        Returns:
            각 입력 텍스트에 대응하는 부동소수점 벡터 리스트.
        """
        target_model = model or settings.embedding_model
        response = await asyncio.to_thread(
            self._client.models.embed_content,
            model=target_model,
            contents=texts,
            config=types.EmbedContentConfig(
                output_dimensionality=settings.embedding_dim,
            ),
        )
        return [list(item.values) for item in response.embeddings]

    # ------------------------------------------------------------------
    # 파일 단위 요약(파일 첨부 AI 파이프라인)에서 사용하는 추가 메서드.
    # PDF/이미지를 Files API 로 업로드하거나 Office/HWP 의 텍스트+이미지를
    # 단일 응답 스키마로 묶어 호출할 때 사용한다.
    # ------------------------------------------------------------------
    async def upload_file(self, path: Path, *, mime_type: str | None = None) -> Any:
        """Files API 로 파일을 업로드하고 ACTIVE 상태가 될 때까지 폴링한다.

        파일명에 비-ASCII 문자가 있으면 httpx 가 헤더를 ASCII 로 인코딩하지 못해
        실패하므로 SHA1 기반의 ASCII-safe 임시 사본을 만들어 업로드한다.
        """

        def _upload_sync() -> Any:
            upload_path, cleanup_dir = self._make_ascii_safe(path)
            try:
                config_kwargs: dict[str, Any] = {"display_name": path.name}
                if mime_type:
                    config_kwargs["mime_type"] = mime_type
                config = types.UploadFileConfig(**config_kwargs)
                uploaded = self._client.files.upload(file=str(upload_path), config=config)
                return self._wait_active(uploaded)
            finally:
                if cleanup_dir is not None:
                    shutil.rmtree(cleanup_dir, ignore_errors=True)

        return await asyncio.to_thread(_upload_sync)

    async def delete_file(self, file_obj: Any) -> None:
        """업로드된 Files API 자원을 정리한다. 실패해도 호출 흐름을 막지 않는다."""

        def _delete_sync() -> None:
            try:
                self._client.files.delete(name=file_obj.name)
            except Exception:
                pass

        await asyncio.to_thread(_delete_sync)

    async def generate_structured(
        self,
        *,
        contents: list[Any],
        schema: type[SchemaT],
        instruction: str,
        model: str | None = None,
    ) -> SchemaT:
        """Pydantic 응답 스키마로 강제 디코딩되는 단일 호출.

        파일 요약/이미지 OCR/SVG 분석처럼 JSON 형식이 보장되어야 하는 곳에서 사용한다.
        """

        target_model = model or settings.gemini_model
        response = await asyncio.to_thread(
            self._client.models.generate_content,
            model=target_model,
            contents=contents,
            config=types.GenerateContentConfig(
                temperature=0.1,
                top_p=0.95,
                response_mime_type="application/json",
                response_schema=schema,
                system_instruction=instruction,
            ),
        )
        parsed = response.parsed
        if parsed is None:
            raise ValueError("Gemini returned no parsed response.")
        return parsed

    def _wait_active(self, file_obj: Any) -> Any:
        """Files API 업로드 직후 ACTIVE 상태가 될 때까지 폴링한다."""

        deadline = time.monotonic() + settings.gemini_file_active_timeout_sec
        current = file_obj
        while True:
            state = getattr(current.state, "name", str(current.state))
            if state == "ACTIVE":
                return current
            if state == "FAILED":
                raise RuntimeError(f"File processing failed for {current.name}")
            if time.monotonic() > deadline:
                raise TimeoutError(f"File {current.name} did not become ACTIVE in time")
            time.sleep(settings.gemini_file_active_poll_sec)
            current = self._client.files.get(name=current.name)

    @staticmethod
    def _make_ascii_safe(path: Path) -> tuple[Path, Path | None]:
        try:
            path.name.encode("ascii")
            return path, None
        except UnicodeEncodeError:
            digest = hashlib.sha1(str(path.resolve()).encode("utf-8")).hexdigest()[:12]
            safe_name = f"{digest}{path.suffix.lower()}"
            tmp_dir = Path(tempfile.mkdtemp(prefix="gemupload_"))
            safe_path = tmp_dir / safe_name
            shutil.copy2(path, safe_path)
            return safe_path, tmp_dir


# 프로세스당 1 회만 만드는 싱글톤. 키 누락 시 첫 `get_gemini_client()` 호출에서 RuntimeError 가 난다.
_client: GeminiClient | None = None


def get_gemini_client() -> GeminiClient:
    """모듈 전역 싱글톤 GeminiClient 를 반환한다.

    같은 프로세스 안에서는 항상 동일 인스턴스를 돌려준다.
    """
    global _client
    if _client is None:
        _client = GeminiClient()
    return _client
