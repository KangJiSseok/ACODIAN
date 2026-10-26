# jevos-v5 한국어 라우팅 로컬 점검

- 날짜: 2026-10-26
- 실행: SHA-256 고정 `infra/jev/Dockerfile` 이미지를 로컬 Docker에서 실행하고 `python -m app.graph.v1.evaluate_router` 사용
- 모델 fingerprint: `d6c9958f24d8c5aa698625ae743cf93fadb22eba84ef7dde7580b7558e8bc94b`
- 평가셋: 관계 2건, 유사도 2건, 혼합 1건, 모호 1건 (`ai/app/graph/v1/fixtures/graph_routing_questions.json`)
- 결과: 필요한 경로 누락 0/6, 두 경로 모두 선택 5/6

작은 로컬 평가셋에서는 누락이 없었지만 두 경로를 거의 항상 실행해 라우팅 비용·속도 이득을 확인하지 못했다. `GRAPHRAG_ROUTER_MODE=shadow`를 유지한다. staging에서 실사용 한국어 질문, 답변 근거, 검색 지연, EC2 메모리를 확인한 뒤 active 적용을 판단한다.
