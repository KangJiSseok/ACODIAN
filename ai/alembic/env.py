"""Alembic 마이그레이션 실행 진입점.

`alembic upgrade head` 같은 명령이 호출되면 이 파일이 로드되고,
오프라인/온라인 모드에 따라 적절한 함수가 실행된다.

Alembic 이 처음인 분에게 짧게:
- Alembic 은 SQLAlchemy 의 표준 DB 마이그레이션 도구다.
- "리비전" 단위로 스키마 변경을 기록하고, 환경마다 재현 가능하게 적용한다.
- 명령은 보통 ai/ 디렉토리에서 실행한다 (alembic.ini 가 거기 있어야 함):
    alembic heads             # 현재 head 확인
    alembic upgrade head      # 미적용 마이그레이션을 모두 적용
    alembic revision -m "..." # 새 빈 리비전 생성

오프라인 모드 (`alembic upgrade --sql head` 등):
- DB 에 연결하지 않고 SQL 만 출력한다.
- 운영 DB 에 적용 전에 변경 SQL 을 미리 검토하거나, DBA 에게 SQL 만
  넘길 때 쓴다.

온라인 모드 (기본):
- 실제 DB 에 연결해 마이그레이션을 적용한다.

DB URL:
- alembic.ini 에 적힌 placeholder 가 아니라
  ``settings.sync_database_url`` (psycopg 동기 드라이버) 로 override 한다.
- 런타임 앱은 asyncpg 비동기 드라이버를 쓰지만, 마이그레이션은 동기 엔진이
  단순하고 안정적이라 분리해서 쓴다.
"""

from logging.config import fileConfig

from alembic import context
from sqlalchemy import engine_from_config, pool

from app.config.settings import settings
from app.store.base import metadata

# Alembic Config 객체. alembic.ini 의 값들이 이미 로드되어 있다.
config = context.config

# alembic.ini 의 [loggers]/[handlers]/[formatters] 설정으로 로깅 초기화.
if config.config_file_name is not None:
    fileConfig(config.config_file_name)

# autogenerate 가 비교할 대상 메타데이터.
# Base.metadata 에 등록된 모델들의 테이블 정의가 비교 기준이 된다.
# (= "코드의 모델" 과 "실제 DB 스키마" 의 차이를 자동으로 마이그레이션으로 만들 때 사용)
target_metadata = metadata


def run_migrations_offline() -> None:
    """오프라인 모드: SQL 만 출력하고 실제 DB 에는 적용하지 않는다."""
    context.configure(
        url=settings.sync_database_url,
        target_metadata=target_metadata,
        literal_binds=True,
        dialect_opts={"paramstyle": "named"},
    )
    with context.begin_transaction():
        context.run_migrations()


def run_migrations_online() -> None:
    """온라인 모드: 실제 DB 에 연결해 마이그레이션을 실행한다."""
    configuration = config.get_section(config.config_ini_section, {})
    # alembic.ini 의 placeholder URL 을 실제 DSN 으로 덮어쓴다.
    configuration["sqlalchemy.url"] = settings.sync_database_url
    connectable = engine_from_config(
        configuration,
        prefix="sqlalchemy.",
        # NullPool: 마이그레이션은 보통 한 번만 실행되므로 connection pool 이 필요 없다.
        poolclass=pool.NullPool,
    )
    with connectable.connect() as connection:
        context.configure(connection=connection, target_metadata=target_metadata)
        with context.begin_transaction():
            context.run_migrations()


if context.is_offline_mode():
    run_migrations_offline()
else:
    run_migrations_online()
