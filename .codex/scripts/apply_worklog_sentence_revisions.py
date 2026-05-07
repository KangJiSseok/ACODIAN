from __future__ import annotations

import argparse
import json
from pathlib import Path


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


def join_sentences(parts: list[str]):
    return ' '.join(parts)


def main():
    parser = argparse.ArgumentParser(description='Apply sentence-level worklog revisions back into SQL.')
    parser.add_argument('--sql', required=True)
    parser.add_argument('--revisions-json', required=True, help='JSON array of revisions')
    parser.add_argument('--out', help='Output SQL path, defaults to overwrite input')
    args = parser.parse_args()

    sql_path = Path(args.sql)
    out_path = Path(args.out) if args.out else sql_path
    revisions = json.loads(Path(args.revisions_json).read_text(encoding='utf-8'))
    revision_map = {(int(item['worklog_id']), int(item['sentence_index'])): item for item in revisions}

    changed = 0
    lines = []
    for line in sql_path.read_text(encoding='utf-8').splitlines():
        if not line.startswith('  ('):
            lines.append(line)
            continue
        vals = split_sql_values(line)
        if not vals or len(vals) < 18:
            lines.append(line)
            continue
        worklog_id = int(vals[0])
        work_content = unquote(vals[5]) or ''
        sentences = split_sentences(work_content)
        dirty = False
        for idx in range(len(sentences)):
            key = (worklog_id, idx + 1)
            if key not in revision_map:
                continue
            rev = revision_map[key]
            old_sentence = rev['old_sentence'].strip()
            if sentences[idx].strip() != old_sentence:
                raise ValueError(f"Sentence mismatch for worklog {worklog_id} sentence {idx + 1}")
            sentences[idx] = rev['new_sentence'].strip()
            dirty = True
            changed += 1
        if dirty:
            vals[5] = quote(join_sentences(sentences))
            line = '  (' + ', '.join(vals) + ')' + (',' if line.rstrip().endswith(',') else ';')
        lines.append(line)

    out_path.write_text('\n'.join(lines) + '\n', encoding='utf-8')
    print(f'applied revisions: {changed}')
    print(f'output: {out_path}')


if __name__ == '__main__':
    main()
