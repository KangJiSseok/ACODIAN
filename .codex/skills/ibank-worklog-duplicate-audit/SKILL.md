---
name: ibank-worklog-duplicate-audit
description: Audit iBank worklogs for exact duplicates, same-task duplicates, or same-title suspicious pairs, then prepare team-lead judgments and minimal clarification rewrites. Use when the user suspects duplicated worklogs or wants merge, delete, or clarify candidates.
---

# iBank Worklog Duplicate Audit

## Required assets
- `.codex/sql/ibank-worklog-seed-*.sql`
- review artifacts under `.codex/reviews/`
- optional helper script: `.codex/scripts/apply_worklog_expression_revisions.py`

## Use this when
- the user says two worklogs look the same
- same-title rows may represent the same task
- the user wants `same-task / distinct / revise` judgment
- only minimal clarification edits should be applied

## Default workflow
1. Check for exact duplicate rows first.
2. If none, inspect same-team or same-title suspicious pairs.
3. Route suspicious pairs to the team lead only.
4. Record `same-task`, `distinct`, or `revise` per pair.
5. If the user wants fixes, create minimal revision JSON only for `revise` pairs.
6. Apply the minimal edits into a new version like `v6`.

## Output
- duplicate-audit packets under `.codex/reviews/*-duplicate-check/`
- optional clarified SQL version such as `.codex/sql/ibank-worklog-seed-v6.sql`
