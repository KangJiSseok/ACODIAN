# CI 인증 자격증명 주입·형식 실패 대응

GitLab CI 파이프라인에서 `GHCR_USER` / `GHCR_TOKEN` / `SSH_PRIVATE_KEY` 같은 자격증명 변수가
runtime 에 원하는 형태로 주입되지 않아 `docker login` · `ssh` 가 실패하는 케이스를 다룬다.
규정 기준은 ADR-010 · OPS-006 · OPS-009 · OPS-010 · OPS-012.

## 증상 (Symptoms)

### A. `docker login` 이 `Must provide --username with --password-stdin` 로 실패

로그 원문:

```text
$ echo "$GHCR_TOKEN" | docker login ghcr.io -u "$GHCR_USER" --password-stdin
Must provide --username with --password-stdin
ERROR: Job failed: exit code 1
```

발생 위치: `api_image` job 의 `before_script` (dev/master push 파이프라인).
이 메시지는 docker CLI 가 `-u ""` (빈 문자열) 을 받았을 때 나오는 고유 시그니처다.

### B. `ssh` 가 `Load key ... error in libcrypto` + `Permission denied (publickey)` 로 실패

로그 원문:

```text
Load key "/root/.ssh/id_ed25519": error in libcrypto
ubuntu@<EC2>: Permission denied (publickey).
ERROR: Job failed: exit code 255
```

발생 위치: `deploy_dev` / `deploy_prod` 의 SSH 접속 시 (`.deploy_base.before_script` 이후 첫 `ssh`).
`libcrypto` 에러가 먼저 나오고 `Permission denied` 가 뒤따르는 게 핵심 시그니처 —
권한 문제가 아니라 **키 파일 자체를 openssl 이 파싱 못 함**.

---

## 1차 점검 (Triage — 5분)

### 공통
1. GitLab → Settings → CI/CD → Variables 에서 해당 Key 의 **Protected** · **Environments** · **Type** 컬럼이 기대값과 일치하는가
2. job 이 실행된 브랜치가 Protected branch(`dev` / `master`) 인가. Protected variable 은 비-Protected 브랜치에 주입되지 않는다
3. 최근 MR 머지로 Variables 설정이 건드려지지는 않았는지 (`Settings → Audit Events` 확인)

### A 케이스
- `.gitlab-ci.yml` 참조명 확인: `grep -n 'GHCR_USER\|GHCR_TOKEN' .gitlab-ci.yml`
- Variables 목록의 Key 명을 글자 단위로 대조 — 유사 이름(`GHCR_NAME`, `GHCR_USERNAME`) 이 섞여 있지 않은지

### B 케이스
- `SSH_PRIVATE_KEY` Type 이 `Variable` 인지 `File` 인지
- 값 마지막이 `-----END OPENSSH PRIVATE KEY-----` 뒤에 **LF 개행 1개**를 포함하는지
- 키 생성 시 passphrase 없이 발급됐는지

---

## 복구 절차 (Recovery)

### 0. 간접 probe 주입 (원인 확정용, OPS-012)

실패하는 job 의 `before_script` 최상단에 임시로 아래 2줄만 추가한다.
값 자체는 절대 노출하지 않는다.

```yaml
before_script:
  - env | awk -F= '/^GHCR|^SSH/ {print $1}'
  - echo "USER set?=${GHCR_USER+yes} len=${#GHCR_USER} TOKEN set?=${GHCR_TOKEN+yes} len=${#GHCR_TOKEN}"
  # 기존 라인들...
```

결과 해석표:

| 출력 | 의미 |
|---|---|
| 이름 목록에 `GHCR_USER` 없음 | **Key 불일치** — 다른 이름(GHCR_NAME 등)으로 등록됐거나 보이지 않는 문자 섞임 |
| `set?=yes len=0` | **값이 빈 문자열** — Masked 저장 실패 잔재, Value 공란 저장 의심 |
| `set?=yes len>0` | 주입 정상. 다른 원인(권한·레지스트리·네트워크) 재조사 |

### A-1. GHCR Key 불일치 복구

1. 잘못된 Key(예: `GHCR_NAME`) 를 **Delete**
2. 새로 Add variable:
   - Key: `GHCR_USER` — **키보드로 직접 타이핑** (복붙 금지)
   - Value: GitHub 개인 계정 username (Organization 이름 아님)
   - Type: `Variable`
   - Protect: ON
   - Mask: OFF (username 은 마스킹 조건에 걸리면 저장 실패함)
   - Environments: `All (default) *`
3. Retry 로 probe 재확인 → `set?=yes len>0`

### A-2. GHCR_TOKEN 값 문제 복구 (희소 케이스)

