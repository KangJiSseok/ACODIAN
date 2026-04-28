# 빌드 컨텍스트는 ai/ 디렉터리(`docker build ai`).
# .gitlab-ci.yml 은 `docker build -f ai/Dockerfile ai` 형태로 호출한다.
# 근거: ADR-016 (ai 자동배포 편입 — single-stage 채택 사유 본문 참조).

FROM python:3.12-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1

WORKDIR /app

# 시스템 의존성 — psycopg(C 확장), cryptography, asyncpg 등 wheel 미제공 케이스 대비.
RUN apt-get update \
 && apt-get install -y --no-install-recommends build-essential libpq-dev curl \
 && rm -rf /var/lib/apt/lists/*

COPY requirements.txt ./
RUN pip install -r requirements.txt

# 앱 소스 — 빌드 컨텍스트 = ai/ 이므로 app/ 와 alembic/ 만 복사한다.
COPY app ./app
COPY alembic ./alembic
COPY alembic.ini ./

# non-root user (ADR-016 — DO-005 와 동일 정책)
RUN groupadd -r app && useradd -r -g app -d /app -s /sbin/nologin app \
 && chown -R app:app /app
USER app

EXPOSE 8200

# uvicorn 단일 worker 로 시작. 부하 증가 시 worker/gunicorn 확장은 별 ADR.
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8200"]
