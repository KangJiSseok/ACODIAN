from __future__ import annotations

import argparse
import re
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DEFAULT_OUT = ROOT / '.codex' / 'sql' / 'ibank-worklog-ai-summary-update.sql'
DEFAULT_SUMMARY = ROOT / '.codex' / 'sql' / 'ibank-worklog-ai-summary-update-summary.md'


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


def split_sentences(text: str):
    parts = []
    for raw in re.split(r'(?<=[.!?。])\s+', text.strip()):
        s = raw.strip()
        if not s:
            continue
        if not s.endswith(('.', '!', '?', '。')):
            s += '.'
        parts.append(s)
    return parts


def compact(text: str, limit: int):
    text = re.sub(r'\s+', ' ', text).strip()
    if len(text) <= limit:
        return text
    return text[: limit - 1].rstrip() + '…'


TITLE_SUFFIXES = [
    '관련 1차 확인 요청',
    '이슈 접수 및 영향 범위 확인',
    '현상 재현 요청',
    '고객사 문의 접수',
    '원인 분석',
    '영향 범위 점검',
    '로그/데이터 비교 분석',
    '원인 후보 분리',
    '대응안 반영',
    '수정 적용',
    '보정 작업 진행',
    '기준 보정 작업',
    '수정 후 재검증',
    '재현 결과 재확인',
    '검증 결과 정리',
    '재검증 결과 정리',
    '검토 의견 정리',
    '승인 전 검토',
    '후속 조치 검토',
    '재발 방지안 정리',
    '개선안 반영',
    '운영 기준 보강',
    '보류 사유 정리',
    '외부 확인 대기',
    '추가 검증 대기',
    '보고 내용 정리',
    '본부/사업부 공유안 작성',
    '현황 보고 정리',
    '처리 범위 조정',
    '우선순위 변경으로 중단',
    '취소 및 후속 전환',
]


def infer_topic(title: str):
    for suffix in sorted(TITLE_SUFFIXES, key=len, reverse=True):
        if title.endswith(suffix):
            return title[: -len(suffix)].strip()
    return title.strip()


def has_batchim(text: str):
    for ch in reversed(text.strip()):
        code = ord(ch)
        if 0xAC00 <= code <= 0xD7A3:
            return (code - 0xAC00) % 28 != 0
    return False


def josa(text: str, pair: str):
    a, b = pair.split('/')
    return a if has_batchim(text) else b


def fix_josa(text: str, *terms: str):
    out = text
    for term in sorted({t for t in terms if t}, key=len, reverse=True):
        for a, b in [('은', '는'), ('을', '를'), ('이', '가'), ('과', '와')]:
            out = out.replace(f'{term}{a}', f'{term}{josa(term, a + "/" + b)}')
            out = out.replace(f'{term}{b}', f'{term}{josa(term, a + "/" + b)}')
    return out


def build_ai_summary(title: str, request_content: str, work_content: str, status_code: str, importance_code: str):
    topic = infer_topic(title)
    sentences = split_sentences(work_content)
    lead = sentences[0] if sentences else request_content
    tail = sentences[-1] if len(sentences) > 1 else ''
    body = compact(f'{lead} {tail}'.strip(), 210)
    return fix_josa(f'[{status_code}/{importance_code}] {title} - {body}', topic, title)


def parse_worklogs(paths: list[Path]):
    rows = []
    seen = set()
    duplicates = []
    for path in paths:
        for line in path.read_text(encoding='utf-8').splitlines():
            vals = split_sql_values(line)
            if not vals or len(vals) < 18:
                continue
            worklog_id = int(vals[0])
            if worklog_id in seen:
                duplicates.append(worklog_id)
                continue
            seen.add(worklog_id)
            rows.append({
                'worklog_id': worklog_id,
                'team_id': int(vals[2]),
                'title': unquote(vals[3]) or '',
                'request_content': unquote(vals[4]) or '',
                'work_content': unquote(vals[5]) or '',
                'status_code': unquote(vals[6]) or '',
                'importance_code': unquote(vals[7]) or '',
                'source': path,
            })
    if duplicates:
        raise ValueError(f'Duplicate worklog_id across inputs: {sorted(duplicates)[:20]}')
    return rows


def write_update_sql(rows: list[dict], out: Path, summary_out: Path, source_paths: list[Path]):
    lines = [
        '-- iBank ai_summary update SQL generated from reviewed worklog seed rows',
        '-- Preserves existing worklog content; updates ai_summary and ai_processing_status only.',
        'BEGIN;',
        '',
        'UPDATE tb_worklog AS wl',
        'SET',
        '  ai_summary = v.ai_summary,',
        "  ai_processing_status = 'COMPLETED'",
        'FROM (VALUES',
    ]
    vals = []
    for row in sorted(rows, key=lambda r: r['worklog_id']):
        summary = build_ai_summary(row['title'], row['request_content'], row['work_content'], row['status_code'], row['importance_code'])
        vals.append(f"  ({row['worklog_id']}, {row['team_id']}, {sql_str(summary)})")
    lines.append(',\n'.join(vals))
    lines += [
        ') AS v(worklog_id, team_id, ai_summary)',
        'WHERE wl.worklog_id = v.worklog_id',
        '  AND wl.team_id = v.team_id;',
        '',
        'COMMIT;',
        '',
    ]
    out.write_text('\n'.join(lines), encoding='utf-8')

    counts = Counter(row['team_id'] for row in rows)
    summary_lines = [
        '# iBank Worklog AI Summary Update Summary',
        '',
        f'- output file: `{out.relative_to(ROOT)}`',
        f'- total updates: {len(rows)}',
        '- updated columns: `ai_summary`, `ai_processing_status`',
        '',
        '## Source files',
    ]
    for path in source_paths:
        summary_lines.append(f'- `{path.relative_to(ROOT)}`')
    summary_lines += ['', '## Per-team counts']
    for team_id in sorted(counts):
        summary_lines.append(f'- {team_id}: {counts[team_id]}')
    summary_out.write_text('\n'.join(summary_lines) + '\n', encoding='utf-8')


def resolve_path(value: str | None, default: Path):
    if not value:
        return default
    path = Path(value)
    return path if path.is_absolute() else ROOT / path


def main():
    parser = argparse.ArgumentParser(description='Generate UPDATE SQL for tb_worklog.ai_summary from worklog seed SQL files.')
    parser.add_argument('--worklog-sql', action='append', required=True, help='Input worklog seed SQL. Repeat for split seed files.')
    parser.add_argument('--out', help='Output SQL path')
    parser.add_argument('--summary-out', help='Output summary markdown path')
    args = parser.parse_args()

    sources = [resolve_path(p, None) for p in args.worklog_sql]
    out = resolve_path(args.out, DEFAULT_OUT)
    summary_out = resolve_path(args.summary_out, DEFAULT_SUMMARY)
    rows = parse_worklogs(sources)
    write_update_sql(rows, out, summary_out, sources)
    print(f'wrote {out} with {len(rows)} ai_summary updates')


if __name__ == '__main__':
    main()
