---
name: ibank-worklog-enrichment
description: Create iBank enrichment seed data for worklog tags, team-scoped user evaluations, and current user skills. Use when the user asks to add tb_meta_tag, tb_worklog_tag, tb_user_evaluation, or tb_user_skill rows from iBank worklogs, teams, departments, and personas.
---

# iBank Worklog Enrichment

Use this skill after teams, staffing, and worklogs already exist or are being generated.

## Required Context

Read only what the current task needs:

- `.codex/guides/ibank-worklog-enrichment-guidelines.md`
- `.codex/skills/ibank-worklog-enrichment/references/schema-contract.md`
- `.codex/agents/user-001.toml` ~ `.codex/agents/user-035.toml`
- `.codex/personas/user-persona-index.md`
- relevant team MD files under `.codex/team-md/`
- relevant worklog SQL under `.codex/sql/`

## Workflow

1. Identify the worklog source version or team scope.
2. Build a user/team/department map from `tb_user`, `tb_department`, `tb_team`, and `tb_user_team`.
3. Generate worklog tags:
   - create or reuse `tb_meta_tag`
   - attach 1 to 5 tags per worklog in `tb_worklog_tag`
   - keep `tb_meta_tag.usage_count` consistent with generated links
4. Generate team-scoped evaluations:
   - create one evaluation per evaluatee per team when there is enough worklog evidence
   - use only the evaluatee's own department head or user `1` as evaluator
   - include team ID/name, period, strengths, risks, and evidence in `content`
5. Generate current user skills:
   - infer from all selected past-to-current worklogs, not team by team
   - write current snapshot rows only for `tb_user_skill`
   - use stable skill names and levels from 1 to 5
6. Validate referential integrity and uniqueness before delivering SQL.

## Core Rules

### Tags

- Every non-deleted worklog must receive 1 to 5 tags.
- Prefer 2 to 4 tags unless the worklog clearly spans many themes.
- Use concise Korean noun phrases without `#`.
- Mix tag types: domain, system component, work type, issue/risk, process artifact.
- Avoid tags that are too broad (`업무`, `개발`, `회의`) unless paired with a specific tag.
- Avoid personal names, raw team names, and one-off phrasing that will not help semantic search.

### Evaluations

- `tb_user_evaluation` has no `team_id`; encode team context inside `content`.
- Evaluate a user only from evidence in the selected team worklogs.
- Valid evaluator choices:
  - evaluatee's department head
  - user `1` as 본부장
- Department heads are evaluated by user `1`.
- Do not create self-evaluations. If user `1` appears as evaluatee, skip by default unless the user defines a higher evaluator.
- Content must be professional Korean, specific, and evidence-based.

### Current Skills

- Treat `tb_user_skill` as a current snapshot, not history.
- Infer skills from all selected worklogs up to the current generation point.
- Do not create duplicate `(user_id, skill_name)` pairs.
- Prefer 3 to 7 skills per active user unless the user asks for a different density.
- Use skill levels:
  - `1`: assisted/basic exposure
  - `2`: can perform routine work
  - `3`: independently reliable
  - `4`: leads complex work or resolves incidents
  - `5`: organization-level expert or repeated cross-team authority

## SQL Output

Prefer three separate SQL files:

- `tb_meta_tag` + `tb_worklog_tag`
- `tb_user_evaluation`
- `tb_user_skill`

If the user asks for a combined file, use this order:

1. `tb_meta_tag`
2. `tb_worklog_tag`
3. `tb_user_evaluation`
4. `tb_user_skill`

Use stable IDs when the surrounding seed files use explicit IDs. Otherwise use insert patterns that can resolve IDs by unique natural keys.

Before finalizing `tb_user_evaluation`, run a re-review pass that checks exact duplicate contents and repeated stock phrases. Rewrite the repeated parts while preserving evaluator authorization, team prefix, evidence, and conclusion.

## Validation Checklist

- each selected worklog has 1 to 5 tag links
- `tb_worklog_tag` has no duplicate `(worklog_id, tag_id)`
- each tag exists in `tb_meta_tag`
- tag `usage_count` matches or intentionally increments linked usage
- every evaluation has `evaluatee_user_id <> evaluator_user_id`
- every evaluator is authorized by department or is user `1`
- evaluation content names the team scope because the table has no `team_id`
- every skill level is between 1 and 5
- every user/skill pair is unique
