from __future__ import annotations

import argparse
import re
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SQL_DIR = ROOT / '.codex' / 'sql'
WORKLOG_SQL = SQL_DIR / 'ibank-worklog-seed-v6.sql'
OUT = SQL_DIR / 'ibank-worklog-dependency.sql'
SUMMARY = SQL_DIR / 'ibank-worklog-dependency-summary.md'

DOWNSTREAM_TEAM_FLOW = {
    106: 101,
    107: 102,
    108: 103,
    109: 104,
    110: 105,
}

DOWNSTREAM_LINK_KEYWORDS = {
    106: ['WMS', '재고', '정합성', '배치'],
    107: ['정산', '주문 취소', '포인트', '매장'],
    108: ['권한', '로그', '기준정보', '품질'],
    109: ['레거시 모듈', '검색', '임베딩', 'RAG', 'pgvector'],
    110: ['릴리즈', '운영 이관', '대시보드', '보고', 'KPI'],
}

TITLE_PHASE_SUFFIX = {
    'request': ['관련 1차 확인 요청', '이슈 접수 및 영향 범위 확인', '현상 재현 요청', '고객사 문의 접수'],
    'analyze': ['원인 분석', '영향 범위 점검', '로그/데이터 비교 분석', '원인 후보 분리'],
    'fix': ['대응안 반영', '수정 적용', '보정 작업 진행', '기준 보정 작업'],
    'verify': ['수정 후 재검증', '재현 결과 재확인', '검증 결과 정리', '재검증 결과 정리'],
    'review': ['검토 의견 정리', '승인 전 검토', '후속 조치 검토'],
    'improve': ['재발 방지안 정리', '개선안 반영', '운영 기준 보강'],
    'hold': ['보류 사유 정리', '외부 확인 대기', '추가 검증 대기'],
    'report': ['보고 내용 정리', '본부/사업부 공유안 작성', '현황 보고 정리'],
    'cancel': ['처리 범위 조정', '우선순위 변경으로 중단', '취소 및 후속 전환'],
}


def split_sql_values(line: str):
    s = line.strip().rstrip(',;')
    if not s.startswith('('):
        return None
    s = s[1:-1]
    vals, cur = [], []
    in_str = False
    i = 0
    while i < len(s):
        ch = s[i]
        if ch == "'":
            cur.append(ch)
            if in_str and i + 1 < len(s) and s[i + 1] == "'":
                cur.append("'")
                i += 2
                continue
            in_str = not in_str
            i += 1
            continue
        if ch == ',' and not in_str:
            vals.append(''.join(cur).strip())
            cur = []
            i += 1
            continue
        cur.append(ch)
        i += 1
    vals.append(''.join(cur).strip())
    return vals


def unquote(v: str | None):
    if v is None or v == 'NULL':
        return None
    if v.startswith("'") and v.endswith("'"):
        return v[1:-1].replace("''", "'")
    return v


def sql_str(v: str | None):
    if v is None:
        return 'NULL'
    return "'" + v.replace("'", "''") + "'"


def parse_title(title: str):
    for phase, suffixes in TITLE_PHASE_SUFFIX.items():
        for suffix in sorted(suffixes, key=len, reverse=True):
            if title.endswith(suffix):
                topic = title[: -len(suffix)].rstrip()
                return phase, topic
    return 'other', title


def parse_worklogs(worklog_sql: Path):
    rows = []
    for line in worklog_sql.read_text(encoding='utf-8').splitlines():
        if not line.startswith('  ('):
            continue
        vals = split_sql_values(line)
        if not vals or len(vals) < 18:
            continue
        title = unquote(vals[3]) or ''
        phase, topic = parse_title(title)
        rows.append({
            'worklog_id': int(vals[0]),
            'author_id': int(vals[1]),
            'team_id': int(vals[2]),
            'title': title,
            'request_content': unquote(vals[4]) or '',
            'work_content': unquote(vals[5]) or '',
            'status_code': unquote(vals[6]) or '',
            'instruction_date': unquote(vals[9]) or '',
            'created_at': unquote(vals[16]) or '',
            'updated_at': unquote(vals[17]) or '',
            'phase': phase,
            'topic': topic,
        })
    return rows


def choose_cross_team_predecessor(downstream_team_id: int, upstream_rows: list[dict]):
    keywords = DOWNSTREAM_LINK_KEYWORDS.get(downstream_team_id, [])
    matched = []
    for row in upstream_rows:
        hay = f"{row['title']} {row['request_content']} {row['work_content']}"
        score = sum(1 for k in keywords if k in hay)
        if score > 0:
            matched.append((score, row['status_code'] == 'COMPLETED', row['worklog_id'], row))
    if matched:
        matched.sort(key=lambda x: (x[0], x[1], x[2]), reverse=True)
        return matched[0][3]
    completed = [r for r in upstream_rows if r['status_code'] == 'COMPLETED']
    if completed:
        return sorted(completed, key=lambda r: r['worklog_id'], reverse=True)[0]
    return sorted(upstream_rows, key=lambda r: r['worklog_id'], reverse=True)[0] if upstream_rows else None