`GHCR_TOKEN` 은 정상, `GHCR_USER` 만 빈 값일 때는 A-1 으로 충분하지만,
Token 쪽이 의심되면 GitHub → Developer settings → Personal access tokens (classic) 에서
`write:packages`, `read:packages`, `repo` 스코프로 재발급 후 Variables 재등록.
PAT 만료 기한은 캘린더 D-7 알림으로 관리한다(ADR-007).

### B. SSH PEM 개행 복구

1. 원본 키 파일 무결성 재확인:
   ```bash
   xxd ~/.ssh/id_ed25519 | tail -2   # 마지막 바이트가 0a(LF) 인지 확인
   ```
2. Windows/웹 경유 없이 EC2 에서 `cat ~/.ssh/id_ed25519` 결과를 그대로 복사
3. GitLab UI → `SSH_PRIVATE_KEY` **Delete**
4. 새로 Add variable:
   - Key: `SSH_PRIVATE_KEY`
   - Value: PEM 전체. 편집기에서 **마지막 줄을 비워 둔 채** 저장 (trailing newline 보존)
   - Type: `Variable` (또는 `File` — File 로 바꾸면 `.gitlab-ci.yml` 의 `printf '%s' "$SSH_PRIVATE_KEY" > ~/.ssh/id_ed25519` 를 `cp "$SSH_PRIVATE_KEY" ~/.ssh/id_ed25519` 로 교체 필요)
   - Protect: ON
   - Mask: OFF (multi-line 은 Masked 조건 미충족)
   - Expand variable reference: OFF (값 내 `$` 보호, OPS-006)
5. 배포 job Retry → `ssh` 성공 확인

### 공통 마무리

- probe 라인은 원인 확정 후 동일 MR 또는 후속 MR 로 **반드시 제거** (OPS-012)
- MR 설명에 probe 실행 결과 발췌(예: `GHCR_USER set?=yes len=8`) 를 검증 증거로 남긴다 (OPS-009)

---

## 근본 원인 분류 (Root Cause Categories)

| 분류 | 증상 신호 | 예방 지점 |
|---|---|---|
| **Key 불일치** (오타·유사 문자·trailing space) | probe 이름 목록에 기대 Key 가 없음 | ADR-010 (1), OPS-009 |
| **값이 빈 문자열** (Masked 저장 실패 잔재, Value 공란 저장) | `set?=yes len=0` | ADR-010 (3), OPS-009 |
| **Protected × 브랜치 불일치** | 비-Protected 브랜치 job 에서 주입 실패 | ADR-010 (5), OPS-006 |
| **Environment scope 불일치** | `environment:` 미선언 job 에 env-scoped 변수 | ADR-010 (5) |
| **PEM trailing newline 누락** | `error in libcrypto` → `Permission denied` | OPS-010 |
| **CRLF 개행 오염** | 이관 경로에 Windows 편집기/웹 위젯 | OPS-010 |
| **Variable Type 혼동** (File vs Variable) | key 파일에 경로 문자열이 들어감, `printf` vs `cp` 미스매치 | B 복구 절차 참조 |

---

## 후속 조치 (Follow-up)

1. probe 제거 커밋을 원인 확정 MR 에 포함 (OPS-012 강제)
2. 새 신호가 나오면 "근본 원인 분류" 표에 행을 추가하고, 해당 예방 지점(ADR-010 / OPS-009~012) 을 갱신
3. `SSH_PRIVATE_KEY` 는 OPS-005 에 따라 개인 접속 키와 분리된 CI 전용 키페어로 운영한다. 키 재생성 시 EC2 `authorized_keys` 와 GitLab Variables 를 **동시** 갱신
4. 분기 1회 GitLab CI/CD Variables 감사 로그를 훑어 Protected/Mask 해제 같은 비정상 변경이 없는지 점검

---

## 과거 발생 기록

### 2026-04-24 — GHCR_USER 미등록 + SSH PEM trailing newline 누락

- **증상 A**: dev push 의 `api_image` 에서 `docker login` 이 `Must provide --username with --password-stdin` 로 반복 실패
- **진단**: env 이름 목록에 `GHCR_USER` 가 없고 `GHCR_NAME` 만 존재 → Key 불일치 확정
- **복구**: `GHCR_NAME` 삭제 후 `GHCR_USER` 로 재등록 (값: GitHub 개인 계정 username `sbturtle`)
- **증상 B**: 같은 날 후속 `deploy_dev` 가 `Load key ... error in libcrypto` 로 실패
- **진단**: `SSH_PRIVATE_KEY` 값 마지막 `-----END OPENSSH PRIVATE KEY-----` 뒤 LF 누락
- **복구**: 키 재등록 시 마지막 줄 개행 포함
- **후속 반영**: ADR-010 신설, OPS-009 / OPS-010 / OPS-011 / OPS-012 신설, 본 runbook 생성
