from __future__ import annotations

import argparse
import re
from collections import Counter, defaultdict
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable


ROOT = Path(__file__).resolve().parents[1]


@dataclass(frozen=True)
class Worklog:
    worklog_id: int
    author_id: int
    team_id: int
    title: str
    request_content: str
    work_content: str
    status_code: str
    importance_code: str
    actual_hours: float | None
    instruction_date: str | None
    due_date: str | None
    completion_date: str | None
    is_deleted: bool
    source: str


@dataclass(frozen=True)
class User:
    user_id: int
    department_id: int | None
    user_name: str
    role_code: str


@dataclass(frozen=True)
class Team:
    team_id: int
    team_name: str
    status_code: str
    description: str
    start_date: str
    expected_end_date: str


@dataclass(frozen=True)
class EvaluationReviewStats:
    exact_duplicates_before: int
    exact_duplicates_after: int
    repeated_phrase_hits_before: int
    repeated_phrase_hits_after: int
    rewritten_rows: int


TAG_RULES: list[tuple[str, tuple[str, ...]]] = [
    ("AI 검색", ("AI 검색", "RAG", "임베딩", "pgvector", "유사 업무 검색")),
    ("임베딩", ("임베딩", "벡터")),
    ("검색 품질", ("검색 품질", "검색 결과", "유사 업무", "자연어 질의")),
    ("데이터 정합성", ("데이터 정합성", "정합성", "불일치", "차이", "누락 데이터")),
    ("데이터 품질", ("데이터 품질", "품질 리포트", "품질 진단", "프로파일링")),
    ("기준정보", ("기준정보", "기준 코드", "기준코드", "메타데이터", "기준 사전")),
    ("정산", ("정산", "일마감", "세금계산서", "회계", "보정 승인")),
    ("WMS", ("WMS", "창고", "재고", "입출고")),
    ("배치", ("배치", "스케줄", "재실행", "적재", "ETL")),
    ("API", ("API", "외부 연동", "연동", "응답", "파라미터")),
    ("JWT", ("JWT", "토큰", "인증")),
    ("권한", ("권한", "접근 제어", "인가", "권한 매트릭스")),
    ("대시보드", ("대시보드", "지표 화면", "화면", "KPI")),
    ("KPI", ("KPI", "지표", "처리량", "장애 건수")),
    ("인프라", ("인프라", "서버", "환경", "방화벽", "접속")),
    ("배포", ("배포", "릴리즈", "파이프라인", "아티팩트", "캐시")),
    ("UAT", ("UAT", "사용자 검수", "검수")),
    ("장애분석", ("장애", "P0", "알람 코드", "장애 상담")),
    ("회귀테스트", ("회귀", "재현", "재검증")),
    ("로그분석", ("로그", "이력", "흔적")),
    ("성능개선", ("성능", "응답 시간", "지연", "적체")),
    ("보안", ("보안", "하드닝", "민감")),
    ("알림", ("알림", "배송 알림", "재시도 큐")),
    ("풀필먼트", ("풀필먼트", "피킹", "출고", "배송 추적")),
    ("수급", ("부품", "수급", "결품", "협력사", "납기")),
    ("생산계획", ("생산", "생산 라인", "생산 계획")),
    ("수주", ("수주", "B2B", "주문", "출고 승인")),
    ("스마트팩토리", ("스마트팩토리", "설비", "알람", "부품 교체")),
    ("상담요약", ("상담", "요약", "상담 기록")),
    ("고객사 대응", ("고객사", "현업", "문의", "운영 커뮤니케이션")),
    ("요구사항", ("요구사항", "요건", "요청 정리")),
    ("리포트", ("리포트", "보고", "현황 보고")),
]

STAGE_RULES: list[tuple[str, tuple[str, ...]]] = [
    ("문의 접수", ("접수", "재현 요청", "문의")),
    ("원인 분석", ("원인", "분석", "후보 분리")),
    ("수정 반영", ("수정", "반영", "작업 진행")),
    ("기준 보정", ("보정", "기준")),
    ("검증", ("검증", "재검증", "결과 정리")),
    ("보류 관리", ("보류", "대기")),
    ("검토 보고", ("검토", "승인", "보고")),
    ("운영 기준 보강", ("운영 기준", "기준 보강", "개선안")),
]

SKILL_TAG_MAP: dict[str, str] = {
    "AI 검색": "AI 검색 품질 개선",
    "임베딩": "임베딩 기반 검색 설계",
    "검색 품질": "검색 결과 품질 검증",
    "데이터 정합성": "데이터 정합성 분석",
    "데이터 품질": "데이터 품질 진단",
    "기준정보": "기준정보 관리",
    "정산": "정산 데이터 분석",
    "WMS": "WMS 운영 분석",
    "배치": "배치 운영 점검",
    "API": "API 연동 분석",
    "JWT": "인증 토큰 검증",
    "권한": "권한 정책 검토",
    "대시보드": "운영 대시보드 설계",
    "KPI": "KPI 지표 검증",
    "인프라": "인프라 환경 점검",
    "배포": "배포 안정화",
    "UAT": "사용자 검수 대응",
    "장애분석": "장애 원인 분석",
    "회귀테스트": "회귀 테스트",
    "로그분석": "로그 기반 원인 추적",
    "성능개선": "성능 병목 개선",
    "보안": "보안 하드닝",
    "알림": "알림 서비스 안정화",
    "풀필먼트": "풀필먼트 운영 분석",
    "수급": "공급망 데이터 분석",
    "생산계획": "생산 계획 데이터 검증",
    "수주": "B2B 수주 운영 분석",
    "스마트팩토리": "스마트팩토리 장애 분석",
    "상담요약": "AI 상담 요약 품질 검증",
    "고객사 대응": "고객사 이슈 대응",
    "요구사항": "요구사항 구조화",
    "리포트": "운영 리포트 작성",
}

