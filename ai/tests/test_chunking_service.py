from app.service.chunking_service import ChunkingService
from app.store.embedding_store import WorklogEmbeddingSource


def test_worklog_chunk_includes_stable_context_only() -> None:
    source = WorklogEmbeddingSource(
        worklog_id=1,
        title="임베딩 품질 저하 관련 1차 확인 요청",
        request_content="비슷한 업무를 찾기 위한 검색 품질을 확인해 달라는 요청을 받았다.",
        work_content="검색 결과 체감과 기술 조치를 같이 비교하며 임베딩 품질을 점검했다.",
        author_id=31,
        author_name="황지후",
        author_role="AI 검색 PoC 리드 / 팀장",
        team_id=109,
        team_name="AI 업무일지 검색 PoC TF",
        department_id=1,
        predecessor_titles=["레거시 모듈 분리 검증 결과 정리"],
        predecessor_worklog_ids=[245],
        tag_ids=[],
    )

    chunks = ChunkingService().create_worklog_chunks(source)

    assert len(chunks) == 1
    assert "AI 업무일지 검색 PoC TF" in chunks[0]
    assert "AI 검색 PoC 리드 / 팀장" in chunks[0]
    assert "레거시 모듈 분리 검증 결과 정리" in chunks[0]
    assert "상태:" not in chunks[0]
    assert "중요도:" not in chunks[0]


def test_long_worklog_is_split_with_context_prefix(monkeypatch) -> None:
    from app.service import chunking_service

    monkeypatch.setattr(chunking_service.settings, "chunk_threshold_chars", 140)
    source = WorklogEmbeddingSource(
        worklog_id=2,
        title="긴 업무일지",
        request_content="긴 업무를 나눠 저장한다.",
        work_content="\n".join(
            [
                "첫 번째 문단은 임베딩 검색의 기준 문맥을 설명하고 후속 검증 기준을 함께 남긴다.",
                "두 번째 문단은 별도 청크로 넘어갈 만큼 충분히 긴 내용을 담고 운영자가 다시 찾을 표현을 보존한다.",
                "세 번째 문단도 검색 가능한 근거를 유지해야 하므로 문장 경계가 깨지지 않아야 한다.",
            ]
        ),
        author_id=1,
        author_name="작성자",
        author_role="팀장",
        team_id=101,
        team_name="테스트 팀",
        department_id=1,
        predecessor_titles=[],
        predecessor_worklog_ids=[],
        tag_ids=[],
    )

    chunks = ChunkingService().create_worklog_chunks(source)

    assert len(chunks) > 1
    assert all("제목: 긴 업무일지" in chunk for chunk in chunks)
