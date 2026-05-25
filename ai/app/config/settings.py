from functools import lru_cache
from pathlib import Path

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


DEFAULT_LIGHTRAG_LLM_FALLBACK_MODELS: tuple[str, ...] = ()


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
    gemini_file_active_poll_sec: float = 2.0              # Files API 업로드 ACTIVE 폴링 주기 (초)
    gemini_file_active_timeout_sec: int = 120             # Files API ACTIVE 대기 타임아웃 (초)

    # ---- 파일 AI 요약: S3 (api 와 같은 버킷/자격증명을 공유) ----
    # api 의 ``storage.s3.*`` 와 동일 값을 주입한다. ai 는 GET (download) 만 수행한다.
    aws_region: str = ""
    aws_s3_bucket: str = ""
    aws_s3_base_prefix: str = ""
    aws_access_key_id: str = ""
    aws_secret_access_key: str = ""

    # ---- 임베딩 / 청킹 (기획서 2.4) ----
    embedding_model: str = "gemini-embedding-001"           # 768 차원 출력 모델
    embedding_dim: int = 768                              # 벡터 차원. ERD VECTOR(768) 와 *반드시* 동기화.
    chunk_threshold_chars: int = 3000                     # 이 길이 초과 시 Gemini 지능형 청킹 사용

    # ---- 외부 시스템 연동 ----
    api_base_url: str = "http://localhost:8080"           # api 서비스 호출 기본 URL
    pgvector_dsn: str = "postgresql://postgres:postgres@localhost:5432/axwms"
    redis_url: str = "redis://localhost:6379/0"           # Celery broker / 캐시
    celery_broker_url: str | None = None                  # 미지정 시 redis_url 사용
    celery_result_backend: str | None = None              # 미지정 시 redis_url 사용
    celery_task_always_eager: bool = False                # 테스트용 동기 실행 플래그
    neo4j_uri: str = "bolt://localhost:7687"              # LightRAG Neo4j graph storage
    neo4j_user: str = "neo4j"
    neo4j_password: str = "neo4j-local-password"

    # ---- LightRAG v3 업무일지 index ----
    lightrag_working_dir: str = "./data/lightrag-v3"
    lightrag_llm_model: str = "gemini-2.5-flash"
    lightrag_embedding_model: str = "gemini-embedding-001"
    lightrag_llm_fallback_models: str = ",".join(DEFAULT_LIGHTRAG_LLM_FALLBACK_MODELS)
    lightrag_query_llm_max_retries_per_model: int = 2
    lightrag_query_llm_retry_initial_delay_seconds: float = 0.5
    lightrag_query_llm_retry_max_delay_seconds: float = 4.0
    lightrag_embedding_max_token_size: int = 2048
    lightrag_index_max_batch_size: int = 10
    lightrag_insert_timeout_seconds: int = 120
    lightrag_kg_language: str = "Korean"
    lightrag_workspace: str = ""
    lightrag_vector_storage: str = "QdrantVectorDBStorage"
    lightrag_qdrant_url: str = "http://localhost:6333"
    lightrag_qdrant_api_key: str = ""
    lightrag_query_timeout_seconds: int = 120
    lightrag_query_top_k: int = 40
    lightrag_query_chunk_top_k: int = 20
    lightrag_query_response_type: str = "Multiple Paragraphs"
    worklog_detail_base_url: str = "https://k14s209.p.ssafy.io:8443/worklog/detail"
    lightrag_graph_storage: str = "Neo4JStorage"
    lightrag_neo4j_database: str = "neo4j"
    gemini_api_keys: str = ""                            # 추가 Gemini 프로젝트 키 후보. comma-separated, 실제 키는 env로만 주입한다.
    graphrag_workspace: str = "worklogs-v1"
    graphrag_index_max_batch_size: int = 10
    graphrag_insert_timeout_seconds: int = 120
    graphrag_query_timeout_seconds: int = 30
    graphrag_query_max_depth: int = 2
    graphrag_query_limit: int = 20
    graphrag_neo4j_database: str = "neo4j"

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

    @property
    def lightrag_llm_model_candidates(self) -> list[str]:
        """LightRAG query LLM primary/fallback 후보를 순서대로 반환한다.

        `LIGHTRAG_LLM_MODEL` 을 첫 후보로 두고,
        `LIGHTRAG_LLM_FALLBACK_MODELS` 의 comma-separated 값을 뒤에 붙인다.
        기본값은 downgrade fallback 없이 primary model만 사용한다.
        fallback은 운영자가 환경변수로 명시한 경우에만 escape hatch로 사용한다.
        공백과 중복은 제거해 같은 모델을 불필요하게 재호출하지 않는다.
        """
        configured_fallback_models = self.lightrag_llm_fallback_models.strip()
        fallback_models = (
            configured_fallback_models.split(",")
            if configured_fallback_models
            else DEFAULT_LIGHTRAG_LLM_FALLBACK_MODELS
        )
        raw_candidates = [self.lightrag_llm_model, *fallback_models]
        candidates: list[str] = []
        for raw_model in raw_candidates:
            model = raw_model.strip()
            if model and model not in candidates:
                candidates.append(model)
        return candidates

    @property
    def gemini_api_key_candidates(self) -> list[str]:
        """Gemini API key 후보를 순서 보존 + 중복 제거 형태로 반환한다.

        `GEMINI_API_KEY`는 하위 호환을 위한 primary key로 먼저 시도하고,
        `GEMINI_API_KEYS`의 comma-separated 후보를 뒤에 붙인다.
        빈 항목/공백/중복은 제거하며 key 원문은 로그에 남기지 않는다.
        """
        raw_candidates = [self.gemini_api_key, *self.gemini_api_keys.split(",")]
        candidates: list[str] = []
        for raw_key in raw_candidates:
            key = raw_key.strip()
            if key and key not in candidates:
                candidates.append(key)
        return candidates

    @property
    def lightrag_query_llm_retry_attempts_per_model(self) -> int:
        """LightRAG query LLM 모델별 총 시도 횟수를 반환한다."""
        return max(self.lightrag_query_llm_max_retries_per_model, 0) + 1

    @property
    def lightrag_query_llm_retry_initial_delay(self) -> float:
        """LightRAG query LLM retry initial delay를 0 이상으로 정규화한다."""
        return max(self.lightrag_query_llm_retry_initial_delay_seconds, 0.0)

    @property
    def lightrag_query_llm_retry_max_delay(self) -> float:
        """LightRAG query LLM retry max delay를 initial delay 이상으로 정규화한다."""
        return max(
            self.lightrag_query_llm_retry_max_delay_seconds,
            self.lightrag_query_llm_retry_initial_delay,
        )

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
