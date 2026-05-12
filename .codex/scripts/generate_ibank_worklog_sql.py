from __future__ import annotations

import argparse
from pathlib import Path
from datetime import date, datetime, timedelta
import hashlib
import random
import re
from collections import Counter, defaultdict

ROOT = Path(__file__).resolve().parents[2]
TEAM_DIR = ROOT / '.codex' / 'team-md'
SQL_DIR = ROOT / '.codex' / 'sql'
OUT = SQL_DIR / 'ibank-worklog-seed.sql'
SUMMARY = SQL_DIR / 'ibank-worklog-summary.md'
SEED = 20260506
rng = random.Random(SEED)

TEAM_ID_RE = re.compile(r'team_id: (\d+)')
TEAM_NAME_RE = re.compile(r'team_name: (.+)')
START_RE = re.compile(r'start_date: (\d{4}-\d{2}-\d{2})')
END_RE = re.compile(r'expected_end_date: (\d{4}-\d{2}-\d{2})')
TABLE_ROW_RE = re.compile(r'^\|\s*(\d+)\s*\|\s*([^|]+?)\s*\|\s*([^|]+?)\s*\|\s*([^|]+?)\s*\|\s*([^|]+?)\s*\|\s*([^|]+?)\s*\|\s*([^|]+?)\s*\|$', re.M)

TEAM_CONFIG = {
    101: {'count': 72, 'topics': ['P0 회귀', '배치 윈도우 충돌', 'WMS 로그 누락', 'API 응답 이상', '레거시 모듈 분리 후 장애', '운영 체크리스트 누락', '야간 재기동 영향'], 'cross': ['WMS', 'API', '배치', '인프라']},
    102: {'count': 68, 'topics': ['매장 주문 취소', '정산 누락', '에러 토스트 노출', '권한 매트릭스 누락', '화면 상태 꼬임', '기기별 매장 오류', '고객 요청 재현'], 'cross': ['정산', '권한', '대시보드', 'API']},
    103: {'count': 58, 'topics': ['JWT 만료 처리', '접근 제어 누락', '권한 매트릭스 보정', '인증 실패 로그 분석', 'UAT 권한 예외', '보안 예외 승인'], 'cross': ['JWT', '권한', 'UAT', '로그']},
    104: {'count': 74, 'topics': ['레거시 모듈 분리', '스테이징 회귀', 'API 응답 계약 차이', 'UX 상태 처리 누락', '결함 우선순위 재조정', '화면 연동 오류', '배포 후보 정리'], 'cross': ['API', '배포', '대시보드', '결함']},
    105: {'count': 54, 'topics': ['릴리즈 노트 누락', '파이프라인 캐시 꼬임', '아티팩트 정리 실패', '운영 이관 체크리스트 누락', 'UAT 재오픈', '분기 배포 승인'], 'cross': ['배포', '인프라', 'UAT', '운영 이관']},
    106: {'count': 66, 'topics': ['입출고 수량 불일치', '재고 스냅샷 차이', '창고별 수량 편차', '보정 이력 누락', '재고 문의 재발', '배치 후 정합성 흔들림'], 'cross': ['WMS', '배치', '데이터 정합성', 'API']},
    107: {'count': 64, 'topics': ['일마감 정산 차이', '포인트 누락', '주문 취소 후 정산 꼬임', '보정 승인 지연', '회계 검증 재오픈', '매장 문의 반복'], 'cross': ['정산', 'API', '운영 검증', '회계']},
    108: {'count': 56, 'topics': ['중복 데이터', '누락 필드', '기준정보 불일치', '데이터 프로파일링 결과 차이', '품질 리포트 기준 수정', '연계 로그 품질 저하'], 'cross': ['데이터 정합성', 'KPI', '로그', '품질 리포트']},
    109: {'count': 58, 'topics': ['임베딩 품질 저하', '유사 업무 미검출', 'RAG 응답 흔들림', 'pgvector 인덱스 점검', '태그 기준 불일치', '검색어 해석 차이'], 'cross': ['AI 검색', '임베딩', '유사도', 'RAG']},
    110: {'count': 52, 'topics': ['KPI 산식 수정', '장애 건수 집계 차이', '업무 지연 노출 기준', '처리량 비교 오류', '마감 임박 업무 집계', '보고용 필터 조정'], 'cross': ['KPI', '대시보드', '지연 업무', '보고']},
    111: {'count': 36, 'topics': ['부품 결품 위험', '협력사 납기 차이', '입출고 수량 불일치', '부품 기준정보 불일치', '생산 계획 변경 미반영', '공급망 보정 룰 검증'], 'cross': ['마스터데이터', 'WMS', '데이터 정합성', '보정 로직']},
    112: {'count': 34, 'topics': ['배송 알림 누락', '출고 이벤트 지연', '알림 재시도 큐 적체', '배송 상태 화면 차이', '대량 주문 알림 중복', '풀필먼트 리포트 산식'], 'cross': ['배치', '대시보드', '배포 검증', '고객 안내']},
    113: {'count': 34, 'topics': ['B2B 수주 변경', '출고 수량과 청구 수량 차이', '단가 조건 변경 지연', '정산 보정 승인', '세금계산서 재발행 요청', '외부 주문 API 응답 차이'], 'cross': ['정산', 'WMS', '요구사항', 'API']},
    114: {'count': 34, 'topics': ['알람 코드 누락', 'AI 장애 상담 요약 품질', '엔지니어 수정본 차이', '설비 재가동 조건 누락', '요약 품질 KPI 산식', 'AI 평가셋 재구성'], 'cross': ['AI 검색', 'KPI', 'API', '품질 리포트']},
}

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
    109: ['레거시 모듈', '검색', '임베딩', '로그'],
    110: ['릴리즈', '운영 이관', '대시보드', '보고'],
}

TITLE_PATTERNS = {
    'request': ['{topic} 관련 1차 확인 요청', '{topic} 이슈 접수 및 영향 범위 확인', '{topic} 현상 재현 요청'],
    'analyze': ['{topic} 원인 분석', '{topic} 영향 범위 점검', '{topic} 로그/데이터 비교 분석'],
    'fix': ['{topic} 대응안 반영', '{topic} 수정 적용', '{topic} 보정 작업 진행'],
    'verify': ['{topic} 수정 후 재검증', '{topic} 재현 결과 재확인', '{topic} 검증 결과 정리'],
    'review': ['{topic} 검토 의견 정리', '{topic} 승인 전 검토', '{topic} 후속 조치 검토'],
    'improve': ['{topic} 재발 방지안 정리', '{topic} 개선안 반영', '{topic} 운영 기준 보강'],
    'hold': ['{topic} 보류 사유 정리', '{topic} 외부 확인 대기', '{topic} 추가 검증 대기'],
    'report': ['{topic} 보고 내용 정리', '{topic} 본부/사업부 공유안 작성', '{topic} 현황 보고 정리'],
    'cancel': ['{topic} 처리 범위 조정', '{topic} 우선순위 변경으로 중단', '{topic} 취소 및 후속 전환'],
}

REQUEST_PATTERNS = {
    'request': ['{source}에서 {topic} 이슈 관련 문의가 올라와 오늘 안에 먼저 영향 범위를 확인해 달라는 요청이 들어왔다.', '{source} 기준으로 {topic} 이슈가 다시 보인다는 이야기가 있어 우선 재현 조건부터 정리해 달라고 전달받았다.'],
    'analyze': ['전일 정리한 {topic} 건을 기준으로 로그와 업무일지 흐름을 다시 맞춰 보라는 요청을 받았다.', '{lead} 팀장이 {topic} 건은 단순 현상 기록으로 끝내지 말고 원인 후보까지 좁혀 달라고 했다.'],
    'fix': ['{lead} 팀장이 {topic} 대응안을 바로 반영해 보고 부작용 여부까지 같이 확인해 달라고 했다.', '{source}에서 {topic} 건을 오늘 안에 한 번이라도 줄여 보자는 요청이 와서 우선 수정 가능한 구간부터 손봤다.'],
    'verify': ['수정 후에도 {topic} 이슈가 같은 조건에서 반복되는지 다시 확인해 달라는 요청이 이어졌다.', '직전 조치가 실제 운영에서 먹히는지 {topic} 기준으로 재검증해 달라는 요청을 받았다.'],
    'review': ['진행한 대응이 팀 목적에 맞는지 정리해서 검토 의견을 남겨 달라는 요청이 있었다.', '{topic} 건은 후속 작업 우선순위까지 같이 보자는 의견이 있어 검토 메모를 남겼다.'],
    'improve': ['같은 {topic} 이슈가 다시 올라오지 않게 기준이나 체크포인트를 보강해 달라는 요청을 받았다.', '{lead} 팀장이 {topic} 건은 재발 방지 관점까지 정리해 두자고 해서 개선안을 적었다.'],
    'hold': ['외부 확인이 늦어져 {topic} 처리는 잠시 보류하고 현재까지 확인한 내용만 남겨 달라는 요청이 있었다.', '{topic} 건은 연계 부서 답변을 기다려야 해서 진행 중단 사유를 먼저 정리해 두기로 했다.'],
    'report': ['{topic} 대응 경과를 사업부장/본부장 보고 관점으로 짧게 정리해 달라는 요청이 들어왔다.', '이번 주 공유 자료에 {topic} 건을 넣어야 해서 핵심만 추려 달라는 요청을 받았다.'],
    'cancel': ['우선순위가 바뀌면서 {topic} 건은 바로 처리하지 않고 범위부터 다시 조정하자는 지시가 내려왔다.', '{topic}는 다른 과제로 흡수하기로 하여 현재 진행분을 정리해 두라는 요청을 받았다.'],
}

