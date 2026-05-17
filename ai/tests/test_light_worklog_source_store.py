import asyncio
from types import SimpleNamespace

import pytest

from app.light.v3.store.worklog_source_store import (
    LightWorklogPredecessor,
    LightWorklogTag,
    LightWorklogSourceStore,
)


def test_fetch_predecessors_traces_all_depths_without_count_limit(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    graph = {
        1: [
            LightWorklogPredecessor(worklog_id=2, title="직접 선행 2"),
            LightWorklogPredecessor(worklog_id=3, title="직접 선행 3"),
            LightWorklogPredecessor(worklog_id=4, title="직접 선행 4"),
            LightWorklogPredecessor(worklog_id=5, title="직접 선행 5"),
        ],
        2: [LightWorklogPredecessor(worklog_id=6, title="2단계 선행 6")],
        6: [LightWorklogPredecessor(worklog_id=7, title="3단계 선행 7")],
        7: [LightWorklogPredecessor(worklog_id=8, title="4단계 선행 8")],
        8: [LightWorklogPredecessor(worklog_id=9, title="5단계 선행 9")],
    }

    async def fake_fetch_direct_predecessors(
        self: LightWorklogSourceStore,
        session: object,
        worklog_id: int,
    ) -> list[LightWorklogPredecessor]:
        return graph.get(worklog_id, [])

    monkeypatch.setattr(
        LightWorklogSourceStore,
        "_fetch_direct_predecessors",
        fake_fetch_direct_predecessors,
    )

    predecessors = asyncio.run(
        LightWorklogSourceStore()._fetch_predecessors(
            session=object(),
            worklog_id=1,
        )
    )

    assert [predecessor.worklog_id for predecessor in predecessors] == [
        2,
        3,
        4,
        5,
        6,
        7,
        8,
        9,
    ]
    assert [predecessor.title for predecessor in predecessors] == [
        "직접 선행 2",
        "직접 선행 3",
        "직접 선행 4",
        "직접 선행 5",
        "2단계 선행 6",
        "3단계 선행 7",
        "4단계 선행 8",
        "5단계 선행 9",
    ]


def test_fetch_predecessors_deduplicates_cycles(monkeypatch: pytest.MonkeyPatch) -> None:
    graph = {
        1: [LightWorklogPredecessor(worklog_id=2, title="직접 선행 2")],
        2: [
            LightWorklogPredecessor(worklog_id=1, title="순환 루트 1"),
            LightWorklogPredecessor(worklog_id=3, title="2단계 선행 3"),
        ],
        3: [LightWorklogPredecessor(worklog_id=2, title="순환 선행 2")],
    }

    async def fake_fetch_direct_predecessors(
        self: LightWorklogSourceStore,
        session: object,
        worklog_id: int,
    ) -> list[LightWorklogPredecessor]:
        return graph.get(worklog_id, [])

    monkeypatch.setattr(
        LightWorklogSourceStore,
        "_fetch_direct_predecessors",
        fake_fetch_direct_predecessors,
    )

    predecessors = asyncio.run(
        LightWorklogSourceStore()._fetch_predecessors(
            session=object(),
            worklog_id=1,
        )
    )

    assert [predecessor.worklog_id for predecessor in predecessors] == [2, 3]


def test_fetch_tags_keeps_name_and_description() -> None:
    class _Result:
        def all(self) -> list[SimpleNamespace]:
            return [
                SimpleNamespace(
                    tag_name="정산",
                    description="정산 배치와 대사 업무",
                ),
                SimpleNamespace(tag_name="장애분석", description=None),
            ]

    class _Session:
        async def execute(self, statement: object) -> _Result:
            compiled = str(statement.compile())
            assert "tb_meta_tag.description" not in compiled
            assert "is_deleted" not in compiled
            return _Result()

    tags = asyncio.run(
        LightWorklogSourceStore()._fetch_tags(
            session=_Session(),
            worklog_id=101,
        )
    )

    assert tags == [
        LightWorklogTag(
            tag_name="정산",
            description="정산 배치와 대사 업무",
        ),
        LightWorklogTag(tag_name="장애분석", description=None),
    ]

from app.light.v3.store.worklog_custom_kg_source_store import (
    LightWorklogCustomKgPredecessor,
    LightWorklogCustomKgSourceStore,
    LightWorklogCustomKgTag,
)


def test_custom_kg_fetch_tags_keeps_id_description_and_excludes_deleted() -> None:
    class _ColumnsResult:
        def all(self) -> list[SimpleNamespace]:
            return [
                SimpleNamespace(column_name="description"),
                SimpleNamespace(column_name="is_deleted"),
            ]

    class _Result:
        def all(self) -> list[SimpleNamespace]:
            return [
                SimpleNamespace(
                    tag_id=11,
                    tag_name="정산",
                    description="정산 배치와 대사 업무",
                )
            ]

    class _Session:
        def __init__(self) -> None:
            self.execute_count = 0

        async def execute(self, statement: object) -> _Result:
            self.execute_count += 1
            compiled = str(statement.compile())
            if self.execute_count == 1:
                assert "information_schema.columns" in compiled
                return _ColumnsResult()
            assert "tb_meta_tag.tag_id" in compiled
            assert "tb_meta_tag.description" in compiled
            assert "tb_meta_tag.is_deleted IS false" in compiled
            return _Result()

    tags = asyncio.run(
        LightWorklogCustomKgSourceStore()._fetch_tags(
            session=_Session(),
            worklog_id=101,
        )
    )

    assert tags == [
        LightWorklogCustomKgTag(
            tag_id=11,
            tag_name="정산",
            description="정산 배치와 대사 업무",
        )
    ]


def test_custom_kg_fetch_tags_supports_legacy_meta_tag_schema() -> None:
    class _ColumnsResult:
        def all(self) -> list[SimpleNamespace]:
            return []

    class _Result:
        def all(self) -> list[SimpleNamespace]:
            return [SimpleNamespace(tag_id=11, tag_name="정산", description=None)]

    class _Session:
        def __init__(self) -> None:
            self.execute_count = 0

        async def execute(self, statement: object) -> _Result:
            self.execute_count += 1
            compiled = str(statement.compile())
            if self.execute_count == 1:
                assert "information_schema.columns" in compiled
                return _ColumnsResult()
            assert "tb_meta_tag.tag_id" in compiled
            assert "tb_meta_tag.description" not in compiled
            assert "tb_meta_tag.is_deleted" not in compiled
            return _Result()

    tags = asyncio.run(
        LightWorklogCustomKgSourceStore()._fetch_tags(
            session=_Session(),
            worklog_id=101,
        )
    )

    assert tags == [LightWorklogCustomKgTag(tag_id=11, tag_name="정산", description=None)]


def test_custom_kg_fetch_direct_predecessors_uses_direct_edge_and_excludes_deleted() -> None:
    class _Result:
        def all(self) -> list[SimpleNamespace]:
            return [SimpleNamespace(depends_on_worklog_id=88, title="직접 선행")]

    class _Session:
        async def execute(self, statement: object) -> _Result:
            compiled = str(statement.compile())
            assert "tb_worklog_dependency.worklog_id" in compiled
            assert "tb_worklog.is_deleted IS false" in compiled
            return _Result()

    predecessors = asyncio.run(
        LightWorklogCustomKgSourceStore()._fetch_direct_predecessors(
            session=_Session(),
            worklog_id=101,
        )
    )

    assert predecessors == [LightWorklogCustomKgPredecessor(worklog_id=88, title="직접 선행")]
