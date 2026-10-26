import asyncio
import pytest

from app.model.embedding import WorklogEmbeddingResult
from app.task import embedding_tasks


def test_embedding_task_releases_db_pool_after_async_run(monkeypatch) -> None:
    closed = []

    class Service:
        async def embed_worklog(self, worklog_id):
            return WorklogEmbeddingResult(worklogId=worklog_id, chunkCount=2)

    async def close():
        closed.append(True)

    monkeypatch.setattr(embedding_tasks, "EmbeddingService", Service)
    monkeypatch.setattr(embedding_tasks, "dispose_engine", close)
    assert asyncio.run(embedding_tasks._generate_worklog_embedding(101)) == {
        "worklog_id": 101,
        "chunk_count": 2,
    }
    assert closed == [True]


def test_embedding_task_passes_temporary_failure_to_retry_policy(monkeypatch) -> None:
    calls = []

    async def fail(worklog_id):
        raise TimeoutError("provider timeout")

    def retry(task, exc, **kwargs):
        calls.append((type(exc), kwargs["target_id"]))

    monkeypatch.setattr(embedding_tasks, "_generate_worklog_embedding", fail)
    monkeypatch.setattr(embedding_tasks, "retry_ai_task", retry)
    with pytest.raises(TimeoutError):
        embedding_tasks.generate_worklog_embedding.run(101)
    assert calls == [(TimeoutError, 101)]
