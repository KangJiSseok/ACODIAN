#!/usr/bin/env python3
"""Generate ACODIAN git-log-based dev seed SQL and its manifest."""

from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
from dataclasses import dataclass
from datetime import date, datetime, timedelta
from pathlib import Path
from typing import Iterable, Sequence

TEAM_NAME = "ACODIAN"
DEFAULT_TEAM_ID = 201
DEFAULT_USER_START_ID = 201
DEFAULT_WORKLOG_START_ID = 2001
DEFAULT_COUNT = 50
PASSWORD_HASH = "$2b$12$tzatvdD02DDLmrqIXLtCa.ucFmjdnydHO73TWemkBYQSHVAUnYNXW"
DUMMY_PROFILE_BASE = "https://d1sif143wcm5pd.cloudfront.net/local/default/profile"
REQUIRED_MODULES = ("api", "web", "ai")
ALLOWED_MODULES = ("api", "web", "ai", "infra", "docs")
NOISE_PATTERNS = (
    "stash",
    "wip",
    "omx",
    "wiki",
    "runtime",
    "checkpoint",
    "scratch",
    "temp",
)

USER_NAMES = (
    "강지석",
    "이성훈",
    "진경석",
    "안성훈",
    "정인호",
    "서백균",
)
POSITIONS = ("수석", "책임", "선임", "선임", "선임", "전임")
TEAM_ROLES = (
    "기술 리드 / 팀장",
    "백엔드 API 안정화",
    "프론트엔드 UX 개선",
    "AI 검색/요약 연동",
    "인증·알림 품질 점검",
    "인프라·문서 검증",
)
MODULE_LABELS = {
    "api": "API",
    "web": "WEB",
    "ai": "AI",
    "infra": "INFRA",
    "docs": "DOCS",
}

STATUS_SEQUENCE = ("COMPLETED", "COMPLETED", "COMPLETED", "IN_PROGRESS", "COMPLETED", "PENDING")
IMPORTANCE_SEQUENCE = ("HIGH", "NORMAL", "NORMAL", "HIGH", "LOW", "NORMAL")


@dataclass(frozen=True)
class CommitEntry:
    hash: str
    commit_date: str
    author: str
    subject: str
    files: tuple[str, ...]
    module: str


@dataclass(frozen=True)
class WorklogEntry:
    worklog_id: int
    author_id: int
    team_id: int
    title: str
    request_content: str
    work_content: str
    status_code: str
    importance_code: str
    actual_hours: float
    instruction_date: str
    due_date: str
    completion_date: str | None
    created_at: str
    updated_at: str
    commit: CommitEntry
    copy_persona: str
    copy_variant: int


@dataclass(frozen=True)
class PersonaProfile:
    source: str
    label: str
    keywords: tuple[str, ...]
    focus_terms: tuple[str, ...]
    tone_markers: tuple[str, ...]
    status_reasons: dict[str, tuple[str, ...]]


@dataclass(frozen=True)
class WorkContext:
    area: str
    subject: str
    object_name: str
    action: str
    effect: str
    validation: str


PERSONA_PROFILES: dict[int, PersonaProfile] = {
    201: PersonaProfile(
        source=".codex/agents/user-004.toml",
        label="기술 리드 검토형",
        keywords=("리스크", "우선순위", "일정", "검토"),
        focus_terms=("리스크", "우선순위", "일정", "검토"),
        tone_markers=("먼저", "같이", "놓치지 않게"),
        status_reasons={
            "PENDING": ("팀 리스크 확인 대상으로 등록", "우선순위 검토 대기"),
            "IN_PROGRESS": ("영향 범위와 일정 리스크 검토 시작", "배포 전 확인 순서 조정 시작"),
            "COMPLETED": ("검토 결과와 후속 확인 기준 정리 완료", "팀 공유 가능한 수준으로 리스크 판단 완료"),
        },
    ),
    202: PersonaProfile(
        source=".codex/agents/user-026.toml",
        label="백엔드 장애 수정형",
        keywords=("API", "로그", "재현", "영향"),
        focus_terms=("API", "로그", "재현", "영향"),
        tone_markers=("바로", "다시", "짧게"),
        status_reasons={
            "PENDING": ("API 영향 확인 대기", "로그 재현 경로 확인 대상으로 등록"),
            "IN_PROGRESS": ("원인-조치-영향 범위 분석 시작", "로그와 재현 조건 확인 시작"),
            "COMPLETED": ("수정 영향과 재검증 포인트 기록 완료", "API 영향 확인 및 후속 배포 메모 완료"),
        },
    ),
    203: PersonaProfile(
        source=".codex/agents/user-030.toml",
        label="프론트 화면 흐름형",
        keywords=("화면", "사용자", "동선", "회귀"),
        focus_terms=("화면", "사용자", "동선", "회귀"),
        tone_markers=("눌러 보며", "사용자 기준으로", "다시"),
        status_reasons={
            "PENDING": ("화면 회귀 확인 대상으로 등록", "사용자 동선 확인 대기"),
            "IN_PROGRESS": ("화면 흐름과 데이터 연결 점검 시작", "사용자 동선 기준 회귀 확인 시작"),
            "COMPLETED": ("화면 회귀와 표시 기준 정리 완료", "사용자 흐름 확인 결과 기록 완료"),
        },
    ),
    204: PersonaProfile(
        source=".codex/agents/user-031.toml",
        label="AI 검색 품질형",
        keywords=("AI", "검색", "임베딩", "유사도"),
        focus_terms=("AI", "검색", "임베딩", "유사도"),
        tone_markers=("예시를 두고", "품질 관점에서", "흔들림 없이"),
        status_reasons={
            "PENDING": ("AI 검색 품질 확인 대상으로 등록", "임베딩 영향 확인 대기"),
            "IN_PROGRESS": ("검색 결과와 재시도 조건 점검 시작", "유사도 영향 분석 시작"),
            "COMPLETED": ("AI 품질 확인과 재시도 기준 기록 완료", "검색 결과 영향 정리 완료"),
        },
    ),
    205: PersonaProfile(
        source=".codex/agents/user-029.toml + .codex/agents/user-025.toml",
        label="권한·품질 점검형",
        keywords=("권한", "알림", "품질", "재확인"),
        focus_terms=("권한", "알림", "품질", "재확인"),
        tone_markers=("꼼꼼히", "놓치지 않게", "한 번 더"),
        status_reasons={
            "PENDING": ("권한·알림 품질 확인 대상으로 등록", "재확인 체크리스트 대기"),
            "IN_PROGRESS": ("권한 흐름과 품질 체크 시작", "알림 영향과 재확인 범위 점검 시작"),
            "COMPLETED": ("권한·알림 영향과 품질 확인 기록 완료", "재확인 결과와 후속 체크 포인트 정리 완료"),
        },
    ),
    206: PersonaProfile(
        source=".codex/agents/user-033.toml + .codex/agents/user-034.toml",
        label="인프라·문서 운영형",
        keywords=("배포", "환경", "문서", "운영"),
        focus_terms=("배포", "환경", "문서", "운영"),
        tone_markers=("운영 기준으로", "환경 차이를 보며", "문서까지"),
        status_reasons={
            "PENDING": ("배포 환경 확인 대상으로 등록", "운영 문서 보강 여부 확인 대기"),
            "IN_PROGRESS": ("환경 차이와 운영 절차 점검 시작", "배포 체크리스트와 문서 확인 시작"),
            "COMPLETED": ("운영 확인 근거와 문서 보강 지점 정리 완료", "배포 안정화 확인 결과 기록 완료"),
        },
    ),
}


