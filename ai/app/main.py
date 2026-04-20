from fastapi import FastAPI

from app.config.settings import settings
from app.router import api_router


def create_app() -> FastAPI:
    app = FastAPI(
        title=settings.app_name,
        version=settings.app_version,
        docs_url="/docs",
        redoc_url="/redoc",
    )
    app.include_router(api_router, prefix=settings.api_prefix)

    return app


app = create_app()
