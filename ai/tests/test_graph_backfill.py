import asyncio
from importlib import import_module

from app.graph.v1.model.worklog_index import WorklogGraphIndexItem, WorklogGraphIndexResponse


def test_backfill_uses_bounded_batches_and_stops_at_end(monkeypatch) -> None:
    module = import_module("app.graph.v1.backfill")
    batches = [list(range(1, 11)), [11], []]
    indexed = []

    class Result:
        def __init__(self, ids):
            self.ids = ids

        def scalars(self):
            return self

        def all(self):
            return self.ids

    class Session:
        async def __aenter__(self):
            return self

        async def __aexit__(self, *args):
            return None

        async def execute(self, query):
            return Result(batches.pop(0))

    class Service:
        async def index_worklogs(self, ids):
            indexed.append(ids)
            return WorklogGraphIndexResponse(items=[
                WorklogGraphIndexItem(worklogId=id_, indexed=True) for id_ in ids
            ])

    monkeypatch.setattr(module, "get_session_factory", lambda: Session)
    monkeypatch.setattr(module, "GraphWorklogIndexService", Service)
    assert asyncio.run(module.backfill()) == 11
    assert indexed == [list(range(1, 11)), [11]]
