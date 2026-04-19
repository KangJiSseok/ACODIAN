from fastapi import APIRouter

from app.router.health import router as health_router
from app.router.pipeline import router as pipeline_router
from app.router.search import router as search_router

api_router = APIRouter()
api_router.include_router(health_router)
api_router.include_router(pipeline_router)
api_router.include_router(search_router)
