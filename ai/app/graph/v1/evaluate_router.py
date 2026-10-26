"""한국어 라우팅 fixture 평가: python -m app.graph.v1.evaluate_router."""

import asyncio
import json
from pathlib import Path

import httpx

from app.config.settings import settings
from app.graph.v1.service.jev_router import JevRouter


async def evaluate() -> int:
    async with httpx.AsyncClient(timeout=settings.jev_timeout_seconds) as client:
        response = await client.get(f"{settings.jev_url.rstrip('/')}/health")
        response.raise_for_status()
    cases = json.loads((Path(__file__).parent / "fixtures/graph_routing_questions.json").read_text())
    router = JevRouter()
    missing = 0
    both = 0
    for index, case in enumerate(cases, start=1):
        route = await router.classify_strict(case["query"])
        omitted = (case["relation"] and not route.relation) or (case["similarity"] and not route.similarity)
        missing += int(omitted)
        both += int(route.relation and route.similarity)
        print(f"case={index} relation={route.relation} similarity={route.similarity} omitted={omitted}")
    print(f"cases={len(cases)} omitted={missing} both={both}")
    return missing


if __name__ == "__main__":
    raise SystemExit(min(1, asyncio.run(evaluate())))