INTRO_PATTERNS = {
    'request': [
        '오전에는 {topic} 건이 실제로 어느 구간에서 먼저 드러나는지부터 다시 훑어 보면서, 관련 화면과 로그, 지난 메모를 한 번에 같이 열어 놓고 보기 시작했다.',
        '{topic} 문의가 들어온 직후 바로 손대기보다 먼저 재현 순서와 영향 범위를 나눠 보려고 운영 기록과 이전 업무일지를 옆에 두고 다시 읽었다.',
        '이번 {topic} 건은 현상만 적고 넘기면 다시 같은 질문이 돌아올 것 같아서, 초반부터 확인 순서를 세우는 데 시간을 먼저 썼다.',
    ],
    'analyze': [
        '{topic}를 단일 현상으로 보지 않으려고 비슷한 시기 기록, 로그 변화, 팀 내 메모를 한 줄씩 다시 맞춰 가며 보기 시작했다.',
        '이번 {topic} 분석은 단순 원인 추정으로 끝내지 않으려고 이전 대응분과 현재 흔적을 같은 축에서 비교하는 방식으로 정리했다.',
        '{topic} 쪽 흐름을 다시 따라가 보니 지금 보이는 현상보다 앞단에서 이미 어긋난 흔적이 있어, 그 지점을 중심으로 비교했다.',
    ],
    'fix': [
        '오늘은 {topic} 건을 바로 줄이는 데 초점을 두고 수정 가능한 구간부터 손봤고, 건드린 범위가 너무 넓어지지 않게 체크 포인트를 같이 묶었다.',
        '{topic} 대응은 급한 불을 끄는 조치와 이후 검증 포인트를 같이 남겨야 할 것 같아, 수정과 기록을 번갈아 가며 진행했다.',
        '우선 {topic}에 직접 닿는 부분부터 반영하되, 이후 후속 수정이 겹쳐도 맥락이 남도록 변경 이유를 같이 적는 방식으로 정리했다.',
    ],
    'verify': [
        '직전 조치 이후 {topic} 이슈가 실제로 줄었는지 보려고 같은 조건으로 다시 확인했고, 운영에서 체감되는 차이도 같이 받아 봤다.',
        '{topic}가 정말 잠잠해졌는지 보기 위해 재현 조건을 일부러 다시 맞춰 보고, 직전 수정분이 의도대로 먹히는지 한 번 더 확인했다.',
        '이번 {topic} 검증은 단순히 재현이 안 된다는 수준보다, 다시 흔들릴 여지가 남아 있는지까지 같이 보려는 쪽으로 진행했다.',
    ],
    'review': [
        '이번 {topic} 대응이 팀 목적과 맞는지, 그리고 지금 기록만으로도 후속자가 흐름을 이어갈 수 있는지 중심으로 다시 검토했다.',
        '{topic} 건은 결과만 보는 것보다 어떤 판단으로 여기까지 왔는지 보여야 해서, 검토 메모도 그 순서를 따라 다시 정리했다.',
        '후속 작업 우선순위까지 같이 봐야 하는 {topic} 건이라, 단순 승인보다 남은 리스크와 결정 포인트를 같이 짚는 방식으로 검토했다.',
    ],
    'improve': [
        '같은 {topic} 이슈가 며칠 뒤 비슷한 표현으로 다시 올라오지 않게 하려면 무엇을 기준으로 남겨야 할지부터 중심을 잡고 개선안을 적었다.',
        '이번 {topic} 건은 임시 조치보다 기준 문장을 어떻게 남겨야 검색과 추적이 쉬워지는지가 더 중요해 보여 그 관점으로 다듬었다.',
        '{topic}를 다시 찾을 때 표현만 달라져 놓치지 않게 하려면 기록 방식 자체를 조금 손봐야 할 것 같아, 운영 기준 쪽을 같이 봤다.',
    ],
    'hold': [
        '{topic} 건은 바로 닫기 어려워 현재까지 확인한 범위와 아직 외부 확인이 필요한 지점을 분리해서 남기는 쪽으로 우선 정리했다.',
        '이번 {topic}는 당장 결론을 내리기보다 멈춘 이유와 다시 이어볼 조건을 분리해 적어 두는 게 더 중요해 보여 그렇게 정리했다.',
        '{topic}는 추가 답변이 오기 전까지 섣불리 닫으면 다시 처음부터 봐야 할 것 같아, 중간 정리 메모를 남기는 데 초점을 뒀다.',
    ],
    'report': [
        '이번 {topic} 대응은 단순 진행 보고보다 운영 영향과 남은 리스크가 같이 보이게 정리하는 쪽으로 방향을 잡았다.',
        '{topic} 보고 메모는 길게 쓰기보다 지금 결정해야 할 것과 뒤로 미뤄도 되는 것을 나눠 보이게 정리했다.',
        '공유용으로 넘어가는 {topic} 건이라 표현은 줄이되, 판단에 필요한 수치와 조건은 빠지지 않게 정리했다.',
    ],
    'cancel': [
        '{topic} 자체가 사라진 것은 아니지만 지금은 우선순위가 바뀐 만큼, 어디까지 보았는지만 남기고 범위를 다시 묶는 쪽으로 정리했다.',
        '이번 {topic} 건은 당장 이어서 파기보다 멈춘 이유와 흡수될 과제를 같이 남기는 편이 이후 추적에 낫다고 판단했다.',
        '{topic}를 취소 처리하더라도 맥락까지 끊기면 나중에 같은 질문이 다시 올 수 있어, 현재까지 본 조건을 중심으로 남겼다.',
    ],
    'complete': [
        '마지막으로 {topic} 건을 다시 닫아 보면서, 이후 같은 검색어로 찾아도 전후 맥락이 이어지게 내용을 한 번 더 다듬어 두었다.',
        '{topic} 건을 완료로 넘기기 전에 이번에 본 조건과 다음에 다시 흔들릴 수 있는 포인트를 같이 적어 두는 쪽을 택했다.',
        '이번 {topic} 기록은 닫는 순간보다 나중에 다시 찾을 때 바로 이어지는지가 중요해 보여, 마무리 문장까지 다시 손봤다.',
    ],
    'recurrence': [
        '잠잠해졌던 {topic}가 비슷한 시점에 다시 흔들려서, 예전 조치가 어디까지 유효했는지부터 현재 흔적과 겹쳐 보며 다시 확인했다.',
        '한동안 보이지 않던 {topic}가 재등장해 이번에는 이전 기록과 비교하면서 어떤 조건에서 다시 살아나는지 먼저 봤다.',
        '{topic}가 재발한 건은 직전 처리 이력만 보면 놓치는 부분이 있어, 과거 메모와 현재 로그를 같이 펴 놓고 다시 비교했다.',
    ],
}

