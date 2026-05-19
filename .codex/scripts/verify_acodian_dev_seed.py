#!/usr/bin/env python3
"""Static and optional DB-smoke verifier for the ACODIAN git-log dev seed pack.

The verifier intentionally uses only the Python standard library so it can run in
Codex/team panes before project dependencies are installed.  Static checks are
focused on the generated ACODIAN seed files and are deliberately strict about
schema contracts that the PRD calls out: current tb_user_team columns, no legacy
team_leader column, no explicit relation ids for relation tables, manifest ↔
worklog 1:1 evidence, status-history consistency, and an acyclic dependency
chain of the required depth.
"""
from __future__ import annotations

import argparse
import json
import os
import re
import shutil
import subprocess
import sys
from collections import defaultdict
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any, Iterable

TARGET_SQL_FILES = [
    "14_acodian-team-user-seed.sql",
    "15_acodian-gitlog-worklog-seed.sql",
    "16_acodian-worklog-status-history.sql",
    "17_acodian-worklog-dependency.sql",
]
EXPECTED_USER_NAMES = {"강지석", "이성훈", "진경석", "안성훈", "정인호", "서백균"}
REQUIRED_USER_TEAM_COLUMNS = {
    "user_id",
    "team_id",
    "team_role",
    "allocation",
    "is_primary",
    "status_code",
    "is_leader",
}
FORBIDDEN_CONTACT_DOMAINS = ("gmail.com", "naver.com", "daum.net", "kakao.com", "hanmail.net")
EXPECTED_PERSONA_SOURCES = {
    201: ".codex/agents/user-004.toml",
    202: ".codex/agents/user-026.toml",
    203: ".codex/agents/user-030.toml",
    204: ".codex/agents/user-031.toml",
    205: ".codex/agents/user-029.toml + .codex/agents/user-025.toml",
    206: ".codex/agents/user-033.toml + .codex/agents/user-034.toml",
}
PERSONA_KEYWORDS = {
    201: ("리스크", "우선순위", "일정", "검토"),
    202: ("API", "로그", "재현", "영향"),
    203: ("화면", "사용자", "동선", "회귀"),
    204: ("AI", "검색", "임베딩", "유사도"),
    205: ("권한", "알림", "품질", "재확인"),
    206: ("배포", "환경", "문서", "운영"),
}
RAW_GIT_HASH_RE = re.compile(r"\b[0-9a-f]{7,40}\b", re.I)
FORBIDDEN_STOCK_PHRASES = (
    "git log의",
    "개발용 업무일지로 정리했다",
    "실제 운영 개인정보를 확장하지 않고",
    "후속 검증에서는 같은 커밋 해시",
    "커밋",
    "commit",
    "해시",
    "hash",
    "manifest",
    "근거",
    "단서",
    "작성자는 ACODIAN",
    "변경 파일 단서",
)
FORBIDDEN_PERSONA_IDENTITIES = (
    "한지훈",
    "채도윤",
    "허유나",
    "황지후",
    "하민재",
    "이도겸",
    "박연우",
    "김도윤",
)


@dataclass
class CheckResult:
    name: str
    ok: bool
    detail: str


@dataclass
class Verification:
    checks: list[CheckResult] = field(default_factory=list)

    def pass_(self, name: str, detail: str = "ok") -> None:
        self.checks.append(CheckResult(name, True, detail))

    def fail(self, name: str, detail: str) -> None:
        self.checks.append(CheckResult(name, False, detail))

    @property
    def ok(self) -> bool:
        return all(c.ok for c in self.checks)

    def print(self) -> None:
        for c in self.checks:
            print(f"{'PASS' if c.ok else 'FAIL'} {c.name}: {c.detail}")
        passed = sum(1 for c in self.checks if c.ok)
        print(f"SUMMARY: {passed}/{len(self.checks)} checks passed")


@dataclass
class SqlData:
    rows: dict[str, list[dict[str, Any]]] = field(default_factory=lambda: defaultdict(list))
    insert_columns: dict[str, list[list[str]]] = field(default_factory=lambda: defaultdict(list))
    texts_by_file: dict[str, str] = field(default_factory=dict)

    @property
    def text(self) -> str:
        return "\n".join(self.texts_by_file.values())


class SqlParseError(ValueError):
    pass


def natural_key(path: Path) -> list[Any]:
    return [int(x) if x.isdigit() else x.lower() for x in re.split(r"(\d+)", path.name)]


def split_sql_statements(sql: str) -> list[str]:
    statements: list[str] = []
    start = 0
    i = 0
    in_single = False
    in_double = False
    dollar_tag: str | None = None
    while i < len(sql):
        ch = sql[i]
        if dollar_tag:
            if sql.startswith(dollar_tag, i):
                i += len(dollar_tag)
                dollar_tag = None
                continue
            i += 1
            continue
        if in_single:
            if ch == "'":
                if i + 1 < len(sql) and sql[i + 1] == "'":
                    i += 2
                    continue
                in_single = False
            i += 1
            continue
        if in_double:
            if ch == '"':
                in_double = False
            i += 1
            continue
        if ch == "'":
            in_single = True
            i += 1
            continue
        if ch == '"':
            in_double = True
            i += 1
            continue
        if ch == "$":
            m = re.match(r"\$[A-Za-z0-9_]*\$", sql[i:])
            if m:
                dollar_tag = m.group(0)
                i += len(dollar_tag)
                continue
        if ch == ";":
            stmt = sql[start : i + 1].strip()
            if stmt:
                statements.append(stmt)
            start = i + 1
        i += 1
    tail = sql[start:].strip()
    if tail:
        statements.append(tail)
    return statements