DEPT_HEAD_BY_DEPT = {1: 2, 2: 3, 3: 4}

OPENING_VARIANTS = (
    "{name}님은 '{role}' 역할에서 {titles} 흐름을 맡아 팀 안에서 확인 기준을 세웠다.",
    "{name}님은 '{role}' 역할로 {titles} 등을 처리하며 이슈의 출발점과 후속 확인 범위를 나눠 기록했다.",
    "{name}님은 '{role}' 역할을 수행하면서 {titles} 중심의 업무를 이어 갔고, 판단 근거를 업무일지에 남겼다.",
    "{name}님은 '{role}' 관점에서 {titles} 관련 기록을 정리하며 팀 내 의사결정에 필요한 단서를 제공했다.",
)

STRENGTH_VARIANTS = (
    "주요 강점은 {tags} 영역을 {count}건 다루며 역할 범위를 실무 증거로 남긴 점이다",
    "{tags} 관련 업무가 {count}건 확인되어, 담당 영역을 일회성 대응이 아니라 반복 가능한 방식으로 다룬 점이 보인다",
    "{tags} 축의 기록이 {count}건 누적되어, 본인이 맡은 범위를 흔들리지 않게 좁혀 간 점이 강점이다",
    "{tags} 업무를 {count}건 처리하면서 이슈 맥락과 검증 조건을 같이 남긴 점이 좋다",
)

EVIDENCE_VARIANTS = (
    "완료 처리된 업무는 {completed}건이고 총 투입 시간은 약 {hours:.1f}시간으로 집계된다.",
    "완료 {completed}건, 누적 {hours:.1f}시간의 기록에서 책임 구간이 비교적 선명하게 드러난다.",
    "총 {hours:.1f}시간의 투입과 완료 {completed}건을 보면 팀 일정 안에서 실무 기여가 확인된다.",
    "완료 건수는 {completed}건이며, 약 {hours:.1f}시간의 작업 기록이 남아 있다.",
)

RISK_VARIANTS = (
    "보완점은 {risk}",
    "다만 {risk}",
    "다음에는 {risk}",
    "재검토 의견으로는 {risk}",
)

CONCLUSION_VARIANTS = (
    "종합 판단: 다음 유사 팀에서도 {tags} 영역의 실무 담당으로 배치할 수 있다.",
    "종합 판단: 후속 과제에서는 {tags} 쪽 확인과 정리 역할을 계속 맡겨도 무리가 없다.",
    "종합 판단: {tags} 관련 이슈가 반복될 때 우선 검토자로 세울 만하다.",
    "종합 판단: 같은 유형의 과제에서 {tags} 중심의 안정적인 기여가 기대된다.",
)

REVIEW_REPLACEMENTS = (
    (
        "팀 목표와 연결되는 기록 밀도도 충분했다.",
        (
            "팀 목표에 필요한 근거가 업무 흐름 안에 남아 있다.",
            "팀 목표와 맞닿은 판단 지점이 비교적 분명하게 기록됐다.",
            "팀장이 이어서 볼 수 있는 수준의 맥락이 확보되어 있다.",
            "업무 기록만 보아도 팀 내 역할과 산출물이 크게 어긋나지 않는다.",
        ),
    ),
    (
        "역할 범위를 꾸준히 남겼다",
        (
            "담당 범위를 반복적으로 확인시켰다",
            "맡은 영역의 판단 기준을 남겼다",
            "자신의 책임 구간을 비교적 분명히 했다",
            "후속자가 이어 볼 수 있는 단서를 남겼다",
        ),
    ),
    (
        "역할 범위를 실무 증거로 남긴 점이다",
        (
            "맡은 범위가 기록에서 확인된다는 점이다",
            "담당 범위가 업무 근거와 함께 남았다는 점이다",
            "책임 구간을 실제 기록으로 설명한 점이다",
            "후속 판단에 필요한 실무 단서를 남긴 점이다",
        ),
    ),
    (
        "팀 내 의사결정에 필요한 단서를 제공했다",
        (
            "팀 내 판단에 필요한 근거를 보탰다",
            "후속 의사결정자가 볼 수 있는 기준을 남겼다",
            "검토자가 바로 확인할 수 있는 맥락을 정리했다",
            "팀 리더가 판단할 만한 포인트를 앞에 세웠다",
        ),
    ),
    (
        "안정적인 기여가 기대된다",
        (
            "역할을 기대할 수 있다",
            "검토 역할을 맡길 수 있다",
            "기여를 기대할 수 있다",
            "후속 대응 역할을 기대할 수 있다",
        ),
    ),
)


def split_tuples(values_block: str) -> list[str]:
    tuples: list[str] = []
    in_quote = False
    level = 0
    start: int | None = None
    i = 0
    while i < len(values_block):
        ch = values_block[i]
        if ch == "'":
            if in_quote and i + 1 < len(values_block) and values_block[i + 1] == "'":
                i += 2
                continue
            in_quote = not in_quote
        elif not in_quote:
            if ch == "(":
                if level == 0:
                    start = i + 1
                level += 1
            elif ch == ")":
                level -= 1
                if level == 0 and start is not None:
                    tuples.append(values_block[start:i])
                    start = None
        i += 1
    return tuples