def build_dependencies(worklogs: list[dict]):
    by_team = defaultdict(list)
    for row in worklogs:
        by_team[row['team_id']].append(row)

    dependencies = []
    pair_set = set()
    internal_count = 0
    cross_count = 0

    # same-team topic chains
    by_team_topic = defaultdict(list)
    for row in worklogs:
        by_team_topic[(row['team_id'], row['topic'])].append(row)

    first_rows_by_team_topic = {}
    for key, rows in by_team_topic.items():
        rows.sort(key=lambda r: (r['instruction_date'], r['worklog_id']))
        first_rows_by_team_topic[key] = rows[0]
        prev = None
        for row in rows:
            if prev is not None:
                pair = (row['worklog_id'], prev['worklog_id'])
                if row['worklog_id'] != prev['worklog_id'] and pair not in pair_set:
                    dependencies.append({
                        'worklog_id': row['worklog_id'],
                        'depends_on_worklog_id': prev['worklog_id'],
                        'created_at': row['created_at'],
                        'dependency_type': 'same_team_topic',
                    })
                    pair_set.add(pair)
                    internal_count += 1
            prev = row

    # cross-team predecessor for the first row of each downstream topic
    for downstream_team_id, upstream_team_id in DOWNSTREAM_TEAM_FLOW.items():
        upstream_rows = by_team.get(upstream_team_id, [])
        if not upstream_rows:
            continue
        for (team_id, topic), first_row in first_rows_by_team_topic.items():
            if team_id != downstream_team_id:
                continue
            upstream = choose_cross_team_predecessor(downstream_team_id, upstream_rows)
            if not upstream:
                continue
            pair = (first_row['worklog_id'], upstream['worklog_id'])
            if first_row['worklog_id'] == upstream['worklog_id'] or pair in pair_set:
                continue
            dependencies.append({
                'worklog_id': first_row['worklog_id'],
                'depends_on_worklog_id': upstream['worklog_id'],
                'created_at': first_row['created_at'],
                'dependency_type': 'cross_team_flow',
            })
            pair_set.add(pair)
            cross_count += 1

    return dependencies, internal_count, cross_count


def build_sql(worklog_sql: Path = WORKLOG_SQL, out: Path = OUT, summary_out: Path = SUMMARY):
    worklogs = parse_worklogs(worklog_sql)
    dependencies, internal_count, cross_count = build_dependencies(worklogs)

    lines = []
    lines.append('-- iBank worklog dependency seed SQL generated from worklog seed rows')
    lines.append('BEGIN;')
    lines.append('')
    lines.append('INSERT INTO tb_worklog_dependency (worklog_id, depends_on_worklog_id, created_at) VALUES')
    vals = []
    for dep in dependencies:
        vals.append('  (' + ', '.join([
            str(dep['worklog_id']),
            str(dep['depends_on_worklog_id']),
            sql_str(dep['created_at']),
        ]) + ')')
    lines.append(',\n'.join(vals) + ';')
    lines.append('')
    lines.append("SELECT setval(pg_get_serial_sequence('tb_worklog_dependency', 'dependency_id'), (SELECT COALESCE(MAX(dependency_id), 1) FROM tb_worklog_dependency), true);")
    lines.append('')
    lines.append('COMMIT;')
    lines.append('')
    out.write_text('\n'.join(lines), encoding='utf-8')

    summary = [
        '# iBank Worklog Dependency Summary',
        '',
        f'- source worklog file: `{worklog_sql.relative_to(ROOT)}`',
        f'- output file: `{out.relative_to(ROOT)}`',
        f'- total worklogs: {len(worklogs)}',
        f'- total dependencies: {len(dependencies)}',
        f'- same-team topic links: {internal_count}',
        f'- cross-team flow links: {cross_count}',
    ]
    summary_out.write_text('\n'.join(summary) + '\n', encoding='utf-8')
    return len(worklogs), len(dependencies), internal_count, cross_count


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Generate iBank worklog dependency SQL from a worklog seed file.')
    parser.add_argument('--worklog-sql', help='Input worklog SQL path')
    parser.add_argument('--out', help='Output dependency SQL path')
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

    w, d, i, c = build_sql(worklog_sql=worklog_sql, out=out, summary_out=summary_out)
    print(f'generated dependency for {w} worklogs -> {d} rows (same-team={i}, cross-team={c})')