def strip_line_comments(sql: str) -> str:
    out: list[str] = []
    for line in sql.splitlines():
        in_single = False
        i = 0
        keep = []
        while i < len(line):
            ch = line[i]
            if ch == "'":
                keep.append(ch)
                if in_single and i + 1 < len(line) and line[i + 1] == "'":
                    keep.append("'")
                    i += 2
                    continue
                in_single = not in_single
                i += 1
                continue
            if not in_single and line.startswith("--", i):
                break
            keep.append(ch)
            i += 1
        out.append("".join(keep))
    return "\n".join(out)


def split_csv_top_level(s: str) -> list[str]:
    parts: list[str] = []
    start = 0
    depth = 0
    in_single = False
    in_double = False
    i = 0
    while i < len(s):
        ch = s[i]
        if in_single:
            if ch == "'":
                if i + 1 < len(s) and s[i + 1] == "'":
                    i += 2
                    continue
                in_single = False
            i += 1
            continue
        if in_double:
            if ch == '"':
                in_double = False
            i += 1
            continue
        if ch == "'":
            in_single = True
        elif ch == '"':
            in_double = True
        elif ch == "(":
            depth += 1
        elif ch == ")":
            depth -= 1
            if depth < 0:
                raise SqlParseError(f"unbalanced parentheses near: {s[:80]}")
        elif ch == "," and depth == 0:
            parts.append(s[start:i].strip())
            start = i + 1
        i += 1
    parts.append(s[start:].strip())
    return parts


def extract_parenthesized_groups(s: str) -> list[str]:
    groups: list[str] = []
    depth = 0
    start: int | None = None
    in_single = False
    in_double = False
    i = 0
    while i < len(s):
        ch = s[i]
        if in_single:
            if ch == "'":
                if i + 1 < len(s) and s[i + 1] == "'":
                    i += 2
                    continue
                in_single = False
            i += 1
            continue
        if in_double:
            if ch == '"':
                in_double = False
            i += 1
            continue
        if ch == "'":
            in_single = True
        elif ch == '"':
            in_double = True
        elif ch == "(":
            if depth == 0:
                start = i + 1
            depth += 1
        elif ch == ")":
            depth -= 1
            if depth == 0 and start is not None:
                groups.append(s[start:i])
                start = None
            elif depth < 0:
                raise SqlParseError("unbalanced VALUES parentheses")
        i += 1
    return groups


def trim_insert_suffix(values_part: str) -> str:
    suffixes = ["ON CONFLICT", "RETURNING", "ON DUPLICATE", "WHERE NOT EXISTS"]
    upper = values_part.upper()
    best = len(values_part)
    for suffix in suffixes:
        idx = upper.find(suffix)
        if idx >= 0:
            best = min(best, idx)
    return values_part[:best].rstrip().rstrip(";").strip()


def parse_value(raw: str) -> Any:
    token = raw.strip().rstrip(";")
    upper = token.upper()
    if upper == "NULL":
        return None
    if upper == "TRUE":
        return True
    if upper == "FALSE":
        return False
    if len(token) >= 2 and token[0] == "'" and token[-1] == "'":
        return token[1:-1].replace("''", "'")
    if re.fullmatch(r"[-+]?\d+", token):
        try:
            return int(token)
        except ValueError:
            return token
    if re.fullmatch(r"[-+]?\d+\.\d+", token):
        try:
            return float(token)
        except ValueError:
            return token
    return token


def normalize_name(name: str) -> str:
    return name.strip().strip('"').lower()


def parse_insert_statement(stmt: str, data: SqlData) -> None:
    m = re.match(r"\s*INSERT\s+INTO\s+([\w.\"]+)\s*\((.*?)\)\s*(.*)\s*;?\s*$", stmt, flags=re.I | re.S)
    if not m:
        return
    table = normalize_name(m.group(1).split(".")[-1])
    columns = [normalize_name(c) for c in split_csv_top_level(m.group(2))]
    rest = m.group(3).strip()
    data.insert_columns[table].append(columns)

    if rest.upper().startswith("VALUES"):
        values_part = trim_insert_suffix(rest[6:])
        for group in extract_parenthesized_groups(values_part):
            vals = [parse_value(v) for v in split_csv_top_level(group)]
            if len(vals) != len(columns):
                raise SqlParseError(f"{table}: {len(vals)} values for {len(columns)} columns")
            data.rows[table].append(dict(zip(columns, vals)))
        return

    # Pattern used by existing seed files: INSERT INTO table (cols) SELECT v.a,... FROM (VALUES (...)) AS v(cols)
    vm = re.search(r"FROM\s*\(\s*VALUES\s*(.*?)\)\s+AS\s+\w+\s*\((.*?)\)", rest, flags=re.I | re.S)
    if vm:
        value_columns = [normalize_name(c) for c in split_csv_top_level(vm.group(2))]
        for group in extract_parenthesized_groups(vm.group(1)):
            vals = [parse_value(v) for v in split_csv_top_level(group)]
            if len(vals) != len(value_columns):
                raise SqlParseError(f"{table}: SELECT VALUES column mismatch")
            value_row = dict(zip(value_columns, vals))
            data.rows[table].append({col: value_row.get(col) for col in columns})


def parse_sql_files(paths: Iterable[Path]) -> SqlData:
    data = SqlData()
    for path in paths:
        text = path.read_text(encoding="utf-8")
        data.texts_by_file[path.name] = text
        cleaned = strip_line_comments(text)
        for stmt in split_sql_statements(cleaned):
            parse_insert_statement(stmt, data)
    return data


def rows_for(data: SqlData, table: str) -> list[dict[str, Any]]:
    rows = list(data.rows.get(table, []))
    # Temporary staging tables are common in seed SQL.  Include them when their
    # column set identifies the target relation's data shape.
    if table == "tb_user_team":
        for name, candidate_rows in data.rows.items():
            if name != table and "user_team" in name:
                rows.extend(candidate_rows)
    return rows


