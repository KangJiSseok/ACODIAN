"""비동기 SQLAlchemy 세션 팩토리.

ai 영역의 *런타임* DB 접근은 모두 이 모듈의 헬퍼를 통해 이루어진다.
직접 `create_async_engine` 을 부르거나 자기만의 세션을 만들지 않는다.

핵심 개념 (SQLAlchemy / 비동기 DB 가 처음인 분에게):
- AsyncEngine: DB 연결 풀과 SQL 실행 엔진. 프로세스당 하나면 충분하다 (싱글톤).
- async_sessionmaker: AsyncSession 객체를 찍어내는 팩토리. 역시 프로세스당 하나.
- AsyncSession: 한 트랜잭션/요청 단위의 세션. 매 요청마다 새로 만들고 끝나면 닫는다.

FastAPI 라우터에서는 의존성 주입으로 세션을 받는다:

    from fastapi import Depends
    from sqlalchemy.ext.asyncio import AsyncSession
    from app.store.session import get_session

    @router.get("/...")
    async def handler(session: AsyncSession = Depends(get_session)):
        ...   # 여기서 session 으로 SELECT/INSERT 등 수행

마이그레이션(Alembic) 은 이 모듈을 *거치지 않고* 동기(psycopg) 드라이버로 실행한다.
sync / async 드라이버는 settings 가 같은 DSN 을 변환해 분리 노출한다
(`settings.sync_database_url` / `settings.async_database_url`).
"""

from collections.abc import AsyncIterator

from sqlalchemy.ext.asyncio import (
    AsyncEngine,
    AsyncSession,
    async_sessionmaker,
    create_async_engine,
)

from app.config.settings import settings

# 모듈 전역 캐시. lazy 초기화 — import 시점에는 DB 연결을 시도하지 않는다.
# 첫 호출 때 한 번만 만들어지고, 이후 동일 인스턴스를 반환한다.
_engine: AsyncEngine | None = None
_session_factory: async_sessionmaker[AsyncSession] | None = None


def get_engine() -> AsyncEngine:
    """싱글톤 AsyncEngine 반환.

    `create_async_engine` 자체는 실제 연결을 만들지 않는다 (lazy).
    첫 쿼리가 실행될 때 풀에서 connection 을 꺼낸다.
    """
    global _engine
    if _engine is None:
        _engine = create_async_engine(
            settings.async_database_url,
            future=True,           # SQLAlchemy 2.0 스타일을 명시적으로 요청
            pool_pre_ping=True,    # 풀에서 꺼낸 연결이 죽어 있으면 자동으로 재연결
        )
    return _engine


def get_session_factory() -> async_sessionmaker[AsyncSession]:
    """싱글톤 async_sessionmaker 반환.

    expire_on_commit=False 의 의미:
    - 기본값 True 면 commit 직후 ORM 인스턴스의 속성이 expire 되어,
      다음 접근 시 다시 SELECT 가 발생한다.
    - FastAPI 의 응답 직렬화 도중 lazy load 가 일어나면 이미 닫힌 세션에서
      쿼리가 시도되어 에러가 난다.
    - 그래서 비동기 + FastAPI 조합에서는 보통 False 로 둔다.
    """
    global _session_factory
    if _session_factory is None:
        _session_factory = async_sessionmaker(
            get_engine(),
            expire_on_commit=False,
        )
    return _session_factory


async def get_session() -> AsyncIterator[AsyncSession]:
    """FastAPI 의존성용 세션 generator.

    사용법:
        async def handler(session: AsyncSession = Depends(get_session)):
            ...

    `async with` 블록을 빠져나오면 세션이 자동으로 닫힌다.
    commit/rollback 은 호출 측 책임이다 (서비스/라우터 레이어).
    """
    factory = get_session_factory()
    async with factory() as session:
        yield session
