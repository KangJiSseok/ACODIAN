from app.graph.v1.service.worklog_graph_builder import GraphContextTextUnit

GRAPH_WORKLOG_QUERY_PROMPT = """
업무일지 GraphRAG subgraph를 근거로만 답변한다.
근거 없는 추론은 하지 않는다.
""".strip()


def build_graph_worklog_answer_prompt(
    *,
    query: str,
    text_units: list[GraphContextTextUnit],
) -> str:
    context = "\n\n".join(
        f"[{text_unit.text_unit_id}]\n"
        f"sourceWorklogIds={text_unit.source_worklog_ids}\n"
        f"sourceTeamIds={text_unit.source_team_ids}\n"
        f"{text_unit.text}"
        for text_unit in text_units
    )
    return f"""
{GRAPH_WORKLOG_QUERY_PROMPT}

규칙:
- 아래 Authorized Context에 있는 업무일지 근거만 사용한다.
- 외부 지식, 일반 상식, 추측을 사용하지 않는다.
- shared Global/DRIFT/community report를 사용하지 않는다.
- 근거가 부족하면 확인 가능한 내용이 없다고 답한다.
- 한국어로 간결하게 답한다.

사용자 질문:
{query}

Authorized Context:
{context}
""".strip()
