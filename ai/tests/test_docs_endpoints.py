"""문서(Swagger / ReDoc / OpenAPI) 엔드포인트 회귀 테스트.

main.py 의 정책:
- 비-운영 환경: ``/ai/docs``, ``/ai/redoc``, ``/ai/openapi.json`` 노출
- 운영 환경     : 모두 비활성화 (None)

이 테스트는 비-운영 기본 환경에서 prefix 가 라우터와 일관되게 ``/ai/`` 아래에
노출되는지, 그리고 prefix 없는 옛 경로가 더 이상 동작하지 않는지를 검증한다.
"""

from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_swagger_ui_under_ai_prefix() -> None:
    """``/ai/docs`` 가 Swagger UI HTML 을 반환한다."""
    response = client.get("/ai/docs")
    assert response.status_code == 200
    assert "swagger" in response.text.lower()


def test_redoc_under_ai_prefix() -> None:
    """``/ai/redoc`` 가 ReDoc HTML 을 반환한다."""
    response = client.get("/ai/redoc")
    assert response.status_code == 200
    assert "redoc" in response.text.lower()


def test_openapi_json_under_ai_prefix() -> None:
    """``/ai/openapi.json`` 이 OpenAPI 스펙을 반환하고 health 가 등록되어 있다."""
    response = client.get("/ai/openapi.json")
    assert response.status_code == 200
    spec = response.json()
    assert spec["openapi"].startswith("3.")
    assert "/ai/health" in spec["paths"]


def test_legacy_root_docs_paths_are_disabled() -> None:
    """prefix 없는 기존 ``/docs`` / ``/redoc`` / ``/openapi.json`` 은 더 이상 노출되지 않는다."""
    for path in ("/docs", "/redoc", "/openapi.json"):
        response = client.get(path)
        assert response.status_code == 404, f"{path} should not be exposed"
