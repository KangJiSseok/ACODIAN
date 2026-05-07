---
name: ibank-dummy-data
description: Create iBank dummy teams, staffing, worklogs, reviews, and SQL files using the repo's fixed user personas, CEO review lens, and semantic-search-oriented data quality rules. Use when the user wants team MDs, team assignment, realistic Korean worklogs, or SQL seed files for iBank dummy data.
---

# iBank Dummy Data

Use this skill for the iBank dummy-data workflow already prepared in this repo.

## What this skill owns

- Team MD creation
- Team lead nomination
- `tb_team`, `tb_user_team`, `tb_team_admin` planning
- Persona-driven `tb_worklog` generation
- Team-lead review / approval comments
- Final `.sql` output for seed data

## Required local assets

Before doing any work, use these repo-local assets:

- CEO agent: `.codex/agents/ceo.toml`
- User personas: `.codex/agents/user-001.toml` ~ `.codex/agents/user-035.toml`
- Persona index: `.codex/personas/user-persona-index.md`
- Team template: `.codex/templates/team-md-template.md`
- Team lead checklist: `.codex/templates/team-lead-worklog-review-checklist.md`
- Flow guide: `.codex/guides/ibank-dummy-data-flow.md`
- Preferences: `.codex/guides/ibank-dummy-data-preferences.md`

Read only the files needed for the current step.

## Fixed business rules

- `user_id` structure is fixed:
  - `1`: 본부장
  - `2`: 데이터컨설팅사업부 사업부장
  - `3`: 솔루션사업부 사업부장
  - `4`: 솔루션개발사업부 사업부장
  - `5~15`: 데이터컨설팅사업부 사원
  - `16~25`: 솔루션사업부 사원
  - `26~35`: 솔루션개발사업부 사원
- `department_id` mapping is fixed:
  - `1`: 데이터컨설팅사업부
  - `2`: 솔루션사업부
  - `3`: 솔루션개발사업부
- `tb_user.position_name` and `tb_user.title_name` are separate concepts:
  - `position_name` must be one of `사원`, `대리`, `과장`, `차장`, `부장`, `상무`, `이사`.
  - `title_name` must be `본부장` for user `1`, `사업부장` for users `2~4`, and `팀원` for users `5~35`.
  - Never put organization titles such as `본부장` or `사업부장` in `position_name`.
  - Team leadership belongs in `tb_user_team.is_leader` and `team_role`; it does not change `title_name`.
  - For staff users `5~35`, keep ranks realistic and conservative: mostly `사원`/`대리`/`과장`, only a few `차장`, and avoid `부장` unless the user explicitly asks for a more senior organization.
- `tb_user.password_hash` uses the shared BCrypt hash from `.codex/guides/ibank-dummy-data-preferences.md`
- Excluded tables:
  - `tb_meta_tag`
  - `tb_file`
  - `tb_worklog_embedding`
  - `tb_file_embedding`

## Workflow

### 1) Team MD

When the user gives a team idea:

1. Read `.codex/templates/team-md-template.md`
2. Draft a team MD under `.codex/team-md/`
3. Apply the iBank CEO lens:
   - Does this team fit iBank?
   - Which department should own it?
   - Who should be the team lead?
4. Keep dates within the allowed range and team duration within 6 months

If the user asks for a larger batch, create multiple team MDs first before generating worklogs.

### 2) Staffing

When assigning people:

- Each team must have 4~9 users in `tb_user_team`
- Exactly one active `is_leader = true`
- Prefer a leader from the owning department's staff users
- Include 1~3 cross-department collaborators
- Department heads may be included when review/oversight is needed
- User `1` (본부장) joins only key TF teams, but is included in all `team_admin`
- The team creator is the owning department head and is included in `tb_team_admin`

Output should make role ownership obvious:
- `team_role`
- `allocation`
- `is_primary`
- `is_leader`

### 3) Worklog generation

