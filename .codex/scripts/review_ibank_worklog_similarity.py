from __future__ import annotations

import argparse
import json
import re
from collections import Counter, defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
TEAM_DIR = ROOT / '.codex' / 'team-md'

TEAM_ID_RE = re.compile(r'team_id: (\d+)')
TEAM_NAME_RE = re.compile(r'team_name: (.+)')
TABLE_ROW_RE = re.compile(r'^\|\s*(\d+)\s*\|\s*([^|]+?)\s*\|\s*([^|]+?)\s*\|\s*([^|]+?)\s*\|\s*([^|]+?)\s*\|\s*([^|]+?)\s*\|\s*([^|]+?)\s*\|$', re.M)


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


def quote(v: str | None):
    if v is None:
        return 'NULL'
    return "'" + v.replace("'", "''") + "'"


def parse_team_meta():
    out = {}
    for path in sorted(TEAM_DIR.glob('1*.md')):
        text = path.read_text(encoding='utf-8')
        team_id = int(TEAM_ID_RE.search(text).group(1))
        team_name = TEAM_NAME_RE.search(text).group(1).strip()
        lead_id = None
        lead_name = None
        members = []
        for m in TABLE_ROW_RE.finditer(text):
            uid = int(m.group(1))
            name = m.group(2).strip()
            is_leader = m.group(6).strip().lower() == 'true'
            members.append({'user_id': uid, 'name': name, 'is_leader': is_leader})
            if is_leader:
                lead_id = uid
                lead_name = name
        out[team_id] = {
            'team_name': team_name,
            'lead_id': lead_id,
            'lead_name': lead_name,
            'members': members,
        }
    return out


def split_sentences(text: str):
    parts = []
    for s in text.split('. '):
        s = s.strip()
        if not s:
            continue
        if not s.endswith('.'):
            s += '.'
        parts.append(s)
    return parts


def normalize_sentence(text: str):
    text = text.lower().replace('.', '').strip()
    text = re.sub(r'\s+', ' ', text)
    return text


def parse_worklog_rows(sql_path: Path):
    rows = []
    for line in sql_path.read_text(encoding='utf-8').splitlines():
        if not line.startswith('  ('):
            continue
        vals = split_sql_values(line)
        if not vals or len(vals) < 18:
            continue
        rows.append({
            'worklog_id': int(vals[0]),
            'author_id': int(vals[1]),
            'team_id': int(vals[2]),
            'title': unquote(vals[3]),
            'request_content': unquote(vals[4]),
            'work_content': unquote(vals[5]),
        })
    return rows


def build_review_payload(sql_path: Path, min_count: int, top_per_team: int):
    team_meta = parse_team_meta()
    rows = parse_worklog_rows(sql_path)
    groups = defaultdict(lambda: defaultdict(list))
    for row in rows:
        for idx, sentence in enumerate(split_sentences(row['work_content']), start=1):
            norm = normalize_sentence(sentence)
            groups[row['team_id']][norm].append({
                'worklog_id': row['worklog_id'],
                'author_id': row['author_id'],
                'sentence_index': idx,
                'sentence': sentence,
                'title': row['title'],
            })

    teams = []
    for team_id in sorted(groups):
        repeated = []
        for norm, occs in groups[team_id].items():
            if len(occs) < min_count:
                continue
            repeated.append({
                'normalized_sentence': norm,
                'repeat_count': len(occs),
                'sentence': occs[0]['sentence'],
                'occurrences': occs,
                'author_ids': sorted({o['author_id'] for o in occs}),
                'worklog_ids': sorted({o['worklog_id'] for o in occs}),
            })
        repeated.sort(key=lambda item: (-item['repeat_count'], item['sentence']))
        repeated = repeated[:top_per_team]
        if not repeated:
            continue
        meta = team_meta[team_id]
        teams.append({
            'team_id': team_id,
            'team_name': meta['team_name'],
            'lead_id': meta['lead_id'],
            'lead_name': meta['lead_name'],
            'repeated_sentences': repeated,
        })
    return {
        'sql_path': str(sql_path),
        'min_count': min_count,
        'top_per_team': top_per_team,
        'teams': teams,
    }


def write_markdown(payload: dict, md_path: Path):
    lines = [
        '# Worklog Similarity Review Packet',
        '',
        f"- source: `{payload['sql_path']}`",
        f"- min_count: {payload['min_count']}",
        f"- top_per_team: {payload['top_per_team']}",
        '',
    ]
    for team in payload['teams']:
        lines += [
            f"## Team {team['team_id']} - {team['team_name']}",
            '',
            f"- lead: {team['lead_id']} {team['lead_name']}",
            '',
        ]
        for idx, item in enumerate(team['repeated_sentences'], start=1):
            lines += [
                f"### Repeat {idx}",
                f"- repeat_count: {item['repeat_count']}",
                f"- sentence: {item['sentence']}",
                f"- author_ids: {', '.join(map(str, item['author_ids']))}",
                f"- worklog_ids: {', '.join(map(str, item['worklog_ids']))}",
                '',
                '| worklog_id | author_id | sentence_index | title |',
                '| --- | --- | --- | --- |',
            ]
            for occ in item['occurrences'][:12]:
                lines.append(f"| {occ['worklog_id']} | {occ['author_id']} | {occ['sentence_index']} | {occ['title']} |")
            if len(item['occurrences']) > 12:
                lines.append(f'| ... | ... | ... | +{len(item["occurrences"]) - 12} more |')
            lines.append('')
    md_path.parent.mkdir(parents=True, exist_ok=True)
    md_path.write_text('\n'.join(lines) + '\n', encoding='utf-8')


def main():
    parser = argparse.ArgumentParser(description='Extract repeated worklog sentences for team-lead review.')
    parser.add_argument('--sql', required=True, help='Input worklog SQL file')
    parser.add_argument('--min-count', type=int, default=6)
    parser.add_argument('--top-per-team', type=int, default=2)
    parser.add_argument('--out-json', required=True)
    parser.add_argument('--out-md', required=True)
    args = parser.parse_args()

    payload = build_review_payload(Path(args.sql), args.min_count, args.top_per_team)
    out_json = Path(args.out_json)
    out_json.parent.mkdir(parents=True, exist_ok=True)
    out_json.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    write_markdown(payload, Path(args.out_md))
    print(f"teams flagged: {len(payload['teams'])}")
    for team in payload['teams']:
        print(f"- {team['team_id']} {team['team_name']}: {len(team['repeated_sentences'])} sentence groups")


if __name__ == '__main__':
    main()
