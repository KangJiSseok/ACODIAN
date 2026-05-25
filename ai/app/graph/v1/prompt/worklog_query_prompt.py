"""GraphRAG v1 query prompt placeholder.

초기 GraphRAG v1 skeleton은 자연어 Cypher/LLM 답변 생성을 수행하지 않고,
parameterized subgraph retrieval 결과만 반환한다. LLM 답변 생성이 추가될 때 이 파일에
프롬프트 문자열만 둔다.
"""

GRAPH_WORKLOG_QUERY_PROMPT = """
업무일지 GraphRAG subgraph를 근거로만 답변한다.
근거 없는 추론은 하지 않는다.
""".strip()
