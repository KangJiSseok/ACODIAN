"""기존 업무일지를 작은 배치로 GraphRAG에 재색인한다: python -m app.graph.v1.backfill."""

import asyncio
import logging

from sqlalchemy import select

from app.config.settings import settings
from app.graph.v1.service.worklog_index_service import GraphWorklogIndexService
from app.store.models import Worklog
from app.store.session import get_session_factory

logger = logging.getLogger(__name__)


async def backfill() -> int:
    cursor = 0
    count = 0
    batch_size = settings.graphrag_index_max_batch_size
    if batch_size < 1:
        raise ValueError("GRAPHRAG_INDEX_MAX_BATCH_SIZE must be positive")
    factory = get_session_factory()
    service = GraphWorklogIndexService()
    while True:
        async with factory() as session:
            ids = list((await session.execute(
                select(Worklog.worklog_id)
                .where(Worklog.worklog_id > cursor, Worklog.is_deleted.is_(False))
                .order_by(Worklog.worklog_id)
                .limit(batch_size)
            )).scalars().all())
        if not ids:
            return count
        response = await service.index_worklogs(ids)
        failures = [item.worklog_id for item in response.items if not item.indexed]
        if failures:
            raise RuntimeError(f"GraphRAG backfill failed for worklog IDs: {failures}")
        cursor = ids[-1]
        count += len(ids)
        logger.info("GraphRAG backfill indexed=%d", count)


if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO)
    asyncio.run(backfill())