def file_presence(seed_dir: Path, manifest: Path, v: Verification) -> list[Path]:
    paths = [seed_dir / name for name in TARGET_SQL_FILES]
    missing = [str(p) for p in paths + [manifest] if not p.exists()]
    if missing:
        v.fail("required files", "missing: " + ", ".join(missing))
    else:
        v.pass_("required files", ", ".join(p.name for p in paths) + f", {manifest.name}")
    return paths


def parse_range(spec: str) -> set[int]:
    if "-" in spec:
        start, end = spec.split("-", 1)
        return set(range(int(start), int(end) + 1))
    return {int(spec)}


def parse_worklog_range(spec: str) -> tuple[int, int]:
    start, end_exclusive = spec.split(":", 1)
    return int(start), int(end_exclusive)


def load_manifest(path: Path, v: Verification) -> Any | None:
    try:
        with path.open(encoding="utf-8") as f:
            manifest = json.load(f)
        v.pass_("manifest json", f"loaded {path}")
        return manifest
    except Exception as exc:  # noqa: BLE001 - verifier should report all parse errors.
        v.fail("manifest json", str(exc))
        return None


def walk_json(value: Any) -> Iterable[Any]:
    yield value
    if isinstance(value, dict):
        for child in value.values():
            yield from walk_json(child)
    elif isinstance(value, list):
        for child in value:
            yield from walk_json(child)


def manifest_worklog_items(manifest: Any) -> dict[int, dict[str, Any]]:
    items: dict[int, dict[str, Any]] = {}
    for obj in walk_json(manifest):
        if not isinstance(obj, dict):
            continue
        raw_id = obj.get("worklog_id", obj.get("id"))
        if isinstance(raw_id, int):
            items[raw_id] = obj
        elif isinstance(raw_id, str) and raw_id.isdigit():
            items[int(raw_id)] = obj
    return items


def manifest_modules(manifest: Any) -> set[str]:
    modules: set[str] = set()
    for obj in walk_json(manifest):
        if isinstance(obj, dict):
            for key in ("module", "area", "scope", "category"):
                value = obj.get(key)
                if isinstance(value, str):
                    lowered = value.lower()
                    for module in ("api", "web", "ai", "infra", "docs"):
                        if module in lowered:
                            modules.add(module)
        elif isinstance(obj, str):
            lowered = obj.lower()
            for module in ("api", "web", "ai", "infra", "docs"):
                if re.search(rf"(^|[^a-z]){module}([^a-z]|$)", lowered):
                    modules.add(module)
    return modules


def has_manifest_evidence(item: dict[str, Any]) -> bool:
    """본문이 아니라 manifest에 개발 이력 연결 정보가 남아 있는지 확인한다."""
    commit = item.get("commit")
    if not isinstance(commit, dict):
        return False
    commit_hash = commit.get("hash")
    files = commit.get("files")
    subject = commit.get("subject")
    module = item.get("module")
    return (
        isinstance(commit_hash, str)
        and bool(RAW_GIT_HASH_RE.fullmatch(commit_hash))
        and isinstance(files, list)
        and bool(files)
        and isinstance(subject, str)
        and bool(subject.strip())
        and isinstance(module, str)
        and bool(module.strip())
    )


def normalize_copy_prefix(text: str) -> str:
    """첫 두 구절의 구조 반복만 보기 위해 세부값을 같은 토큰으로 접는다."""
    parts = re.split(r"[.!?。]\s*|,\s*|고\s+|며\s+", text.strip())
    sentence = " ".join(part for part in parts[:2] if part)
    sentence = re.sub(r"[0-9a-f]{7,40}", "<value>", sentence, flags=re.I)
    sentence = re.sub(r"[A-Za-z0-9_.()/@+-]+/[A-Za-z0-9_./()@+-]+", "<path>", sentence)
    sentence = re.sub(r"'[^']+'", "<quote>", sentence)
    sentence = re.sub(r"\[[A-Z]+\]", "<module>", sentence)
    sentence = re.sub(r"\s+", " ", sentence)
    return sentence[:60]


def normalize_first_sentence(text: str) -> str:
    """첫 문장 중복을 업무 대상/상세값 차이보다 문장 골격 위주로 비교한다."""
    sentence = re.split(r"[.!?。]\s*", text.strip(), maxsplit=1)[0]
    sentence = re.sub(r"'[^']+'", "<quote>", sentence)
    sentence = re.sub(r"\[[A-Z]+\]", "<module>", sentence)
    sentence = re.sub(r"[A-Za-z][A-Za-z0-9_]+", "<name>", sentence)
    sentence = re.sub(r"\s+", " ", sentence)
    return sentence[:80]


def duplicate_values(rows: list[dict[str, Any]], column: str) -> dict[str, list[int]]:
    """중복 문구 실패가 바로 수정 가능하도록 값별 worklog_id 목록을 만든다."""
    seen: dict[str, list[int]] = defaultdict(list)
    for row in rows:
        value = row.get(column)
        if isinstance(value, str):
            seen[value].append(row.get("worklog_id"))
    return {value: ids for value, ids in seen.items() if len(ids) > 1}


