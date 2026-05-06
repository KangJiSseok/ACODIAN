from pathlib import Path

import yaml


def test_semantic_search_query_fixture_is_readable() -> None:
    fixture_path = Path(__file__).parent / "fixtures" / "semantic_search_queries.yaml"

    queries = yaml.safe_load(fixture_path.read_text(encoding="utf-8"))

    assert len(queries) == 10
    assert queries[0]["query"] == "야간 배치가 겹치면서 운영이 흔들린 사례"
    assert queries[0]["expected_team_ids"] == [101, 105, 106]
    assert queries[-1]["query"] == "마감일을 넘겨서 완료된 업무"
