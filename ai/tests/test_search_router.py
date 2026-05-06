from fastapi.testclient import TestClient

from app.main import app
from app.model.search import SemanticWorklogSearchItem, SemanticWorklogSearchResponse

client = TestClient(app)


def test_search_worklogs_contract(monkeypatch) -> None:
    from app.router import search

    async def fake_search_worklogs(request):
        assert request.keyword == "야간 배치가 겹치면서 운영이 흔들린 사례"
        assert request.team_id == 101
        assert request.status_code == "IN_PROGRESS"
        assert request.allowed_team_ids == [101, 106]
        return SemanticWorklogSearchResponse(
            items=[
                SemanticWorklogSearchItem(
                    worklogId=7,
                    score=0.91,
                    chunkIndex=0,
                    matchedChunk="배치 윈도우 충돌 문의가 들어온 직후...",
                    predecessorWorklogIds=[1],
                )
            ],
            page=1,
            pageSize=20,
            totalCount=1,
            totalPages=1,
            isFirst=True,
            isLast=True,
            hasNext=False,
            hasPrevious=False,
        )

    monkeypatch.setattr(search.search_service, "search_worklogs", fake_search_worklogs)

    response = client.post(
        "/ai/search/worklogs",
        json={
            "keyword": "야간 배치가 겹치면서 운영이 흔들린 사례",
            "teamId": 101,
            "statusCode": "IN_PROGRESS",
            "allowedTeamIds": [101, 106],
            "page": 1,
            "pageSize": 20,
        },
    )

    assert response.status_code == 200
    assert response.json() == {
        "items": [
            {
                "worklogId": 7,
                "score": 0.91,
                "chunkIndex": 0,
                "matchedChunk": "배치 윈도우 충돌 문의가 들어온 직후...",
                "predecessorWorklogIds": [1],
            }
        ],
        "page": 1,
        "pageSize": 20,
        "totalCount": 1,
        "totalPages": 1,
        "isFirst": True,
        "isLast": True,
        "hasNext": False,
        "hasPrevious": False,
    }
