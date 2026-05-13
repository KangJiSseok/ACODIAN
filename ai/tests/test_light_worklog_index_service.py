import asyncio

import pytest

from app.light.v3.service.worklog_document_builder import LightRagWorklogDocument
from app.light.v3.service.lightrag_adapter import (
    LightRagConfigurationError,
    LightRagInsertFailedError,
    LightRagInsertTimeoutError,
)
from app.light.v3.service.worklog_index_service import (
    ERROR_LIGHTRAG_INSERT_FAILED,
    ERROR_LIGHTRAG_INSERT_TIMEOUT,
    ERROR_WORKLOG_NOT_FOUND,
    LightWorklogIndexService,
)
from app.light.v3.service.worklog_source_reader import LightRagWorklogSource


def make_source(worklog_id: int) -> LightRagWorklogSource:
    return LightRagWorklogSource(
        worklog_id=worklog_id,
        title=f"업무일지 {worklog_id}",
        request_content=f"요청 {worklog_id}",
        work_content=f"수행 {worklog_id}",
        author_id=10,
        author_name="이서연",
        author_role="팀원",
        team_id=20,
        team_name="AI 검색 TF",
        department_id=30,
        predecessor_titles=[],
        predecessor_worklog_ids=[],
        tag_ids=[],
    )


class FakeSourceReader:
    def __init__(self, sources_by_id: dict[int, LightRagWorklogSource]) -> None:
        self.sources_by_id = sources_by_id
        self.requested_worklog_ids: list[int] = []

    async def fetch_sources(self, worklog_ids: list[int]) -> dict[int, LightRagWorklogSource]:
        self.requested_worklog_ids = worklog_ids
        return {
            worklog_id: source
            for worklog_id, source in self.sources_by_id.items()
            if worklog_id in worklog_ids
        }


class FakeIndexAdapter:
    def __init__(self, error: Exception | None = None) -> None:
        self.error = error
        self.indexed_documents: list[LightRagWorklogDocument] = []

    async def index_documents(self, documents: list[LightRagWorklogDocument]) -> None:
        self.indexed_documents = documents
        if self.error is not None:
            raise self.error


def fake_document_builder(source: LightRagWorklogSource) -> LightRagWorklogDocument:
    return LightRagWorklogDocument(
        document_id=f"worklog-{source.worklog_id}",
        file_path=f"worklog://{source.worklog_id}",
        text=f"source_type: WORKLOG\nworklog_id: {source.worklog_id}",
    )


def test_prepare_documents_keeps_found_order_and_reports_missing_ids() -> None:
    source_101 = make_source(101)
    source_103 = make_source(103)
    reader = FakeSourceReader({101: source_101, 103: source_103})
    built_source_ids: list[int] = []

    def fake_document_builder(source: LightRagWorklogSource) -> LightRagWorklogDocument:
        built_source_ids.append(source.worklog_id)
        return LightRagWorklogDocument(
            document_id=f"worklog-{source.worklog_id}",
            file_path=f"worklog://{source.worklog_id}",
            text=f"source_type: WORKLOG\nworklog_id: {source.worklog_id}",
        )

    service = LightWorklogIndexService(
        source_reader=reader,
        document_builder=fake_document_builder,
    )

    prepared = asyncio.run(service.prepare_documents([101, 102, 103]))

    assert reader.requested_worklog_ids == [101, 102, 103]
    assert built_source_ids == [101, 103]
    assert [document.document_id for document in prepared.documents] == [
        "worklog-101",
        "worklog-103",
    ]
    assert [document.file_path for document in prepared.documents] == [
        "worklog://101",
        "worklog://103",
    ]
    assert prepared.found_worklog_ids == [101, 103]
    assert prepared.missing_worklog_ids == [102]


def test_index_worklogs_indexes_found_documents_and_preserves_response_order() -> None:
    reader = FakeSourceReader({101: make_source(101), 103: make_source(103)})
    adapter = FakeIndexAdapter()
    service = LightWorklogIndexService(
        source_reader=reader,
        document_builder=fake_document_builder,
        index_adapter=adapter,
    )

    response = asyncio.run(service.index_worklogs([101, 102, 103]))

    assert reader.requested_worklog_ids == [101, 102, 103]
    assert [document.document_id for document in adapter.indexed_documents] == [
        "worklog-101",
        "worklog-103",
    ]
    assert [document.file_path for document in adapter.indexed_documents] == [
        "worklog://101",
        "worklog://103",
    ]
    assert response.model_dump(by_alias=True) == {
        "items": [
            {"worklogId": 101, "indexed": True, "error": None},
            {"worklogId": 102, "indexed": False, "error": ERROR_WORKLOG_NOT_FOUND},
            {"worklogId": 103, "indexed": True, "error": None},
        ]
    }


def test_index_worklogs_does_not_call_adapter_when_all_worklogs_are_missing() -> None:
    reader = FakeSourceReader({})
    adapter = FakeIndexAdapter()
    service = LightWorklogIndexService(
        source_reader=reader,
        document_builder=fake_document_builder,
        index_adapter=adapter,
    )

    response = asyncio.run(service.index_worklogs([101, 102]))

    assert adapter.indexed_documents == []
    assert response.model_dump(by_alias=True) == {
        "items": [
            {"worklogId": 101, "indexed": False, "error": ERROR_WORKLOG_NOT_FOUND},
            {"worklogId": 102, "indexed": False, "error": ERROR_WORKLOG_NOT_FOUND},
        ]
    }


def test_index_worklogs_maps_timeout_to_all_found_worklogs() -> None:
    reader = FakeSourceReader({101: make_source(101), 102: make_source(102)})
    adapter = FakeIndexAdapter(LightRagInsertTimeoutError("timeout"))
    service = LightWorklogIndexService(
        source_reader=reader,
        document_builder=fake_document_builder,
        index_adapter=adapter,
    )

    response = asyncio.run(service.index_worklogs([101, 102]))

    assert response.model_dump(by_alias=True) == {
        "items": [
            {"worklogId": 101, "indexed": False, "error": ERROR_LIGHTRAG_INSERT_TIMEOUT},
            {"worklogId": 102, "indexed": False, "error": ERROR_LIGHTRAG_INSERT_TIMEOUT},
        ]
    }


def test_index_worklogs_maps_general_failure_to_found_worklogs_only() -> None:
    reader = FakeSourceReader({101: make_source(101)})
    adapter = FakeIndexAdapter(LightRagInsertFailedError("failed"))
    service = LightWorklogIndexService(
        source_reader=reader,
        document_builder=fake_document_builder,
        index_adapter=adapter,
    )

    response = asyncio.run(service.index_worklogs([101, 102]))

    assert response.model_dump(by_alias=True) == {
        "items": [
            {"worklogId": 101, "indexed": False, "error": ERROR_LIGHTRAG_INSERT_FAILED},
            {"worklogId": 102, "indexed": False, "error": ERROR_WORKLOG_NOT_FOUND},
        ]
    }


def test_index_worklogs_propagates_configuration_error_for_endpoint_level_500() -> None:
    reader = FakeSourceReader({101: make_source(101)})
    adapter = FakeIndexAdapter(LightRagConfigurationError("missing api key"))
    service = LightWorklogIndexService(
        source_reader=reader,
        document_builder=fake_document_builder,
        index_adapter=adapter,
    )

    with pytest.raises(LightRagConfigurationError):
        asyncio.run(service.index_worklogs([101]))