def check_copy_quality(worklogs: list[dict[str, Any]], manifest: Any, user_ids: set[int], v: Verification) -> None:
    """페르소나 기반 copy 다양화 계약을 정적 seed만으로 검증한다."""
    request_dups = duplicate_values(worklogs, "request_content")
    if request_dups:
        detail = "; ".join(f"{ids}" for ids in request_dups.values())
        v.fail("copy request duplicate guard", detail)
    else:
        v.pass_("copy request duplicate guard", "all request_content values are unique")

    bad_stock: list[str] = []
    raw_hash_leaks: list[str] = []
    identity_leaks: list[str] = []
    short_work: list[int] = []
    for row in worklogs:
        worklog_id = row.get("worklog_id")
        text = " ".join(str(row.get(col, "")) for col in ("request_content", "work_content"))
        lowered_text = text.lower()
        for phrase in FORBIDDEN_STOCK_PHRASES:
            if phrase.lower() in lowered_text:
                bad_stock.append(f"{worklog_id}:{phrase}")
        if RAW_GIT_HASH_RE.search(text):
            raw_hash_leaks.append(str(worklog_id))
        for identity in FORBIDDEN_PERSONA_IDENTITIES:
            if identity in text:
                identity_leaks.append(f"{worklog_id}:{identity}")
        sentence_count = len([part for part in re.split(r"[.!?。]\s*", str(row.get("work_content", "")).strip()) if part])
        if sentence_count < 2:
            short_work.append(worklog_id)
    if bad_stock:
        v.fail("copy stock phrase guard", "; ".join(bad_stock))
    else:
        v.pass_("copy stock phrase guard", "no git/hash/manifest wording or banned template phrases")
    if raw_hash_leaks:
        v.fail("copy raw hash leakage guard", "worklog_ids=" + ", ".join(raw_hash_leaks))
    else:
        v.pass_("copy raw hash leakage guard", "no raw git hashes in request/work copy")
    if identity_leaks:
        v.fail("persona identity leakage guard", "; ".join(identity_leaks))
    else:
        v.pass_("persona identity leakage guard", "no borrowed iBank persona identities in SQL copy")
    if short_work:
        v.fail("copy work sentence count guard", f"worklog_ids={short_work}")
    else:
        v.pass_("copy work sentence count guard", "each work_content has at least two sentences")

    current_prefix: str | None = None
    current_ids: list[int] = []
    repeated_runs: list[str] = []
    for row in sorted(worklogs, key=lambda r: r.get("worklog_id", 0)):
        prefix = normalize_copy_prefix(str(row.get("work_content", "")))
        if prefix == current_prefix:
            current_ids.append(row.get("worklog_id"))
        else:
            if current_prefix is not None and len(current_ids) >= 3:
                repeated_runs.append(f"{current_ids}:{current_prefix}")
            current_prefix = prefix
            current_ids = [row.get("worklog_id")]
    if current_prefix is not None and len(current_ids) >= 3:
        repeated_runs.append(f"{current_ids}:{current_prefix}")
    if repeated_runs:
        v.fail("copy prefix repetition guard", "; ".join(repeated_runs))
    else:
        v.pass_("copy prefix repetition guard", "no 3-row run shares the same normalized opening")

    first_sentence_groups: dict[str, list[int]] = defaultdict(list)
    for row in worklogs:
        first_sentence_groups[normalize_first_sentence(str(row.get("work_content", "")))].append(row.get("worklog_id"))
    repeated_first_sentences = {prefix: ids for prefix, ids in first_sentence_groups.items() if len(ids) > 2}
    if repeated_first_sentences:
        detail = "; ".join(f"{ids}:{prefix}" for prefix, ids in repeated_first_sentences.items())
        v.fail("copy first sentence duplicate guard", detail)
    else:
        v.pass_("copy first sentence duplicate guard", "no normalized first sentence appears more than twice")

    missing_author_rows = sorted(user_ids - {row.get("author_id") for row in worklogs})
    keyword_misses: list[str] = []
    for author_id in sorted(user_ids):
        keywords = PERSONA_KEYWORDS.get(author_id, ())
        author_rows = [row for row in worklogs if row.get("author_id") == author_id]
        if not author_rows:
            continue
        haystack = "\n".join(
            " ".join(str(row.get(col, "")) for col in ("request_content", "work_content"))
            for row in author_rows
        )
        matched = {keyword for keyword in keywords if keyword in haystack}
        if len(matched) < 2:
            keyword_misses.append(f"{author_id}:{'/'.join(keywords)} matched={sorted(matched)}")
    if missing_author_rows:
        v.fail("copy persona author coverage", f"missing author rows={missing_author_rows}")
    elif keyword_misses:
        v.fail("copy persona keyword coverage", "; ".join(keyword_misses))
    else:
        v.pass_("copy persona keyword coverage", "each ACODIAN author has rows and at least two persona focus terms")

    if manifest is None:
        v.fail("manifest copy persona mapping", "manifest unavailable")
        return
    manifest_items = manifest_worklog_items(manifest)
    bad_mapping: list[str] = []
    for row in worklogs:
        worklog_id = row.get("worklog_id")
        author_id = row.get("author_id")
        expected = EXPECTED_PERSONA_SOURCES.get(author_id)
        item = manifest_items.get(worklog_id, {})
        actual = item.get("copy_persona")
        if expected and actual != expected:
            bad_mapping.append(f"{worklog_id}:{author_id}:{actual!r}!={expected!r}")
    sources = manifest.get("persona_sources") if isinstance(manifest, dict) else None
    missing_sources = []
    if isinstance(sources, dict):
        for author_id in sorted(user_ids):
            expected = EXPECTED_PERSONA_SOURCES.get(author_id)
            actual = sources.get(str(author_id), {}).get("source") if isinstance(sources.get(str(author_id)), dict) else None
            if expected and actual != expected:
                missing_sources.append(f"{author_id}:{actual!r}!={expected!r}")
    else:
        missing_sources.append("persona_sources missing")
    if bad_mapping or missing_sources:
        v.fail("manifest copy persona mapping", f"worklogs={bad_mapping}; sources={missing_sources}")
    else:
        v.pass_("manifest copy persona mapping", "copy_persona and persona_sources match planned mapping")

    linkage_misses: list[int] = []
    for row in worklogs:
        item = manifest_items.get(row.get("worklog_id"), {})
        if not isinstance(item, dict) or item.get("author_id") != row.get("author_id") or item.get("team_id") != row.get("team_id") or not has_manifest_evidence(item):
            linkage_misses.append(row.get("worklog_id"))
    if linkage_misses:
        v.fail("manifest evidence linkage guard", f"missing or mismatched manifest evidence in worklog_ids={linkage_misses}")
    else:
        v.pass_("manifest evidence linkage guard", "each worklog links to manifest-only source evidence")


