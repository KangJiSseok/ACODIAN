from app.model.search_model import SearchRequest, SearchResponse, SearchResult


class SearchService:
    def search(self, payload: SearchRequest) -> SearchResponse:
        items = [
            SearchResult(
                source_id="stub-worklog-1",
                score=0.0,
                content=f"시맨틱 검색 기본 설정이 완료되면 '{payload.query}' 결과가 여기에 표시됩니다.",
            )
        ]
        return SearchResponse(items=items[: payload.limit])