When generating worklogs:

- Use only authors who are actually assigned in `tb_user_team`
- Respect persona differences from the user TOML files
- Write natural Korean worklogs; never reveal templates or generation rules
- Keep `title`, `request_content`, `work_content` contextually connected
- Mix these search-quality patterns intentionally:
  - repeated keywords across teams and time
  - issue -> analysis -> fix -> recurrence -> improvement
  - similar work from 3 months earlier
  - same-season comparison with the prior year
  - overdue incomplete work
  - delayed completion
  - cross-department collaboration
  - team lead review
  - department head review
  - executive reporting
- Use keywords naturally when context fits:
  - `WMS`, `정산`, `배치`, `API`, `JWT`, `권한`, `대시보드`, `KPI`, `AI 검색`, `임베딩`, `데이터 정합성`, `인프라`, `배포`

Target distributions when the user asks for a large set:

- `status_code`
  - `COMPLETED` ~50%
  - `IN_PROGRESS` ~25%
  - `PENDING` ~10%
  - `ON_HOLD` ~10%
  - `CANCELLED` ~5%
- `importance_code`
  - `NORMAL` ~50%
  - `HIGH` ~30%
  - `URGENT` ~10%
  - `LOW` ~10%

### 4) Team-lead review

If review is requested:

1. Read `.codex/templates/team-lead-worklog-review-checklist.md`
2. Use the designated team lead persona
3. Return one of:
   - approve
   - request revision
   - reject
4. The reason must mention team fit, role fit, timing, dependency, or collaboration quality

### 5) SQL output

When the user asks for SQL:

- Output `.sql`-ready inserts
- Prefer generating in this order:
  1. `tb_department` if needed
  2. `tb_user`
  3. `tb_team`
  4. `tb_team_admin`
  5. `tb_user_team`
  6. `tb_worklog`
  7. `tb_worklog_status_history` when needed
- Keep IDs stable and internally consistent
- Reuse the shared BCrypt hash for all users unless the user overrides it

## Output modes

Choose the smallest mode that matches the request:

- **Team-only**: team MD + lead suggestion
- **Staffing**: team MD + staffing plan
- **Worklogs**: team MD + staffing assumptions + worklogs
- **SQL pack**: full `.sql` seed output

## Starting from an existing draft

The user does not need to explicitly call this skill every time.

If the user already provides partial artifacts directly in chat, continue from that point:

- rough team idea
- partial team MD
- staffing draft
- worklog draft
- review request for existing worklogs

When the user provides a draft:

- preserve the provided facts as the current source of truth
- do not restart from zero
- fill only the missing layers
- state which stage you are continuing from

Continuation mapping:

- team concept draft -> continue at **Team MD**
- existing team MD -> continue at **Staffing**
- existing staffing -> continue at **Worklog generation**
- existing worklogs -> continue at **Team-lead review** or **SQL output**

## If the user gives only partial information

Make reasonable assumptions and continue when safe.

Typical safe assumptions:

- Email/phone follow a mostly consistent pattern with a small amount of variation
- Team names should sound like real TF / 운영팀 / 개선팀 names
- Most teams are `ACTIVE`; historical teams may be `INACTIVE`

Ask only if the missing information would materially change the seed design, such as:

- how many teams to generate now
- whether to output one SQL file or multiple
- whether the request is for one team or the full 10+ team batch

## Example prompts that should trigger this skill

- `10개 팀 더미 설계해줘`
- `iBank 더미 데이터 SQL 만들어줘`
- `AI 검색 TF 팀 md부터 만들고 팀원 배정해줘`
- `위 팀 기준으로 worklog 80개 SQL로 만들어줘`
- `팀장 리뷰까지 포함해서 더미 업무일지 생성해줘`
- `내가 만든 팀 md 초안 줄게, 이어서 팀원 배정해줘`
- `이 worklog 초안 검토해서 SQL로 묶어줘`
