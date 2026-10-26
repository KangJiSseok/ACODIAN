"""GraphRAG 색인을 Celery worker에서 수행한다."""

import asyncio
import logging
import time

from app.graph.v1.service.graph_rag_adapter import close_graph_rag_worklog_adapter
from app.graph.v1.model.worklog_index import WorklogGraphIndexResponse
from app.graph.v1.service.worklog_index_service import GraphWorklogIndexService
from app.store.session import dispose_engine
from app.task.celery_app import celery_app

logger = logging.getLogger(__name__)


@celery_app.task(name="worklog.graph_index", autoretry_for=(Exception,), retry_backoff=True, retry_kwargs={"max_retries": 5})
def index_graph_worklog(worklog_id: int, enqueued_at_ms: int) -> dict[str, int | bool]:
    result = asyncio.run(_index_and_close(worklog_id))
    item = result.items[0]
    if not item.indexed and item.error != "WORKLOG_NOT_FOUND":
        logger.warning("GraphRAG index failed worklog_id=%s error=%s", worklog_id, item.error)
        raise RuntimeError("GraphRAG index failed")
    logger.info("GraphRAG index lag_ms=%d indexed=%s", max(0, int(time.time() * 1000) - enqueued_at_ms), item.indexed)
    return {"worklog_id": worklog_id, "indexed": item.indexed}


async def _index_and_close(worklog_id: int) -> WorklogGraphIndexResponse:
    try:
        return await GraphWorklogIndexService().index_worklogs([worklog_id])
    finally:
        try:
            await close_graph_rag_worklog_adapter()
        finally:
            await dispose_engine()
