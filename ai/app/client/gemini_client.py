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

from google import genai

from app.config.settings import settings


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
                (기본 ``text-embedding-004``, 768 차원 — ERD ``VECTOR(768)`` 와 일치).

        Returns:
            각 입력 텍스트에 대응하는 부동소수점 벡터 리스트.
        """
        target_model = model or settings.embedding_model
        response = await asyncio.to_thread(
            self._client.models.embed_content,
            model=target_model,
            contents=texts,
        )
        return [list(item.values) for item in response.embeddings]


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
