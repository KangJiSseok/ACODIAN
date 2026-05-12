"""Settings 회귀 테스트.

신규 키와 헬퍼 프로퍼티가 ERD/기획서 값과 일치하는지 검증한다.
값이 흔들리면 마이그레이션·모델 사이 정합성이 깨질 수 있어 회귀 테스트로 보호한다.
"""

from app.config.settings import Settings, get_settings


def test_settings_has_embedding_defaults(monkeypatch) -> None:
    """기획서 2.4 / ERD 와 동기화된 기본값을 확인한다."""
    monkeypatch.delenv("EMBEDDING_MODEL", raising=False)
    monkeypatch.delenv("EMBEDDING_DIM", raising=False)
    monkeypatch.delenv("CHUNK_THRESHOLD_CHARS", raising=False)
    settings = Settings(_env_file=None)
    assert settings.embedding_model == "gemini-embedding-001"
    assert settings.embedding_dim == 768                 # ERD VECTOR(768) 와 동기화
    assert settings.chunk_threshold_chars == 3000        # 기획서 2.4.1 분기 기준


def test_settings_has_gemini_request_policy() -> None:
    """Gemini 호출 공통 정책 키가 정상 범위 안에 있는지 확인한다."""
    settings = get_settings()
    assert settings.gemini_request_timeout_seconds > 0
    assert settings.gemini_max_retries >= 0


def test_async_database_url_uses_asyncpg_driver() -> None:
    """런타임 비동기 URL 은 asyncpg 드라이버 prefix 를 가져야 한다."""
    settings = get_settings()
    assert settings.async_database_url.startswith("postgresql+asyncpg://")


def test_sync_database_url_uses_psycopg_driver() -> None:
    """Alembic 용 동기 URL 은 psycopg 드라이버 prefix 를 가져야 한다."""
    settings = get_settings()
    assert settings.sync_database_url.startswith("postgresql+psycopg://")
