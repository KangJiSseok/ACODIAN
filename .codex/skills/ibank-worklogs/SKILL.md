---
name: ibank-worklogs
description: Generate realistic Korean iBank worklogs for one or more teams using the fixed user personas and semantic-search-oriented recurrence patterns. Use when the user wants worklogs or worklog SQL.
---

# iBank Worklogs

## Required assets

- `.codex/agents/user-001.toml` ~ `.codex/agents/user-035.toml`
- `.codex/guides/ibank-dummy-data-preferences.md`

## Preconditions

- authors must be users already assigned to the team
- team period and ownership must already be known

## Generation rules

- natural Korean only
- no template-revealing language
- title / request / work content must connect naturally
- reflect each user's personality and writing habit
- intentionally mix recurrence and semantic-search patterns

## Key patterns

- repeated keywords across time and teams
- issue -> analysis -> fix -> recurrence -> improvement
- similar tasks 3 months apart
- prior-year seasonal repeats
- delayed completion
- overdue incomplete work
- cross-department collaboration
- team-lead review / department-head review / CEO report

## Output

- markdown preview or `.sql`-ready rows for `tb_worklog`
- optional predecessor/dependency rows for `tb_worklog_dependency` when requested
- optional `tb_worklog_status_history` rows when requested