DETAIL_PATTERNS = {
    'request': [
        '초기 문의 문장만 보면 단순 요청처럼 보였지만, 실제로는 누가 먼저 봐야 덜 돌아갈지가 더 중요한 건처럼 느껴졌다.',
        '현상 설명이 짧아서 오히려 확인 기준이 흔들릴 수 있어, 같은 단어라도 어떤 맥락으로 적혔는지 같이 분리해 두었다.',
        '처음부터 범위를 넓히면 메모만 길어질 것 같아, 어디까지를 오늘 확인 대상으로 둘지 먼저 선을 그었다.',
    ],
    'analyze': [
        '같은 단어가 들어가도 기록마다 의미가 조금씩 달라, 이번에는 표현 차이보다 실제 맥락이 이어지는지를 더 우선해 봤다.',
        '원인 후보를 무작정 늘리기보다, 지금 남은 흔적이 어떤 순서로 생겼는지부터 정리하는 편이 이후 검증에 도움이 될 것 같았다.',
        '비교 기준을 한 번만 잘못 잡아도 엉뚱한 결론이 나올 수 있어, 로그와 메모를 같은 시간 축으로 다시 맞췄다.',
    ],
    'fix': [
        '수정 자체보다도 왜 이 구간을 먼저 건드렸는지가 남아 있어야 다음 변경과 충돌이 적을 것 같아 그 배경을 같이 적었다.',
        '이번 조치는 완전한 해법보다는 흔들림을 먼저 줄이는 데 목적이 있어, 바꾼 범위와 남겨 둔 범위를 분리해서 표시했다.',
        '한 번에 많이 고치면 재검증 포인트가 흐려질 수 있어, 이번에는 영향이 큰 곳부터 짧게 끊어 반영하는 쪽을 택했다.',
    ],
    'verify': [
        '검증 단계에서는 결과보다 조건이 더 중요해 보여, 어떤 입력과 어떤 상황에서 다시 확인했는지까지 같이 적어 두었다.',
        '이번 확인은 재현 여부 한 줄로 끝내기보다, 다시 문제를 찾을 때 바로 써먹을 검색 조건을 남기는 데 더 무게를 뒀다.',
        '당장은 잠잠해 보여도 며칠 뒤 비슷한 문의가 들어올 수 있어, 이번 검증에서는 흔들릴 만한 모서리를 같이 봤다.',
    ],
    'review': [
        '검토 메모가 단순 찬반으로 끝나면 다음 결정이 다시 꼬일 수 있어, 왜 이 순서가 맞는지까지 같이 적었다.',
        '누가 봐도 같은 판단을 내릴 수 있게 하려면 리스크와 후속 작업이 같이 보여야 해서 그 부분을 먼저 묶었다.',
        '이번 검토는 현재 조치의 적절성만이 아니라, 이후 같은 유형이 왔을 때 재사용 가능한 판단 기준을 남기는 데도 초점을 뒀다.',
    ],
    'improve': [
        '운영 기준을 조금만 보강해도 같은 문의가 다른 표현으로 반복될 가능성을 줄일 수 있어, 문장 선택까지 같이 손봤다.',
        '검색 관점에서 다시 걸릴 수 있도록 표현을 맞추는 작업도 필요해 보여, 조치 내용과 키워드 기준을 함께 다듬었다.',
        '개선안은 한 번 읽고 끝나는 메모보다 이후 검색과 분류에 다시 쓰일 수 있어야 해서 그 방향으로 정리했다.',
    ],
    'hold': [
        '보류 메모가 흐리면 다시 열었을 때 처음부터 다시 보게 될 것 같아, 확인된 사실과 미확인 항목을 분리해 두었다.',
        '멈춘 이유를 적되 단순 대기라고만 남기면 부족해서, 어떤 답이 오면 바로 이어볼 수 있는지도 같이 적어 두었다.',
        '지금 바로 해결되지 않더라도 다음 사람이 같은 질문을 다시 하지 않게 만드는 것이 더 중요해 보여 그쪽에 힘을 뒀다.',
    ],
    'report': [
        '보고 문맥에서는 세부 로그보다 결정 포인트가 먼저 보여야 해, 표현을 줄이면서도 영향 범위는 빠지지 않게 남겼다.',
        '숫자만 올리면 현장 맥락이 빠질 수 있어, 일정 영향과 운영 영향이 함께 보이도록 메모를 다시 정리했다.',
        '짧은 보고라도 후속 판단에 필요한 재검증 포인트는 남겨야 해서 그 부분은 일부러 뺐다 넣지 않았다.',
    ],
    'cancel': [
        '취소라고 해도 문제의 맥락까지 지우면 나중에 같은 건이 반복될 수 있어, 멈춘 배경과 현재 상태를 같이 남겼다.',
        '다른 과제로 흡수되는 건이라 이후 검색으로 다시 연결될 수 있게, 쓰는 단어를 너무 새로 바꾸지 않으려고 했다.',
        '이번에는 종료 사유보다도 어떤 조건에서 다시 열릴 수 있는지 보이게 남기는 편이 더 실무적이라고 봤다.',
    ],
}

COLLAB_PATTERNS = {
    'ops': [
        '운영 문의 쪽 표현과 내부 메모 표현이 달라 중간에서 해석을 맞추는 시간이 생각보다 더 들었다.',
        '현장 기준으로는 사소해 보여도 실제 운영 흐름에서는 자주 막히는 지점이라, 체감되는 불편을 같이 적어 두었다.',
        '같은 화면이라도 보는 사람마다 중요한 포인트가 달라 보여, 문의 문장과 실제 사용 흐름을 같이 붙여 놨다.',
    ],
    'analyst': [
        '단순 수치 차이보다 기준이 언제부터 흔들렸는지 보는 편이 중요해 보여, 비교 시점을 일부러 나눠서 확인했다.',
        '수치만 맞아도 설명이 안 되는 구간이 있어, 이번에는 기록 간 표현 차이도 같이 묶어 보면서 정리했다.',
        '분석 메모가 길어지더라도 다음 검증자가 같은 근거를 바로 따라갈 수 있게 비교 기준을 선명하게 남겼다.',
    ],
    'dev': [
        '코드나 설정 쪽 변경은 나중에 다른 수정과 겹치기 쉬워, 이번에는 손댄 이유와 안 건드린 이유를 같이 적었다.',
        '실제 수정 구간보다 주변 영향이 더 클 수 있어 보여, 변경 포인트와 재검증 포인트를 짝으로 남겼다.',
        '바로 적용한 조치가 이후 릴리즈 일정과 충돌하지 않게 하려면 맥락 메모가 필요해 보여 그 부분을 신경 써 적었다.',
    ],
    'lead': [
        '팀 기준으로는 누가 이어받아도 바로 판단할 수 있어야 해서, 후속 작업과 책임 구간이 같이 보이게 남겼다.',
        '단순히 진행 상황만 적기보다 다음 액션이 누구에게 넘어가야 하는지까지 같이 보이도록 정리했다.',
        '리더 관점에서는 처리 여부보다도 남는 리스크가 중요해 보여, 일정과 운영 영향이 같이 보이게 적었다.',
    ],
    'oversight': [
        '직접 손을 대기보다는 범위와 우선순위 판단이 맞는지 쪽에 더 신경을 썼고, 누가 결정하면 되는지를 먼저 정리했다.',
        '이번 건은 실무 세부보다 결정 지점이 중요해 보여, 승인 판단에 필요한 근거만 남기고 나머지는 팀으로 다시 넘겼다.',
        '조치 자체보다는 어떤 순서로 정리해야 덜 돌아가는지가 더 중요해 보여, 판단 기준 위주로 메모를 남겼다.',
    ],
}

RESULT_PATTERNS = {
    'request': [
        '지금 단계에서는 결론보다 다음 사람이 바로 이어서 볼 수 있는 출발점을 남기는 것이 더 중요하다고 봤다.',
        '확인 범위를 지나치게 넓히지 않고도 다음 액션으로 바로 넘어갈 수 있게 메모 구조를 다듬어 두었다.',
    ],
    'analyze': [
        '분석 결과를 단정으로 적기보다 다음 검증에서 걸러질 수 있게 여지를 남겨 두는 편이 맞다고 판단했다.',
        '이번 정리는 원인 후보를 줄이는 데 목적이 있어, 다음 확인자가 바로 이어서 좁혀 갈 수 있는 형태로 남겼다.',
    ],
    'fix': [
        '지금은 완결보다 후속 재검증이 더 중요해 보여, 수정 결과를 짧게 닫기보다 다시 확인할 조건을 같이 붙여 두었다.',
        '당장 해결된 것처럼 보이더라도 후속 검증에서 다시 볼 수 있게 이번 반영 범위를 선명하게 남겼다.',
    ],
    'verify': [
        '이번 검증 결과는 단순 완료 표식보다, 며칠 뒤 같은 문의가 다시 왔을 때 바로 꺼내 볼 조건으로 남겨 두었다.',
        '확인 결과를 짧게 끝내지 않고 재발 가능성까지 같이 남겨 두는 편이 이후 운영에 도움이 될 것 같았다.',
    ],
    'review': [
        '검토 메모는 승인을 위한 문장보다도 다음 결정이 빨라지게 하는 방향으로 남기는 편이 맞다고 봤다.',
        '이번 판단은 지금 한 번의 승인보다 후속 조치 순서가 덜 꼬이게 만드는 데 더 초점을 뒀다.',
    ],
    'improve': [
        '개선안은 바로 적용 여부와 별개로, 이후 비슷한 문장을 찾았을 때 같은 계열로 묶이도록 만드는 데 의미가 있다고 봤다.',
        '이번 정리는 재발 방지와 검색 재현성을 같이 잡는 쪽으로 남기는 것이 맞다고 판단했다.',
    ],
    'hold': [
        '보류 메모라도 다시 열었을 때 맥락을 잃지 않는 것이 중요해 보여, 시작 조건과 재개 조건을 같이 남겼다.',
        '지금은 멈춰도 다음 이어받는 사람이 다시 해석하지 않게 하는 것이 더 중요하다고 봤다.',
    ],
    'report': [
        '공유용 메모라서 길게 늘어놓지 않았지만, 지금 의사결정에 필요한 포인트는 빠지지 않게 남겼다.',
        '보고 문장도 이후 검증과 연결될 수 있어야 해서, 핵심 조건은 가능한 한 그대로 유지했다.',
    ],
    'cancel': [
        '취소 처리 후에도 같은 표현으로 다시 검색됐을 때 지금 맥락이 바로 연결되도록 메모 흐름은 살려 두었다.',
        '범위는 닫더라도 맥락은 남겨 두는 쪽이 이후 운영상 덜 돌아갈 것 같아 그렇게 정리했다.',
    ],
}

