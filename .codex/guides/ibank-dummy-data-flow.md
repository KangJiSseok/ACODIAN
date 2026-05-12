# iBank Dummy Team & Worklog Flow Guide

## 이번에 준비된 것
- `.codex/agents/user-001.toml` ~ `.codex/agents/user-035.toml`
- `.codex/agents/ceo.toml`
- `.codex/personas/user-persona-index.md`
- `.codex/templates/team-md-template.md`
- `.codex/templates/team-lead-worklog-review-checklist.md`

## 추천 진행 순서
1. **팀 정보 제공**
   - 팀명
   - 주관 부서
   - 팀 기간
   - 해결하려는 핵심 문제
   - 원하는 협업 부서
2. **팀 MD 생성**
   - 내가 `team-md` 문서를 만들고 팀장 후보를 지정
3. **팀원 선정**
   - user_id 기준으로 적합한 팀원/협업자/user_team 구성을 제안
4. **워크로그 생성**
   - 각 팀원 페르소나에 맞는 자연어 worklog를 생성
5. **팀장 리뷰/승인**
   - 지정된 팀장 페르소나 기준으로 승인/보완 요청 의견 생성
6. **SQL 출력**
   - 원하면 마지막에 `tb_team`, `tb_user_team`, `tb_team_admin`, `tb_worklog` INSERT 형태로 정리

## 내가 지금 바로 받아서 처리할 수 있는 입력 예시
### 1) 팀 MD부터 만들고 싶을 때
"""
주관 부서: 3
팀명: AI 검색 정확도 개선 TF
기간: 2025-01-06 ~ 2025-05-30
목표: 검색 품질 저하와 태그 불일치 문제를 개선하고, 3개월 전 유사 이슈와 비교 가능한 흐름을 만들고 싶어.
협업 부서: 1, 2
"""

### 2) 팀원 배정까지 같이 하고 싶을 때
"""
위 팀에 대해 팀장 1명, 핵심 인력 3명, 타 부서 협업 2명, 사업부장 검토 1명을 포함해서 user_team 구성을 제안해줘.
"""

### 3) worklog까지 만들고 싶을 때
"""
위 팀 기준으로 worklog 60개를 만들어줘.
- 검색 테스트 패턴을 충분히 섞어줘
- 사업부장 검토 worklog도 포함해줘
- SQL INSERT로 출력해줘
"""

### 4) 팀장 리뷰까지 원할 때
"""
방금 만든 worklog 중 15개를 팀장 시점에서 검토하고 승인/보완요청 코멘트를 붙여줘.
"""

## 앞으로 내가 질문하면 어떻게 동작하는가
- **팀 정보만 주면** → 팀 MD 초안 + 팀장 후보 제안
- **팀 구성까지 요청하면** → user_team / team_admin 배정안 제안
- **worklog까지 요청하면** → 각 user persona 스타일을 반영한 업무일지 생성
- **승인까지 요청하면** → 지정된 팀장 페르소나로 리뷰/승인 코멘트 생성
- **SQL로 달라고 하면** → `.sql` 파일 기준으로 INSERT 문 형태로 정리

## 나중에 더 필요할 수 있는 정보
아래는 "에이전트 생성"에는 필요 없었지만, 실제 대량 더미 SQL 생성 단계에서는 있으면 좋은 정보입니다.
- password hash 알고리즘(예: bcrypt/argon2)
- 랜덤성이 매번 달라도 되는지, 고정 시드가 필요한지
