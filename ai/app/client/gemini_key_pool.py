"""Gemini API key 후보 순환/정규화 유틸리티.

실제 API key 원문은 외부로 노출하지 않고, 로그에는 후보 index만 사용한다.
"""

from __future__ import annotations

from dataclasses import dataclass
from threading import Lock
from typing import Iterable


@dataclass(frozen=True)
class GeminiApiKeyCandidate:
    """정규화된 Gemini API key 후보.

    Attributes:
        index: 정규화된 후보 목록에서의 0-based index. 로그/테스트용 식별자.
        value: 실제 API key 값. 로그에 출력하지 않는다.
    """

    index: int
    value: str


def normalize_gemini_api_keys(raw_keys: Iterable[str]) -> tuple[GeminiApiKeyCandidate, ...]:
    """공백/빈 항목/중복을 제거한 key 후보 tuple을 만든다."""
    values: list[str] = []
    for raw_key in raw_keys:
        key = raw_key.strip()
        if key and key not in values:
            values.append(key)
    return tuple(GeminiApiKeyCandidate(index=index, value=value) for index, value in enumerate(values))


class GeminiApiKeyPool:
    """Gemini API key 후보를 요청 단위로 round-robin 시작점에서 반환한다."""

    def __init__(self, raw_keys: Iterable[str]) -> None:
        self._candidates = normalize_gemini_api_keys(raw_keys)
        if not self._candidates:
            raise RuntimeError("GEMINI_API_KEY is not configured")
        self._lock = Lock()
        self._cursor = 0

    @property
    def candidates(self) -> tuple[GeminiApiKeyCandidate, ...]:
        return self._candidates

    def ordered_candidates_for_call(self) -> tuple[GeminiApiKeyCandidate, ...]:
        """이번 호출에서 시도할 key 후보를 round-robin 시작점 기준으로 반환한다."""
        with self._lock:
            start = self._cursor
            self._cursor = (self._cursor + 1) % len(self._candidates)

        return self._candidates[start:] + self._candidates[:start]
