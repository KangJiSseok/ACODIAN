---
name: ibank-worklog-generator
description: Generate versioned iBank tb_worklog seed SQL from team MDs and persona files. Use when the user asks to create or regenerate worklog SQL, raise narrative variety, tune wording/tone, or produce a new version like v3, v4, or v6.
---

# iBank Worklog Generator

## Required assets
- `.codex/scripts/generate_ibank_worklog_sql.py`
- `.codex/team-md/*.md`
- `.codex/agents/user-001.toml` ~ `.codex/agents/user-035.toml`
- `.codex/scripts/generate_ibank_worklog_dependency_sql.py`
- `.codex/scripts/generate_ibank_ai_summary_update_sql.py`

## Use this when
- the user wants a fresh worklog seed SQL
- the user wants a new version tag like `v7`
- the user wants higher variety, different wording, or less repetitive output
- the user wants predecessor / dependency links generated together
- the user wants to fill or refresh `tb_worklog.ai_summary` while preserving existing worklog rows

## Default workflow
1. Choose a base variant. Prefer `v2` unless the user asks otherwise.
2. Choose a version tag like `v7`.
3. Run the worklog generator:
   - `python3 .codex/scripts/generate_ibank_worklog_sql.py --variant v2 --version-tag v7`
4. Immediately generate predecessor links for the same version:
   - `python3 .codex/scripts/generate_ibank_worklog_dependency_sql.py --worklog-sql .codex/sql/ibank-worklog-seed-v7.sql --out .codex/sql/ibank-worklog-dependency.sql --summary-out .codex/sql/ibank-worklog-dependency-summary.md`
5. Check both summaries under `.codex/sql/ibank-worklog-summary-v7.md` and `.codex/sql/ibank-worklog-dependency-summary.md`.
6. If `ai_summary` is NULL or stale, generate a separate UPDATE file instead of rewriting existing INSERT values:
   - `python3 .codex/scripts/generate_ibank_ai_summary_update_sql.py --worklog-sql .codex/sql/ibank-worklog-seed-v7.sql --out .codex/sql/ibank-worklog-ai-summary-update.sql --summary-out .codex/sql/ibank-worklog-ai-summary-update-summary.md`
   - For split seed files, pass multiple `--worklog-sql` arguments.
   - Preserve `request_content`, `work_content`, status, dates, and IDs; only set `ai_summary` and keep `ai_processing_status = 'COMPLETED'`.
7. If requested, hand off to `ibank-worklog-review-loop` or `ibank-worklog-history-sync`.

## Output
- `.codex/sql/ibank-worklog-seed-<version>.sql`
- `.codex/sql/ibank-worklog-summary-<version>.md`
- `.codex/sql/ibank-worklog-dependency.sql`
- `.codex/sql/ibank-worklog-dependency-summary.md`
- optional `.codex/sql/ibank-worklog-ai-summary-update.sql`
- optional `.codex/sql/ibank-worklog-ai-summary-update-summary.md`