def split_fields(tuple_body: str) -> list[str]:
    fields: list[str] = []
    in_quote = False
    start = 0
    i = 0
    while i < len(tuple_body):
        ch = tuple_body[i]
        if ch == "'":
            if in_quote and i + 1 < len(tuple_body) and tuple_body[i + 1] == "'":
                i += 2
                continue
            in_quote = not in_quote
        elif ch == "," and not in_quote:
            fields.append(tuple_body[start:i].strip())
            start = i + 1
        i += 1
    fields.append(tuple_body[start:].strip())
    return fields


def parse_value(raw: str):
    raw = raw.strip()
    upper = raw.upper()
    if upper == "NULL":
        return None
    if upper == "TRUE":
        return True
    if upper == "FALSE":
        return False
    if raw.startswith("'") and raw.endswith("'"):
        return raw[1:-1].replace("''", "'")
    if re.fullmatch(r"-?\d+", raw):
        return int(raw)
    if re.fullmatch(r"-?\d+\.\d+", raw):
        return float(raw)
    return raw


def parse_standard_insert(sql_text: str, table: str) -> list[dict[str, object]]:
    pattern = re.compile(
        rf"INSERT\s+INTO\s+{re.escape(table)}\s*\((?P<cols>.*?)\)\s+VALUES\s*(?P<vals>.*?);",
        re.IGNORECASE | re.DOTALL,
    )
    rows: list[dict[str, object]] = []
    for match in pattern.finditer(sql_text):
        cols = [c.strip() for c in match.group("cols").replace("\n", " ").split(",")]
        vals = re.split(r"\nON\s+CONFLICT\b", match.group("vals"), maxsplit=1, flags=re.IGNORECASE)[0]
        for tuple_body in split_tuples(vals):
            values = [parse_value(field) for field in split_fields(tuple_body)]
            if len(values) == len(cols):
                rows.append(dict(zip(cols, values)))
    return rows


def parse_values_alias_insert(sql_text: str, table: str) -> list[dict[str, object]]:
    pattern = re.compile(
        rf"INSERT\s+INTO\s+{re.escape(table)}\s*\((?P<cols>.*?)\).*?FROM\s+\(VALUES\s*(?P<vals>.*?)\)\s+AS\s+v",
        re.IGNORECASE | re.DOTALL,
    )
    rows: list[dict[str, object]] = []
    for match in pattern.finditer(sql_text):
        cols = [c.strip() for c in match.group("cols").replace("\n", " ").split(",")]
        for tuple_body in split_tuples(match.group("vals")):
            values = [parse_value(field) for field in split_fields(tuple_body)]
            if len(values) == len(cols):
                rows.append(dict(zip(cols, values)))
    return rows


def q(value: object) -> str:
    if value is None:
        return "NULL"
    if isinstance(value, bool):
        return "true" if value else "false"
    if isinstance(value, (int, float)):
        return str(value)
    return "'" + str(value).replace("'", "''") + "'"