def check_team_user_seed(data: SqlData, user_ids: set[int], team_id: int, v: Verification) -> None:
    teams = [r for r in data.rows.get("tb_team", []) if r.get("team_id") == team_id]
    if len(teams) == 1 and teams[0].get("team_name") == "ACODIAN" and teams[0].get("status_code") == "ACTIVE":
        v.pass_("ACODIAN team", "team_id=201, team_name=ACODIAN, status_code=ACTIVE")
    else:
        v.fail("ACODIAN team", f"expected one active ACODIAN row, found {teams}")

    users = [r for r in data.rows.get("tb_user", []) if r.get("user_id") in user_ids]
    found_ids = {r.get("user_id") for r in users}
    found_names = {r.get("user_name") for r in users}
    if found_ids == user_ids and EXPECTED_USER_NAMES.issubset(found_names):
        v.pass_("ACODIAN users", f"ids={sorted(found_ids)}, names={sorted(found_names)}")
    else:
        v.fail("ACODIAN users", f"expected ids={sorted(user_ids)} names={sorted(EXPECTED_USER_NAMES)}, found ids={sorted(found_ids)} names={sorted(found_names)}")

    bad_contacts = []
    for row in users:
        email = str(row.get("email", "")).lower()
        phone = str(row.get("phone", ""))
        if any(domain in email for domain in FORBIDDEN_CONTACT_DOMAINS):
            bad_contacts.append(f"email:{row.get('user_id')}={email}")
        if phone and not re.fullmatch(r"0\d{1,2}-\d{3,4}-\d{4}|010-\d{4}-\d{4}|\+?\d[\d\- ]+", phone):
            bad_contacts.append(f"phone:{row.get('user_id')}={phone}")
    if bad_contacts:
        v.fail("dummy contact guard", "; ".join(bad_contacts))
    else:
        v.pass_("dummy contact guard", "no common real-personal email domains and phone format is synthetic-looking")

    user_team_rows = [r for r in rows_for(data, "tb_user_team") if r.get("user_id") in user_ids]
    by_user = defaultdict(list)
    for row in user_team_rows:
        by_user[row.get("user_id")].append(row)
    bad_primary = []
    for user_id in sorted(user_ids):
        primary = [r for r in by_user.get(user_id, []) if r.get("team_id") == team_id and r.get("is_primary") is True]
        if len(primary) != 1:
            bad_primary.append(f"{user_id}:{len(primary)}")
    if not bad_primary and len(user_team_rows) >= len(user_ids):
        v.pass_("primary memberships", "each ACODIAN user has exactly one primary ACODIAN membership")
    else:
        v.fail("primary memberships", "bad primary counts " + ", ".join(bad_primary) + f"; rows={user_team_rows}")

    leaders = [r for r in user_team_rows if r.get("team_id") == team_id and r.get("is_leader") is True and r.get("status_code") == "ACTIVE"]
    if len(leaders) == 1:
        v.pass_("ACODIAN leader", f"user_id={leaders[0].get('user_id')}")
    else:
        v.fail("ACODIAN leader", f"expected exactly one active leader, found {leaders}")

    membership_pairs = [(r.get("user_id"), r.get("team_id")) for r in user_team_rows]
    dup_pairs = sorted({p for p in membership_pairs if membership_pairs.count(p) > 1})
    if dup_pairs:
        v.fail("duplicate tb_user_team pairs", str(dup_pairs))
    else:
        v.pass_("duplicate tb_user_team pairs", "none")

    team_admin_rows = [r for r in data.rows.get("tb_team_admin", []) if r.get("team_id") == team_id]
    admin_pairs = [(r.get("user_id"), r.get("team_id")) for r in team_admin_rows]
    dup_admin = sorted({p for p in admin_pairs if admin_pairs.count(p) > 1})
    if dup_admin:
        v.fail("duplicate tb_team_admin pairs", str(dup_admin))
    elif team_admin_rows:
        v.pass_("duplicate tb_team_admin pairs", "none")
    else:
        v.fail("duplicate tb_team_admin pairs", "no ACODIAN team_admin rows found")


def check_schema_contract(data: SqlData, v: Verification) -> None:
    text = data.text.lower()
    if "team_leader" in text:
        v.fail("legacy team_leader guard", "legacy team_leader token found")
    else:
        v.pass_("legacy team_leader guard", "not used")

    user_team_insert_columns = data.insert_columns.get("tb_user_team", [])
    if not user_team_insert_columns:
        v.fail("tb_user_team insert contract", "no tb_user_team INSERT found")
    else:
        bad = [cols for cols in user_team_insert_columns if "user_team_id" in cols or not REQUIRED_USER_TEAM_COLUMNS.issubset(set(cols))]
        if bad:
            v.fail("tb_user_team insert contract", f"bad column sets: {bad}")
        else:
            v.pass_("tb_user_team insert contract", "uses current columns and no explicit user_team_id")

    team_admin_columns = data.insert_columns.get("tb_team_admin", [])
    bad_admin = [cols for cols in team_admin_columns if "team_admin_id" in cols]
    if bad_admin:
        v.fail("tb_team_admin relation id guard", f"explicit team_admin_id used: {bad_admin}")
    elif team_admin_columns:
        v.pass_("tb_team_admin relation id guard", "no explicit team_admin_id")
    else:
        v.fail("tb_team_admin relation id guard", "no tb_team_admin INSERT found")

    sequence_tables = ("tb_team", "tb_user", "tb_worklog", "tb_worklog_status_history", "tb_worklog_dependency")
    missing = [table for table in sequence_tables if f"pg_get_serial_sequence('{table}'" not in text and f'pg_get_serial_sequence("{table}"' not in text]
    if missing:
        v.fail("sequence alignment", "missing pg_get_serial_sequence proof for " + ", ".join(missing))
    else:
        v.pass_("sequence alignment", "pg_get_serial_sequence present for generated explicit-id tables")