REPEAT_PATTERNS = [
    '이전에도 비슷한 표현으로 지나간 적이 있어 이번에는 예전 메모와 무엇이 달라졌는지를 먼저 분리해 봤다.',
    '한 번 정리했던 주제라 완전히 새로운 이슈처럼 다루기보다, 지난번에 놓친 지점이 무엇인지부터 다시 확인했다.',
    '같은 키워드가 다시 보였지만 맥락이 조금 달라 보여, 재발인지 변형인지 구분하는 데 시간을 더 썼다.',
]

VARIANT_BRIDGES = {
    'v1': [
        '{team_name} 안에서 후속자가 바로 이어받기 쉬운 형태를 먼저 의식했다.',
        '{topic} 건은 지금 당장 해결 여부보다 다음 액션 연결성이 더 중요하다고 보고 정리했다.',
        '이번에는 {extra_keyword} 관점으로 다시 찾아도 흐름이 이어지게 남기는 쪽을 먼저 의식했다.',
        '{topic} 메모는 결론만 남기기보다 뒤에서 이어볼 기준이 남도록 쓰는 편이 낫다고 봤다.',
    ],
    'v2': [
        '이번에는 단순 처리 기록보다 왜 {topic}를 이런 순서로 봤는지가 읽히게 남기려는 쪽으로 문장을 다듬었다.',
        '{team_name} 안에서 같은 건을 나중에 다시 꺼내 봐도 판단 흐름이 남도록, 중간에 고민한 기준까지 일부 같이 적었다.',
        '한 번에 매끈하게 끝난 {topic} 건은 아니라서, 이번 메모에는 흔들렸던 판단 지점도 같이 남겨 두었다.',
        '{extra_keyword} 쪽 기록과 다시 이어 붙여 봐도 어색하지 않게, 이번에는 중간 판단 근거를 일부러 남겼다.',
        '이번 {topic} 정리는 결과만 적기보다 어떤 기준에서 범위를 좁혔는지가 같이 읽히도록 다듬었다.',
        '{team_name} 기준으로 다시 찾았을 때도 앞뒤 판단이 이어지게 하려는 쪽에 조금 더 무게를 뒀다.',
    ],
}

OBS_CONTEXT = [
    '{topic} 건은 단일 지점 문제가 아니라 {extra_keyword} 쪽 기록까지 같이 건드리고 있었다.',
    '{topic} 이슈를 따라가다 보니 {extra_keyword} 구간의 흔적도 같이 엮여 있었다.',
    '{topic}만 따로 보면 단순 현상 같았는데, 실제로는 {extra_keyword} 흐름과 붙어 있었다.',
    '{topic} 확인 범위를 넓혀 보니 {extra_keyword} 쪽 메모와도 자연스럽게 이어졌다.',
]
OBS_SHAPE = [
    '{topic}는 겉으로 보이는 현상보다 뒤에 남은 흔적이 더 길었다.',
    '처음에 본 {topic} 증상보다 뒤쪽 로그와 메모가 더 많은 이야기를 하고 있었다.',
    '{topic}는 바로 눈에 띄는 현상보다, 재발 조건이 어디에 남는지가 더 중요해 보였다.',
    '{topic}는 한 번에 닫힐 성격보다는 다시 흔들릴 지점을 같이 봐야 하는 건으로 보였다.',
]
OBS_SEARCH = [
    '비슷한 시기 업무일지와 놓고 보니, {topic}와 {extra_keyword}를 다른 표현으로 적은 문장이 몇 건 더 보였다.',
    '검색어를 달리 넣어도 {topic}와 결이 비슷한 이전 기록이 걸려 나와, 완전히 새로운 건은 아니라고 판단했다.',
    '{extra_keyword}를 직접 쓰지 않은 기록까지 엮여 나와서, 단순 단어 매칭 이상으로 봐야 했다.',
    '직전 달 메모와 비교해도 주제는 달랐지만 결국 {topic}를 설명하는 문장이 섞여 있었다.',
]

NEXT_PATTERNS = {
    'request': ['우선은 누가 먼저 확인해야 빠른지 정리해 두고 담당자에게 넘겼다.', '당장 손대기보다 영향 범위를 먼저 그려 두는 쪽으로 정리했다.', '재현 조건과 확인 순서를 먼저 남겨 다음 사람이 바로 이어볼 수 있게 했다.', '급하게 손대기보다 확인 포인트부터 정리해 넘기는 편이 낫다고 판단했다.'],
    'analyze': ['확인 결과는 팀장이 바로 이어 볼 수 있게 세부 메모까지 남겼다.', '다음 단계에서 검증할 포인트를 체크리스트처럼 짧게 붙여 두었다.', '원인 후보를 너무 넓게 두지 않으려고 다음 확인 순서까지 같이 적어 두었다.', '비교 기준이 흐트러지지 않도록 다음 검증 포인트를 따로 묶어 두었다.'],
    'fix': ['오늘 바꾼 부분은 다음 근무자도 바로 이어볼 수 있도록 배경과 범위를 같이 남겨 두었다.', '수정 자체보다 재발 여부를 다시 보기 위한 기준을 남기는 데 신경을 썼다.', '어디를 왜 바꿨는지 같이 적어야 이후 수정과 충돌하지 않을 것 같아 그렇게 남겼다.', '이번 수정이 끝이 아니라 다음 재검증 출발점이 되도록 범위를 분명히 적어 두었다.'],
    'verify': ['같은 현상이 며칠 뒤 다시 올라올 가능성을 염두에 두고 검색어와 로그 조건도 같이 적어 두었다.', '완전히 닫기 전에 한 번 더 비교해 볼 포인트를 따로 표시했다.', '이번 확인 결과가 다음 재현 작업에도 바로 쓰일 수 있게 조건을 정리해 두었다.', '단순 완료 처리보다 다시 찾을 때 도움이 되는 조건을 남기는 쪽에 무게를 뒀다.'],
    'review': ['승인보다는 보완 포인트가 보이면 바로 다음 사람에게 넘길 수 있게 판단 근거를 짧게 남겼다.', '단순 의견이 아니라 왜 이 순서가 맞는지까지 같이 적어 두었다.', '팀장이 봤을 때 바로 결정할 수 있게 리스크와 후속 작업을 같이 묶었다.', '검토 메모도 다음 액션으로 이어질 수 있게 판단 이유를 남겼다.'],
    'improve': ['이번에는 임시 조치로 끝내지 않으려고 운영 기준과 체크 포인트를 같이 정리했다.', '같은 표현으로 검색했을 때 후속 업무도 같이 걸리도록 키워드 선택을 일부 맞춰 두었다.', '후속자가 같은 문제를 다시 찾을 때 덜 헤매도록 기준 문장을 조금 더 명확히 남겼다.', '개선안은 조치보다 기준을 남기는 쪽이 중요하다고 보고 그렇게 정리했다.'],
    'hold': ['보류 상태에서도 다음 사람이 맥락을 잃지 않게 현재까지 본 사실과 미확인 항목을 나눠 적었다.', '외부 답변이 오면 바로 이어서 확인할 수 있도록 조건만 먼저 정리했다.', '멈춘 이유와 다시 시작할 조건을 분리해 적어 두었다.', '보류라고 해서 기록까지 흐리게 남기지 않으려고 확인 범위를 선명하게 적었다.'],
    'report': ['보고용 메모라 길게 쓰지 않고, 지금 결정해야 할 것과 남은 리스크만 남겼다.', '숫자 하나만 보지 않도록 운영 영향과 일정 영향을 같이 붙여 두었다.', '보고 문서라서 표현은 줄였지만 판단에 필요한 수치와 조건은 남겼다.', '공유용이라 불필요한 설명은 빼고 결정 포인트만 바로 보이게 정리했다.'],
    'cancel': ['완전히 버린 이슈는 아니어서, 나중에 비슷한 키워드로 찾을 수 있게 맥락은 남겨 두었다.', '당장 닫더라도 왜 멈췄는지 보이게 적어 두는 쪽을 택했다.', '범위 조정으로 멈춘 것이지 문제 자체가 사라진 건 아니라는 점을 분명히 남겼다.', '취소 메모도 나중에 같은 맥락을 찾을 수 있게 조건 중심으로 남겨 두었다.'],
}

