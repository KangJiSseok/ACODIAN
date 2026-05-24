# Web E2E 실행 안내

`pnpm e2e`와 `pnpm e2e:ui`는 별도 mock 명령으로 분기하지 않고 실제 API 서버를 호출한다.

## 필수 환경변수

- `NEXT_PUBLIC_API_BASE_URL`: 브라우저가 호출할 API base URL. 예: `https://k14s209.p.ssafy.io:8443/api`
- `WEB_E2E_DIRECTOR_EMAIL`, `WEB_E2E_DIRECTOR_PASSWORD`
- `WEB_E2E_DEPT_HEAD_EMAIL`, `WEB_E2E_DEPT_HEAD_PASSWORD`
- `WEB_E2E_TEAM_LEAD_EMAIL`, `WEB_E2E_TEAM_LEAD_PASSWORD`
- `WEB_E2E_MEMBER_EMAIL`, `WEB_E2E_MEMBER_PASSWORD`

이메일은 기본 iBank dummy 계정을 기본값으로 사용하지만, 비밀번호는 저장소에 커밋하지 말고 로컬 `.env` 또는 CI secret으로 주입한다.

## 데이터 정책

- 테스트는 실제 원격 서버 상태를 전제로 하므로 mock fixture 문자열에 의존하지 않는다.
- 서버 데이터가 없을 수 있는 목록/다운로드/병합 후보는 empty state 또는 `test.skip`으로 분리한다.
- 원격 서버 데이터를 파괴할 수 있는 병합, 전체 읽음, 기존 조직/팀/업무 수정은 기본 E2E에서 수행하지 않는다.
- 생성/수정 mutation이 필요한 테스트를 추가할 때는 `E2E_${Date.now()}_${test.info().parallelIndex}` 같은 unique marker와 cleanup 경로를 함께 둔다.

## 로컬 실행

```bash
cd web
pnpm e2e --project=chromium
pnpm e2e:ui
```

`pnpm e2e:ui`는 UI Mode에서도 성공 테스트의 action snapshot을 볼 수 있도록 `--trace on`을 함께 사용한다. 이미 UI Mode 창을 띄워둔 상태라면 기존 프로세스를 종료한 뒤 다시 실행한다.

로컬 Next dev server는 Playwright가 자동으로 실행하며 기본 host는 IPv4 `127.0.0.1`이다. 이미 다른 dev server를 쓰려면 `WEB_E2E_BASE_URL`을 지정한다. 보호 라우트 검증을 위해 Playwright 실행 중에만 `NEXT_PUBLIC_E2E_AUTH_BOOTSTRAP=1`이 dev server에 주입된다.

## UI Mode / trace 확인

UI Mode에서 오른쪽 `After`가 `about:blank`로 보이면 먼저 UI Mode를 `pnpm e2e:ui`로 다시 실행했는지 확인한다. 그 다음 왼쪽 테스트 전체나 가운데 `Passed` 요약 행이 아니라 `page.goto`, `click`, `expect` 같은 개별 action을 선택한다. 개별 action에도 스냅샷이 없으면 다음 명령으로 trace를 남겨 HTML report에서 다시 확인한다.

```bash
cd web
pnpm e2e:trace --project=chromium
pnpm e2e:report
```

기본 설정은 UI Mode 디버깅을 우선해 성공 테스트도 trace를 남기는 `WEB_E2E_TRACE=on`이다. trace 파일 생성을 줄이고 싶을 때만 `WEB_E2E_TRACE=retain-on-failure pnpm e2e --project=chromium`을 사용한다. 커밋된 E2E 테스트에는 `page.pause()`를 두지 않고, 일시 디버깅이 필요할 때만 로컬에서 `pnpm exec playwright test --debug` 또는 임시 `page.pause()`를 사용한다.