def run_git(args: Sequence[str]) -> str:
    """git 명령 실패를 사용자에게 바로 설명할 수 있게 stderr를 포함해 중단한다."""
    completed = subprocess.run(
        ["git", *args],
        check=False,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    if completed.returncode != 0:
        raise SystemExit(f"git {' '.join(args)} failed: {completed.stderr.strip()}")
    return completed.stdout


def resolve_base_ref(requested: str | None) -> str:
    """origin/dev를 우선 쓰되 없으면 upstream, 마지막으로 HEAD로 안전하게 축소한다."""
    candidates: list[str] = []
    if requested:
        candidates.append(requested)
    candidates.extend(["origin/dev", "@{u}", "HEAD"])
    for candidate in dict.fromkeys(candidates):
        result = subprocess.run(
            ["git", "rev-parse", "--verify", candidate],
            check=False,
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
        )
        if result.returncode == 0:
            return candidate
    raise SystemExit("No usable git base ref found")


def infer_module(subject: str, files: Sequence[str]) -> str:
    """Conventional Commit scope와 변경 파일을 함께 보아 seed 모듈을 안정적으로 분류한다."""
    scope_match = re.match(r"^[a-zA-Z]+\(([^)]+)\):", subject)
    if scope_match:
        scopes = re.split(r"[,/ ]+", scope_match.group(1).lower())
        for scope in scopes:
            if scope in ALLOWED_MODULES:
                return scope
    for file_path in files:
        first = file_path.split("/", 1)[0].lower()
        if first in ALLOWED_MODULES:
            return first
        if file_path.startswith("api/"):
            return "api"
        if file_path.startswith("web/"):
            return "web"
        if file_path.startswith("ai/"):
            return "ai"
        if file_path.startswith("infra/"):
            return "infra"
        if file_path.startswith("docs/") or file_path.endswith(".md"):
            return "docs"
    lowered = subject.lower()
    for module in ALLOWED_MODULES:
        if module in lowered:
            return module
    return "docs"


def is_noise(subject: str, files: Sequence[str]) -> bool:
    """런타임·작업자 상태처럼 업무일지 근거로 부적합한 커밋을 제외한다."""
    lowered_subject = subject.lower()
    if any(pattern in lowered_subject for pattern in NOISE_PATTERNS):
        return True
    noisy_prefixes = (".omx/", "omx_wiki/", ".codex/session", ".codex/tmp")
    return bool(files) and all(path.startswith(noisy_prefixes) for path in files)


def parse_git_log(base_ref: str) -> list[CommitEntry]:
    """최신 non-merge git log를 파일 목록과 함께 읽어 manifest 근거 단위를 만든다."""
    raw = run_git([
        "log",
        "--no-merges",
        "--date=short",
        "--format=%x1e%H%x1f%ad%x1f%an%x1f%s",
        "--name-only",
        base_ref,
    ])
    entries: list[CommitEntry] = []
    for block in raw.split("\x1e"):
        block = block.strip("\n")
        if not block:
            continue
        lines = block.splitlines()
        header = lines[0].split("\x1f")
        if len(header) != 4:
            continue
        commit_hash, commit_date, author, subject = header
        files = tuple(line.strip() for line in lines[1:] if line.strip())
        if is_noise(subject, files):
            continue
        module = infer_module(subject, files)
        entries.append(CommitEntry(commit_hash, commit_date, author, subject, files, module))
    return entries


def select_commits(commits: Sequence[CommitEntry], count: int) -> list[CommitEntry]:
    """최신성을 유지하면서 api/web/ai 필수 포함 조건을 보정한다."""
    if len(commits) < count:
        raise SystemExit(f"Need at least {count} usable commits, found {len(commits)}")
    selected = list(commits[:count])
    for required in REQUIRED_MODULES:
        if any(entry.module == required for entry in selected):
            continue
        replacement = next((entry for entry in commits[count:] if entry.module == required), None)
        if replacement is None:
            raise SystemExit(f"No usable {required} commit found for required module coverage")
        for index in range(len(selected) - 1, -1, -1):
            candidate = selected[index]
            if candidate.module not in REQUIRED_MODULES or sum(1 for item in selected if item.module == candidate.module) > 1:
                selected[index] = replacement
                break
    return list(reversed(selected))


def sql(value: object) -> str:
    """Python 값을 PostgreSQL literal로 변환해 생성 SQL을 단순하고 결정적으로 유지한다."""
    if value is None:
        return "NULL"
    if isinstance(value, bool):
        return "true" if value else "false"
    if isinstance(value, (int, float)):
        return str(value)
    escaped = str(value).replace("'", "''")
    return f"'{escaped}'"


def compact_subject(subject: str) -> str:
    """커밋 타입/scope 접두어를 제거해 업무 제목과 본문에 넣을 핵심 문구를 만든다."""
    return re.sub(r"^[a-zA-Z]+(?:\([^)]+\))?!?:\s*", "", subject).strip() or subject.strip()


def sanitize_copy_fragment(text: str) -> str:
    """업무일지 본문에 개발 이력 식별자가 직접 드러나지 않도록 표현을 정리한다."""
    cleaned = re.sub(r"\b[0-9a-f]{7,40}\b", "", text, flags=re.I)
    banned_words = ("커밋", "commit", "해시", "hash", "manifest", "근거", "단서")
    for word in banned_words:
        cleaned = re.sub(re.escape(word), "", cleaned, flags=re.I)
    cleaned = re.sub(r"\s+", " ", cleaned).strip(" -:;,.")
    return cleaned or "개발 작업"


def title_for(entry: CommitEntry) -> str:
    """tb_worklog.title의 50자 제한을 넘지 않는 모듈 표기 제목을 만든다."""
    label = MODULE_LABELS.get(entry.module, entry.module.upper())
    title = f"[{label}] {compact_subject(entry.subject)}"
    return title[:50]


def author_id_for(entry: CommitEntry, fallback_index: int, user_ids: Sequence[int]) -> int:
    """git 작성자가 한 명에 치우쳐도 ACODIAN 6명 페르소나가 모두 드러나게 분산한다."""
    del entry
    return user_ids[fallback_index % len(user_ids)]


def stable_index(parts: Sequence[object], size: int) -> int:
    """random 없이 row 단서만으로 문구 후보를 고르게 선택한다."""
    joined = "|".join(str(part) for part in parts)
    return sum((index + 1) * ord(char) for index, char in enumerate(joined)) % size


def has_jongseong(text: str) -> bool:
    """간단한 한글 받침 판별로 조사 어색함을 줄인다."""
    if not text:
        return False
    code = ord(text[-1])
    if 0xAC00 <= code <= 0xD7A3:
        return (code - 0xAC00) % 28 != 0
    return text[-1].isdigit()


def josa(text: str, with_jong: str, without_jong: str) -> str:
    """업무일지 생성 문장에서 을/를, 은/는 같은 조사를 고른다."""
    return text + (with_jong if has_jongseong(text) else without_jong)


def file_text_for(entry: CommitEntry) -> str:
    """변경 파일 목록을 소문자 검색 문자열로 접어 작업 영역 추론에 사용한다."""
    return " ".join(path.lower() for path in entry.files)


def first_named_file(entry: CommitEntry) -> str:
    """경로 대신 사람이 업무일지에 적기 쉬운 파일/컴포넌트 이름만 고른다."""
    for path in entry.files:
        name = Path(path).stem
        if name and name not in {"index", "page", "route"}:
            return name
    if entry.files:
        return Path(entry.files[0]).stem or "관련 파일"
    return "관련 파일"


def persona_for(author_id: int) -> PersonaProfile:
    """ACODIAN 사용자 ID에 연결된 iBank 페르소나 문체 소스를 돌려준다."""
    return PERSONA_PROFILES.get(author_id, PERSONA_PROFILES[201])


def infer_object_name(entry: CommitEntry) -> str:
    """subject와 파일명에서 업무일지에 쓸 작업 대상 표현을 추론한다."""
    files = file_text_for(entry)
    subject = compact_subject(entry.subject).lower()
    combined = f"{subject} {files}"
    named = first_named_file(entry)
    if "controller" in combined:
        return f"{named} 컨트롤러"
    if "service" in combined:
        return f"{named} 서비스 로직"
    if "dto" in combined or "request" in combined or "response" in combined:
        return "요청·응답 구조"
    if "repository" in combined or "jooq" in combined or "query" in combined:
        return "조회 조건"
    if "test" in combined or "spec" in combined:
        return "회귀 테스트"
    if "tsx" in files or "component" in combined:
        return f"{named} 화면 컴포넌트"
    if "hook" in combined:
        return f"{named} hook"
    if "prompt" in combined or "chain" in combined or "model" in combined:
        return "AI 처리 흐름"
    if "nginx" in combined or "compose" in combined or "docker" in combined or "application.yml" in combined:
        return "배포 환경 설정"
    if entry.module == "web":
        return "화면 동선"
    if entry.module == "api":
        return "API 처리 흐름"
    if entry.module == "ai":
        return "AI 연동 흐름"
    if entry.module == "infra":
        return "운영 설정"
    return "문서 정리"


def infer_action(entry: CommitEntry) -> str:
    """subject의 동사를 실제 업무 수행 표현으로 바꾼다."""
    subject = compact_subject(entry.subject).lower()
    if any(token in subject for token in ("add", "create", "추가", "구현")):
        return "추가"
    if any(token in subject for token in ("fix", "bug", "수정", "보정", "해결")):
        return "보정"
    if any(token in subject for token in ("remove", "delete", "drop", "삭제", "제거")):
        return "정리"
    if any(token in subject for token in ("refactor", "rename", "split", "정리", "분리", "개선")):
        return "개선"
    if any(token in subject for token in ("test", "검증", "테스트")):
        return "검증"
    if any(token in subject for token in ("docs", "document", "문서")):
        return "문서화"
    if any(token in subject for token in ("connect", "link", "wire", "연결", "연동")):
        return "연결"
    return "반영"


def infer_effect(entry: CommitEntry, object_name: str) -> str:
    """작업 대상별 사용자·운영 효과를 자연문 조각으로 만든다."""
    lowered = object_name.lower()
    if "컨트롤러" in object_name or "요청" in object_name:
        return "요청과 응답이 같은 흐름으로 처리되도록"
    if "서비스" in object_name:
        return "후처리 흐름이 중간에 끊기지 않도록"
    if "조회" in object_name:
        return "목록과 상세 조건이 서로 어긋나지 않도록"
    if "테스트" in object_name:
        return "같은 문제가 다시 생겼을 때 바로 잡을 수 있도록"
    if "화면" in object_name or "hook" in lowered:
        return "사용자 조작 후 상태가 화면에 분명히 남도록"
    if "AI" in object_name:
        return "입력과 결과 품질이 흔들리지 않도록"
    if "배포" in object_name or "운영" in object_name:
        return "staging 운영 환경에서 설정 차이가 줄어들도록"
    if entry.module == "docs":
        return "팀원이 같은 절차로 확인할 수 있도록"
    return "다음 작업자가 같은 흐름을 이어갈 수 있도록"


def infer_validation(entry: CommitEntry, object_name: str) -> str:
    """작업 후 확인한 내용을 두 번째 문장에 쓸 수 있게 분류한다."""
    if "테스트" in object_name:
        return "실패 조건과 통과 조건"
    if "화면" in object_name or "hook" in object_name:
        return "사용자 동선과 표시 상태"
    if "AI" in object_name:
        return "검색어 예시와 응답 품질"
    if "배포" in object_name or "운영" in object_name:
        return "환경 변수와 배포 절차"
    if entry.module == "api":
        return "요청 파라미터와 응답 형태"
    return "변경 전후 흐름"


def infer_work_context(entry: CommitEntry) -> WorkContext:
    """git 이력의 subject/files를 업무 행위 중심의 문장 재료로 변환한다."""
    object_name = infer_object_name(entry)
    return WorkContext(
        area=MODULE_LABELS.get(entry.module, entry.module.upper()),
        subject=sanitize_copy_fragment(compact_subject(entry.subject)),
        object_name=object_name,
        action=infer_action(entry),
        effect=infer_effect(entry, object_name),
        validation=infer_validation(entry, object_name),
    )


def copy_context(entry: CommitEntry, worklog_id: int, author_id: int, status_code: str) -> dict[str, str]:
    """request/work/status 문구가 같은 업무 맥락을 공유하도록 포맷 인자를 만든다."""
    profile = persona_for(author_id)
    work = infer_work_context(entry)
    focus = profile.focus_terms[stable_index((worklog_id, author_id, work.subject), len(profile.focus_terms))]
    tone = profile.tone_markers[stable_index((entry.module, worklog_id, status_code), len(profile.tone_markers))]
    return {
        "area": work.area,
        "subject": work.subject,
        "subject_eun": josa(work.subject, "은", "는"),
        "object_name": work.object_name,
        "object_eul": josa(work.object_name, "을", "를"),
        "action": work.action,
        "effect": work.effect,
        "validation": work.validation,
        "validation_eun": josa(work.validation, "은", "는"),
        "validation_eul": josa(work.validation, "을", "를"),
        "focus": focus,
        "tone": tone,
        "worklog_id": str(worklog_id),
        "author_id": str(author_id),
        "status_code": status_code,
    }


def request_content_for(entry: CommitEntry, worklog_id: int, author_id: int, status_code: str) -> tuple[str, int]:
    """작성자 관심사와 업무 맥락을 섞어 실제 요청처럼 보이는 문구를 만든다."""
    context = copy_context(entry, worklog_id, author_id, status_code)
    templates = (
        "{area} 영역의 {subject} 작업에서 {object_name}까지 같이 봐 달라는 요청을 받았다.",
        "{object_name} {action} 결과가 실제 업무 흐름에 맞는지 {focus} 관점으로 확인하기로 했다.",
        "{subject} 건은 {effect} {object_name}부터 점검해 달라는 요청이 있었다.",
        "{tone} {object_eul} 살펴보고 {validation}에 문제가 없는지 정리해 달라는 요청을 받았다.",
        "{area} 작업 진행 중 {focus} 이슈가 남지 않도록 {subject} 범위를 다시 확인하기로 했다.",
        "{object_name} 변경 후 사용자가 겪는 흐름이 자연스러운지 확인하고 필요한 후속 조치를 적어 달라는 요청이 있었다.",
        "{subject_eun} {focus} 판단이 필요한 건이라 {validation}까지 같이 확인해 달라는 요청이 있었다.",
        "{area} 담당자와 맞춰 보며 {object_name} {action} 이후 남는 확인 항목을 정리하기로 했다.",
        "{object_name} 처리 흐름이 끊기지 않는지 보고, 필요한 경우 {focus} 항목을 바로 보강하기로 했다.",
        "{validation} 확인이 필요한 작업이라 {subject} 범위를 먼저 정리해 달라는 요청을 받았다.",
    )
    variant = stable_index((worklog_id, author_id, entry.module, entry.subject, status_code), len(templates))
    return templates[variant].format(**context), variant


def work_content_for(entry: CommitEntry, worklog_id: int, author_id: int, status_code: str, copy_variant: int) -> str:
    """수행 작업과 확인 결과가 분리된 한국어 업무 본문을 만든다."""
    context = copy_context(entry, worklog_id, author_id, status_code)
    lead_templates = (
        "{object_name}에서 {subject}에 필요한 {action} 작업을 처리하고, {effect} 흐름을 맞췄다.",
        "{area} 영역의 {object_eul} 살펴 {subject} 작업에 맞게 {action} 범위를 정리했다.",
        "{tone} {object_eul} 확인하면서 {subject} 처리 순서와 예외 상황을 함께 정돈했다.",
        "{subject} 작업을 진행하며 {object_name}의 입력, 처리, 결과가 한 흐름으로 이어지게 조정했다.",
        "{focus} 관점에서 {object_name} {action} 범위를 다시 나누고 필요한 확인 항목을 묶었다.",
    )
    lead = lead_templates[copy_variant % len(lead_templates)].format(**context)
    if status_code == "PENDING":
        closings = (
            "{validation_eun} 아직 추가 확인이 필요해 다음 작업에서 {focus}부터 이어볼 예정이다.",
            "남은 {validation} 확인이 있어 지금은 {focus} 목록에 올려 두었다.",
            "{focus} 판단에 필요한 {validation_eul} 더 모아야 해서 완료 처리는 보류했다.",
        )
    elif status_code == "IN_PROGRESS":
        closings = (
            "현재는 {validation_eul} 확인했고, {focus} 관점의 마지막 점검이 남아 있다.",
            "{validation_eun} 보는 중이고, 이어서 {focus} 항목을 맞춰 볼 예정이다.",
            "일부 흐름은 확인했지만 {focus} 관련 케이스를 한 번 더 눌러 보는 중이다.",
        )
    else:
        closings = (
            "{validation_eul} 다시 확인했고, {focus} 관련 남은 문제는 따로 발견되지 않았다.",
            "{focus} 관점에서 {validation_eul} 확인해 추가 조치 없이 마무리할 수 있는 상태로 봤다.",
            "{validation_eul} 확인한 뒤 QA에서 다시 볼 항목만 따로 남겼다.",
        )
    closing = closings[stable_index((worklog_id, author_id, entry.subject, copy_variant), len(closings))]
    return f"{lead} {closing.format(**context)}"


def build_worklogs(commits: Sequence[CommitEntry], team_id: int, user_start_id: int, worklog_start_id: int) -> list[WorklogEntry]:
    """선택된 커밋을 고정 ID 범위의 ACODIAN 업무일지 행으로 변환한다."""
    user_ids = tuple(range(user_start_id, user_start_id + len(USER_NAMES)))
    worklogs: list[WorklogEntry] = []
    for index, entry in enumerate(commits):
        worklog_id = worklog_start_id + index
        author_id = author_id_for(entry, index, user_ids)
        status_code = STATUS_SEQUENCE[index % len(STATUS_SEQUENCE)]
        importance_code = IMPORTANCE_SEQUENCE[index % len(IMPORTANCE_SEQUENCE)]
        commit_day = date.fromisoformat(entry.commit_date)
        instruction_day = commit_day
        due_day = commit_day + timedelta(days=2 if status_code == "PENDING" else 1)
        completion_day = commit_day if status_code == "COMPLETED" else None
        created_at = f"{instruction_day.isoformat()} 10:{index % 6}{index % 10}:00"
        updated_at = f"{(completion_day or instruction_day).isoformat()} 18:{index % 6}{index % 10}:00"
        actual_hours = [1.0, 1.5, 2.0, 2.5, 3.0][index % 5]
        request, copy_variant = request_content_for(entry, worklog_id, author_id, status_code)
        worklogs.append(
            WorklogEntry(
                worklog_id=worklog_id,
                author_id=author_id,
                team_id=team_id,
                title=title_for(entry),
                request_content=request,
                work_content=work_content_for(entry, worklog_id, author_id, status_code, copy_variant),
                status_code=status_code,
                importance_code=importance_code,
                actual_hours=actual_hours,
                instruction_date=instruction_day.isoformat(),
                due_date=due_day.isoformat(),
                completion_date=completion_day.isoformat() if completion_day else None,
                created_at=created_at,
                updated_at=updated_at,
                commit=entry,
                copy_persona=persona_for(author_id).source,
                copy_variant=copy_variant,
            )
        )
    return worklogs


def render_team_user_sql(team_id: int, user_start_id: int) -> str:
    """ACODIAN 팀·사용자·멤버십 seed SQL을 현재 post-migration 컬럼 기준으로 만든다."""
    user_rows = []
    for offset, name in enumerate(USER_NAMES):
        user_id = user_start_id + offset
        email = f"acodian{offset + 1:02d}@dev.local"
        phone = f"010-9201-{offset + 1:04d}"
        role_code = "TEAM_LEAD" if offset == 0 else "MEMBER"
        title_name = "팀장" if offset == 0 else "팀원"
        user_rows.append(
            f"  ({sql(user_id)}, 3, {sql(name)}, {sql(email)}, {sql(PASSWORD_HASH)}, {sql(POSITIONS[offset])}, {sql(title_name)}, '2026-05-01', {sql(role_code)}, {sql(f'{DUMMY_PROFILE_BASE}/{offset + 1:02d}.png')}, {sql(phone)}, 'ACTIVE')"
        )
    admin_values = f"({user_start_id}, {team_id})"
    membership_rows = []
    for offset, role in enumerate(TEAM_ROLES):
        user_id = user_start_id + offset
        membership_rows.append(
            f"  ({user_id}, {team_id}, {sql(role)}, '주담당', true, 'ACTIVE', {sql(offset == 0)})"
        )
    return "\n".join([
        "-- ACODIAN team/user dev seed generated from git log",
        "-- Scope: tb_user, tb_team, tb_team_admin, tb_user_team",
        "BEGIN;",
        "",
        "INSERT INTO tb_user (user_id, department_id, user_name, email, password_hash, position_name, title_name, join_date, role_code, profile_image_url, phone, employment_status) VALUES",
        ",\n".join(user_rows),
        "ON CONFLICT (user_id) DO UPDATE SET",
        "  department_id = EXCLUDED.department_id,",
        "  user_name = EXCLUDED.user_name,",
        "  email = EXCLUDED.email,",
        "  password_hash = EXCLUDED.password_hash,",
        "  position_name = EXCLUDED.position_name,",
        "  title_name = EXCLUDED.title_name,",
        "  join_date = EXCLUDED.join_date,",
        "  role_code = EXCLUDED.role_code,",
        "  profile_image_url = EXCLUDED.profile_image_url,",
        "  phone = EXCLUDED.phone,",
        "  employment_status = EXCLUDED.employment_status;",
        "",
        "INSERT INTO tb_team (team_id, team_name, status_code, description, start_date, expected_end_date, deleted_at) VALUES",
        f"  ({team_id}, {sql(TEAM_NAME)}, 'ACTIVE', {sql('AX-WMS 실제 개발 이력을 바탕으로 API, WEB, AI 업무를 추적하는 ACODIAN 개발용 팀')}, '2026-05-01', '2026-06-30', NULL)",
        "ON CONFLICT (team_id) DO UPDATE SET",
        "  team_name = EXCLUDED.team_name,",
        "  status_code = EXCLUDED.status_code,",
        "  description = EXCLUDED.description,",
        "  start_date = EXCLUDED.start_date,",
        "  expected_end_date = EXCLUDED.expected_end_date,",
        "  deleted_at = EXCLUDED.deleted_at;",
        "",
        "INSERT INTO tb_team_admin (user_id, team_id)",
        "SELECT v.user_id, v.team_id",
        f"FROM (VALUES {admin_values}) AS v(user_id, team_id)",
        "ON CONFLICT (user_id, team_id) DO NOTHING;",
        "",
        "INSERT INTO tb_user_team (user_id, team_id, team_role, allocation, is_primary, status_code, is_leader) VALUES",
        ",\n".join(membership_rows),
        "ON CONFLICT (user_id, team_id) DO UPDATE SET",
        "  team_role = EXCLUDED.team_role,",
        "  allocation = EXCLUDED.allocation,",
        "  is_primary = EXCLUDED.is_primary,",
        "  status_code = EXCLUDED.status_code,",
        "  is_leader = EXCLUDED.is_leader;",
        "",
        "SELECT setval(pg_get_serial_sequence('tb_user', 'user_id'), GREATEST((SELECT MAX(user_id) FROM tb_user), 1), true);",
        "SELECT setval(pg_get_serial_sequence('tb_team', 'team_id'), GREATEST((SELECT MAX(team_id) FROM tb_team), 1), true);",
        "SELECT setval(pg_get_serial_sequence('tb_team_admin', 'team_admin_id'), GREATEST((SELECT MAX(team_admin_id) FROM tb_team_admin), 1), true);",
        "SELECT setval(pg_get_serial_sequence('tb_user_team', 'user_team_id'), GREATEST((SELECT MAX(user_team_id) FROM tb_user_team), 1), true);",
        "",
        "COMMIT;",
        "",
    ])


def render_worklog_sql(worklogs: Sequence[WorklogEntry]) -> str:
    """ACODIAN 업무일지 seed SQL을 manifest와 같은 순서로 만든다."""
    rows = []
    for item in worklogs:
        rows.append(
            "  (" + ", ".join([
                sql(item.worklog_id),
                sql(item.author_id),
                sql(item.team_id),
                sql(item.title),
                sql(item.request_content),
                sql(item.work_content),
                sql(item.status_code),
                sql(item.importance_code),
                sql(item.actual_hours),
                sql(item.instruction_date),
                sql(item.due_date),
                sql(item.completion_date),
                "NULL",
                "false",
                "'COMPLETED'",
                "false",
                sql(item.created_at),
                sql(item.updated_at),
            ]) + ")"
        )
    return "\n".join([
        "-- ACODIAN git-log worklog dev seed",
        "-- Generated from source history; re-run generator for deterministic output.",
        "BEGIN;",
        "",
        "INSERT INTO tb_worklog (worklog_id, author_id, team_id, title, request_content, work_content, status_code, importance_code, actual_hours, instruction_date, due_date, completion_date, ai_summary, ai_summary_edited, ai_processing_status, is_deleted, created_at, updated_at) VALUES",
        ",\n".join(rows),
        "ON CONFLICT (worklog_id) DO UPDATE SET",
        "  author_id = EXCLUDED.author_id,",
        "  team_id = EXCLUDED.team_id,",
        "  title = EXCLUDED.title,",
        "  request_content = EXCLUDED.request_content,",
        "  work_content = EXCLUDED.work_content,",
        "  status_code = EXCLUDED.status_code,",
        "  importance_code = EXCLUDED.importance_code,",
        "  actual_hours = EXCLUDED.actual_hours,",
        "  instruction_date = EXCLUDED.instruction_date,",
        "  due_date = EXCLUDED.due_date,",
        "  completion_date = EXCLUDED.completion_date,",
        "  ai_summary = EXCLUDED.ai_summary,",
        "  ai_summary_edited = EXCLUDED.ai_summary_edited,",
        "  ai_processing_status = EXCLUDED.ai_processing_status,",
        "  is_deleted = EXCLUDED.is_deleted,",
        "  created_at = EXCLUDED.created_at,",
        "  updated_at = EXCLUDED.updated_at;",
        "",
        "SELECT setval(pg_get_serial_sequence('tb_worklog', 'worklog_id'), GREATEST((SELECT MAX(worklog_id) FROM tb_worklog), 1), true);",
        "",
        "COMMIT;",
        "",
    ])


def status_steps(item: WorklogEntry) -> list[tuple[str | None, str, str, str]]:
    """업무일지 최종 상태와 일치하는 상태 이력 단계를 만든다."""
    profile = persona_for(item.author_id)
    pending_reasons = profile.status_reasons["PENDING"]
    progress_reasons = profile.status_reasons["IN_PROGRESS"]
    completed_reasons = profile.status_reasons["COMPLETED"]
    steps: list[tuple[str | None, str, str, str]] = [
        (None, "PENDING", pending_reasons[item.copy_variant % len(pending_reasons)], item.created_at),
    ]
    if item.status_code in ("IN_PROGRESS", "COMPLETED"):
        steps.append(("PENDING", "IN_PROGRESS", progress_reasons[(item.copy_variant + 1) % len(progress_reasons)], item.created_at))
    if item.status_code == "COMPLETED":
        steps.append(("IN_PROGRESS", "COMPLETED", completed_reasons[(item.copy_variant + 2) % len(completed_reasons)], item.updated_at))
    return steps


def render_status_history_sql(worklogs: Sequence[WorklogEntry]) -> str:
    """모든 ACODIAN 업무일지의 최종 상태를 재현하는 상태 이력 SQL을 만든다."""
    rows = []
    for item in worklogs:
        for previous, new, reason, changed_at in status_steps(item):
            rows.append(
                "  (" + ", ".join([
                    sql(item.worklog_id),
                    sql(previous),
                    sql(new),
                    sql(reason),
                    sql(changed_at),
                    sql(item.author_id),
                ]) + ")"
            )
    return "\n".join([
        "-- ACODIAN worklog status history dev seed",
        "BEGIN;",
        "",
        f"DELETE FROM tb_worklog_status_history WHERE worklog_id BETWEEN {worklogs[0].worklog_id} AND {worklogs[-1].worklog_id};",
        "",
        "INSERT INTO tb_worklog_status_history (worklog_id, previous_status_code, new_status_code, reason, changed_at, changed_by) VALUES",
        ",\n".join(rows),
        ";",
        "",
        "SELECT setval(pg_get_serial_sequence('tb_worklog_status_history', 'history_id'), GREATEST((SELECT MAX(history_id) FROM tb_worklog_status_history), 1), true);",
        "",
        "COMMIT;",
        "",
    ])


def dependency_pairs(worklogs: Sequence[WorklogEntry]) -> list[tuple[int, int]]:
    """cycle 없이 depth 4 이상 chain을 보장하는 업무일지 의존성 목록을 만든다."""
    ids = [item.worklog_id for item in worklogs]
    pairs: list[tuple[int, int]] = []
    for index in range(1, min(5, len(ids))):
        pairs.append((ids[index], ids[index - 1]))
    for index in range(5, len(ids), 5):
        pairs.append((ids[index], ids[index - 2]))
    return pairs


def render_dependency_sql(worklogs: Sequence[WorklogEntry]) -> str:
    """명시 ID 없이 relation table에만 ACODIAN 업무 의존성을 삽입한다."""
    rows = [f"  ({current_id}, {depends_on_id}, '2026-05-19 09:00:00')" for current_id, depends_on_id in dependency_pairs(worklogs)]
    return "\n".join([
        "-- ACODIAN worklog dependency dev seed",
        "BEGIN;",
        "",
        f"DELETE FROM tb_worklog_dependency WHERE worklog_id BETWEEN {worklogs[0].worklog_id} AND {worklogs[-1].worklog_id} OR depends_on_worklog_id BETWEEN {worklogs[0].worklog_id} AND {worklogs[-1].worklog_id};",
        "",
        "INSERT INTO tb_worklog_dependency (worklog_id, depends_on_worklog_id, created_at) VALUES",
        ",\n".join(rows),
        "ON CONFLICT (worklog_id, depends_on_worklog_id) DO NOTHING;",
        "",
        "SELECT setval(pg_get_serial_sequence('tb_worklog_dependency', 'dependency_id'), GREATEST((SELECT MAX(dependency_id) FROM tb_worklog_dependency), 1), true);",
        "",
        "COMMIT;",
        "",
    ])


def manifest_for(base_ref: str, worklogs: Sequence[WorklogEntry], team_id: int, user_start_id: int) -> dict[str, object]:
    """SQL 검증기가 사용할 commit ↔ worklog 1:1 manifest를 만든다."""
    return {
        "schema_version": 1,
        "generated_at": f"{worklogs[-1].commit.commit_date}T00:00:00Z",
        "generator": ".codex/scripts/generate_acodian_gitlog_dev_seed.py",
        "base_ref": base_ref,
        "team": {"team_id": team_id, "team_name": TEAM_NAME},
        "persona_sources": {
            str(user_id): {
                "source": profile.source,
                "label": profile.label,
                "keywords": list(profile.keywords),
            }
            for user_id, profile in sorted(PERSONA_PROFILES.items())
            if user_start_id <= user_id < user_start_id + len(USER_NAMES)
        },
        "users": [
            {
                "user_id": user_start_id + offset,
                "user_name": name,
                "email": f"acodian{offset + 1:02d}@dev.local",
                "dummy_profile": True,
                "is_leader": offset == 0,
            }
            for offset, name in enumerate(USER_NAMES)
        ],
        "worklogs": [
            {
                "worklog_id": item.worklog_id,
                "team_id": item.team_id,
                "author_id": item.author_id,
                "status_code": item.status_code,
                "module": item.commit.module,
                "copy_persona": item.copy_persona,
                "copy_variant": item.copy_variant,
                "commit": {
                    "hash": item.commit.hash,
                    "date": item.commit.commit_date,
                    "author": item.commit.author,
                    "subject": item.commit.subject,
                    "files": list(item.commit.files[:20]),
                },
            }
            for item in worklogs
        ],
        "dependency_edges": [
            {"worklog_id": current_id, "depends_on_worklog_id": depends_on_id}
            for current_id, depends_on_id in dependency_pairs(worklogs)
        ],
        "constraints": {
            "worklog_count": len(worklogs),
            "required_modules": list(REQUIRED_MODULES),
            "required_depth": 4,
            "user_id_range": [user_start_id, user_start_id + len(USER_NAMES) - 1],
            "worklog_id_range": [worklogs[0].worklog_id, worklogs[-1].worklog_id],
        },
    }


def write_outputs(out_dir: Path, manifest_path: Path, worklogs: Sequence[WorklogEntry], base_ref: str, team_id: int, user_start_id: int) -> None:
    """생성 대상 SQL 4개와 manifest 1개를 원자적으로 덮어쓴다."""
    out_dir.mkdir(parents=True, exist_ok=True)
    files = {
        "14_acodian-team-user-seed.sql": render_team_user_sql(team_id, user_start_id),
        "15_acodian-gitlog-worklog-seed.sql": render_worklog_sql(worklogs),
        "16_acodian-worklog-status-history.sql": render_status_history_sql(worklogs),
        "17_acodian-worklog-dependency.sql": render_dependency_sql(worklogs),
    }
    for name, content in files.items():
        (out_dir / name).write_text(content, encoding="utf-8")
    manifest_path.parent.mkdir(parents=True, exist_ok=True)
    manifest = manifest_for(base_ref, worklogs, team_id, user_start_id)
    manifest_path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def parse_args(argv: Sequence[str]) -> argparse.Namespace:
    """CLI 인자를 PRD/Test Spec 기본값과 같은 값으로 해석한다."""
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base-ref", default=None, help="git ref to read; defaults to origin/dev, upstream, then HEAD")
    parser.add_argument("--count", type=int, default=DEFAULT_COUNT, help="target worklog count")
    parser.add_argument("--out-dir", type=Path, default=Path("api/src/main/resources/db/dev-seed"))
    parser.add_argument("--team-id", type=int, default=DEFAULT_TEAM_ID)
    parser.add_argument("--user-start-id", type=int, default=DEFAULT_USER_START_ID)
    parser.add_argument("--worklog-start-id", type=int, default=DEFAULT_WORKLOG_START_ID)
    parser.add_argument("--manifest", type=Path, default=Path("api/src/main/resources/db/dev-seed/acodian-gitlog-worklog-manifest.json"))
    return parser.parse_args(argv)


def main(argv: Sequence[str]) -> int:
    """git log를 읽어 ACODIAN 개발용 seed pack을 생성한다."""
    args = parse_args(argv)
    if not 48 <= args.count <= 52:
        raise SystemExit("--count must stay within acceptance range 48..52")
    base_ref = resolve_base_ref(args.base_ref)
    commits = parse_git_log(base_ref)
    selected = select_commits(commits, args.count)
    worklogs = build_worklogs(selected, args.team_id, args.user_start_id, args.worklog_start_id)
    write_outputs(args.out_dir, args.manifest, worklogs, base_ref, args.team_id, args.user_start_id)
    modules = {item.commit.module for item in worklogs}
    print(f"Generated {len(worklogs)} ACODIAN worklogs from {base_ref}")
    print(f"Modules: {', '.join(sorted(modules))}")
    print(f"Output dir: {args.out_dir}")
    print(f"Manifest: {args.manifest}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
