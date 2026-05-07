---
name: ibank-worklog-review-loop
description: Run iBank worklog quality review loops over generated SQL using team-lead review, optional department-head review, and member rewrites for similarity or repeated-expression cleanup. Use when the user asks to review, polish, de-duplicate, or version up a generated worklog set.
---

# iBank Worklog Review Loop

## Required assets
- `.codex/scripts/review_ibank_worklog_similarity.py`
- `.codex/scripts/apply_worklog_sentence_revisions.py`
- `.codex/scripts/apply_worklog_expression_revisions.py`
- `.codex/scripts/generate_ibank_worklog_dependency_sql.py`
- `.codex/templates/team-lead-worklog-review-checklist.md`
- project personas under `.codex/agents/`

## Use this when
- generated worklogs need team-lead review
- department-head review must happen after lead review
- repeated wording or repeated expressions should be reduced
- predecessor / dependency links should be checked together with worklogs
- a reviewed version like `v4`, `v5`, or later should be produced

## Default workflow
1. Extract review candidates from the chosen SQL version.
2. Run lead review per team.
3. If requested, run department-head review even when the lead approved.
4. Ask member personas to write revision JSON only for `revise` candidates.
5. Apply revisions back into a copied versioned SQL file.
6. Rebuild `tb_worklog_dependency` for the revised version and check that predecessor links still make sense for:
   - same-team phase flow
   - repeated-topic chains
   - cross-team successor TF links like `101 -> 106`
7. Re-run the review gate and summarize remaining candidates.

## Dependency review focus
- avoid self-dependency
- avoid duplicated dependency pairs
- verify that repeated or revised worklogs still point to the right predecessor task
- verify that cross-team links still represent real predecessor context rather than generic topic overlap

## Output
- review packets under `.codex/reviews/<version>/`
- revised SQL version such as `.codex/sql/ibank-worklog-seed-v5.sql`
- refreshed dependency SQL such as `.codex/sql/ibank-worklog-dependency.sql`
