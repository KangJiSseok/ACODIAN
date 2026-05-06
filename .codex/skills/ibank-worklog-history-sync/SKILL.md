---
name: ibank-worklog-history-sync
description: Rebuild iBank tb_worklog_status_history SQL from a selected worklog seed version and verify status/timestamp consistency. Use when a worklog seed changed, a new version was created, or the user asks to refresh, sync, or validate status history.
---

# iBank Worklog History Sync

## Required assets
- `.codex/scripts/generate_ibank_worklog_history_sql.py`
- `.codex/sql/ibank-worklog-seed*.sql`

## Use this when
- a worklog SQL version changed and history must be regenerated
- worklog dependency SQL was regenerated and the worklog/history set should stay aligned as one package
- the user asks whether status-history still matches worklogs
- timestamp or final-status consistency must be checked

## Default workflow
1. Pick the source worklog SQL, for example `.codex/sql/ibank-worklog-seed-v6.sql`.
2. Run:
   - `python3 .codex/scripts/generate_ibank_worklog_history_sql.py --worklog-sql <source> --out .codex/sql/ibank-worklog-status-history.sql --summary-out .codex/sql/ibank-worklog-status-history-summary.md`
3. Validate that:
   - every worklog has history
   - final status matches
   - last `changed_at` does not exceed `updated_at`
4. Report the transition-path counts and any remaining issues.

## Output
- `.codex/sql/ibank-worklog-status-history.sql`
- `.codex/sql/ibank-worklog-status-history-summary.md`
