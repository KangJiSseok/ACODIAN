_SUBGRAPH_CYPHER_BY_DEPTH = {
    1: """
    MATCH (worklog:GraphRagEntity)
    WHERE worklog.worklogId IS NOT NULL
      AND (
        $search_text = ""
        OR toLower(coalesce(worklog.title, "")) CONTAINS toLower($search_text)
        OR toLower(coalesce(worklog.workContent, "")) CONTAINS toLower($search_text)
      )
      AND (
        $allowed_team_ids IS NULL
        OR worklog.teamId IN $allowed_team_ids
      )
    OPTIONAL MATCH path = (worklog)-[edge:GRAPH_RAG_EDGE*0..1]-(related:GraphRagEntity)
    WITH worklog, nodes(path) AS path_nodes, relationships(path) AS path_edges
    WHERE (
        $allowed_team_ids IS NULL
        OR all(node IN path_nodes WHERE node.worklogId IS NULL OR node.teamId IN $allowed_team_ids)
      )
    LIMIT $limit
    RETURN worklog AS root, path_nodes AS nodes, path_edges AS edges
    """,
    2: """
    MATCH (worklog:GraphRagEntity)
    WHERE worklog.worklogId IS NOT NULL
      AND (
        $search_text = ""
        OR toLower(coalesce(worklog.title, "")) CONTAINS toLower($search_text)
        OR toLower(coalesce(worklog.workContent, "")) CONTAINS toLower($search_text)
      )
      AND (
        $allowed_team_ids IS NULL
        OR worklog.teamId IN $allowed_team_ids
      )
    OPTIONAL MATCH path = (worklog)-[edge:GRAPH_RAG_EDGE*0..2]-(related:GraphRagEntity)
    WITH worklog, nodes(path) AS path_nodes, relationships(path) AS path_edges
    WHERE (
        $allowed_team_ids IS NULL
        OR all(node IN path_nodes WHERE node.worklogId IS NULL OR node.teamId IN $allowed_team_ids)
      )
    LIMIT $limit
    RETURN worklog AS root, path_nodes AS nodes, path_edges AS edges
    """,
    3: """
    MATCH (worklog:GraphRagEntity)
    WHERE worklog.worklogId IS NOT NULL
      AND (
        $search_text = ""
        OR toLower(coalesce(worklog.title, "")) CONTAINS toLower($search_text)
        OR toLower(coalesce(worklog.workContent, "")) CONTAINS toLower($search_text)
      )
      AND (
        $allowed_team_ids IS NULL
        OR worklog.teamId IN $allowed_team_ids
      )
    OPTIONAL MATCH path = (worklog)-[edge:GRAPH_RAG_EDGE*0..3]-(related:GraphRagEntity)
    WITH worklog, nodes(path) AS path_nodes, relationships(path) AS path_edges
    WHERE (
        $allowed_team_ids IS NULL
        OR all(node IN path_nodes WHERE node.worklogId IS NULL OR node.teamId IN $allowed_team_ids)
      )
    LIMIT $limit
    RETURN worklog AS root, path_nodes AS nodes, path_edges AS edges
    """,
}


def select_subgraph_cypher(max_depth: int) -> str:
    bounded_depth = min(max(max_depth, 1), max(_SUBGRAPH_CYPHER_BY_DEPTH))
    return _SUBGRAPH_CYPHER_BY_DEPTH[bounded_depth]
