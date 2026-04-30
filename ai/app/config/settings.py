from functools import lru_cache
from pathlib import Path

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    app_name: str = "AX-WMS AI Service"
    app_version: str = "0.1.0"
    environment: str = "local"
    api_prefix: str = "/ai"
    host: str = "0.0.0.0"
    port: int = 8000
    log_level: str = "INFO"
    cors_origins: list[str] = Field(default_factory=lambda: ["http://localhost:3000"])

    # ---- Gemini 호출 공통 ----
    gemini_api_key: str = ""                              # 비밀값. .env 또는 환경변수로만 주입한다.
    gemini_model: str = "gemini-2.5-flash"                # 텍스트 생성 기본 모델 (요약/태그/청킹 등)
    gemini_request_timeout_seconds: int = 30              # 단일 호출 타임아웃 (초)
    gemini_max_retries: int = 3                           # 호출 실패 시 재시도 최대 횟수

    # ---- 임베딩 / 청킹 (기획서 2.4) ----
    embedding_model: str = "text-embedding-004"           # 768 차원 출력 모델
    embedding_dim: int = 768                              # 벡터 차원. ERD VECTOR(768) 와 *반드시* 동기화.
    chunk_threshold_chars: int = 3000                     # 이 길이 초과 시 Gemini 지능형 청킹 사용

    # ---- 외부 시스템 연동 ----
    api_base_url: str = "http://localhost:8080"           # api 서비스 호출 기본 URL
    pgvector_dsn: str = "postgresql://postgres:postgres@localhost:5432/axwms"
    redis_url: str = "redis://localhost:6379/0"           # Celery broker / 캐시
    celery_broker_url: str | None = None                  # 미지정 시 redis_url 사용
    celery_result_backend: str | None = None              # 미지정 시 redis_url 사용
    celery_task_always_eager: bool = False                # 테스트용 동기 실행 플래그
    neo4j_uri: str = "bolt://localhost:7687"              # 선택적 그래프 확장 (현재 미사용)
    neo4j_user: str = "neo4j"
    neo4j_password: str = "neo4j"

    model_config = SettingsConfigDict(
        # ai/.env 를 자동 로드한다 (없으면 위 기본값을 사용).
        env_file=Path(__file__).resolve().parents[2] / ".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
        extra="ignore",
    )

    @property
    def async_database_url(self) -> str:
        """런타임 비동기 SQL 접근에 쓰는 URL (asyncpg 드라이버).

        FastAPI 핸들러 / SQLAlchemy AsyncSession 이 사용한다.
        """
        return self._with_driver("asyncpg")

    @property
    def sync_database_url(self) -> str:
        """Alembic 마이그레이션이 쓰는 동기 URL (psycopg 드라이버).

        Alembic 은 동기 엔진으로 동작하는 게 단순하므로 별도 URL 을 노출한다.
        """
        return self._with_driver("psycopg")

    def _with_driver(self, driver: str) -> str:
        """``pgvector_dsn`` 에 SQLAlchemy 드라이버 prefix 를 적용한다.

        - ``postgresql://...``     -> ``postgresql+{driver}://...``
        - ``postgres://...``       -> ``postgresql+{driver}://...``
        - ``postgresql+xxx://...`` -> 그대로 (이미 명시된 드라이버를 존중)
        """
        dsn = self.pgvector_dsn
        if dsn.startswith("postgresql+"):
            return dsn
        if dsn.startswith("postgresql://"):
            return dsn.replace("postgresql://", f"postgresql+{driver}://", 1)
        if dsn.startswith("postgres://"):
            return dsn.replace("postgres://", f"postgresql+{driver}://", 1)
        return dsn


@lru_cache
def get_settings() -> Settings:
    return Settings()


settings = get_settings()