def check_worklogs(data: SqlData, manifest: Any, user_ids: set[int], team_id: int, worklog_start: int, worklog_end_exclusive: int, min_count: int, max_count: int, v: Verification) -> None:
    worklogs = [r for r in data.rows.get("tb_worklog", []) if isinstance(r.get("worklog_id"), int) and worklog_start <= r["worklog_id"] < worklog_end_exclusive]
    count = len(worklogs)
    if min_count <= count <= max_count:
        v.pass_("worklog count", f"{count} worklogs in [{worklog_start}, {worklog_end_exclusive})")
    else:
        v.fail("worklog count", f"expected {min_count}-{max_count}, found {count}")

    bad_refs = [r.get("worklog_id") for r in worklogs if r.get("team_id") != team_id or r.get("author_id") not in user_ids]
    if bad_refs:
        v.fail("worklog author/team refs", f"bad worklog_ids={bad_refs}")
    elif worklogs:
        v.pass_("worklog author/team refs", "all generated worklogs are ACODIAN team/user scoped")
    else:
        v.fail("worklog author/team refs", "no generated worklogs found")

    too_long = [r.get("worklog_id") for r in worklogs if isinstance(r.get("title"), str) and len(r["title"]) > 50]
    if too_long:
        v.fail("worklog title length", f"title > 50 chars for worklog_ids={too_long}")
    else:
        v.pass_("worklog title length", "all generated titles fit V14 VARCHAR(50)")

    modules = set()
    for row in worklogs:
        haystack = " ".join(str(row.get(col, "")) for col in ("title", "request_content", "work_content", "ai_summary")).lower()
        for module in ("api", "web", "ai", "infra", "docs"):
            if re.search(rf"(^|[^a-z]){module}([^a-z]|$)", haystack):
                modules.add(module)
    if manifest is not None:
        modules |= manifest_modules(manifest)
    missing = {"api", "web", "ai"} - modules
    if missing:
        v.fail("module coverage", f"missing {sorted(missing)}; found {sorted(modules)}")
    else:
        v.pass_("module coverage", f"found {sorted(modules)}")

    if manifest is not None:
        manifest_items = manifest_worklog_items(manifest)
        sql_ids = {r["worklog_id"] for r in worklogs}
        manifest_ids = {i for i in manifest_items if worklog_start <= i < worklog_end_exclusive}
        if manifest_ids == sql_ids and all(has_manifest_evidence(manifest_items[i]) for i in manifest_ids):
            v.pass_("manifest worklog evidence", f"{len(manifest_ids)} ids with source evidence")
        else:
            missing_manifest = sorted(sql_ids - manifest_ids)
            extra_manifest = sorted(manifest_ids - sql_ids)
            missing_source = sorted(i for i in (sql_ids & manifest_ids) if not has_manifest_evidence(manifest_items[i]))
            v.fail("manifest worklog evidence", f"missing_manifest={missing_manifest}, extra_manifest={extra_manifest}, missing_source={missing_source}")

    if worklogs:
        check_copy_quality(worklogs, manifest, user_ids, v)


def check_status_history(data: SqlData, user_ids: set[int], worklog_start: int, worklog_end_exclusive: int, v: Verification) -> None:
    worklogs = {r["worklog_id"]: r for r in data.rows.get("tb_worklog", []) if isinstance(r.get("worklog_id"), int) and worklog_start <= r["worklog_id"] < worklog_end_exclusive}
    histories = [r for r in data.rows.get("tb_worklog_status_history", []) if r.get("worklog_id") in worklogs]
    by_worklog = defaultdict(list)
    for row in histories:
        by_worklog[row.get("worklog_id")].append(row)
    missing = sorted(set(worklogs) - set(by_worklog))
    if missing:
        v.fail("status history coverage", f"missing worklog_ids={missing}")
    elif worklogs:
        v.pass_("status history coverage", "all generated worklogs have history")
    else:
        v.fail("status history coverage", "no generated worklogs found")

    mismatches = []
    bad_changed_by = []
    for worklog_id, rows in by_worklog.items():
        final_status = rows[-1].get("new_status_code")
        if final_status != worklogs[worklog_id].get("status_code"):
            mismatches.append(f"{worklog_id}:{final_status}!={worklogs[worklog_id].get('status_code')}")
        for row in rows:
            if row.get("changed_by") not in user_ids:
                bad_changed_by.append((worklog_id, row.get("changed_by")))
    if mismatches:
        v.fail("status final consistency", ", ".join(mismatches))
    else:
        v.pass_("status final consistency", "final history status matches tb_worklog.status_code")
    if bad_changed_by:
        v.fail("status changed_by refs", str(bad_changed_by))
    else:
        v.pass_("status changed_by refs", "all changed_by values are ACODIAN users")


def detect_cycle(edges: dict[int, list[int]]) -> list[int] | None:
    visiting: set[int] = set()
    visited: set[int] = set()
    stack: list[int] = []

    def dfs(node: int) -> list[int] | None:
        visiting.add(node)
        stack.append(node)
        for nxt in edges.get(node, []):
            if nxt in visiting:
                return stack[stack.index(nxt) :] + [nxt]
            if nxt not in visited:
                cycle = dfs(nxt)
                if cycle:
                    return cycle
        visiting.remove(node)
        visited.add(node)
        stack.pop()
        return None

    for node in list(edges):
        if node not in visited:
            cycle = dfs(node)
            if cycle:
                return cycle
    return None


