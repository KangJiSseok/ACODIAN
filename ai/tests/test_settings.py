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


def test_settings_has_lightrag_kg_language_default(monkeypatch) -> None:
    """LightRAG KG 추출 언어 기본값이 한국어 지시로 고정되는지 확인한다."""
    monkeypatch.delenv("LIGHTRAG_KG_LANGUAGE", raising=False)
    settings = Settings(_env_file=None)
    assert settings.lightrag_kg_language == "Korean"


def test_settings_accepts_lightrag_kg_language_env_override(monkeypatch) -> None:
    """운영 환경에서 LightRAG KG 추출 언어를 명시 override할 수 있어야 한다."""
    monkeypatch.setenv("LIGHTRAG_KG_LANGUAGE", "한국어")
    settings = Settings(_env_file=None)
    assert settings.lightrag_kg_language == "한국어"


def test_settings_has_lightrag_qdrant_defaults(monkeypatch) -> None:
    """LightRAG v3 vector storage는 기본적으로 로컬 Qdrant를 바라본다."""
    monkeypatch.delenv("LIGHTRAG_VECTOR_STORAGE", raising=False)
    monkeypatch.delenv("LIGHTRAG_QDRANT_URL", raising=False)
    monkeypatch.delenv("LIGHTRAG_QDRANT_API_KEY", raising=False)
    settings = Settings(_env_file=None)
    assert settings.lightrag_vector_storage == "QdrantVectorDBStorage"
    assert settings.lightrag_qdrant_url == "http://localhost:6333"
    assert settings.lightrag_qdrant_api_key == ""


def test_settings_accepts_lightrag_qdrant_env_override(monkeypatch) -> None:
    """환경별 Qdrant URL/API key를 Settings로 주입할 수 있어야 한다."""
    monkeypatch.setenv("LIGHTRAG_VECTOR_STORAGE", "QdrantVectorDBStorage")
    monkeypatch.setenv("LIGHTRAG_QDRANT_URL", "http://qdrant:6333")
    monkeypatch.setenv("LIGHTRAG_QDRANT_API_KEY", "fake-key")
    settings = Settings(_env_file=None)
    assert settings.lightrag_vector_storage == "QdrantVectorDBStorage"
    assert settings.lightrag_qdrant_url == "http://qdrant:6333"
    assert settings.lightrag_qdrant_api_key == "fake-key"


def test_async_database_url_uses_asyncpg_driver() -> None:
    """런타임 비동기 URL 은 asyncpg 드라이버 prefix 를 가져야 한다."""
    settings = get_settings()
    assert settings.async_database_url.startswith("postgresql+asyncpg://")


def test_sync_database_url_uses_psycopg_driver() -> None:
    """Alembic 용 동기 URL 은 psycopg 드라이버 prefix 를 가져야 한다."""
    settings = get_settings()
    assert settings.sync_database_url.startswith("postgresql+psycopg://")


def test_settings_has_lightrag_qdrant_defaults(monkeypatch) -> None:
    """LightRAG Qdrant 연동 기본값이 정의되어 있는지 확인한다."""
    monkeypatch.delenv("LIGHTRAG_VECTOR_STORAGE", raising=False)
    monkeypatch.delenv("LIGHTRAG_QDRANT_URL", raising=False)
    monkeypatch.delenv("LIGHTRAG_QDRANT_API_KEY", raising=False)

    settings = Settings(_env_file=None)
    assert settings.lightrag_vector_storage == "QdrantVectorDBStorage"
    assert settings.lightrag_qdrant_url == "http://localhost:6333"
    assert settings.lightrag_qdrant_api_key == ""


