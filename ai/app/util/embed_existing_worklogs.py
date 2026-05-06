"""기존 `tb_worklog` 전체/부분 임베딩 배치 러너.

주의:
- 이 스크립트는 `tb_worklog`의 제목/요청/본문/작성자 역할/선행 업무 제목을
  Gemini API로 전송해 임베딩을 생성한다.
- 실행자는 `--confirm-real-data-export` 플래그로 이 데이터 반출을 명시 승인해야 한다.

예:
    python -m app.util.embed_existing_worklogs --confirm-real-data-export --all

    python -m app.util.embed_existing_worklogs \
      --confirm-real-data-export \
      --team-id 109 \
      --limit 20
"""

from __future__ import annotations

import argparse
import asyncio
from dataclasses import dataclass

from sqlalchemy import text

from app.service.embedding_service import EmbeddingService
from app.store.session import get_session_factory


@dataclass(frozen=True)
class BatchOptions:
    all_worklogs: bool
    worklog_ids: list[int]
    team_ids: list[int]
    limit: int | None
    skip_embedded: bool
    dry_run: bool
    sleep_seconds: float


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Embed existing tb_worklog rows.")
    parser.add_argument(
        "--confirm-real-data-export",
        action="store_true",
        help="실제 tb_worklog 내용을 외부 Gemini API로 전송하는 것을 승인한다.",
    )
    parser.add_argument(
        "--all",
        dest="all_worklogs",
        action="store_true",
        help="삭제되지 않은 모든 worklog를 대상으로 한다.",
    )
    parser.add_argument(
        "--worklog-id",
        action="append",
        type=int,
        default=[],
        help="임베딩할 worklog_id. 여러 번 지정 가능.",
    )
    parser.add_argument(
        "--team-id",
        action="append",
        type=int,
        default=[],
        help="해당 팀의 업무일지를 임베딩한다. 여러 번 지정 가능.",
    )
    parser.add_argument(
        "--limit",
        type=int,
        default=None,
        help="대상 worklog 최대 개수. 미지정 시 조건에 맞는 전체.",
    )
    parser.add_argument(
        "--skip-embedded",
        action="store_true",
        help="이미 tb_worklog_embedding에 chunk가 있는 worklog는 건너뛴다.",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="대상 ID만 출력하고 Gemini 호출/DB 저장은 하지 않는다.",
    )
    parser.add_argument(
        "--sleep-seconds",
        type=float,
        default=0.0,
        help="worklog 1건 처리 후 대기 시간. rate limit 완화용.",
    )
    return parser.parse_args()


async def main() -> None:
    """CLI 진입점.

    전체 배치 실행 전에 반드시 명시 승인 플래그를 확인한다. 이 스크립트는 내부 DB
    데이터를 외부 Gemini API로 보내므로, 실수로 실행되는 일을 막는 게 중요하다.
    """
    args = parse_args()
    if not args.confirm_real_data_export:
        raise SystemExit(
            "실제 DB 내용을 Gemini API로 전송하려면 --confirm-real-data-export 를 지정하세요."
        )

    options = BatchOptions(
        all_worklogs=args.all_worklogs,
        worklog_ids=args.worklog_id,
        team_ids=args.team_id,
        limit=args.limit,
        skip_embedded=args.skip_embedded,
        dry_run=args.dry_run,
        sleep_seconds=args.sleep_seconds,
    )
    worklog_ids = await resolve_worklog_ids(options)
    print(f"[target] count={len(worklog_ids)} ids={worklog_ids}")
    if options.dry_run:
        return

    await embed_worklogs(worklog_ids, options.sleep_seconds)


async def resolve_worklog_ids(options: BatchOptions) -> list[int]:
    """CLI 옵션을 실제 처리 대상 worklog_id 목록으로 변환한다."""
    if not options.all_worklogs and not options.worklog_ids and not options.team_ids:
        raise SystemExit("--all, --worklog-id, --team-id 중 하나를 지정하세요.")

    conditions = ["w.is_deleted = false"]
    params: dict[str, object] = {}

    if not options.all_worklogs:
        filters = []
        if options.worklog_ids:
            filters.append("w.worklog_id = any(:worklog_ids)")
            params["worklog_ids"] = options.worklog_ids
        if options.team_ids:
            filters.append("w.team_id = any(:team_ids)")
            params["team_ids"] = options.team_ids
        conditions.append(f"({' or '.join(filters)})")

    if options.skip_embedded:
        conditions.append(
            """
            not exists (
                select 1
                from tb_worklog_embedding e
                where e.worklog_id = w.worklog_id
            )
            """
        )

    limit_clause = ""
    if options.limit is not None:
        limit_clause = "limit :limit"
        params["limit"] = options.limit

    sql = f"""
        select w.worklog_id
        from tb_worklog w
        where {' and '.join(conditions)}
        order by w.worklog_id
        {limit_clause}
    """

    factory = get_session_factory()
    async with factory() as session:
        rows = (await session.execute(text(sql), params)).scalars().all()
    return list(dict.fromkeys(rows))


async def embed_worklogs(worklog_ids: list[int], sleep_seconds: float) -> None:
    """대상 업무일지를 순차적으로 임베딩한다.

    전체 622건 정도는 한 번에 처리 가능하지만, API quota가 불안하면
    `--sleep-seconds`로 호출 간격을 줄 수 있다.
    """
    service = EmbeddingService()
    success_count = 0
    failure_count = 0
    for index, worklog_id in enumerate(worklog_ids, start=1):
        try:
            result = await service.embed_worklog(worklog_id)
            success_count += 1
            print(
                f"[ok] {index}/{len(worklog_ids)} "
                f"worklog_id={result.worklog_id} chunk_count={result.chunk_count}"
            )
        except Exception as exc:
            failure_count += 1
            print(f"[fail] {index}/{len(worklog_ids)} worklog_id={worklog_id} error={exc!r}")
        if sleep_seconds > 0:
            await asyncio.sleep(sleep_seconds)
    print(f"[done] success={success_count} failure={failure_count}")


if __name__ == "__main__":
    asyncio.run(main())
