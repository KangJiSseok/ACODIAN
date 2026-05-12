"""FastAPI 앱 factory.

`docs_url` / `redoc_url` / `openapi_url` 정책:
- API 라우터는 `settings.api_prefix` (기본 ``/ai``) 아래에 mount 된다.
- 문서 페이지도 같은 prefix 아래로 통일한다 (``/ai/docs``, ``/ai/redoc``, ``/ai/openapi.json``).
  이렇게 두면 nginx 가 ``/ai/*`` 하나만 ai 컨테이너로 라우팅하면 된다.
- 운영(`environment == "production"`) 에서는 문서 엔드포인트를 비활성화해 외부 노출을 차단한다.
  운영 진단이 필요하면 인증된 내부 경로로만 임시 활성화한다.
"""

from fastapi import FastAPI

from app.config.settings import settings
from app.router import api_router


def create_app() -> FastAPI:
    is_production = settings.environment == "production"
    app = FastAPI(
        title=settings.app_name,
        version=settings.app_version,
        # 운영에서는 문서 페이지를 노출하지 않는다 (None 으로 비활성화).
        docs_url=None if is_production else f"{settings.api_prefix}/docs",
        redoc_url=None if is_production else f"{settings.api_prefix}/redoc",
        openapi_url=None if is_production else f"{settings.api_prefix}/openapi.json",
    )
    app.include_router(api_router, prefix=settings.api_prefix)

    return app


app = create_app()
