import asyncio
from types import SimpleNamespace

import pytest
from pydantic import ValidationError

from app.graph.v1.model.worklog_query import WorklogGraphQueryRequest
from app.graph.v1.store.graph_authorized_corpus_store import (
    GraphAuthorizedWorklogCandidate,
    GraphWorklogAuthorizedCorpusStore,
)
from app.graph.v1.store.worklog_source_store import GraphWorklogPredecessor, GraphWorklogTag
from tests.graph_authorized_corpus_fixture import FilteringAuthorizedCorpusSession


class _Result:
    def __init__(self, rows: list[SimpleNamespace]) -> None:
        self._rows = rows

    def all(self) -> list[SimpleNamespace]:
        return self._rows


def test_team_ids_scope_excludes_disallowed_deleted_and_cross_team_fixture_rows() -> None:
    request = WorklogGraphQueryRequest.model_validate(
        {"query": "정산 오류", "scopeMode": "TEAM_IDS", "allowedTeamIds": [7], "limit": 10}
    )

    corpus = asyncio.run(
        GraphWorklogAuthorizedCorpusStore().fetch_authorized_corpus(
            session=FilteringAuthorizedCorpusSession(),
            request=request,
            query_embedding=[0.1, 0.2, 0.3],
        )
    )

    assert corpus.worklog_ids == {101}
    assert corpus.team_ids == {7}
    assert [candidate.worklog_id for candidate in corpus.candidates] == [101]
    assert corpus.sources[101].direct_predecessors == [
        GraphWorklogPredecessor(worklog_id=88, team_id=7, title="same team predecessor")
    ]


def test_team_ids_scope_filters_candidates_sources_and_predecessors() -> None:
    class _Session:
        def __init__(self) -> None:
            self.compiled_statements: list[str] = []

        async def execute(self, statement: object) -> _Result:
            compiled = str(statement.compile())
            self.compiled_statements.append(compiled)
            if len(self.compiled_statements) == 1:
                assert "tb_worklog.team_id IN" in compiled
                assert "tb_worklog.is_deleted IS false" in compiled
                assert "tb_team.deleted_at IS NULL" in compiled
                assert "tb_worklog_embedding.source_type = :source_type_1" in compiled
                return _Result(
                    [
                        SimpleNamespace(
                            worklog_id=101,
                            team_id=7,
                            distance=0.12,
                            chunk_index=0,
                            chunk_content="정산 배치 오류 chunk",
                        )
                    ]
                )
            if len(self.compiled_statements) == 2:
                assert "tb_worklog.worklog_id IN" in compiled
                assert "tb_worklog.team_id IN" in compiled
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
            if len(self.compiled_statements) == 3:
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
            assert "tb_worklog.team_id IN" in compiled
            assert "tb_worklog.is_deleted IS false" in compiled
            assert "tb_team.deleted_at IS NULL" in compiled
            return _Result(
                [
                    SimpleNamespace(
                        worklog_id=101,
                        depends_on_worklog_id=88,
                        team_id=7,
                        title="직접 선행",
                    )
                ]
            )

    request = WorklogGraphQueryRequest.model_validate(
        {"query": "정산 오류", "scopeMode": "TEAM_IDS", "allowedTeamIds": [7], "limit": 5}
    )

    corpus = asyncio.run(
        GraphWorklogAuthorizedCorpusStore().fetch_authorized_corpus(
            session=_Session(),
            request=request,
            query_embedding=[0.1, 0.2, 0.3],
        )
    )

    assert corpus.candidates == [
        GraphAuthorizedWorklogCandidate(
            worklog_id=101,
            score=0.88,
            chunk_index=0,
            matched_chunk="정산 배치 오류 chunk",
        )
    ]
    assert corpus.worklog_ids == {101}
    assert corpus.team_ids == {7}
    assert corpus.sources[101].tags == [
        GraphWorklogTag(tag_id=11, tag_name="정산", description="정산 배치와 대사 업무")
    ]
    assert corpus.sources[101].direct_predecessors == [
        GraphWorklogPredecessor(worklog_id=88, team_id=7, title="직접 선행")
    ]


def test_all_scope_uses_deleted_guards_without_team_filter() -> None:
    class _Session:
        def __init__(self) -> None:
            self.compiled_statements: list[str] = []

        async def execute(self, statement: object) -> _Result:
            compiled = str(statement.compile())
            self.compiled_statements.append(compiled)
            assert (
                "tb_worklog.is_deleted IS false" in compiled
                or "tb_meta_tag.is_deleted IS false" in compiled
            )
            if len(self.compiled_statements) in {1, 2, 4}:
                assert "tb_team.deleted_at IS NULL" in compiled
                assert "tb_worklog.team_id IN" not in compiled
            if len(self.compiled_statements) == 1:
                return _Result(
                    [
                        SimpleNamespace(
                            worklog_id=101,
                            team_id=7,
                            distance=0.2,
                            chunk_index=0,
                            chunk_content="정산 chunk",
                        ),
                        SimpleNamespace(
                            worklog_id=202,
                            team_id=8,
                            distance=0.3,
                            chunk_index=1,
                            chunk_content="외환 chunk",
                        ),
                    ]
                )
            if len(self.compiled_statements) == 2:
                return _Result(
                    [
                        SimpleNamespace(
                            worklog_id=101,
                            title="정산 업무",
                            request_content=None,
                            work_content="정산 수행",
                            author_id=5,
                            user_name="김도윤",
                            team_id=7,
                            team_name="정산 고도화 TF",
                        ),
                        SimpleNamespace(
                            worklog_id=202,
                            title="외환 업무",
                            request_content=None,
                            work_content="외환 수행",
                            author_id=6,
                            user_name="이서연",
                            team_id=8,
                            team_name="외환 고도화 TF",
                        ),
                    ]
                )
            return _Result([])

    request = WorklogGraphQueryRequest.model_validate(
        {"query": "전체 업무", "scopeMode": "ALL"}
    )

    corpus = asyncio.run(
        GraphWorklogAuthorizedCorpusStore().fetch_authorized_corpus(
            session=_Session(),
            request=request,
            query_embedding=[0.1, 0.2, 0.3],
        )
    )

    assert corpus.worklog_ids == {101, 202}
    assert corpus.team_ids == {7, 8}


def test_empty_team_ids_scope_is_rejected_by_request_model() -> None:
    with pytest.raises(ValidationError):
        WorklogGraphQueryRequest.model_validate(
            {"query": "정산 오류", "scopeMode": "TEAM_IDS", "allowedTeamIds": []}
        )