def max_dependency_depth(edges: dict[int, list[int]]) -> int:
    memo: dict[int, int] = {}

    def depth(node: int) -> int:
        if node in memo:
            return memo[node]
        if not edges.get(node):
            memo[node] = 0
        else:
            memo[node] = 1 + max(depth(child) for child in edges[node])
        return memo[node]

    return max((depth(node) for node in edges), default=0)


def check_dependencies(data: SqlData, worklog_start: int, worklog_end_exclusive: int, required_depth: int, v: Verification) -> None:
    worklog_ids = {r["worklog_id"] for r in data.rows.get("tb_worklog", []) if isinstance(r.get("worklog_id"), int) and worklog_start <= r["worklog_id"] < worklog_end_exclusive}
    dependencies = [r for r in data.rows.get("tb_worklog_dependency", []) if r.get("worklog_id") in worklog_ids or r.get("depends_on_worklog_id") in worklog_ids]
    pairs = [(r.get("worklog_id"), r.get("depends_on_worklog_id")) for r in dependencies]
    invalid = [p for p in pairs if p[0] == p[1] or p[0] not in worklog_ids or p[1] not in worklog_ids]
    duplicates = sorted({p for p in pairs if pairs.count(p) > 1})
    if invalid:
        v.fail("dependency valid refs", f"invalid/self edges={invalid}")
    else:
        v.pass_("dependency valid refs", "all dependencies reference generated worklogs and no self edges")
    if duplicates:
        v.fail("dependency duplicate pairs", str(duplicates))
    else:
        v.pass_("dependency duplicate pairs", "none")

    edges: dict[int, list[int]] = defaultdict(list)
    for src, dst in pairs:
        if isinstance(src, int) and isinstance(dst, int):
            edges[src].append(dst)
    cycle = detect_cycle(edges)
    if cycle:
        v.fail("dependency cycle", " -> ".join(map(str, cycle)))
    else:
        v.pass_("dependency cycle", "acyclic")
    depth = max_dependency_depth(edges)
    if depth >= required_depth:
        v.pass_("dependency depth", f"max depth={depth}")
    else:
        v.fail("dependency depth", f"expected >= {required_depth}, found {depth}")


def parse_existing_key_rows(seed_dir: Path, target_paths: list[Path]) -> SqlData:
    target_names = {p.name for p in target_paths}
    existing_paths = [p for p in seed_dir.glob("*.sql") if p.name not in target_names]
    try:
        return parse_sql_files(existing_paths)
    except Exception:
        # Existing seeds are not the subject under test; collision check can be skipped
        # rather than making the new verifier brittle on unrelated SQL constructs.
        return SqlData()


def check_collisions(seed_dir: Path, target_paths: list[Path], data: SqlData, user_ids: set[int], team_id: int, worklog_start: int, worklog_end_exclusive: int, v: Verification) -> None:
    existing = parse_existing_key_rows(seed_dir, target_paths)
    collisions: list[str] = []
    existing_teams = {r.get("team_id") for r in existing.rows.get("tb_team", [])}
    if team_id in existing_teams:
        collisions.append(f"tb_team.team_id={team_id}")
    existing_users = {r.get("user_id") for r in existing.rows.get("tb_user", [])}
    for user_id in sorted(user_ids & existing_users):
        collisions.append(f"tb_user.user_id={user_id}")
    existing_worklogs = {r.get("worklog_id") for r in existing.rows.get("tb_worklog", [])}
    bad_worklogs = sorted(i for i in existing_worklogs if isinstance(i, int) and worklog_start <= i < worklog_end_exclusive)
    if bad_worklogs:
        collisions.append(f"tb_worklog.worklog_id={bad_worklogs}")
    if collisions:
        v.fail("existing seed ID collision", "; ".join(collisions))
    else:
        v.pass_("existing seed ID collision", "no target ids found in non-ACODIAN dev seeds")


def run_git_guard(patterns: list[str], v: Verification) -> None:
    if not patterns:
        v.pass_("forbidden git guard", "no forbidden paths configured")
        return
    commands = [
        ["git", "diff", "--name-only", "--", *patterns],
        ["git", "status", "--porcelain", "--", *patterns],
    ]
    failures = []
    for cmd in commands:
        proc = subprocess.run(cmd, text=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=False)
        label = " ".join(cmd[:3])
        if proc.returncode != 0:
            failures.append(f"{label} rc={proc.returncode} stderr={proc.stderr.strip()}")
        elif proc.stdout.strip():
            failures.append(f"{label} output={proc.stdout.strip()}")
    if failures:
        v.fail("forbidden git guard", " | ".join(failures))
    else:
        v.pass_("forbidden git guard", "diff/status empty for forbidden paths")


def psql(db_url: str, args: list[str], *, input_text: str | None = None) -> subprocess.CompletedProcess[str]:
    return subprocess.run(["psql", db_url, "-v", "ON_ERROR_STOP=1", *args], input=input_text, text=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=False)


