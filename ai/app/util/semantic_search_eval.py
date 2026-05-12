"""시맨틱 검색 수동 평가 CLI.

이 스크립트는 이미 생성된 `tb_worklog_embedding`을 대상으로 검색 품질을 확인한다.
업무일지 본문을 외부로 다시 보내지 않고, 사용자가 입력한 검색 질의문만 Gemini API로
전송해 query embedding을 만든다.

예:
    python -m app.util.semantic_search_eval \
      --confirm-external-query-embedding \
      --fixture

    python -m app.util.semantic_search_eval \
      --confirm-external-query-embedding \
      --query "야간 배치가 겹치면서 운영이 흔들린 사례"
"""

from __future__ import annotations

import argparse
import asyncio
from dataclasses import dataclass
from pathlib import Path

import yaml
from sqlalchemy import text

from app.model.search import SemanticWorklogSearchRequest
from app.service.search_service import SearchService
from app.store.session import get_session_factory


DEFAULT_FIXTURE_PATH = (
    Path(__file__).resolve().parents[2]
    / "tests"
    / "fixtures"
    / "semantic_search_queries.yaml"
)


@dataclass(frozen=True)
class EvaluationQuery:
    """fixture 한 행을 CLI에서 다루기 쉬운 형태로 변환한 값."""

    query: str
    expected_team_ids: list[int]
    note: str | None


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Evaluate semantic worklog search.")
    parser.add_argument(
        "--confirm-external-query-embedding",
        action="store_true",
        help="검색 질의문을 외부 Gemini API로 전송하는 것을 승인한다.",
    )
    parser.add_argument(
        "--fixture",
        action="store_true",
        help="tests/fixtures/semantic_search_queries.yaml 질의 세트를 실행한다.",
    )
    parser.add_argument(
        "--fixture-path",
        type=Path,
        default=DEFAULT_FIXTURE_PATH,
        help="실행할 YAML fixture 경로.",
    )
    parser.add_argument(
        "--query",
        action="append",
        default=[],
        help="직접 실행할 검색 질의. 여러 번 지정 가능.",
    )
    parser.add_argument(
        "--top-k",
        type=int,
        default=5,
        help="질의당 출력할 상위 결과 수.",
    )
    return parser.parse_args()


async def main() -> None:
    """CLI 진입점."""
    args = parse_args()
    if not args.confirm_external_query_embedding:
        raise SystemExit(
            "검색 질의문을 Gemini API로 전송하려면 "
            "--confirm-external-query-embedding 를 지정하세요."
        )
    queries = load_queries(args)
    if not queries:
        raise SystemExit("--fixture 또는 --query 중 하나를 지정하세요.")

    service = SearchService()
    for index, item in enumerate(queries, start=1):
        response = await service.search_worklogs(
            SemanticWorklogSearchRequest(keyword=item.query, page=1, pageSize=args.top_k)
        )
        worklog_ids = [result.worklog_id for result in response.items]
        team_ids_by_worklog = await fetch_team_ids(worklog_ids)
        actual_team_ids = [team_ids_by_worklog[worklog_id] for worklog_id in worklog_ids]
        expected_hits = sorted(set(actual_team_ids) & set(item.expected_team_ids))

        print(f"\n[{index}] {item.query}")
        if item.expected_team_ids:
            print(f"expected_team_ids={item.expected_team_ids} hit_team_ids={expected_hits}")
        if item.note:
            print(f"note={item.note}")
        for rank, result in enumerate(response.items, start=1):
            team_id = team_ids_by_worklog[result.worklog_id]
            print(
                f"  {rank}. worklog_id={result.worklog_id} "
                f"team_id={team_id} score={result.score:.4f}"
            )


def load_queries(args: argparse.Namespace) -> list[EvaluationQuery]:
    """CLI 옵션에서 평가 질의 목록을 만든다."""
    queries: list[EvaluationQuery] = []
    if args.fixture:
        rows = yaml.safe_load(args.fixture_path.read_text(encoding="utf-8"))
        queries.extend(
            EvaluationQuery(
                query=row["query"],
                expected_team_ids=row.get("expected_team_ids", []),
                note=row.get("note"),
            )
            for row in rows
        )
    queries.extend(
        EvaluationQuery(query=query, expected_team_ids=[], note=None)
        for query in args.query
    )
    return queries


async def fetch_team_ids(worklog_ids: list[int]) -> dict[int, int]:
    """검색 결과 worklog_id에 대응하는 team_id를 조회한다."""
    if not worklog_ids:
        return {}
    factory = get_session_factory()
    async with factory() as session:
        rows = (
            await session.execute(
                text(
                    """
                    select worklog_id, team_id
                    from tb_worklog
                    where worklog_id = any(:worklog_ids)
                    """
                ),
                {"worklog_ids": worklog_ids},
            )
        ).all()
    return {row.worklog_id: row.team_id for row in rows}


if __name__ == "__main__":
    asyncio.run(main())
