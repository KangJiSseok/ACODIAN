# iBank Worklog Enrichment Guidelines

## Goal

Generate schema-safe enrichment data from iBank worklogs:

- 1 to 5 tags for every selected worklog
- team-scoped evaluations for each user based on their work in that team
- current user skill snapshots based on all selected past-to-current work

## Source Of Truth

Use the SQL schema as the contract. Important consequences:

- tags are stored through `tb_meta_tag` and `tb_worklog_tag`
- evaluations are stored in `tb_user_evaluation`, which does not have `team_id`
- skills are stored in `tb_user_skill`, which has no history table
- evaluator authorization must be enforced by generation rules, not by DB constraints

## Tag Guidelines

Each worklog must receive 1 to 5 tags.

Good tags are reusable, search-friendly Korean noun phrases:

- business domain: `정산`, `WMS`, `KPI`, `권한`, `AI 검색`
- system area: `배치`, `API`, `JWT`, `대시보드`, `데이터마트`
- work type: `성능개선`, `장애분석`, `검증`, `리팩터링`, `운영점검`
- artifact/process: `요건정리`, `검수대응`, `릴리즈`, `회귀테스트`

Avoid:

- personal names
- team names copied directly as tags
- one-off sentence fragments
- vague tags used alone, such as `업무`, `개발`, `회의`, `확인`

Recommended density:

- 1 tag: narrow administrative or single-purpose work
- 2 to 3 tags: normal worklog
- 4 to 5 tags: cross-system work, incident work, or work with both domain and technical evidence

## Evaluation Guidelines

Create evaluations by team work unit, but store the team context in `content` because `tb_user_evaluation` has no `team_id`.

Recommended content shape:

```text
[팀 평가: <team_id>/<team_name> | 기간: YYYY-MM-DD~YYYY-MM-DD]
<evaluatee_name>님은 <핵심 역할>을 맡아 <근거가 되는 작업>을 수행했다.
강점은 <구체 강점>이며, <협업/품질/납기 근거>가 확인된다.
보완점은 <현실적인 개선 포인트>이다.
종합 판단: <다음 팀에서 기대되는 역할>.
```

Evaluator rule:

- member: own department head evaluates
- department head: user `1` evaluates
- user `1`: skip by default, because self-evaluation is invalid

For cross-department teams, do not let the owning department head evaluate every user. Each evaluatee must be evaluated by their own department head or user `1`.

## Current Skill Guidelines

Generate skills as the current state of each user, not a team-specific history.

Evidence to consider:

- repeated worklog themes
- completed and in-progress responsibility level
- incident ownership
- cross-team collaboration
- review or leadership behavior
- technical/business domain variety

Skill naming:

- Use stable names such as `정산 데이터 분석`, `API 연동 설계`, `배치 운영`, `AI 검색 품질 개선`.
- Do not overfit to a single team name or one worklog title.
- Merge near-duplicates before SQL output.

Level rubric:

- `1`: assisted/basic exposure
- `2`: routine execution
- `3`: independent delivery
- `4`: complex ownership or leadership
- `5`: organization-level authority

Default density is 3 to 7 skills per active user.

## SQL Generation Notes

Default to three separate SQL files:

- tag SQL: `tb_meta_tag`, `tb_worklog_tag`
- evaluation SQL: `tb_user_evaluation`
- skill SQL: `tb_user_skill`

If a combined SQL is explicitly requested, use this order:

1. `tb_meta_tag`
2. `tb_worklog_tag`
3. `tb_user_evaluation`
4. `tb_user_skill`

Before final output, re-review `tb_user_evaluation` for exact duplicate content and repeated stock phrases. Rewrite repeated phrasing without changing the evaluator, evaluatee, team prefix, evidence count, or authorization rule.

Validation before final output:

- every selected worklog has 1 to 5 tag links
- every tag link references an existing tag
- no duplicate `(worklog_id, tag_id)`
- every evaluator is authorized
- no self-evaluation
- every skill level is 1 to 5
- no duplicate `(user_id, skill_name)`
