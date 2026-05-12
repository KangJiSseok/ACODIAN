"""DB 세션/엔진 헬퍼 회귀 테스트.

실제 DB 에 연결하지 않고도 다음을 검증한다:
- 엔진/팩토리가 비동기 객체로 만들어지는지
- 같은 호출이 같은 인스턴스를 반환하는지 (싱글톤)

create_async_engine 은 lazy 동작이라 DB 가 떠 있지 않아도 객체 생성 자체는 성공한다.
실제 쿼리 시점에야 연결을 시도하므로, 이 테스트는 "객체 모양" 만 검증한다.
"""

from sqlalchemy.ext.asyncio import AsyncEngine, AsyncSession, async_sessionmaker

from app.store.session import get_engine, get_session_factory


def test_engine_is_async_and_singleton() -> None:
    """get_engine 은 AsyncEngine 을 반환하고, 두 번 호출해도 같은 인스턴스다."""
    engine_a = get_engine()
    engine_b = get_engine()
    assert isinstance(engine_a, AsyncEngine)
    assert engine_a is engine_b


def test_session_factory_returns_async_sessions() -> None:
    """팩토리는 async_sessionmaker, 거기서 만든 세션은 AsyncSession 이어야 한다."""
    factory = get_session_factory()
    assert isinstance(factory, async_sessionmaker)
    session = factory()
    assert isinstance(session, AsyncSession)
