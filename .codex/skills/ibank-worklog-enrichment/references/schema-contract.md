# iBank Enrichment Schema Contract

This reference captures the schema rules needed for tags, evaluations, and user skills.

## Tables

### tb_meta_tag

- `tag_id bigserial primary key`
- `tag_name varchar(100) not null unique`
- `usage_count integer not null default 0`
- timestamps: `created_at`, `updated_at`

Use one row per normalized tag name. Keep `usage_count` aligned with generated `tb_worklog_tag` links, or increment from a known existing count.

### tb_worklog_tag

- `worklog_tag_id bigserial primary key`
- `worklog_id bigint not null references tb_worklog on delete cascade`
- `tag_id bigint not null references tb_meta_tag`
- `is_ai_generated boolean not null default true`
- unique `(worklog_id, tag_id)`

Generated data must attach 1 to 5 tags per worklog.

### tb_user_evaluation

- `evaluation_id bigserial primary key`
- `evaluatee_user_id bigint not null references tb_user on delete cascade`
- `evaluator_user_id bigint not null references tb_user`
- `content text not null`
- `created_at timestamp not null default CURRENT_TIMESTAMP`

The table has no `team_id`, so team-level evaluations must include team context inside `content`.

### tb_user_skill

- `user_skill_id bigserial primary key`
- `user_id bigint not null references tb_user on delete cascade`
- `skill_name varchar(100) not null`
- `skill_level smallint not null check 1..5`
- unique `(user_id, skill_name)`
- timestamps: `created_at`, `updated_at`

This table is a current snapshot and has no history table. Generate current skills from cumulative worklog evidence.

## Authorization Model

Evaluation authorization is not enforced by database constraints, so enforce it during generation:

- user `1`: 본부장, can evaluate any user except self by default
- user `2`: 데이터컨설팅사업부 사업부장, can evaluate department `1` users
- user `3`: 솔루션사업부 사업부장, can evaluate department `2` users
- user `4`: 솔루션개발사업부 사업부장, can evaluate department `3` users

For a normal member, prefer their own department head as evaluator. For a department head, use user `1`. For user `1`, skip evaluation unless the user supplies a special evaluator rule.

## Department/User Map

- department `1`: 데이터컨설팅사업부
  - head: user `2`
  - members: users `5` through `15`
- department `2`: 솔루션사업부
  - head: user `3`
  - members: users `16` through `25`
- department `3`: 솔루션개발사업부
  - head: user `4`
  - members: users `26` through `35`
- user `1`: 본부 본부장

If live seed files disagree with this fixed map, stop and reconcile before generating evaluation rows.
