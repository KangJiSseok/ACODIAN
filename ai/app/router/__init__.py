"""ai 모듈의 API 라우터 모음.

이 파일은 *모든* 기능 라우터를 한 곳에서 묶어 ``api_router`` 로 노출한다.
``app.main.create_app`` 이 ``api_router`` 를 ``settings.api_prefix`` (기본 ``/ai``) 아래에 mount 한다.

새 라우터를 추가하는 절차:
1. ``app/router/<feature>.py`` 에 ``router = APIRouter(...)`` 인스턴스를 정의한다.
2. 아래 import 줄과 include_router 줄의 주석을 *자기 줄만* 해제한다.
3. 다른 사람의 줄은 건드리지 않는다 (협업 충돌 회피, AGENTS.md 참고).
4. AGENTS.md 의 파일 소유권 매트릭스에도 자기 행을 추가한다.

자기 라우터를 등록하지 않으면 엔드포인트가 노출되지 않는다.
"""

from fastapi import APIRouter

from app.light.v3.router.worklog_index import router as light_worklogs_v3_router
from app.light.v3.router.worklog_query import router as light_worklogs_v3_query_router
from app.router.embedding import router as embedding_router
from app.router.file import router as file_router
from app.router.health import router as health_router
from app.router.pipeline import router as pipeline_router
from app.router.search import router as search_router
from app.router.worklog_polish import router as worklog_polish_router

# 자기 라우터 줄의 주석만 해제. 다른 줄은 건드리지 않는다 (AGENTS.md 참고).
# from app.router.tagging import router as tagging_router      # owner: 태그

api_router = APIRouter()
api_router.include_router(health_router)
api_router.include_router(pipeline_router)
api_router.include_router(embedding_router)
api_router.include_router(search_router)
api_router.include_router(worklog_polish_router)
api_router.include_router(file_router)
api_router.include_router(light_worklogs_v3_router)
api_router.include_router(light_worklogs_v3_query_router)
# api_router.include_router(tagging_router)