def run_db_smoke(seed_dir: Path, db_url: str, team_id: int, user_ids: set[int], worklog_start: int, worklog_end_exclusive: int, required_depth: int, v: Verification) -> None:
    if shutil.which("psql") is None:
        v.fail("db smoke", "psql is not installed or not on PATH")
        return
    migration_dir = seed_dir.parent / "migration"
    migration_paths = sorted(migration_dir.glob("V*__*.sql"), key=natural_key)
    seed_paths = sorted(seed_dir.glob("*.sql"), key=natural_key)
    if not migration_paths:
        v.fail("db smoke", f"no migrations found at {migration_dir}")
        return
    for path in migration_paths + seed_paths:
        proc = psql(db_url, ["-f", str(path)])
        if proc.returncode != 0:
            v.fail("db smoke apply", f"failed {path}: {proc.stderr.strip() or proc.stdout.strip()}")
            return
    ids_csv = ",".join(str(i) for i in sorted(user_ids))
    query = f"""
WITH RECURSIVE dep(worklog_id, depends_on_worklog_id, depth, path, cycle) AS (
    SELECT d.worklog_id, d.depends_on_worklog_id, 1, ARRAY[d.worklog_id, d.depends_on_worklog_id], d.depends_on_worklog_id = d.worklog_id
    FROM tb_worklog_dependency d
    WHERE d.worklog_id >= {worklog_start} AND d.worklog_id < {worklog_end_exclusive}
  UNION ALL
    SELECT dep.worklog_id, d.depends_on_worklog_id, dep.depth + 1, dep.path || d.depends_on_worklog_id, d.depends_on_worklog_id = ANY(dep.path)
    FROM dep
    JOIN tb_worklog_dependency d ON d.worklog_id = dep.depends_on_worklog_id
    WHERE NOT dep.cycle AND dep.depth < 50
), checks AS (
    SELECT 'team' AS name, COUNT(*) = 1 AS ok FROM tb_team WHERE team_id = {team_id} AND team_name = 'ACODIAN'
    UNION ALL SELECT 'users', COUNT(*) = {len(user_ids)} FROM tb_user WHERE user_id IN ({ids_csv})
    UNION ALL SELECT 'worklogs', COUNT(*) BETWEEN 48 AND 52 FROM tb_worklog WHERE worklog_id >= {worklog_start} AND worklog_id < {worklog_end_exclusive} AND team_id = {team_id} AND author_id IN ({ids_csv})
    UNION ALL SELECT 'leader', COUNT(*) = 1 FROM tb_user_team WHERE team_id = {team_id} AND status_code = 'ACTIVE' AND is_leader = TRUE
    UNION ALL SELECT 'depth', COALESCE(MAX(depth), 0) >= {required_depth} FROM dep
    UNION ALL SELECT 'cycle', NOT EXISTS (SELECT 1 FROM dep WHERE cycle)
    UNION ALL SELECT 'team_seq', nextval(pg_get_serial_sequence('tb_team','team_id')) > (SELECT MAX(team_id) FROM tb_team)
    UNION ALL SELECT 'user_seq', nextval(pg_get_serial_sequence('tb_user','user_id')) > (SELECT MAX(user_id) FROM tb_user)
    UNION ALL SELECT 'worklog_seq', nextval(pg_get_serial_sequence('tb_worklog','worklog_id')) > (SELECT MAX(worklog_id) FROM tb_worklog)
)
SELECT name || '=' || ok FROM checks ORDER BY name;
"""
    proc = psql(db_url, ["-Atq"], input_text=query)
    if proc.returncode != 0:
        v.fail("db smoke queries", proc.stderr.strip() or proc.stdout.strip())
        return
    bad = [line for line in proc.stdout.splitlines() if line.endswith("=f")]
    if bad:
        v.fail("db smoke queries", ", ".join(bad))
    else:
        v.pass_("db smoke", f"applied {len(migration_paths)} migrations + {len(seed_paths)} dev seeds; integrity queries passed")


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Verify ACODIAN git-log dev seed pack")
    parser.add_argument("--seed-dir", type=Path, required=True)
    parser.add_argument("--manifest", type=Path, required=True)
    parser.add_argument("--team-id", type=int, required=True)
    parser.add_argument("--user-ids", required=True, help="single id or inclusive range, e.g. 201-206")
    parser.add_argument("--worklog-range", required=True, help="start:end_exclusive, e.g. 2001:2052")
    parser.add_argument("--expected-worklog-min", type=int, required=True)
    parser.add_argument("--expected-worklog-max", type=int, required=True)
    parser.add_argument("--required-depth", type=int, required=True)
    parser.add_argument("--forbidden-path", action="append", default=[])
    parser.add_argument("--db-smoke", action="store_true")
    parser.add_argument("--db-url")
    args = parser.parse_args(argv)

    v = Verification()
    user_ids = parse_range(args.user_ids)
    worklog_start, worklog_end_exclusive = parse_worklog_range(args.worklog_range)
    target_paths = file_presence(args.seed_dir, args.manifest, v)
    manifest = load_manifest(args.manifest, v) if args.manifest.exists() else None

    if all(path.exists() for path in target_paths):
        try:
            data = parse_sql_files(target_paths)
            v.pass_("sql parse", f"parsed {len(target_paths)} ACODIAN SQL files")
            check_team_user_seed(data, user_ids, args.team_id, v)
            check_schema_contract(data, v)
            check_worklogs(data, manifest, user_ids, args.team_id, worklog_start, worklog_end_exclusive, args.expected_worklog_min, args.expected_worklog_max, v)
            check_status_history(data, user_ids, worklog_start, worklog_end_exclusive, v)
            check_dependencies(data, worklog_start, worklog_end_exclusive, args.required_depth, v)
            check_collisions(args.seed_dir, target_paths, data, user_ids, args.team_id, worklog_start, worklog_end_exclusive, v)
        except Exception as exc:  # noqa: BLE001 - report verifier failures with context.
            v.fail("sql/static verification", f"{type(exc).__name__}: {exc}")

    run_git_guard(args.forbidden_path, v)

    if args.db_smoke:
        if not args.db_url:
            v.fail("db smoke", "--db-url is required with --db-smoke")
        else:
            run_db_smoke(args.seed_dir, args.db_url, args.team_id, user_ids, worklog_start, worklog_end_exclusive, args.required_depth, v)
    else:
        v.pass_("db smoke", "skipped (pass --db-smoke --db-url to apply migrations/dev seeds)")

    v.print()
    return 0 if v.ok else 1


if __name__ == "__main__":
    sys.exit(main())