def read_sql(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def parse_inputs(worklog_paths: list[Path], team_seed_paths: list[Path]):
    users: dict[int, User] = {}
    teams: dict[int, Team] = {}
    user_team_roles: dict[tuple[int, int], str] = {}
    team_leads: dict[int, int] = {}

    for path in team_seed_paths:
        text = read_sql(path)
        for row in parse_standard_insert(text, "tb_user"):
            users[int(row["user_id"])] = User(
                user_id=int(row["user_id"]),
                department_id=None if row["department_id"] is None else int(row["department_id"]),
                user_name=str(row["user_name"]),
                role_code=str(row["role_code"]),
            )
        team_rows = parse_standard_insert(text, "tb_team") + parse_values_alias_insert(text, "tb_team")
        for row in team_rows:
            teams[int(row["team_id"])] = Team(
                team_id=int(row["team_id"]),
                team_name=str(row["team_name"]),
                status_code=str(row["status_code"]),
                description=str(row["description"]),
                start_date=str(row["start_date"]),
                expected_end_date=str(row["expected_end_date"]),
            )
        member_rows = parse_standard_insert(text, "tb_user_team") + parse_values_alias_insert(text, "tb_user_team")
        for row in member_rows:
            user_id = int(row["user_id"])
            team_id = int(row["team_id"])
            user_team_roles[(user_id, team_id)] = str(row["team_role"])
            if row.get("is_leader") is True:
                team_leads[team_id] = user_id

    worklogs: list[Worklog] = []
    seen_worklog_ids: set[int] = set()
    for path in worklog_paths:
        for row in parse_standard_insert(read_sql(path), "tb_worklog"):
            worklog_id = int(row["worklog_id"])
            if worklog_id in seen_worklog_ids:
                raise ValueError(f"duplicate worklog_id detected: {worklog_id}")
            seen_worklog_ids.add(worklog_id)
            worklogs.append(
                Worklog(
                    worklog_id=worklog_id,
                    author_id=int(row["author_id"]),
                    team_id=int(row["team_id"]),
                    title=str(row["title"]),
                    request_content="" if row["request_content"] is None else str(row["request_content"]),
                    work_content=str(row["work_content"]),
                    status_code=str(row["status_code"]),
                    importance_code=str(row["importance_code"]),
                    actual_hours=None if row["actual_hours"] is None else float(row["actual_hours"]),
                    instruction_date=None if row["instruction_date"] is None else str(row["instruction_date"]),
                    due_date=None if row["due_date"] is None else str(row["due_date"]),
                    completion_date=None if row["completion_date"] is None else str(row["completion_date"]),
                    is_deleted=bool(row["is_deleted"]),
                    source=path.name,
                )
            )
    return users, teams, user_team_roles, team_leads, worklogs


def tags_for_worklog(worklog: Worklog) -> list[str]:
    text = f"{worklog.title} {worklog.request_content} {worklog.work_content}"
    domain_tags: list[str] = []
    for tag, keywords in TAG_RULES:
        if any(keyword in text for keyword in keywords):
            domain_tags.append(tag)
    stage_tags: list[str] = []
    for tag, keywords in STAGE_RULES:
        if any(keyword in worklog.title or keyword in worklog.request_content for keyword in keywords):
            stage_tags.append(tag)

    tags: list[str] = []
    tags.extend(domain_tags[:3])
    tags.extend(stage_tags[:1])
    if worklog.importance_code == "URGENT":
        tags.append("긴급 대응")
    if worklog.status_code == "ON_HOLD":
        tags.append("보류 관리")
    if len(tags) < 3:
        tags.extend(domain_tags[3:5])
    if not tags:
        tags.append("운영 이슈")

    deduped: list[str] = []
    for tag in tags:
        if tag not in deduped:
            deduped.append(tag)
    return deduped[:5]


def evaluator_for(user: User) -> int | None:
    if user.user_id == 1:
        return None
    if user.role_code == "DEPT_HEAD":
        return 1
    if user.department_id in DEPT_HEAD_BY_DEPT:
        return DEPT_HEAD_BY_DEPT[user.department_id]
    return 1


def sentence_join(items: Iterable[str], limit: int = 3) -> str:
    unique: list[str] = []
    for item in items:
        if item and item not in unique:
            unique.append(item)
    if not unique:
        return "팀 업무"
    return ", ".join(unique[:limit])


def choose_variant(options: tuple[str, ...], team_id: int, user_id: int, salt: int = 0) -> str:
    return options[(team_id * 7 + user_id * 3 + salt) % len(options)]


def evaluation_risk(urgent: int, on_hold: int, completed: int, count: int) -> str:
    if urgent:
        return f"긴급 이슈 {urgent}건의 후속 검증 기준을 더 짧게 남기면 좋다."
    if on_hold:
        return f"보류 업무 {on_hold}건의 외부 의존성과 재개 조건을 더 분리해 두면 좋다."
    if completed < max(1, count // 3):
        return "진행 중 기록이 많은 편이라 완료 기준과 다음 확인자를 더 앞에 써 두면 좋다."
    return "판단 근거를 충분히 남긴 만큼 다음 과제에서는 결론 문장을 조금 더 압축하면 좋다."


def build_evaluations(
    worklogs: list[Worklog],
    users: dict[int, User],
    teams: dict[int, Team],
    user_team_roles: dict[tuple[int, int], str],
    worklog_tags: dict[int, list[str]],
) -> list[tuple[int, int, str]]:
    grouped: dict[tuple[int, int], list[Worklog]] = defaultdict(list)
    for worklog in worklogs:
        if not worklog.is_deleted:
            grouped[(worklog.team_id, worklog.author_id)].append(worklog)

    evaluations: list[tuple[int, int, str]] = []
    for (team_id, user_id), items in sorted(grouped.items()):
        user = users.get(user_id)
        team = teams.get(team_id)
        if not user or not team:
            raise ValueError(f"missing user/team for evaluation: user={user_id}, team={team_id}")
        evaluator_id = evaluator_for(user)
        if evaluator_id is None or evaluator_id == user_id:
            continue

        titles = [item.title for item in items]
        tags = [tag for item in items for tag in worklog_tags[item.worklog_id]]
        tag_counts = Counter(tags)
        main_tags = [tag for tag, _ in tag_counts.most_common(3)]
        completed = sum(1 for item in items if item.status_code == "COMPLETED")
        on_hold = sum(1 for item in items if item.status_code == "ON_HOLD")
        urgent = sum(1 for item in items if item.importance_code == "URGENT")
        hours = sum(item.actual_hours or 0 for item in items)
        dates = [d for item in items for d in (item.instruction_date, item.completion_date or item.due_date) if d]
        period = f"{min(dates)}~{max(dates)}" if dates else f"{team.start_date}~{team.expected_end_date}"
        role = user_team_roles.get((user_id, team_id), "팀 참여자")

        tag_text = sentence_join(main_tags)
        title_text = sentence_join(titles, 2)
        conclusion_tag_text = sentence_join(main_tags, 2)
        opening = choose_variant(OPENING_VARIANTS, team_id, user_id).format(
            name=user.user_name,
            role=role,
            titles=title_text,
        )
        strength = choose_variant(STRENGTH_VARIANTS, team_id, user_id, 1).format(
            tags=tag_text,
            count=len(items),
        )
        evidence = choose_variant(EVIDENCE_VARIANTS, team_id, user_id, 2).format(
            completed=completed,
            hours=hours,
        )
        risk = choose_variant(RISK_VARIANTS, team_id, user_id, 3).format(
            risk=evaluation_risk(urgent, on_hold, completed, len(items))
        )
        conclusion = choose_variant(CONCLUSION_VARIANTS, team_id, user_id, 4).format(
            tags=conclusion_tag_text
        )

        content = (
            f"[팀 평가: {team.team_id}/{team.team_name} | 기간: {period}]\n"
            f"{opening} {strength}. {evidence} {risk} {conclusion}"
        )
        evaluations.append((user_id, evaluator_id, content))
    return evaluations


def evaluation_duplicate_metrics(evaluations: list[tuple[int, int, str]]) -> tuple[int, int]:
    contents = [content for _, _, content in evaluations]
    exact_duplicates = len(contents) - len(set(contents))
    repeated_phrase_hits = 0
    for phrase, _ in REVIEW_REPLACEMENTS:
        repeated_phrase_hits += sum(content.count(phrase) for content in contents)
    return exact_duplicates, repeated_phrase_hits


def review_evaluations_for_duplicates(evaluations: list[tuple[int, int, str]]) -> tuple[list[tuple[int, int, str]], EvaluationReviewStats]:
    exact_before, phrase_before = evaluation_duplicate_metrics(evaluations)
    reviewed: list[tuple[int, int, str]] = []
    rewritten_rows = 0

    for idx, (evaluatee_id, evaluator_id, content) in enumerate(evaluations):
        original = content
        for phrase, replacements in REVIEW_REPLACEMENTS:
            if phrase in content:
                content = content.replace(phrase, replacements[idx % len(replacements)])
        if content != original:
            rewritten_rows += 1
        reviewed.append((evaluatee_id, evaluator_id, content))

    exact_after, phrase_after = evaluation_duplicate_metrics(reviewed)
    return reviewed, EvaluationReviewStats(
        exact_duplicates_before=exact_before,
        exact_duplicates_after=exact_after,
        repeated_phrase_hits_before=phrase_before,
        repeated_phrase_hits_after=phrase_after,
        rewritten_rows=rewritten_rows,
    )


def skill_level(count: int, completed: int, urgent: int, teams: int, is_lead: bool, is_head: bool) -> int:
    score = count + completed * 0.5 + urgent * 0.75 + max(0, teams - 1) * 1.5
    if is_lead:
        score += 3
    if is_head:
        score += 2
    if score >= 24:
        return 5
    if score >= 12:
        return 4
    if score >= 5:
        return 3
    if score >= 2:
        return 2
    return 1


def build_skills(
    worklogs: list[Worklog],
    users: dict[int, User],
    team_leads: dict[int, int],
    worklog_tags: dict[int, list[str]],
) -> list[tuple[int, str, int]]:
    user_tag_stats: dict[int, Counter[str]] = defaultdict(Counter)
    user_tag_completed: dict[int, Counter[str]] = defaultdict(Counter)
    user_tag_urgent: dict[int, Counter[str]] = defaultdict(Counter)
    user_tag_teams: dict[int, dict[str, set[int]]] = defaultdict(lambda: defaultdict(set))

    for worklog in worklogs:
        if worklog.is_deleted:
            continue
        for tag in worklog_tags[worklog.worklog_id]:
            if tag in SKILL_TAG_MAP:
                user_tag_stats[worklog.author_id][tag] += 1
                if worklog.status_code == "COMPLETED":
                    user_tag_completed[worklog.author_id][tag] += 1
                if worklog.importance_code in {"URGENT", "HIGH"}:
                    user_tag_urgent[worklog.author_id][tag] += 1
                user_tag_teams[worklog.author_id][tag].add(worklog.team_id)

    skills: list[tuple[int, str, int]] = []
    for user_id, counter in sorted(user_tag_stats.items()):
        user = users.get(user_id)
        if not user:
            continue
        top_tags = [tag for tag, _ in counter.most_common(7)]
        if len(top_tags) < 3:
            for fallback in ("고객사 대응", "검증", "리포트"):
                if fallback in SKILL_TAG_MAP and fallback not in top_tags:
                    top_tags.append(fallback)
                if len(top_tags) >= 3:
                    break
        is_head = user.role_code in {"DIRECTOR", "DEPT_HEAD"}
        lead_teams = {team_id for team_id, lead_id in team_leads.items() if lead_id == user_id}
        for tag in top_tags[:7]:
            name = SKILL_TAG_MAP[tag]
            level = skill_level(
                count=counter[tag],
                completed=user_tag_completed[user_id][tag],
                urgent=user_tag_urgent[user_id][tag],
                teams=len(user_tag_teams[user_id][tag]),
                is_lead=bool(lead_teams & user_tag_teams[user_id][tag]),
                is_head=is_head,
            )
            skills.append((user_id, name, level))
    return skills


def write_sql(
    path: Path,
    source_paths: list[Path],
    tag_usage: Counter[str],
    worklog_tags: dict[int, list[str]],
    evaluations: list[tuple[int, int, str]],
    skills: list[tuple[int, str, int]],
) -> None:
    lines: list[str] = []
    lines.append("-- iBank worklog enrichment seed SQL")
    lines.append("-- Sources: " + ", ".join(path.name for path in source_paths))
    lines.append("-- Includes: tb_meta_tag, tb_worklog_tag, tb_user_evaluation, tb_user_skill")
    lines.append("BEGIN;")
    lines.append("")

    lines.append("INSERT INTO tb_meta_tag (tag_name, usage_count)")
    lines.append("VALUES")
    tag_rows = [(tag, tag_usage[tag]) for tag in sorted(tag_usage)]
    for idx, row in enumerate(tag_rows):
        comma = "," if idx < len(tag_rows) - 1 else ""
        lines.append(f"  ({q(row[0])}, {row[1]}){comma}")
    lines.append("ON CONFLICT (tag_name) DO UPDATE SET")
    lines.append("  usage_count = tb_meta_tag.usage_count + EXCLUDED.usage_count,")
    lines.append("  updated_at = CURRENT_TIMESTAMP;")
    lines.append("")

    tag_link_rows = [(worklog_id, tag) for worklog_id in sorted(worklog_tags) for tag in worklog_tags[worklog_id]]
    lines.append("INSERT INTO tb_worklog_tag (worklog_id, tag_id, is_ai_generated)")
    lines.append("SELECT v.worklog_id, mt.tag_id, true")
    lines.append("FROM (VALUES")
    for idx, (worklog_id, tag) in enumerate(tag_link_rows):
        comma = "," if idx < len(tag_link_rows) - 1 else ""
        lines.append(f"  ({worklog_id}, {q(tag)}){comma}")
    lines.append(") AS v(worklog_id, tag_name)")
    lines.append("JOIN tb_meta_tag mt ON mt.tag_name = v.tag_name")
    lines.append("ON CONFLICT (worklog_id, tag_id) DO NOTHING;")
    lines.append("")

    lines.append("INSERT INTO tb_user_evaluation (evaluatee_user_id, evaluator_user_id, content)")
    lines.append("SELECT v.evaluatee_user_id, v.evaluator_user_id, v.content")
    lines.append("FROM (VALUES")
    for idx, row in enumerate(evaluations):
        comma = "," if idx < len(evaluations) - 1 else ""
        lines.append(f"  ({row[0]}, {row[1]}, {q(row[2])}){comma}")
    lines.append(") AS v(evaluatee_user_id, evaluator_user_id, content)")
    lines.append("WHERE NOT EXISTS (")
    lines.append("  SELECT 1")
    lines.append("  FROM tb_user_evaluation e")
    lines.append("  WHERE e.evaluatee_user_id = v.evaluatee_user_id")
    lines.append("    AND e.evaluator_user_id = v.evaluator_user_id")
    lines.append("    AND e.content = v.content")
    lines.append(");")
    lines.append("")

    lines.append("INSERT INTO tb_user_skill (user_id, skill_name, skill_level)")
    lines.append("VALUES")
    for idx, row in enumerate(skills):
        comma = "," if idx < len(skills) - 1 else ""
        lines.append(f"  ({row[0]}, {q(row[1])}, {row[2]}){comma}")
    lines.append("ON CONFLICT (user_id, skill_name) DO UPDATE SET")
    lines.append("  skill_level = EXCLUDED.skill_level,")
    lines.append("  updated_at = CURRENT_TIMESTAMP;")
    lines.append("")

    lines.append("COMMIT;")
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def write_tag_sql(
    path: Path,
    source_paths: list[Path],
    tag_usage: Counter[str],
    worklog_tags: dict[int, list[str]],
) -> None:
    lines: list[str] = []
    lines.append("-- iBank worklog tag seed SQL")
    lines.append("-- Sources: " + ", ".join(source.name for source in source_paths))
    lines.append("-- Includes: tb_meta_tag, tb_worklog_tag")
    lines.append("BEGIN;")
    lines.append("")
    lines.append("INSERT INTO tb_meta_tag (tag_name, usage_count)")
    lines.append("VALUES")
    tag_rows = [(tag, tag_usage[tag]) for tag in sorted(tag_usage)]
    for idx, row in enumerate(tag_rows):
        comma = "," if idx < len(tag_rows) - 1 else ""
        lines.append(f"  ({q(row[0])}, {row[1]}){comma}")
    lines.append("ON CONFLICT (tag_name) DO UPDATE SET")
    lines.append("  usage_count = tb_meta_tag.usage_count + EXCLUDED.usage_count,")
    lines.append("  updated_at = CURRENT_TIMESTAMP;")
    lines.append("")

    tag_link_rows = [(worklog_id, tag) for worklog_id in sorted(worklog_tags) for tag in worklog_tags[worklog_id]]
    lines.append("INSERT INTO tb_worklog_tag (worklog_id, tag_id, is_ai_generated)")
    lines.append("SELECT v.worklog_id, mt.tag_id, true")
    lines.append("FROM (VALUES")
    for idx, (worklog_id, tag) in enumerate(tag_link_rows):
        comma = "," if idx < len(tag_link_rows) - 1 else ""
        lines.append(f"  ({worklog_id}, {q(tag)}){comma}")
    lines.append(") AS v(worklog_id, tag_name)")
    lines.append("JOIN tb_meta_tag mt ON mt.tag_name = v.tag_name")
    lines.append("ON CONFLICT (worklog_id, tag_id) DO NOTHING;")
    lines.append("")
    lines.append("COMMIT;")
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def write_evaluation_sql(
    path: Path,
    source_paths: list[Path],
    evaluations: list[tuple[int, int, str]],
) -> None:
    lines: list[str] = []
    lines.append("-- iBank user evaluation seed SQL")
    lines.append("-- Sources: " + ", ".join(source.name for source in source_paths))
    lines.append("-- Includes: tb_user_evaluation")
    lines.append("BEGIN;")
    lines.append("")
    lines.append("INSERT INTO tb_user_evaluation (evaluatee_user_id, evaluator_user_id, content)")
    lines.append("SELECT v.evaluatee_user_id, v.evaluator_user_id, v.content")
    lines.append("FROM (VALUES")
    for idx, row in enumerate(evaluations):
        comma = "," if idx < len(evaluations) - 1 else ""
        lines.append(f"  ({row[0]}, {row[1]}, {q(row[2])}){comma}")
    lines.append(") AS v(evaluatee_user_id, evaluator_user_id, content)")
    lines.append("WHERE NOT EXISTS (")
    lines.append("  SELECT 1")
    lines.append("  FROM tb_user_evaluation e")
    lines.append("  WHERE e.evaluatee_user_id = v.evaluatee_user_id")
    lines.append("    AND e.evaluator_user_id = v.evaluator_user_id")
    lines.append("    AND e.content = v.content")
    lines.append(");")
    lines.append("")
    lines.append("COMMIT;")
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def write_skill_sql(
    path: Path,
    source_paths: list[Path],
    skills: list[tuple[int, str, int]],
) -> None:
    lines: list[str] = []
    lines.append("-- iBank current user skill seed SQL")
    lines.append("-- Sources: " + ", ".join(source.name for source in source_paths))
    lines.append("-- Includes: tb_user_skill")
    lines.append("BEGIN;")
    lines.append("")
    lines.append("INSERT INTO tb_user_skill (user_id, skill_name, skill_level)")
    lines.append("VALUES")
    for idx, row in enumerate(skills):
        comma = "," if idx < len(skills) - 1 else ""
        lines.append(f"  ({row[0]}, {q(row[1])}, {row[2]}){comma}")
    lines.append("ON CONFLICT (user_id, skill_name) DO UPDATE SET")
    lines.append("  skill_level = EXCLUDED.skill_level,")
    lines.append("  updated_at = CURRENT_TIMESTAMP;")
    lines.append("")
    lines.append("COMMIT;")
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def validate(
    worklogs: list[Worklog],
    users: dict[int, User],
    teams: dict[int, Team],
    worklog_tags: dict[int, list[str]],
    evaluations: list[tuple[int, int, str]],
    skills: list[tuple[int, str, int]],
) -> list[str]:
    errors: list[str] = []
    selected_ids = {w.worklog_id for w in worklogs if not w.is_deleted}
    for worklog_id in selected_ids:
        count = len(worklog_tags.get(worklog_id, []))
        if count < 1 or count > 5:
            errors.append(f"worklog {worklog_id} has invalid tag count: {count}")
    tag_pairs = [(wid, tag) for wid, tags in worklog_tags.items() for tag in tags]
    if len(tag_pairs) != len(set(tag_pairs)):
        errors.append("duplicate worklog/tag pair detected")
    for evaluatee_id, evaluator_id, content in evaluations:
        if evaluatee_id == evaluator_id:
            errors.append(f"self evaluation detected: {evaluatee_id}")
        evaluatee = users.get(evaluatee_id)
        if not evaluatee:
            errors.append(f"unknown evaluatee: {evaluatee_id}")
            continue
        allowed = evaluator_for(evaluatee)
        if allowed != evaluator_id:
            errors.append(f"unauthorized evaluator: evaluatee={evaluatee_id}, evaluator={evaluator_id}, allowed={allowed}")
        if "[팀 평가:" not in content:
            errors.append(f"evaluation missing team prefix: evaluatee={evaluatee_id}")
    skill_pairs = [(user_id, skill_name) for user_id, skill_name, _ in skills]
    if len(skill_pairs) != len(set(skill_pairs)):
        errors.append("duplicate user/skill pair detected")
    for user_id, skill_name, level in skills:
        if user_id not in users:
            errors.append(f"unknown skill user: {user_id}")
        if not (1 <= level <= 5):
            errors.append(f"invalid skill level: user={user_id}, skill={skill_name}, level={level}")
    for worklog in worklogs:
        if worklog.author_id not in users:
            errors.append(f"unknown worklog author: {worklog.author_id}")
        if worklog.team_id not in teams:
            errors.append(f"unknown worklog team: {worklog.team_id}")
    return errors


def write_summary(
    path: Path,
    source_paths: list[Path],
    output_paths: list[Path],
    worklogs: list[Worklog],
    tag_usage: Counter[str],
    worklog_tags: dict[int, list[str]],
    evaluations: list[tuple[int, int, str]],
    skills: list[tuple[int, str, int]],
    users: dict[int, User],
    teams: dict[int, Team],
    validation_errors: list[str],
    evaluation_review_stats: EvaluationReviewStats,
) -> None:
    by_source = Counter(w.source for w in worklogs)
    by_team = Counter(w.team_id for w in worklogs)
    by_eval_evaluator = Counter(evaluator_id for _, evaluator_id, _ in evaluations)
    skill_count_by_user = Counter(user_id for user_id, _, _ in skills)
    tag_counts = [len(tags) for tags in worklog_tags.values()]
    avg_tags = sum(tag_counts) / len(tag_counts) if tag_counts else 0

    lines = [
        "# iBank Worklog Enrichment Summary",
        "",
        "## Sources",
        *[f"- `{source.name}`" for source in source_paths],
        "",
        "## Outputs",
        *[f"- `{output.name}`" for output in output_paths],
        "",
        "## Counts",
        f"- worklogs: {len(worklogs)}",
        f"- non-deleted worklogs tagged: {len(worklog_tags)}",
        f"- distinct tags: {len(tag_usage)}",
        f"- worklog tag links: {sum(tag_usage.values())}",
        f"- average tags per worklog: {avg_tags:.2f}",
        f"- team-scoped evaluations: {len(evaluations)}",
        f"- current user skills: {len(skills)}",
        "",
        "## Evaluation Re-Review",
        f"- rewritten rows: {evaluation_review_stats.rewritten_rows}",
        f"- exact duplicate contents before: {evaluation_review_stats.exact_duplicates_before}",
        f"- exact duplicate contents after: {evaluation_review_stats.exact_duplicates_after}",
        f"- repeated stock phrase hits before: {evaluation_review_stats.repeated_phrase_hits_before}",
        f"- repeated stock phrase hits after: {evaluation_review_stats.repeated_phrase_hits_after}",
        "",
        "## Worklogs By Source",
        *[f"- `{source}`: {count}" for source, count in sorted(by_source.items())],
        "",
        "## Teams",
        *[
            f"- {team_id} / {teams[team_id].team_name}: {count} worklogs"
            for team_id, count in sorted(by_team.items())
            if team_id in teams
        ],
        "",
        "## Top Tags",
        *[f"- {tag}: {count}" for tag, count in tag_usage.most_common(20)],
        "",
        "## Evaluations By Evaluator",
        *[
            f"- {evaluator_id} / {users[evaluator_id].user_name}: {count}"
            for evaluator_id, count in sorted(by_eval_evaluator.items())
            if evaluator_id in users
        ],
        "",
        "## Skills By User",
        *[
            f"- {user_id} / {users[user_id].user_name}: {count}"
            for user_id, count in sorted(skill_count_by_user.items())
            if user_id in users
        ],
        "",
        "## Validation",
    ]
    if validation_errors:
        lines.extend([f"- ERROR: {error}" for error in validation_errors])
    else:
        lines.append("- OK: every selected worklog has 1 to 5 tags")
        lines.append("- OK: no duplicate worklog/tag pairs")
        lines.append("- OK: every evaluation uses an authorized evaluator")
        lines.append("- OK: no self-evaluations")
        lines.append("- OK: every skill level is between 1 and 5")
        lines.append("- OK: no duplicate user/skill pairs")
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--worklog-sql",
        action="append",
        required=True,
        help="Worklog SQL file under .codex or absolute path. Repeat for multiple files.",
    )
    parser.add_argument(
        "--team-seed-sql",
        action="append",
        default=[],
        help="Team/user seed SQL file. Repeat for base and active teams.",
    )
    parser.add_argument("--tag-out", default="sql/ibank-worklog-tags-v6-active-111-114.sql")
    parser.add_argument("--evaluation-out", default="sql/ibank-user-evaluations-v6-active-111-114.sql")
    parser.add_argument("--skill-out", default="sql/ibank-user-skills-v6-active-111-114.sql")
    parser.add_argument("--combined-out", default=None)
    parser.add_argument("--summary-out", default="sql/ibank-worklog-enrichment-v6-active-111-114-summary.md")
    args = parser.parse_args()

    worklog_paths = [Path(p) if Path(p).is_absolute() else ROOT / p for p in args.worklog_sql]
    team_seed_inputs = args.team_seed_sql or ["sql/ibank-team-seed.sql", "sql/ibank-active-team-seed-111-114.sql"]
    team_seed_paths = [Path(p) if Path(p).is_absolute() else ROOT / p for p in team_seed_inputs]
    tag_out_path = Path(args.tag_out) if Path(args.tag_out).is_absolute() else ROOT / args.tag_out
    evaluation_out_path = Path(args.evaluation_out) if Path(args.evaluation_out).is_absolute() else ROOT / args.evaluation_out
    skill_out_path = Path(args.skill_out) if Path(args.skill_out).is_absolute() else ROOT / args.skill_out
    combined_out_path = None
    if args.combined_out:
        combined_out_path = Path(args.combined_out) if Path(args.combined_out).is_absolute() else ROOT / args.combined_out
    summary_path = Path(args.summary_out) if Path(args.summary_out).is_absolute() else ROOT / args.summary_out

    users, teams, user_team_roles, team_leads, worklogs = parse_inputs(worklog_paths, team_seed_paths)
    worklog_tags = {worklog.worklog_id: tags_for_worklog(worklog) for worklog in worklogs if not worklog.is_deleted}
    tag_usage = Counter(tag for tags in worklog_tags.values() for tag in tags)
    evaluations = build_evaluations(worklogs, users, teams, user_team_roles, worklog_tags)
    evaluations, evaluation_review_stats = review_evaluations_for_duplicates(evaluations)
    skills = build_skills(worklogs, users, team_leads, worklog_tags)
    validation_errors = validate(worklogs, users, teams, worklog_tags, evaluations, skills)
    if validation_errors:
        raise SystemExit("\n".join(validation_errors))

    tag_out_path.parent.mkdir(parents=True, exist_ok=True)
    evaluation_out_path.parent.mkdir(parents=True, exist_ok=True)
    skill_out_path.parent.mkdir(parents=True, exist_ok=True)
    summary_path.parent.mkdir(parents=True, exist_ok=True)
    write_tag_sql(tag_out_path, worklog_paths, tag_usage, worklog_tags)
    write_evaluation_sql(evaluation_out_path, worklog_paths, evaluations)
    write_skill_sql(skill_out_path, worklog_paths, skills)
    output_paths = [tag_out_path, evaluation_out_path, skill_out_path]
    if combined_out_path is not None:
        combined_out_path.parent.mkdir(parents=True, exist_ok=True)
        write_sql(combined_out_path, worklog_paths, tag_usage, worklog_tags, evaluations, skills)
        output_paths.append(combined_out_path)
    write_summary(
        summary_path,
        worklog_paths,
        output_paths,
        worklogs,
        tag_usage,
        worklog_tags,
        evaluations,
        skills,
        users,
        teams,
        validation_errors,
        evaluation_review_stats,
    )
    print(f"wrote {tag_out_path}")
    print(f"wrote {evaluation_out_path}")
    print(f"wrote {skill_out_path}")
    if combined_out_path is not None:
        print(f"wrote {combined_out_path}")
    print(f"wrote {summary_path}")


if __name__ == "__main__":
    main()
