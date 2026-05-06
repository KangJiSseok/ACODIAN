from dataclasses import dataclass
from datetime import datetime
from sqlalchemy import delete, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.store.models import (
    Team,
    User,
    UserTeam,
    Worklog,
    WorklogDependency,
    WorklogEmbedding,
    WorklogTag,
)


@dataclass(frozen=True)
class WorklogEmbeddingSource:
    """임베딩 입력 텍스트를 만들기 위해 DB에서 모은 업무일지 원본 문맥."""

    worklog_id: int
    title: str
    request_content: str | None
    work_content: str
    author_id: int
    author_name: str
    author_role: str
    team_id: int
    team_name: str
    department_id: int
    predecessor_titles: list[str]
    predecessor_worklog_ids: list[int]
    tag_ids: list[int]


@dataclass(frozen=True)
class WorklogEmbeddingChunk:
    """저장 직전의 chunk와 Gemini embedding vector 묶음."""

    chunk_index: int
    chunk_content: str
    embedding: list[float]


class EmbeddingStore:
    """업무일지 임베딩 저장소 접근 계층.

    SQLAlchemy 쿼리와 `tb_worklog_embedding` 저장은 이 클래스가 소유한다.
    Service는 이 세부 SQL을 알지 못하고 use case 흐름만 조립한다.
    """

    async def fetch_worklog_source(
        self,
        session: AsyncSession,
        worklog_id: int,
    ) -> WorklogEmbeddingSource | None:
        """임베딩할 업무일지와 안정적인 문맥 정보를 조회한다.

        상태/중요도는 자주 바뀌므로 chunk 텍스트에는 넣지 않는다. 다음 검색 MR에서
        최신 `tb_worklog`와 join해 필터링한다.
        """
        row = (
            await session.execute(
                select(
                    Worklog.worklog_id,
                    Worklog.title,
                    Worklog.request_content,
                    Worklog.work_content,
                    Worklog.author_id,
                    User.user_name,
                    User.role_code,
                    User.position_name,
                    User.title_name,
                    User.department_id,
                    Team.team_id,
                    Team.team_name,
                    UserTeam.team_role,
                    UserTeam.is_leader,
                )
                .join(Team, Team.team_id == Worklog.team_id)
                .join(User, User.user_id == Worklog.author_id)
                .outerjoin(
                    UserTeam,
                    (UserTeam.user_id == Worklog.author_id)
                    & (UserTeam.team_id == Worklog.team_id),
                )
                .where(Worklog.worklog_id == worklog_id)
                .where(Worklog.is_deleted.is_(False))
                .where(Team.deleted_at.is_(None))
            )
        ).one_or_none()
        if row is None:
            return None

        predecessor_rows = (
            await session.execute(
                select(WorklogDependency.depends_on_worklog_id, Worklog.title)
                .join(Worklog, Worklog.worklog_id == WorklogDependency.depends_on_worklog_id)
                .where(WorklogDependency.worklog_id == worklog_id)
                .order_by(WorklogDependency.depends_on_worklog_id)
                .limit(3)
            )
        ).all()
        # 현재 더미 데이터는 tag 연결이 없지만, API 검색 계약에는 tagId 필터가 있으므로
        # embedding metadata에는 배열 형태로 저장할 준비를 해 둔다.
        tag_ids = (
            await session.execute(
                select(WorklogTag.tag_id)
                .where(WorklogTag.worklog_id == worklog_id)
                .order_by(WorklogTag.tag_id)
            )
        ).scalars().all()

        author_role = self._format_author_role(
            role_code=row.role_code,
            position_name=row.position_name,
            title_name=row.title_name,
            team_role=row.team_role,
            is_leader=row.is_leader,
        )
        return WorklogEmbeddingSource(
            worklog_id=row.worklog_id,
            title=row.title,
            request_content=row.request_content,
            work_content=row.work_content,
            author_id=row.author_id,
            author_name=row.user_name,
            author_role=author_role,
            team_id=row.team_id,
            team_name=row.team_name,
            department_id=row.department_id,
            predecessor_titles=[predecessor.title for predecessor in predecessor_rows],
            predecessor_worklog_ids=[
                predecessor.depends_on_worklog_id for predecessor in predecessor_rows
            ],
            tag_ids=list(tag_ids),
        )

    async def replace_worklog_embeddings(
        self,
        session: AsyncSession,
        source: WorklogEmbeddingSource,
        chunks: list[WorklogEmbeddingChunk],
    ) -> None:
        """한 업무일지의 기존 임베딩 chunk를 지우고 새 chunk로 교체한다.

        같은 worklog를 다시 임베딩할 때 중복 chunk가 남지 않도록 delete 후 insert한다.
        commit은 호출한 Service가 트랜잭션 경계에서 수행한다.
        """
        await session.execute(
            delete(WorklogEmbedding).where(WorklogEmbedding.worklog_id == source.worklog_id)
        )
        total_chunks = len(chunks)
        for chunk in chunks:
            session.add(
                WorklogEmbedding(
                    worklog_id=source.worklog_id,
                    chunk_index=chunk.chunk_index,
                    total_chunks=total_chunks,
                    chunk_content=chunk.chunk_content,
                    embedding=chunk.embedding,
                    author_id=source.author_id,
                    team_id=source.team_id,
                    department_id=source.department_id,
                    # 상태/중요도는 캐시 컬럼으로도 쓰지 않는다. 검색 시 최신 원본 테이블을 본다.
                    status_code=None,
                    importance_code=None,
                    tag_ids=source.tag_ids,
                    source_type="WORKLOG",
                    created_at=datetime.now(),
                )
            )

    def _format_author_role(
        self,
        *,
        role_code: str,
        position_name: str | None,
        title_name: str | None,
        team_role: str | None,
        is_leader: bool | None,
    ) -> str:
        """작성자 역할을 검색 문맥에 넣기 좋은 짧은 문자열로 정리한다.

        시스템 권한 코드(MEMBER 등)는 역할 설명이 전혀 없을 때만 fallback으로 사용한다.
        """
        parts = [value for value in (team_role, title_name, position_name) if value]
        if not parts:
            parts.append(role_code)
        if is_leader and not any("팀장" in part for part in parts):
            parts.append("팀장")
        return " / ".join(dict.fromkeys(parts))
