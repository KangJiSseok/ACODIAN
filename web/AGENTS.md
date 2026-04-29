# web/AGENTS.md — AX-WMS Web 작업 규칙

## 1. 적용 범위

- 이 파일은 `web` 디렉터리 아래의 Next.js 프론트엔드 작업에 적용한다.
- 루트 `AGENTS.md`의 공통 규칙을 함께 따른다.

## 2. 성능 우선 개발 기준

- 기능 개발 시 UI 동작뿐 아니라 렌더링 비용, 번들 증가, 이미지/폰트 로딩, 레이아웃 이동을 함께 점검한다.
- 사용자에게 보이는 화면 변경은 가능한 한 LCP, CLS, INP에 영향을 주는 요소를 확인한다.
- 큰 컴포넌트, 차트, 에디터, 모달, 3D/캔버스, 이미지가 추가될 때는 lazy loading, dynamic import, 적절한 skeleton/placeholder, 이미지 크기 지정 여부를 검토한다.
- 불필요한 client component 전환을 피하고, 상호작용이 필요한 범위만 `"use client"`로 둔다.
- 상태 변경이나 애니메이션이 레이아웃 전체를 흔들지 않도록 width/height/flex-basis/transform 등 변경 속성을 명확히 선택한다.

## 3. MCP 활용 기준

- 로컬 UI 흐름, 회귀 확인, 스크린샷 검증이 필요하면 Playwright MCP를 우선 사용한다.
- 성능 점수, 접근성, SEO, best-practices를 함께 점검해야 하면 Lighthouse MCP를 사용한다.
- Core Web Vitals 현황이나 실제 URL 기반 개선 기회를 확인해야 하면 Core Web Vitals MCP를 사용한다.
- localhost처럼 외부에서 접근할 수 없는 URL은 Lighthouse MCP 또는 Playwright MCP로 확인하고, PageSpeed/Core Web Vitals 계열 검사는 배포 또는 스테이징 URL에서 수행한다.

## 4. 변경 전후 확인

- 성능에 영향을 줄 수 있는 변경은 가능하면 변경 전/후 결과를 비교한다.
- UI가 큰 폭으로 바뀌는 작업은 desktop/mobile viewport에서 레이아웃 이동과 overflow를 확인한다.
- 성능 측정 결과를 절대 점수만으로 판단하지 말고, 변경 목적과 사용자 경로 기준으로 해석한다.
- 검사 실행이 불가능한 환경이면 완료 보고에 그 이유와 남은 확인 항목을 명시한다.

## 5. 기본 검증

- web 변경 후에는 프로젝트 상황에 맞춰 `pnpm --filter ./web lint`를 우선 실행한다.
- 성능 관련 변경이면 필요에 따라 빌드, Lighthouse MCP, Playwright MCP 확인을 추가한다.
