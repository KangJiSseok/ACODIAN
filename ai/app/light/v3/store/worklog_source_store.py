"""LightRAG v3 업무일지 source 조회 store."""

from dataclasses import dataclass

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.store.models import Team, User, UserTeam, Worklog, WorklogDependency, WorklogTag


@dataclass(frozen=True)
class LightWorklogSourceRow:
    """LightRAG v3 입력 생성을 위해 DB에서 조회한 업무일지 원본 문맥."""

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


class LightWorklogSourceStore:
    """LightRAG v3 업무일지 source 조회 SQL을 소유한다."""

    async def fetch_worklog_source(
        self,
        session: AsyncSession,
        worklog_id: int,
    ) -> LightWorklogSourceRow | None:
        """LightRAG v3 index 입력에 필요한 업무일지와 문맥 정보를 조회한다."""
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
        return LightWorklogSourceRow(
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

    def _format_author_role(
        self,
        *,
        role_code: str,
        position_name: str | None,
        title_name: str | None,
        team_role: str | None,
        is_leader: bool | None,
    ) -> str:
        """작성자 역할을 LightRAG 문맥에 넣기 좋은 짧은 문자열로 정리한다."""
        parts = [value for value in (team_role, title_name, position_name) if value]
        if not parts:
            parts.append(role_code)
        if is_leader and not any("팀장" in part for part in parts):
            parts.append("팀장")
        return " / ".join(dict.fromkeys(parts))
