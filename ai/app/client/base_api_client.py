from types import TracebackType
from typing import Self

import httpx

from app.config.settings import settings


class BaseApiClient:
    def __init__(self) -> None:
        self.base_url = settings.api_base_url.rstrip("/")
        self._client: httpx.AsyncClient | None = None

    async def __aenter__(self) -> Self:
        self._client = httpx.AsyncClient(base_url=self.base_url, timeout=30.0)
        return self

    async def __aexit__(
        self,
        exc_type: type[BaseException] | None,
        exc: BaseException | None,
        traceback: TracebackType | None,
    ) -> None:
        if self._client is not None:
            await self._client.aclose()
            self._client = None

    @property
    def client(self) -> httpx.AsyncClient:
        if self._client is None:
            raise RuntimeError("API client is not initialized")
        return self._client

    def api_path(self, path: str) -> str:
        if self.base_url.endswith("/api") and path.startswith("/api/"):
            return path.removeprefix("/api")
        return path
