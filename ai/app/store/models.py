from datetime import date, datetime
from decimal import Decimal

from pgvector.sqlalchemy import Vector
from sqlalchemy import ARRAY, BigInteger, Boolean, Date, DateTime, ForeignKey, Integer, Numeric, Text, UniqueConstraint
from sqlalchemy.orm import Mapped, mapped_column

from app.store.base import Base


class Worklog(Base):
    __tablename__ = "tb_worklog"

    worklog_id: Mapped[int] = mapped_column(BigInteger, primary_key=True)
    author_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    team_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    title: Mapped[str] = mapped_column(Text, nullable=False)
    request_content: Mapped[str | None] = mapped_column(Text)
    work_content: Mapped[str] = mapped_column(Text, nullable=False)
    status_code: Mapped[str] = mapped_column(Text, nullable=False)
    importance_code: Mapped[str] = mapped_column(Text, nullable=False)
    actual_hours: Mapped[Decimal | None] = mapped_column(Numeric(5, 1))
    instruction_date: Mapped[date | None] = mapped_column(Date)
    due_date: Mapped[date | None] = mapped_column(Date)
    completion_date: Mapped[date | None] = mapped_column(Date)
    ai_summary: Mapped[str | None] = mapped_column(Text)
    ai_summary_edited: Mapped[bool] = mapped_column(Boolean, nullable=False)
    ai_processing_status: Mapped[str] = mapped_column(Text, nullable=False)
    is_deleted: Mapped[bool] = mapped_column(Boolean, nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime, nullable=False)
    updated_at: Mapped[datetime] = mapped_column(DateTime, nullable=False)


class Team(Base):
    __tablename__ = "tb_team"

    team_id: Mapped[int] = mapped_column(BigInteger, primary_key=True)
    team_name: Mapped[str] = mapped_column(Text, nullable=False)
    status_code: Mapped[str] = mapped_column(Text, nullable=False)
    deleted_at: Mapped[datetime | None] = mapped_column(DateTime)


class User(Base):
    __tablename__ = "tb_user"

    user_id: Mapped[int] = mapped_column(BigInteger, primary_key=True)
    department_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    user_name: Mapped[str] = mapped_column(Text, nullable=False)
    role_code: Mapped[str] = mapped_column(Text, nullable=False)
    position_name: Mapped[str | None] = mapped_column(Text)
    title_name: Mapped[str | None] = mapped_column(Text)


class UserTeam(Base):
    __tablename__ = "tb_user_team"

    user_team_id: Mapped[int] = mapped_column(BigInteger, primary_key=True)
    user_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    team_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    team_role: Mapped[str] = mapped_column(Text, nullable=False)
    is_leader: Mapped[bool] = mapped_column(Boolean, nullable=False)
    status_code: Mapped[str] = mapped_column(Text, nullable=False)


class WorklogDependency(Base):
    __tablename__ = "tb_worklog_dependency"

    dependency_id: Mapped[int] = mapped_column(BigInteger, primary_key=True)
    worklog_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    depends_on_worklog_id: Mapped[int] = mapped_column(BigInteger, nullable=False)


class WorklogTag(Base):
    __tablename__ = "tb_worklog_tag"

    worklog_tag_id: Mapped[int] = mapped_column(BigInteger, primary_key=True)
    worklog_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    tag_id: Mapped[int] = mapped_column(BigInteger, nullable=False)


class WorklogEmbedding(Base):
    __tablename__ = "tb_worklog_embedding"
    __table_args__ = (UniqueConstraint("worklog_id", "chunk_index", name="uq_worklog_embedding_chunk"),)

    embedding_id: Mapped[int] = mapped_column(BigInteger, primary_key=True)
    worklog_id: Mapped[int] = mapped_column(ForeignKey("tb_worklog.worklog_id", ondelete="CASCADE"), nullable=False)
    chunk_index: Mapped[int] = mapped_column(Integer, nullable=False)
    total_chunks: Mapped[int] = mapped_column(Integer, nullable=False)
    chunk_content: Mapped[str] = mapped_column(Text, nullable=False)
    embedding: Mapped[list[float]] = mapped_column(Vector(768), nullable=False)
    author_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    team_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    department_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    status_code: Mapped[str | None] = mapped_column(Text)
    importance_code: Mapped[str | None] = mapped_column(Text)
    tag_ids: Mapped[list[int] | None] = mapped_column(ARRAY(BigInteger))
    source_type: Mapped[str] = mapped_column(Text, nullable=False, default="WORKLOG")
    created_at: Mapped[datetime] = mapped_column(DateTime, nullable=False)
