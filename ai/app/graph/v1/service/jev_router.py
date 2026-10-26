"""jevos의 O/X 두 질문을 검색 경로로 변환한다."""

import logging
from dataclasses import dataclass

import httpx

from app.config.settings import Settings, settings

logger = logging.getLogger(__name__)


@dataclass(frozen=True)
class SearchRoute:
    relation: bool
    similarity: bool


BOTH = SearchRoute(relation=True, similarity=True)


class JevRouter:
    def __init__(self, settings_obj: Settings = settings) -> None:
        self._settings = settings_obj

    async def classify(self, query: str) -> SearchRoute:
        try:
            return await self.classify_strict(query)
        except (httpx.HTTPError, ValueError, KeyError, TypeError):
            logger.warning("jevos routing failed; both paths selected")
            return BOTH

    async def classify_strict(self, query: str) -> SearchRoute:
        """평가 시 서비스 오류를 숨기지 않고 운영 시에는 classify가 양쪽으로 복구한다."""
        async with httpx.AsyncClient(timeout=self._settings.jev_timeout_seconds) as client:
            response = await client.post(
                f"{self._settings.jev_url.rstrip('/')}/v1/systemone",
                json={
                    "model": "jev-latest",
                    "state": query,
                    "questions": {
                        "relation": {"type": "noul", "instructions": "Does the Korean question ask for a specific worklog's predecessor, author, or other explicit relationship?"},
                        "similarity": {"type": "noul", "instructions": "Does the Korean question need semantically similar worklogs or relevant text examples?"},
                    },
                },
            )
            response.raise_for_status()
            answers = response.json()["answers"]
            relation = float(answers["relation"]["noul"])
            similarity = float(answers["similarity"]["noul"])
            if not all(0 <= score <= 1 for score in (relation, similarity)):
                raise ValueError("invalid jevos probability")
            if 0.2 < relation < 0.8 or 0.2 < similarity < 0.8:
                return BOTH
            route = SearchRoute(relation=relation >= 0.8, similarity=similarity >= 0.8)
            return route if route.relation or route.similarity else BOTH
