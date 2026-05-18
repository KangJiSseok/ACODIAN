from fastapi.testclient import TestClient

from app.main import create_app
from app.model.tag_merge_model import (
    GenerateTagMergeCandidatesResponse,
    TagMergeCandidateGroup,
    TagMergeCandidateTag,
)
from app.router import tag_merge
from app.store.session import get_session


class FakeTagMergeService:
    async def generate_candidates(self, session, request):
        return GenerateTagMergeCandidatesResponse(
            items=[
                TagMergeCandidateGroup(
                    mergeTargetTag=TagMergeCandidateTag(
                        tagId=1,
                        tagName="a",
                        usageCount=10,
                    ),
                    resultDescription="대표 태그 설명",
                    mergeCandidateTags=[
                        TagMergeCandidateTag(
                            tagId=2,
                            tagName="b",
                            usageCount=3,
                        )
                    ],
                )
            ]
        )


def test_generate_tag_merge_candidates_returns_response(monkeypatch) -> None:
    monkeypatch.setattr(tag_merge, "tag_merge_service", FakeTagMergeService())
    app = create_app()
    app.dependency_overrides[get_session] = lambda: None
    client = TestClient(app)

    response = client.post("/ai/tags/merge-candidates", json={})

    assert response.status_code == 200
    assert response.json()["items"][0]["mergeTargetTag"]["tagName"] == "a"
    assert response.json()["items"][0]["resultDescription"] == "대표 태그 설명"
    assert "description" not in response.json()["items"][0]["mergeTargetTag"]