OVERSIGHT_NOTE = [
    '직접 손을 대기보다는 범위와 우선순위가 맞는지부터 봤다.',
    '이번 건은 누가 맡아야 덜 돌아가는지 판단하는 쪽에 시간을 더 썼다.',
    '운영 영향과 일정 영향이 동시에 보여서 승인 판단 기준을 먼저 남겼다.',
]

ROLE_HINTS = {
    'ops': ['현장에서 먼저 체감되는 영향을 앞에 두고 적었다.', '운영에서 왜 불편했는지부터 보이게 정리했다.', '바로 막히는 지점이 무엇인지 먼저 적어 두었다.'],
    'analyst': ['추정으로 단정하지 않으려고 비교한 기준을 같이 남겼다.', '수치나 조건이 달라진 지점을 먼저 짚어 두었다.', '이전 기록과 무엇이 달랐는지 분리해서 적었다.'],
    'dev': ['수정 이유와 영향 범위를 함께 남겨 다음 변경과 충돌하지 않게 했다.', '단순히 고쳤다고 쓰기보다 어디까지 바뀌는지 같이 적어 두었다.', '로그 기준으로 보던 현상과 코드에서 바뀐 지점을 연결해 적었다.'],
    'lead': ['팀장이 바로 판단할 수 있게 일정과 후속 작업을 같이 정리했다.', '진행만 적지 않고 누구에게 넘겨야 하는지도 분명히 적었다.', '팀 관점에서 남는 리스크를 짧게라도 같이 붙였다.'],
    'oversight': OVERSIGHT_NOTE,
}

STATUS_BY_PHASE = {
    'request': ['PENDING', 'PENDING', 'IN_PROGRESS'],
    'analyze': ['IN_PROGRESS', 'COMPLETED', 'IN_PROGRESS'],
    'fix': ['COMPLETED', 'IN_PROGRESS', 'COMPLETED'],
    'verify': ['COMPLETED', 'COMPLETED', 'IN_PROGRESS'],
    'review': ['COMPLETED', 'COMPLETED', 'ON_HOLD'],
    'improve': ['COMPLETED', 'COMPLETED', 'IN_PROGRESS'],
    'hold': ['ON_HOLD', 'ON_HOLD'],
    'report': ['COMPLETED', 'COMPLETED', 'IN_PROGRESS'],
    'cancel': ['CANCELLED'],
    'complete': ['COMPLETED'],
}

IMPORTANCE_BY_PHASE = {
    'request': ['NORMAL', 'HIGH', 'URGENT'],
    'analyze': ['NORMAL', 'NORMAL', 'HIGH'],
    'fix': ['HIGH', 'NORMAL', 'URGENT'],
    'verify': ['NORMAL', 'NORMAL', 'LOW'],
    'review': ['NORMAL', 'HIGH', 'LOW'],
    'improve': ['NORMAL', 'NORMAL', 'LOW'],
    'hold': ['NORMAL', 'LOW', 'HIGH'],
    'report': ['HIGH', 'NORMAL', 'LOW'],
    'cancel': ['LOW', 'NORMAL'],
    'complete': ['NORMAL', 'HIGH'],
}

SOURCE_POOL = ['현업 요청', '운영 문의', '전일 점검 메모', '팀장 전달', '사업부 검토 의견', '고객사 요청', '릴리즈 후속 확인']

TEAM_TEMPLATES = [
    ['request', 'analyze', 'fix', 'verify', 'review', 'complete'],
    ['request', 'analyze', 'hold', 'verify', 'improve', 'complete'],
    ['request', 'analyze', 'fix', 'recurrence', 'improve', 'review', 'complete'],
    ['request', 'analyze', 'fix', 'report', 'complete'],
    ['request', 'analyze', 'hold', 'cancel'],
]

# normalize synthetic phases
PHASE_ALIAS = {'recurrence': 'analyze'}


def sql_str(v):
    if v is None:
        return 'NULL'
    return "'" + str(v).replace("'", "''") + "'"


def stable_pick(options, *parts):
    key = '||'.join(str(p) for p in parts)
    idx = int(hashlib.sha1(key.encode('utf-8')).hexdigest(), 16) % len(options)
    return options[idx]


def normalize_sentence(text: str) -> str:
    text = text.lower().strip().replace('.', '')
    return re.sub(r'\s+', ' ', text)


def usage_aware_pick(rendered_options, usage_counter, *parts):
    scored = []
    for idx, option in enumerate(rendered_options):
        norm = normalize_sentence(option)
        tie = int(hashlib.sha1(('||'.join(str(p) for p in parts) + f'||{idx}').encode('utf-8')).hexdigest(), 16)
        scored.append(((usage_counter[norm], tie), option))
    scored.sort(key=lambda item: item[0])
    return scored[0][1]


def strip_period(text: str) -> str:
    return text.rstrip().rstrip('.').rstrip()


def has_batchim(text: str):
    for ch in reversed(text.strip()):
        code = ord(ch)
        if 0xAC00 <= code <= 0xD7A3:
            return (code - 0xAC00) % 28 != 0
    return False


def choose_josa(text: str, pair: str):
    with_batchim, without_batchim = pair.split('/')
    return with_batchim if has_batchim(text) else without_batchim


def fix_josa(text: str, *terms: str):
    fixed = text
    for term in sorted({t for t in terms if t}, key=len, reverse=True):
        replacements = {
            f'{term}은': f'{term}{choose_josa(term, "은/는")}',
            f'{term}는': f'{term}{choose_josa(term, "은/는")}',
            f'{term}을': f'{term}{choose_josa(term, "을/를")}',
            f'{term}를': f'{term}{choose_josa(term, "을/를")}',
            f'{term}이': f'{term}{choose_josa(term, "이/가")}',
            f'{term}가': f'{term}{choose_josa(term, "이/가")}',
            f'{term}과': f'{term}{choose_josa(term, "과/와")}',
            f'{term}와': f'{term}{choose_josa(term, "과/와")}',
        }
        for src, dst in replacements.items():
            fixed = fixed.replace(src, dst)
    return fixed


ANTI_REPEAT_CLAUSES = {
    'request': [
        '{topic} 건은 이번에는 확인 범위를 더 잘게 끊어 적었다',
        '{extra_keyword} 쪽 흔적이 먼저 보이는 구간을 따로 표시해 뒀다',
        '같은 문의가 다시 와도 시작 지점을 바로 찾게 하려는 쪽으로 표현을 조금 바꿨다',
    ],
    'analyze': [
        '이번에는 비교 시점을 한 번 더 나눠 보면서 메모를 남겼다',
        '{topic}와 직접 맞닿는 조건을 먼저 세워 두는 쪽으로 정리했다',
        '{extra_keyword}와 겹치는 구간이 어디였는지 바로 보이게 적었다',
    ],
    'fix': [
        '바꾼 부분과 그대로 둔 부분이 바로 구분되게 표현을 손봤다',
        '다음 수정이 겹쳐도 헷갈리지 않게 변경 맥락을 조금 더 붙였다',
        '{topic} 처리 후 다시 볼 지점을 따로 표시해 두는 쪽으로 적었다',
    ],
    'verify': [
        '이번에는 재현 조건 순서를 조금 더 드러내는 식으로 남겼다',
        '후속 검증자가 다시 따라와도 같은 기준을 잡게 하려는 쪽으로 표현을 조정했다',
        '{extra_keyword} 관점으로 다시 찾을 때 바로 이어지게 문장을 다듬었다',
    ],
    'review': [
        '이번 검토에서는 결정 포인트가 먼저 보이도록 표현 순서를 조금 바꿨다',
        '후속 판단자가 근거를 바로 찾게 하려는 쪽으로 메모 리듬을 조정했다',
        '{team_name} 안에서 다시 읽어도 판단 기준이 남게 적었다',
    ],
    'improve': [
        '재발 방지 기준이 먼저 보이도록 문장 순서를 조금 손봤다',
        '{extra_keyword}로 다시 검색해도 같은 계열로 묶이게 표현을 조정했다',
        '{topic} 후속 대응과 연결되는 기준을 한 줄 더 드러냈다',
    ],
    'hold': [
        '보류 이유와 재개 조건이 더 또렷하게 갈라지도록 적었다',
        '다시 열었을 때 처음부터 읽지 않게 하는 쪽으로 문장을 조금 손봤다',
        '{extra_keyword} 관련 답변이 오면 바로 이어볼 수 있게 표현을 정리했다',
    ],
    'report': [
        '보고 관점에서 바로 결정 포인트가 보이도록 한 줄 더 정리했다',
        '운영 영향과 일정 영향 중 무엇이 먼저인지 드러나게 적었다',
        '{team_name} 공유 맥락에 맞게 판단 근거를 조금 더 앞에 뒀다',
    ],
    'cancel': [
        '멈춘 이유보다 재개 조건이 먼저 보이게 표현을 조정했다',
        '나중에 같은 키워드로 찾았을 때도 연결되게 맥락을 조금 더 남겼다',
        '{topic}가 다른 과제로 넘어간 흔적이 보이도록 적었다',
    ],
}

