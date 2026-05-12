---
name: ibank-team-md
description: Create or refine a single iBank team MD, choose the owning department, and nominate the team lead. Use when the user gives a new team idea or a partial team draft and wants a team-related markdown document.
---

# iBank Team MD

Use this skill for one team at a time.

## Inputs this skill accepts

- new team idea
- partial team draft
- team name + department + date range
- already-written MD that needs refinement

## Required assets

- `.codex/agents/ceo.toml`
- `.codex/templates/team-md-template.md`
- `.codex/guides/ibank-dummy-data-preferences.md`
- `.codex/personas/user-persona-index.md`

## What to do

1. Treat the user's draft as source of truth when present
2. Fill only the missing MD sections
3. Keep team duration within 6 months
4. Keep dates within `2024-01-01 ~ 2026-05-05`
5. Apply the iBank CEO lens:
   - does the team fit iBank?
   - which department should own it?
   - who should lead it?

## Output

- team MD markdown
- owning department
- team lead candidate
- short CEO fit verdict
