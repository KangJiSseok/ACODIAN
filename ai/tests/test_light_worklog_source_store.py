import asyncio
from types import SimpleNamespace

from app.light.v3.store.worklog_source_store import (
    LightWorklogPredecessor,
    LightWorklogSourceStore,
    LightWorklogTag,
)


class _Result:
    def __init__(self, rows: list[SimpleNamespace]) -> None:
        self._rows = rows

    def all(self) -> list[SimpleNamespace]:
        return self._rows


def test_fetch_worklog_sources_batches_base_tags_and_direct_predecessors() -> None:
    class _Session:
        def __init__(self) -> None:
            self.compiled_statements: list[str] = []

        async def execute(self, statement: object) -> _Result:
            compiled = str(statement.compile())
            self.compiled_statements.append(compiled)
            if len(self.compiled_statements) == 1:
                assert "tb_worklog.worklog_id IN" in compiled
                assert "tb_worklog.is_deleted IS false" in compiled
                assert "tb_team.deleted_at IS NULL" in compiled
                return _Result(
                    [
                        SimpleNamespace(
                            worklog_id=101,
                            title="정산 배치 오류 분석",
                            request_content="요청",
                            work_content="수행",
                            author_id=5,
                            user_name="김도윤",
                            team_id=7,
                            team_name="정산 고도화 TF",
                        )
                    ]
                )
            if len(self.compiled_statements) == 2:
                assert "tb_worklog_tag.worklog_id IN" in compiled
                assert "tb_meta_tag.description" in compiled
                assert "tb_meta_tag.is_deleted IS false" in compiled
                return _Result(
                    [
                        SimpleNamespace(
                            worklog_id=101,
                            tag_id=11,
                            tag_name="정산",
                            description="정산 배치와 대사 업무",
                        )
                    ]
                )
            assert "tb_worklog_dependency.worklog_id IN" in compiled
            assert "tb_worklog.is_deleted IS false" in compiled
            return _Result(
                [SimpleNamespace(worklog_id=101, depends_on_worklog_id=88, title="직접 선행")]
            )

    session = _Session()

    sources = asyncio.run(
        LightWorklogSourceStore().fetch_worklog_sources(
            session=session,
            worklog_ids=[101, 999, 101],
        )
    )

    assert list(sources) == [101]
    assert sources[101].author_id == 5
    assert sources[101].team_id == 7
    assert sources[101].tags == [
        LightWorklogTag(tag_id=11, tag_name="정산", description="정산 배치와 대사 업무")
    ]
    assert sources[101].direct_predecessors == [
        LightWorklogPredecessor(worklog_id=88, title="직접 선행")
    ]
    assert len(session.compiled_statements) == 3


def test_fetch_tags_keeps_id_name_description_and_excludes_deleted() -> None:
    class _Session:
        async def execute(self, statement: object) -> _Result:
            compiled = str(statement.compile())
            assert "tb_meta_tag.tag_id" in compiled
            assert "tb_meta_tag.description" in compiled
            assert "tb_meta_tag.is_deleted IS false" in compiled
            return _Result(
                [
                    SimpleNamespace(
                        worklog_id=101,
                        tag_id=11,
                        tag_name="정산",
                        description="정산 배치와 대사 업무",
                    ),
                    SimpleNamespace(
                        worklog_id=101,
                        tag_id=12,
                        tag_name="장애분석",
                        description=None,
                    ),
                ]
            )

    tags = asyncio.run(
        LightWorklogSourceStore()._fetch_tags(
            session=_Session(),
            worklog_id=101,
        )
    )

    assert tags == [
        LightWorklogTag(tag_id=11, tag_name="정산", description="정산 배치와 대사 업무"),
        LightWorklogTag(tag_id=12, tag_name="장애분석", description=None),
    ]


def test_fetch_direct_predecessors_uses_direct_edge_and_excludes_deleted() -> None:
    class _Session:
        async def execute(self, statement: object) -> _Result:
            compiled = str(statement.compile())
            assert "tb_worklog_dependency.worklog_id" in compiled
            assert "tb_worklog.is_deleted IS false" in compiled
            return _Result(
                [SimpleNamespace(worklog_id=101, depends_on_worklog_id=88, title="직접 선행")]
            )

    predecessors = asyncio.run(
        LightWorklogSourceStore()._fetch_direct_predecessors(
            session=_Session(),
            worklog_id=101,
        )
    )

    assert predecessors == [LightWorklogPredecessor(worklog_id=88, title="직접 선행")]
