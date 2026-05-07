from __future__ import annotations

import argparse
from pathlib import Path
from datetime import datetime, timedelta
import re
from collections import Counter

ROOT = Path(__file__).resolve().parents[2]
SQL_DIR = ROOT / '.codex' / 'sql'
WORKLOG_SQL = SQL_DIR / 'ibank-worklog-seed.sql'
OUT = SQL_DIR / 'ibank-worklog-status-history.sql'
SUMMARY = SQL_DIR / 'ibank-worklog-status-history-summary.md'
TEAM_DIR = ROOT / '.codex' / 'team-md'

WORKLOG_ROW_RE = re.compile(
    r"^\s*\((\d+),\s*(\d+),\s*(\d+),.+,\s*'(PENDING|IN_PROGRESS|COMPLETED|ON_HOLD|CANCELLED)',\s*'(NORMAL|HIGH|URGENT|LOW)',\s*[0-9.]+,\s*'(\d{4}-\d{2}-\d{2})',\s*'(\d{4}-\d{2}-\d{2})',\s*(NULL|'\d{4}-\d{2}-\d{2}'),\s*NULL,\s*false,\s*'COMPLETED',\s*false,\s*'(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2})',\s*'(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2})'\)[,;]?$"
)
TEAM_ID_RE = re.compile(r'team_id: (\d+)')
ROW_RE = re.compile(r'^\|\s*(\d+)\s*\|\s*([^|]+?)\s*\|\s*([^|]+?)\s*\|\s*([^|]+?)\s*\|\s*([^|]+?)\s*\|\s*([^|]+?)\s*\|\s*([^|]+?)\s*\|$', re.M)


def sql_str(v: str | None) -> str:
    if v is None:
        return 'NULL'
    return "'" + v.replace("'", "''") + "'"


def parse_team_leads():
    leads = {}
    for path in sorted(TEAM_DIR.glob('1*.md')):
        text = path.read_text(encoding='utf-8')
        tid = int(TEAM_ID_RE.search(text).group(1))
        for m in ROW_RE.finditer(text):
            uid = int(m.group(1))
            is_leader = m.group(6).strip().lower() == 'true'
            if is_leader:
                leads[tid] = uid
                break
    return leads


def parse_worklogs(worklog_sql: Path):
    rows = []
    for line in worklog_sql.read_text(encoding='utf-8').splitlines():
        m = WORKLOG_ROW_RE.match(line)
        if not m:
            continue
        rows.append({
            'worklog_id': int(m.group(1)),
            'author_id': int(m.group(2)),
            'team_id': int(m.group(3)),
            'status_code': m.group(4),
            'instruction_date': m.group(6),
            'due_date': m.group(7),
            'completion_date': None if m.group(8) == 'NULL' else m.group(8).strip("'"),
            'created_at': m.group(9),
            'updated_at': m.group(10),
        })
    return rows


def dt(s: str) -> datetime:
    if len(s) == 10:
        return datetime.strptime(s, '%Y-%m-%d')
    return datetime.strptime(s, '%Y-%m-%d %H:%M:%S')


def fmt(d: datetime) -> str:
    return d.strftime('%Y-%m-%d %H:%M:%S')


def clamp_transition(preferred: datetime, earliest: datetime, latest: datetime) -> datetime:
    if latest < earliest:
        return latest
    if preferred < earliest:
        return earliest
    if preferred > latest:
        return latest
    return preferred