def test_settings_accepts_lightrag_qdrant_env_override(monkeypatch) -> None:
    """운영 환경 env로 Qdrant URL/키를 override할 수 있어야 한다."""
    monkeypatch.setenv("LIGHTRAG_VECTOR_STORAGE", "QdrantVectorDBStorage")
    monkeypatch.setenv("LIGHTRAG_QDRANT_URL", "https://qdrant.example.com:6333")
    monkeypatch.setenv("LIGHTRAG_QDRANT_API_KEY", "secret")

    settings = Settings(_env_file=None)
    assert settings.lightrag_vector_storage == "QdrantVectorDBStorage"
    assert settings.lightrag_qdrant_url == "https://qdrant.example.com:6333"
    assert settings.lightrag_qdrant_api_key == "secret"


def test_settings_has_lightrag_neo4j_defaults(monkeypatch) -> None:
    """LightRAG graph storage는 기본적으로 로컬 Neo4j를 바라본다."""
    monkeypatch.delenv("NEO4J_URI", raising=False)
    monkeypatch.delenv("NEO4J_USER", raising=False)
    monkeypatch.delenv("NEO4J_PASSWORD", raising=False)
    monkeypatch.delenv("LIGHTRAG_GRAPH_STORAGE", raising=False)
    monkeypatch.delenv("LIGHTRAG_NEO4J_DATABASE", raising=False)
    settings = Settings(_env_file=None)

    assert settings.neo4j_uri == "bolt://localhost:7687"
    assert settings.neo4j_user == "neo4j"
    assert settings.neo4j_password == "neo4j-local-password"
    assert settings.lightrag_graph_storage == "Neo4JStorage"
    assert settings.lightrag_neo4j_database == "neo4j"


def test_settings_accepts_lightrag_neo4j_env_override(monkeypatch) -> None:
    """운영 환경 env로 LightRAG Neo4j graph storage 값을 override할 수 있어야 한다."""
    monkeypatch.setenv("LIGHTRAG_GRAPH_STORAGE", "Neo4JStorage")
    monkeypatch.setenv("LIGHTRAG_NEO4J_DATABASE", "neo4j")
    settings = Settings(_env_file=None)

    assert settings.lightrag_graph_storage == "Neo4JStorage"
    assert settings.lightrag_neo4j_database == "neo4j"


def test_settings_has_lightrag_query_defaults(monkeypatch) -> None:
    """LightRAG query API 기본 운영값을 제공해야 한다."""
    monkeypatch.delenv("LIGHTRAG_QUERY_TIMEOUT_SECONDS", raising=False)
    monkeypatch.delenv("LIGHTRAG_QUERY_TOP_K", raising=False)
    monkeypatch.delenv("LIGHTRAG_QUERY_CHUNK_TOP_K", raising=False)
    monkeypatch.delenv("LIGHTRAG_QUERY_RESPONSE_TYPE", raising=False)
    monkeypatch.delenv("LIGHTRAG_WORKSPACE", raising=False)

    settings = Settings(_env_file=None)

    assert settings.lightrag_workspace == ""
    assert settings.lightrag_query_timeout_seconds == 120
    assert settings.lightrag_query_top_k == 40
    assert settings.lightrag_query_chunk_top_k == 20
    assert settings.lightrag_query_response_type == "Multiple Paragraphs"


def test_settings_accepts_lightrag_query_env_override(monkeypatch) -> None:
    """LightRAG query API 운영값을 env로 override할 수 있어야 한다."""
    monkeypatch.setenv("LIGHTRAG_QUERY_TIMEOUT_SECONDS", "15")
    monkeypatch.setenv("LIGHTRAG_QUERY_TOP_K", "7")
    monkeypatch.setenv("LIGHTRAG_QUERY_CHUNK_TOP_K", "3")
    monkeypatch.setenv("LIGHTRAG_QUERY_RESPONSE_TYPE", "Single Paragraph")
    monkeypatch.setenv("LIGHTRAG_WORKSPACE", "axwms-smoke")

    settings = Settings(_env_file=None)

    assert settings.lightrag_workspace == "axwms-smoke"
    assert settings.lightrag_query_timeout_seconds == 15
    assert settings.lightrag_query_top_k == 7
    assert settings.lightrag_query_chunk_top_k == 3
    assert settings.lightrag_query_response_type == "Single Paragraph"