SLOT_REPEAT_CLAUSES = {
    'bridge': [
        '이번 기록에서는 판단 흐름이 앞에서부터 보이게 순서를 조정했다',
        '같은 주제를 다시 볼 때도 중간 판단이 남게 적는 쪽을 택했다',
    ],
    'collab': [
        '이번에는 협업 맥락이 먼저 읽히도록 표현을 살짝 바꿨다',
        '같은 역할의 후속자가 읽어도 바로 이어받게 적었다',
    ],
    'result': [
        '메모를 닫을 때도 다음 액션 실마리가 남게 정리했다',
        '이번 결과는 바로 다음 조회에 이어지게 적는 데 더 무게를 뒀다',
    ],
    'repeat': [
        '지난 기록과 이번 기록의 차이가 먼저 보이게 표현을 조금 달리했다',
        '재발인지 변형인지 구분하는 데 도움이 되도록 문장을 한 번 더 다듬었다',
    ],
    'next': [
        '후속 작업자가 보는 순서를 의식해 문장 배치를 조금 손봤다',
        '다음 검색에서도 앞뒤 흐름이 자연스럽게 붙게 정리했다',
    ],
}


def parse_teams():
    teams = {}
    for path in sorted(TEAM_DIR.glob('1*.md')):
        text = path.read_text(encoding='utf-8')
        tid = int(TEAM_ID_RE.search(text).group(1))
        name = TEAM_NAME_RE.search(text).group(1).strip()
        start = date.fromisoformat(START_RE.search(text).group(1))
        end = date.fromisoformat(END_RE.search(text).group(1))
        members = []
        for m in TABLE_ROW_RE.finditer(text):
            uid = int(m.group(1))
            members.append({
                'user_id': uid,
                'name': m.group(2).strip(),
                'role': m.group(3).strip(),
                'allocation': m.group(4).strip(),
                'is_primary': m.group(5).strip().lower() == 'true',
                'is_leader': m.group(6).strip().lower() == 'true',
                'status_code': m.group(7).strip(),
            })
        teams[tid] = {'name': name, 'start': start, 'end': end, 'members': members}
    return teams


def parse_persona_hints():
    hints = {}
    for path in sorted((ROOT / '.codex' / 'agents').glob('user-*.toml')):
        text = path.read_text(encoding='utf-8')
        uid = int(path.stem.split('-')[1])
        basic = re.search(r'- 기본 성격: (.+)', text)
        habit = re.search(r'- 문서 습관: (.+)', text)
        specialty = re.search(r'전문영역: (.+)', text)
        hints[uid] = {
            'basic': basic.group(1).strip() if basic else '',
            'habit': habit.group(1).strip() if habit else '',
            'specialty': specialty.group(1).strip() if specialty else '',
        }
    return hints


def classify_member(role: str, is_leader: bool):
    if is_leader:
        return 'lead'
    if '사업부' in role or '승인' in role or '총괄' in role or '검토' in role or '게이트' in role:
        return 'oversight'
    if any(k in role for k in ['개발', 'API', '배포', '인프라', '보정', '화면', '검색', '배치', '백엔드', '파이프라인']):
        return 'dev'
    if any(k in role for k in ['분석', '정합성', '정산', '검증', '품질', '기준', 'KPI', '리포트', '지표', '데이터']):
        return 'analyst'
    return 'ops'


def pick_author(team, phase, topic_idx):
    members = team['members']
    by_class = defaultdict(list)
    for m in members:
        by_class[classify_member(m['role'], m['is_leader'])].append(m)
    lead = by_class['lead'][0]
    oversight = by_class['oversight'][0] if by_class['oversight'] else lead
    if phase in ('request',):
        pool = by_class['ops'] + by_class['analyst'] + [lead]
    elif phase in ('analyze', 'recurrence'):
        pool = by_class['analyst'] + by_class['ops'] + by_class['dev']
    elif phase in ('fix',):
        pool = by_class['dev'] + by_class['analyst']
    elif phase in ('verify', 'complete'):
        pool = by_class['ops'] + by_class['analyst'] + [lead] + by_class['dev']
    elif phase in ('review', 'report'):
        pool = [lead, oversight]
    elif phase in ('hold', 'cancel'):
        pool = [lead] + by_class['ops'] + by_class['analyst']
    elif phase in ('improve',):
        pool = [lead] + by_class['analyst'] + by_class['dev']
    else:
        pool = members
    pool = [m for m in pool if m]
    return pool[topic_idx % len(pool)]


def diversify_atomic_sentence(text, usage_counter, phase, role_class, topic, extra_keyword, team_name, event_key, slot):
    norm = normalize_sentence(text)
    if usage_counter[norm] == 0:
        usage_counter[norm] += 1
        return text

    phase_key = 'verify' if phase == 'complete' else PHASE_ALIAS.get(phase, phase)
    rendered = [
        clause.format(topic=topic, extra_keyword=extra_keyword, team_name=team_name)
        for clause in (ANTI_REPEAT_CLAUSES.get(phase_key, []) + SLOT_REPEAT_CLAUSES.get(slot, []))
    ]
    rendered = rendered or [
        f'{topic} 기준으로 이번에는 확인 순서를 조금 더 드러내는 방식으로 적었다',
        f'{team_name} 안에서 다시 읽었을 때 차이가 보이도록 표현을 조정했다',
    ]
    clause = usage_aware_pick(rendered, usage_counter, event_key, slot, role_class, topic, extra_keyword, team_name)
    candidate = f"{text.rstrip('.')} , {clause}.".replace(' ,', ',')
    cand_norm = normalize_sentence(candidate)
    if usage_counter[cand_norm] > 0:
        fallback = f"{candidate.rstrip('.')} ({extra_keyword} 쪽 비교 포인트도 같이 남김)."
        candidate = fallback
        cand_norm = normalize_sentence(candidate)
    usage_counter[cand_norm] += 1
    return candidate


def tone_sentence(uid, role_class, persona_hints, topic, phase, team_name, extra_keyword, event_key, usage_counter):
    hint = persona_hints.get(uid, {})
    basic = hint.get('basic', '')
    habit = hint.get('habit', '')
    if basic and habit:
        basic_s = strip_period(basic)
        habit_s = strip_period(habit)
        options = [
            f'{basic_s} 성향이 있어 이번 {topic} 건도 {habit_s}는 방식이 자연스럽게 드러났다.',
            f'평소 {habit_s}는 편이라 이번 {topic} 기록도 그 흐름을 크게 벗어나지 않게 남겼다.',
            f'{topic} 건도 {basic_s} 쪽 성향이 묻어나게, 평소 하던 대로 {habit_s}는 방향으로 정리했다.',
        ]
        options += ROLE_HINTS.get(role_class, ROLE_HINTS['ops'])
        return usage_aware_pick(options, usage_counter, uid, topic, phase, team_name, event_key, 'tone')
    if basic:
        basic_s = strip_period(basic)
        options = [f'{basic_s} 성향이라 이번 {topic} 기록도 그 결이 보이게 남겼다.'] + ROLE_HINTS.get(role_class, ROLE_HINTS['ops'])
        return usage_aware_pick(options, usage_counter, uid, topic, phase, team_name, event_key, 'tone')
    base = usage_aware_pick(ROLE_HINTS.get(role_class, ROLE_HINTS['ops']), usage_counter, uid, topic, phase, team_name, event_key, 'tone')
    return f'{topic} 건을 적을 때도 {base[0].lower() + base[1:] if len(base) > 1 else base}'


