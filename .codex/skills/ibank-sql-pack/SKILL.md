---
name: ibank-sql-pack
description: Produce `.sql` seed output for iBank dummy users, teams, staffing, worklogs, and optional status history. Use when the user wants final SQL files from already-decided drafts or generated content.
---

# iBank SQL Pack

## Required assets

- `.codex/guides/ibank-dummy-data-preferences.md`

## Output order

1. `tb_user`
2. `tb_team`
3. `tb_team_admin`
4. `tb_user_team`
5. `tb_worklog`
6. `tb_worklog_dependency` when requested
7. `tb_worklog_status_history` when requested

## Rules

- keep IDs stable
- reuse the shared BCrypt hash
- preserve all already-approved drafts
- output `.sql`-ready inserts
- when dependency data exists, keep `tb_worklog` -> `tb_worklog_dependency` -> `tb_worklog_status_history` order
- support both:
  - one team at a time
  - large multi-team batch