def history_for_row(row, team_lead):
    wid = row['worklog_id']
    author = row['author_id']
    lead = team_lead
    final_status = row['status_code']
    created = dt(row['created_at'])
    updated = dt(row['updated_at'])
    due = dt(row['due_date']).replace(hour=17, minute=0, second=0)
    completion = dt(row['completion_date']).replace(hour=18, minute=0, second=0) if row['completion_date'] else None
    entries = []

    def add(prev_status, new_status, changed_at, changed_by, reason):
        entries.append({
            'worklog_id': wid,
            'previous_status_code': prev_status,
            'new_status_code': new_status,
            'reason': reason,
            'changed_at': fmt(changed_at),
            'changed_by': changed_by,
        })

    start_reason = '업무 요청 등록 및 초기 접수'
    progress_reason = '담당자 확인 후 진행 상태로 전환'
    complete_reason = '작업 결과 확인 후 완료 처리'
    hold_reason = '외부 확인 또는 추가 검증 대기로 보류'
    cancel_reason = '우선순위 조정 또는 범위 변경으로 취소'

    if final_status == 'PENDING':
        add(None, 'PENDING', created, author, start_reason)
    elif final_status == 'IN_PROGRESS':
        add(None, 'PENDING', created, author, start_reason)
        progress_at = clamp_transition(created + timedelta(hours=4), created, updated)
        add('PENDING', 'IN_PROGRESS', progress_at, lead, progress_reason)
    elif final_status == 'COMPLETED':
        add(None, 'PENDING', created, author, start_reason)
        inprog_at = clamp_transition(updated - timedelta(hours=2), created, updated)
        add('PENDING', 'IN_PROGRESS', inprog_at, lead, progress_reason)
        done_at = completion if completion else updated
        if done_at < inprog_at:
            done_at = inprog_at + timedelta(hours=2)
        if done_at > updated:
            done_at = updated
        add('IN_PROGRESS', 'COMPLETED', done_at, author if author != lead else lead, complete_reason)
    elif final_status == 'ON_HOLD':
        add(None, 'PENDING', created, author, start_reason)
        inprog_at = clamp_transition(updated - timedelta(hours=1), created, updated)
        add('PENDING', 'IN_PROGRESS', inprog_at, lead, progress_reason)
        hold_at = clamp_transition(due, inprog_at, updated)
        add('IN_PROGRESS', 'ON_HOLD', hold_at, lead, hold_reason)
    elif final_status == 'CANCELLED':
        add(None, 'PENDING', created, author, start_reason)
        if (updated - created) > timedelta(hours=8):
            inprog_at = clamp_transition(created + timedelta(hours=3), created, updated)
            add('PENDING', 'IN_PROGRESS', inprog_at, lead, progress_reason)
            cancel_at = updated if updated >= inprog_at else inprog_at
            add('IN_PROGRESS', 'CANCELLED', cancel_at, lead, cancel_reason)
        else:
            cancel_at = updated if updated >= created else created
            add('PENDING', 'CANCELLED', cancel_at, lead, cancel_reason)
    return entries


def build(worklog_sql: Path = WORKLOG_SQL, out: Path = OUT, summary_out: Path = SUMMARY):
    leads = parse_team_leads()
    worklogs = parse_worklogs(worklog_sql)
    history_rows = []
    for row in worklogs:
        history_rows.extend(history_for_row(row, leads[row['team_id']]))

    lines = []
    lines.append('-- iBank worklog status history seed SQL generated from worklog seed rows')
    lines.append('BEGIN;')
    lines.append('')
    lines.append('INSERT INTO tb_worklog_status_history (worklog_id, previous_status_code, new_status_code, reason, changed_at, changed_by) VALUES')
    vals = []
    for h in history_rows:
        vals.append('  (' + ', '.join([
            str(h['worklog_id']),
            sql_str(h['previous_status_code']) if h['previous_status_code'] is not None else 'NULL',
            sql_str(h['new_status_code']),
            sql_str(h['reason']),
            sql_str(h['changed_at']),
            str(h['changed_by']),
        ]) + ')')
    lines.append(',\n'.join(vals) + ';')
    lines.append('')
    lines.append("SELECT setval(pg_get_serial_sequence('tb_worklog_status_history', 'history_id'), (SELECT COALESCE(MAX(history_id), 1) FROM tb_worklog_status_history), true);")
    lines.append('')
    lines.append('COMMIT;')
    lines.append('')
    out.write_text('\n'.join(lines), encoding='utf-8')

    counts = Counter(h['new_status_code'] for h in history_rows)
    summary = [
        '# iBank Worklog Status History Summary',
        '',
        f'- total history rows: {len(history_rows)}',
        f'- source worklogs: {len(worklogs)}',
        f'- source worklog file: `{worklog_sql.relative_to(ROOT)}`',
        f'- output file: `{out.relative_to(ROOT)}`',
        '',
        '## Transition counts',
    ]
    for k in ['PENDING', 'IN_PROGRESS', 'COMPLETED', 'ON_HOLD', 'CANCELLED']:
        summary.append(f'- {k}: {counts.get(k, 0)}')
    summary_out.write_text('\n'.join(summary) + '\n', encoding='utf-8')
    return len(worklogs), len(history_rows)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Generate iBank worklog status history SQL from a worklog seed file.')
    parser.add_argument('--worklog-sql', help='Input worklog SQL path')
    parser.add_argument('--out', help='Output history SQL path')
    parser.add_argument('--summary-out', help='Output summary markdown path')
    args = parser.parse_args()

    worklog_sql = Path(args.worklog_sql) if args.worklog_sql else WORKLOG_SQL
    out = Path(args.out) if args.out else OUT
    summary_out = Path(args.summary_out) if args.summary_out else SUMMARY
    if not worklog_sql.is_absolute():
        worklog_sql = ROOT / worklog_sql
    if not out.is_absolute():
        out = ROOT / out
    if not summary_out.is_absolute():
        summary_out = ROOT / summary_out

    w, h = build(worklog_sql=worklog_sql, out=out, summary_out=summary_out)
    print(f'generated history for {w} worklogs -> {h} rows')
