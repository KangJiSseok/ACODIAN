from datetime import date
from typing import Literal

from pydantic import BaseModel, ConfigDict, Field

TeamStatus = Literal["ACTIVE", "INACTIVE"]
WorklogStatus = Literal["PENDING", "IN_PROGRESS", "COMPLETED", "ON_HOLD", "CANCELLED"]
WorklogImportance = Literal["URGENT", "HIGH", "NORMAL", "LOW"]


class SemanticWorklogSearchRequest(BaseModel):
    """API 서버가 넘겨줄 수 있는 시맨틱 검색 내부 요청.

    public API DTO(`SearchWorklogsApiDto.Request`)에 있는 필드만 기본으로 맞추고,
    권한 처리를 위해 API가 계산한 `allowedTeamIds`만 내부 전용 필드로 추가한다.
    """

    model_config = ConfigDict(populate_by_name=True)

    # public API DTO와 같은 camelCase 계약을 유지하기 위해 Field alias를 사용한다.
    keyword: str = Field(min_length=1, max_length=100)
    team_id: int | None = Field(default=None, alias="teamId", ge=1)
    team_status: TeamStatus | None = Field(default=None, alias="teamStatus")
    status_code: WorklogStatus | None = Field(default=None, alias="statusCode")
    importance_code: WorklogImportance | None = Field(default=None, alias="importanceCode")
    author_id: int | None = Field(default=None, alias="authorId", ge=1)
    tag_id: int | None = Field(default=None, alias="tagId", ge=1)
    created_from: date | None = Field(default=None, alias="createdFrom")
    # API 서버가 권한 계산을 끝낸 뒤 넘겨주는 내부 필드다.
    allowed_team_ids: list[int] | None = Field(default=None, alias="allowedTeamIds")
    page: int = Field(default=1, ge=1)
    page_size: int = Field(default=20, alias="pageSize", ge=1, le=100)


class SemanticWorklogSearchItem(BaseModel):
    """AI가 반환하는 ranked 검색 결과 한 행.

    화면에 바로 보여줄 DTO가 아니라, API 서버가 기존 projection으로 다시 조회할
    수 있게 `worklogId`와 랭킹 근거만 제공한다.
    """

    model_config = ConfigDict(populate_by_name=True)

    worklog_id: int = Field(alias="worklogId")
    score: float
    chunk_index: int = Field(alias="chunkIndex")
    matched_chunk: str = Field(alias="matchedChunk")
    predecessor_worklog_ids: list[int] = Field(alias="predecessorWorklogIds")


class SemanticWorklogSearchResponse(BaseModel):
    """시맨틱 검색 페이지 응답.

    API의 `PageResponse`와 비슷한 모양을 유지해 이후 API 연동 시 변환 비용을 줄인다.
    """

    model_config = ConfigDict(populate_by_name=True)

    items: list[SemanticWorklogSearchItem]
    page: int
    page_size: int = Field(alias="pageSize")
    total_count: int = Field(alias="totalCount")
    total_pages: int = Field(alias="totalPages")
    is_first: bool = Field(alias="isFirst")
    is_last: bool = Field(alias="isLast")
    has_next: bool = Field(alias="hasNext")
    has_previous: bool = Field(alias="hasPrevious")
