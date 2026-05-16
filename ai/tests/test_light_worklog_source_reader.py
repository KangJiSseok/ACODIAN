import asyncio

import pytest

from app.light.v3.service import worklog_source_reader
from app.light.v3.store.worklog_source_store import (
    LightWorklogPredecessor,
    LightWorklogTag,
    LightWorklogSourceRow,
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
        self.calls: list[tuple[object, int]] = []

    async def fetch_worklog_source(
        self,
        session: object,
        worklog_id: int,
    ) -> LightWorklogSourceRow | None:
        self.calls.append((session, worklog_id))
        return self.rows.get(worklog_id)


def test_fetch_sources_uses_light_v3_source_store(monkeypatch: pytest.MonkeyPatch) -> None:
    session = object()
    source_row = LightWorklogSourceRow(
        worklog_id=101,
        title="정산 배치 오류 분석",
        request_content="정산 배치 실패 원인을 확인해 주세요.",
        work_content="로그 기준으로 API timeout과 재시도 누락을 확인했습니다.",
        author_name="김도윤",
        team_name="정산 고도화 TF",
        predecessors=[
            LightWorklogPredecessor(worklog_id=88, title="배치 모니터링 개선"),
        ],
        tags=[
            LightWorklogTag(tag_name="정산", description="정산 배치와 대사 업무"),
            LightWorklogTag(tag_name="장애분석", description=None),
        ],
    )
    fake_store = _FakeSourceStore({101: source_row})

    monkeypatch.setattr(
        worklog_source_reader,
        "get_session_factory",
        lambda: lambda: _AsyncSessionContext(session),
    )
    monkeypatch.setattr(
        worklog_source_reader,
        "LightWorklogSourceStore",
        lambda: fake_store,
    )

    sources = asyncio.run(
        worklog_source_reader.WorklogLightSourceReader().fetch_sources([101, 999])
    )

    assert list(sources) == [101]
    assert sources[101].worklog_id == 101
    assert sources[101].predecessors[0].worklog_id == 88
    assert sources[101].predecessors[0].title == "배치 모니터링 개선"
    assert [tag.tag_name for tag in sources[101].tags] == ["정산", "장애분석"]
    assert fake_store.calls == [(session, 101), (session, 999)]
