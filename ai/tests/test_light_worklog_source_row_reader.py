import asyncio

import pytest

from app.light.v3.service import worklog_source_row_reader
from app.light.v3.store.worklog_source_store import (
    LightWorklogPredecessor,
    LightWorklogSourceRow,
    LightWorklogTag,
)


class _AsyncSessionContext:
    def __init__(self, session: object) -> None:
        self.session = session

    async def __aenter__(self) -> object:
        return self.session

    async def __aexit__(self, exc_type: object, exc: object, tb: object) -> None:
        return None


class _FakeSourceStore:
    def __init__(self, rows: dict[int, LightWorklogSourceRow]) -> None:
        self.rows = rows
        self.calls: list[tuple[object, list[int]]] = []

    async def fetch_worklog_sources(
        self,
        session: object,
        worklog_ids: list[int],
    ) -> dict[int, LightWorklogSourceRow]:
        self.calls.append((session, worklog_ids))
        return {
            worklog_id: self.rows[worklog_id]
            for worklog_id in worklog_ids
            if worklog_id in self.rows
        }


def test_fetch_source_rows_returns_integrated_text_and_custom_kg_row(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    session = object()
    source_row = LightWorklogSourceRow(
        worklog_id=101,
        title="정산 배치 오류 분석",
        request_content="정산 배치 실패 원인을 확인해 주세요.",
        work_content="로그 기준으로 API timeout과 재시도 누락을 확인했습니다.",
        author_id=5,
        author_name="김도윤",
        team_id=7,
        team_name="정산 고도화 TF",
        tags=[
            LightWorklogTag(tag_id=11, tag_name="정산", description="정산 배치와 대사 업무"),
        ],
        direct_predecessors=[
            LightWorklogPredecessor(worklog_id=88, title="배치 모니터링 개선"),
        ],
    )
    fake_store = _FakeSourceStore({101: source_row})

    monkeypatch.setattr(
        worklog_source_row_reader,
        "get_session_factory",
        lambda: lambda: _AsyncSessionContext(session),
    )
    monkeypatch.setattr(
        worklog_source_row_reader,
        "LightWorklogSourceStore",
        lambda: fake_store,
    )

    source_rows = asyncio.run(
        worklog_source_row_reader.WorklogLightSourceRowReader().fetch_source_rows([101, 999])
    )

    assert list(source_rows) == [101]
    assert source_rows[101].author_id == 5
    assert source_rows[101].tags == [
        LightWorklogTag(tag_id=11, tag_name="정산", description="정산 배치와 대사 업무"),
    ]
    assert source_rows[101].direct_predecessors == [
        LightWorklogPredecessor(worklog_id=88, title="배치 모니터링 개선"),
    ]
    assert fake_store.calls == [(session, [101, 999])]
