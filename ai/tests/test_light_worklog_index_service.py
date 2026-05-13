import asyncio

from app.light.v3.service.worklog_document_builder import LightRagWorklogDocument
from app.light.v3.service.worklog_index_service import LightWorklogIndexService
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
    assert prepared.missing_worklog_ids == [102]