def build_observation_parts(topic, team_name, extra_keyword, phase, uid, repeated, event_key, usage_counter):
    parts = [
        usage_aware_pick([s.format(topic=topic, extra_keyword=extra_keyword) for s in OBS_CONTEXT], usage_counter, topic, team_name, extra_keyword, phase, uid, event_key, 'ctx'),
        usage_aware_pick([s.format(topic=topic, extra_keyword=extra_keyword) for s in OBS_SHAPE], usage_counter, topic, phase, uid, event_key, 'shape'),
        usage_aware_pick([s.format(topic=topic, extra_keyword=extra_keyword) for s in OBS_SEARCH], usage_counter, topic, extra_keyword, phase, uid, event_key, 'search'),
    ]
    if repeated:
        parts.append(f'이번에는 지난번에 남긴 {extra_keyword} 관련 메모와도 결이 이어져 보여 후속 흐름으로 같이 잡았다.')
    return parts


def build_next_parts(phase, topic, uid, team_name, extra_keyword, event_key, usage_counter):
    next_phase = 'verify' if phase == 'complete' else PHASE_ALIAS.get(phase, phase)
    base = usage_aware_pick(NEXT_PATTERNS[next_phase], usage_counter, uid, team_name, topic, phase, event_key, 'next')
    tail = usage_aware_pick([
        f'다음에는 {extra_keyword} 관점으로 다시 찾아도 이어질 수 있게 단어 선택을 일부 맞췄다.',
        f'{topic}를 다시 검색했을 때 전후 작업이 함께 걸리도록 기준 표현을 조금 남겨 두었다.',
        f'후속자가 같은 맥락을 다시 찾을 수 있게 {extra_keyword} 쪽 실마리도 덧붙여 두었다.',
        f'{team_name} 안에서 비슷한 건을 다시 찾을 때 헷갈리지 않도록 표현을 조금 정리해 두었다.',
    ], usage_counter, uid, topic, team_name, phase, event_key, 'tail')
    if next_phase in ('request', 'analyze', 'fix'):
        return [base]
    return [base, tail]


def make_title(topic, phase, event_key):
    base_phase = 'verify' if phase == 'complete' else PHASE_ALIAS.get(phase, phase)
    return stable_pick(TITLE_PATTERNS[base_phase], topic, phase, event_key, 'title').format(topic=topic)


def make_request(topic, phase, lead_name, event_key):
    base_phase = 'verify' if phase == 'complete' else PHASE_ALIAS.get(phase, phase)
    source = stable_pick(SOURCE_POOL, topic, phase, lead_name, event_key, 'source')
    return stable_pick(REQUEST_PATTERNS[base_phase], topic, phase, lead_name, event_key, 'request').format(topic=topic, source=source, lead=lead_name)


def build_result_sentence(phase, topic, uid, team_name, event_key, usage_counter):
    base_phase = 'verify' if phase == 'complete' else PHASE_ALIAS.get(phase, phase)
    return usage_aware_pick(RESULT_PATTERNS[base_phase], usage_counter, uid, team_name, topic, event_key, 'result')


def make_work_content(topic, phase, author, team_name, persona_hints, extra_keyword, event_key, sentence_usage, variant='v1', repeated=False):
    role_class = classify_member(author['role'], author['is_leader'])
    base_phase = 'verify' if phase == 'complete' else PHASE_ALIAS.get(phase, phase)
    sentence1 = usage_aware_pick([s.format(topic=topic) for s in INTRO_PATTERNS[phase]], sentence_usage, topic, phase, team_name, author['user_id'], event_key, 'intro')
    observation_parts = build_observation_parts(topic, team_name, extra_keyword, phase, author['user_id'], repeated, event_key, sentence_usage)
    sentence3 = usage_aware_pick(DETAIL_PATTERNS[base_phase], sentence_usage, topic, phase, team_name, author['user_id'], event_key, 'detail')
    sentence4 = usage_aware_pick(COLLAB_PATTERNS[role_class], sentence_usage, topic, phase, team_name, author['user_id'], event_key, 'collab')
    sentence5 = tone_sentence(author['user_id'], role_class, persona_hints, topic, phase, team_name, extra_keyword, event_key, sentence_usage)
    next_parts = build_next_parts(phase, topic, author['user_id'], team_name, extra_keyword, event_key, sentence_usage)
    sentence7 = build_result_sentence(phase, topic, author['user_id'], team_name, event_key, sentence_usage)
    if role_class == 'oversight':
        sentence1 = f"{topic} 건은 직접 손을 대기보다, {team_name} 안에서 누구에게 우선순위를 주고 어떤 판단 기준으로 넘길지가 더 중요해 보여 그 부분부터 정리했다."
    bridge = usage_aware_pick(
        [s.format(topic=topic, team_name=team_name, extra_keyword=extra_keyword) for s in VARIANT_BRIDGES[variant]],
        sentence_usage,
        topic, phase, team_name, author['user_id'], event_key, 'bridge',
    )
    repeat_line = usage_aware_pick(REPEAT_PATTERNS, sentence_usage, topic, phase, team_name, author['user_id'], event_key, 'repeat') if repeated else None
    if variant == 'v2':
        raw_sentences = [sentence1] + observation_parts + [bridge, sentence3, sentence4, sentence5, sentence7] + next_parts
    else:
        raw_sentences = [sentence1] + observation_parts + [sentence3, sentence4, sentence5, bridge] + next_parts
    if repeat_line:
        raw_sentences.insert(2, repeat_line)
    sentences = []
    for idx, sentence in enumerate(raw_sentences):
        if not sentence:
            continue
        slot = 'body'
        if idx == 0:
            slot = 'intro'
        elif sentence == bridge:
            slot = 'bridge'
        elif sentence == sentence4:
            slot = 'collab'
        elif sentence == sentence7:
            slot = 'result'
        elif repeat_line and sentence == repeat_line:
            slot = 'repeat'
        elif sentence in next_parts:
            slot = 'next'
        sentences.append(diversify_atomic_sentence(sentence, sentence_usage, phase, role_class, topic, extra_keyword, team_name, event_key, slot))
    return ' '.join(sentences)


def choose_status(phase):
    return rng.choice(STATUS_BY_PHASE[PHASE_ALIAS.get(phase, phase)])


def choose_importance(topic, phase):
    base = rng.choice(IMPORTANCE_BY_PHASE[PHASE_ALIAS.get(phase, phase)])
    hot = any(k in topic for k in ['P0', '보안', '권한', '릴리즈', '배포', '정산', '장애'])
    if hot and base == 'LOW':
        return 'HIGH'
    return base


def actual_hours(phase, role_class):
    base = {
        'request': 1.0, 'analyze': 3.0, 'fix': 4.0, 'verify': 2.0,
        'review': 1.5, 'improve': 2.5, 'hold': 0.5, 'report': 1.0,
        'cancel': 0.5, 'complete': 1.5, 'recurrence': 2.0,
    }[phase]
    if role_class == 'oversight':
        base = min(base, 1.5)
    return max(0.5, base + rng.choice([-0.5, 0, 0.5, 1.0]))


def daterange(start: date, end: date, offset: int) -> date:
    d = start + timedelta(days=offset)
    if d > end:
        return end
    return d


