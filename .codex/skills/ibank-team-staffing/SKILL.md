---
name: ibank-team-staffing
description: Assign iBank team members for one team using the fixed user_id structure and persona roster. Use when the user has a team MD or team brief and wants `tb_user_team` and `tb_team_admin` planning.
---

# iBank Team Staffing

Use this skill after the team direction is already known.

## Required assets

- `.codex/personas/user-persona-index.md`
- `.codex/guides/ibank-dummy-data-preferences.md`

## Rules

- each team gets 4~9 members
- exactly one active leader
- leader should usually be a staff member from the owning department
- include 1~3 cross-department collaborators
- department head may join for oversight
- user `1` joins only key TF teams, but is included in all `team_admin`
- team creator is the owning department head and is included in `team_admin`

## Output

- recommended members by `user_id`
- leader / primary / collaborator flags
- `team_role`
- `allocation`
- `tb_team_admin` plan
