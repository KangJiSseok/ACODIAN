"""SQLAlchemy ORM Base 정의.

ai 영역의 모든 ORM 모델이 상속할 단일 Base 클래스를 제공한다.

배경 (SQLAlchemy 가 처음인 분에게):
- ORM(Object-Relational Mapping) 은 DB 테이블을 파이썬 클래스로 다루는 방식이다.
- SQLAlchemy 2.0 부터는 `DeclarativeBase` 를 상속해 모델을 만든다
  (구버전의 `declarative_base()` 함수는 더 이상 권장하지 않는다).
- 모든 모델이 *같은* Base 를 상속해야 한다. Alembic 의 autogenerate 가
  "어떤 테이블이 정의되어 있는지" 를 Base 의 metadata 에서 읽어가기 때문이다.

사용 예 (실제 모델 추가 시):

    from sqlalchemy.orm import Mapped, mapped_column
    from app.store.base import Base

    class WorklogEmbedding(Base):
        __tablename__ = "tb_worklog_embedding"
        embedding_id: Mapped[int] = mapped_column(primary_key=True)
        # ...

새 모델 파일은 ai/app/store/models.py 에 둔다 (AGENTS.md 의 파일 소유권 매트릭스 참고).
"""

from sqlalchemy.orm import DeclarativeBase


class Base(DeclarativeBase):
    """ai 영역 ORM 모델의 공통 베이스.

    공통 mixin 이나 공통 컬럼이 필요해지면 이 클래스에 추가한다.
    """


# Alembic 의 env.py 가 `target_metadata` 로 사용하는 핸들.
# Base.metadata 에는 Base 를 상속한 모든 모델의 테이블 정보가 자동으로 등록된다.
metadata = Base.metadata