def generate_team_worklogs(team_id, team, persona_hints, variant='v1'):
    cfg = TEAM_CONFIG[team_id]
    target = cfg['count']
    topics = cfg['topics']
    start = team['start']
    end = team['end']
    total_days = (end - start).days
    rows = []
    cycle_idx = 0
    recurring_seen = defaultdict(int)
    lead_name = next(m['name'] for m in team['members'] if m['is_leader'])
    sentence_usage = Counter()

    while len(rows) < target:
        topic = topics[cycle_idx % len(topics)]
        template = TEAM_TEMPLATES[cycle_idx % len(TEAM_TEMPLATES)]
        if cycle_idx >= len(topics):
            repeated = True
            # later cycles spaced further to emulate recurrence/comparison
            base_offset = min(total_days - 5, int((cycle_idx / max(1, len(topics))) * (total_days * 0.55)) + (cycle_idx % len(topics)) * max(3, total_days // (len(topics) + 2)))
        else:
            repeated = False
            base_offset = min(total_days - 5, (cycle_idx % len(topics)) * max(4, total_days // (len(topics) + 3)))
        prev_date = daterange(start, end, base_offset)

        for phase in template:
            if len(rows) >= target:
                break
            author = pick_author(team, phase, cycle_idx)
            role_class = classify_member(author['role'], author['is_leader'])
            event_key = f'{team_id}:{cycle_idx}:{phase}:{len(rows)}'
            title = make_title(topic, phase, event_key)
            request_content = make_request(topic, phase, lead_name, event_key)
            extra_keyword = cfg['cross'][cycle_idx % len(cfg['cross'])]
            work_content = make_work_content(
                topic,
                phase,
                author,
                team['name'],
                persona_hints,
                extra_keyword,
                event_key=event_key,
                sentence_usage=sentence_usage,
                variant=variant,
                repeated=repeated,
            )
            title = fix_josa(title, topic, extra_keyword)
            request_content = fix_josa(request_content, topic, extra_keyword)
            work_content = fix_josa(work_content, topic, extra_keyword)
            step_gap = rng.randint(1, 6)
            instruction_date = prev_date if phase == template[0] else daterange(start, end, (prev_date - start).days + step_gap)
            prev_date = instruction_date
            due_days = rng.randint(1, 14)
            due_date = daterange(start, end, (instruction_date - start).days + due_days)
            status = choose_status(phase)
            importance = choose_importance(topic, phase)
            if repeated and phase in ('request', 'analyze', 'recurrence'):
                importance = 'HIGH' if importance == 'NORMAL' else importance
            completion_date = None
            if status == 'COMPLETED':
                drift = rng.choice([-1, 0, 1, 2, 3, 5])
                cd = due_date + timedelta(days=drift)
                if cd > end:
                    cd = end
                if cd < instruction_date:
                    cd = instruction_date
                completion_date = cd
            actual = actual_hours(phase, role_class)
            created_ts = datetime.combine(instruction_date, datetime.min.time()).replace(hour=9 + (cycle_idx % 7), minute=(cycle_idx * 7) % 60)
            updated_ts = created_ts if completion_date is None else datetime.combine(completion_date, datetime.min.time()).replace(hour=18 if status == 'COMPLETED' else 15, minute=(cycle_idx * 11) % 60)
            rows.append({
                'author_id': author['user_id'],
                'team_id': team_id,
                'title': title,
                'request_content': request_content,
                'work_content': work_content,
                'status_code': status,
                'importance_code': importance,
                'actual_hours': f'{actual:.1f}',
                'instruction_date': instruction_date.isoformat(),
                'due_date': due_date.isoformat(),
                'completion_date': completion_date.isoformat() if completion_date else None,
                'created_at': created_ts.strftime('%Y-%m-%d %H:%M:%S'),
                'updated_at': updated_ts.strftime('%Y-%m-%d %H:%M:%S'),
            })
        cycle_idx += 1
    return rows


def output_paths(variant: str, version_tag: str | None = None, out_path: str | None = None, summary_path: str | None = None):
    tag = version_tag or variant
    if out_path:
        out = Path(out_path)
        if not out.is_absolute():
            out = ROOT / out
    elif tag == 'v1':
        out = OUT
    else:
        out = SQL_DIR / f'ibank-worklog-seed-{tag}.sql'
    if summary_path:
        summary = Path(summary_path)
        if not summary.is_absolute():
            summary = ROOT / summary
    elif tag == 'v1':
        summary = SUMMARY
    else:
        summary = SQL_DIR / f'ibank-worklog-summary-{tag}.md'
    return out, summary


def build_sql(variant='v1', version_tag: str | None = None, out_path: str | None = None, summary_path: str | None = None, team_ids=None, start_id: int = 1):
    teams = parse_teams()
    if team_ids:
        wanted = set(team_ids)
        teams = {tid: team for tid, team in teams.items() if tid in wanted}
        missing = sorted(wanted - set(teams))
        if missing:
            raise ValueError(f'Missing team MDs for team IDs: {missing}')
    persona_hints = parse_persona_hints()
    all_rows = []
    by_team = {}
    worklog_id = start_id
    out_file, summary_file = output_paths(variant, version_tag, out_path, summary_path)
    version_label = version_tag or variant
    for tid in sorted(teams):
        rows = generate_team_worklogs(tid, teams[tid], persona_hints, variant=variant)
        for row in rows:
            row['worklog_id'] = worklog_id
            worklog_id += 1
        by_team[tid] = rows
        all_rows.extend(rows)

    lines = []
    lines.append('-- iBank worklog seed SQL generated from team staffing docs and personas')
    lines.append(f'-- deterministic seed: {SEED}')
    lines.append(f'-- generation variant: {variant}')
    lines.append(f'-- version tag: {version_label}')
    lines.append('BEGIN;')
    lines.append('')
    lines.append('INSERT INTO tb_worklog (worklog_id, author_id, team_id, title, request_content, work_content, status_code, importance_code, actual_hours, instruction_date, due_date, completion_date, ai_summary, ai_summary_edited, ai_processing_status, is_deleted, created_at, updated_at) VALUES')
    vals = []
    for r in all_rows:
        vals.append(
            '  (' + ', '.join([
                str(r['worklog_id']),
                str(r['author_id']),
                str(r['team_id']),
                sql_str(r['title']),
                sql_str(r['request_content']),
                sql_str(r['work_content']),
                sql_str(r['status_code']),
                sql_str(r['importance_code']),
                r['actual_hours'],
                sql_str(r['instruction_date']),
                sql_str(r['due_date']),
                sql_str(r['completion_date']) if r['completion_date'] else 'NULL',
                'NULL',
                'false',
                sql_str('COMPLETED'),
                'false',
                sql_str(r['created_at']),
                sql_str(r['updated_at']),
            ]) + ')'
        )
    lines.append(',\n'.join(vals) + ';')
    lines.append('')
    lines.append("SELECT setval(pg_get_serial_sequence('tb_worklog', 'worklog_id'), (SELECT COALESCE(MAX(worklog_id), 1) FROM tb_worklog), true);")
    lines.append('')
    lines.append('COMMIT;')
    lines.append('')

    status_counter = Counter(r['status_code'] for r in all_rows)
    importance_counter = Counter(r['importance_code'] for r in all_rows)
    per_team = {tid: len(rows) for tid, rows in by_team.items()}
    summary = ['# iBank Worklog Seed Summary', '', f'- generation variant: {variant}', f'- version tag: {version_label}', f'- start worklog_id: {start_id}', f'- total worklogs: {len(all_rows)}', f'- output file: `{out_file.relative_to(ROOT)}`', '', '## Per-team counts']
    for tid in sorted(per_team):
        summary.append(f'- {tid}: {per_team[tid]}')
    summary += ['', '## Status distribution']
    for k in ['COMPLETED', 'IN_PROGRESS', 'PENDING', 'ON_HOLD', 'CANCELLED']:
        summary.append(f'- {k}: {status_counter.get(k, 0)}')
    summary += ['', '## Importance distribution']
    for k in ['NORMAL', 'HIGH', 'URGENT', 'LOW']:
        summary.append(f'- {k}: {importance_counter.get(k, 0)}')
    summary_file.write_text('\n'.join(summary) + '\n', encoding='utf-8')
    return '\n'.join(lines), summary, out_file


def validate(sql_text):
    # lightweight internal consistency check from generated rows, not full SQL parsing
    lines = sql_text.splitlines()
    worklog_lines = [ln for ln in lines if ln.startswith('  (')]
    return len(worklog_lines)


def main():
    parser = argparse.ArgumentParser(description='Generate iBank worklog seed SQL with richer narrative variants.')
    parser.add_argument('--variant', choices=['v1', 'v2'], default='v1')
    parser.add_argument('--version-tag', help='Optional version tag for output file names, e.g. v3')
    parser.add_argument('--out', help='Optional output SQL path')
    parser.add_argument('--summary-out', help='Optional output summary path')
    parser.add_argument('--team-ids', help='Comma-separated team IDs to generate, e.g. 111,112,113,114')
    parser.add_argument('--start-id', type=int, default=1, help='First worklog_id to use')
    args = parser.parse_args()

    team_ids = [int(v.strip()) for v in args.team_ids.split(',')] if args.team_ids else None
    sql_text, summary, out_file = build_sql(variant=args.variant, version_tag=args.version_tag, out_path=args.out, summary_path=args.summary_out, team_ids=team_ids, start_id=args.start_id)
    out_file.write_text(sql_text, encoding='utf-8')
    count = validate(sql_text)
    print(f'Wrote {out_file} with {count} worklog rows')
    print('\n'.join(summary[:20]))

if __name__ == '__main__':
    main()
