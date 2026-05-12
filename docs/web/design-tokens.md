# Web Design Tokens

## 목적
- AX-WMS `web`의 디자인 토큰 기준을 정리한다.
- `shadcn/ui` primitive와 앱 공통 컴포넌트가 동일한 시맨틱 토큰을 사용하도록 맞춘다.
- 이번 문서는 토큰 범위만 다루며, 컴포넌트 패턴과 도메인 컴포넌트 규칙은 별도 문서에서 다룬다.

## 기준 소스
- 와이어프레임 기준 구현: `C:/Users/SSAFY/WMS-WireFrame/apps/web/src/index.css`
- 참고 문서: `C:/Users/SSAFY/WMS-WireFrame/DESIGN.md`

## 적용 원칙
- 원시 색상보다 시맨틱 토큰을 우선 사용한다.
- 페이지와 도메인 컴포넌트에서 raw hex 사용은 최소화한다.
- `workspace` 셸 계열 토큰과 일반 surface 토큰을 분리한다.
- `Button`, `Card`, `Input`, `Select`, `Badge` 같은 primitive는 반드시 토큰만 소비한다.
- 로그인 화면처럼 별도 연출이 강한 화면은 예외 surface로 분리하고, 기본 업무 화면 토큰과 섞지 않는다.

## Color Tokens

### Core Surface
| Token | Light | Dark | 역할 |
|---|---|---|---|
| `--background` | `#f4f6fc` | `#020617` | 기본 페이지 배경 |
| `--foreground` | `#0f172a` | `#f8fafc` | 기본 본문 텍스트 |
| `--card` | `rgba(255, 255, 255, 0.85)` | `rgba(15, 23, 42, 0.88)` | 카드 배경 |
| `--card-foreground` | `#0f172a` | `#f8fafc` | 카드 텍스트 |
| `--popover` | `rgba(255, 255, 255, 0.95)` | `rgba(15, 23, 42, 0.96)` | 팝오버 배경 |
| `--popover-foreground` | `#0f172a` | `#f8fafc` | 팝오버 텍스트 |
| `--input` | `rgba(255, 255, 255, 0.86)` | `rgba(255, 255, 255, 0.06)` | 입력 필드 배경 |
| `--border` | `rgba(30, 58, 138, 0.15)` | `rgba(59, 130, 246, 0.15)` | 기본 보더 |

### Semantic Brand
| Token | Light | Dark | 역할 |
|---|---|---|---|
| `--primary` | `#1e3a8a` | `#3b82f6` | 주요 CTA, 강조 요소 |
| `--primary-foreground` | `#ffffff` | `#ffffff` | primary 위 텍스트 |
| `--secondary` | `rgba(30, 58, 138, 0.08)` | `rgba(30, 58, 138, 0.18)` | 약한 강조 배경 |
| `--secondary-foreground` | `#1e293b` | `#e0e7ff` | secondary 위 텍스트 |
| `--accent` | `rgba(30, 58, 138, 0.12)` | `rgba(59, 130, 246, 0.18)` | hover, active, 보조 강조 |
| `--accent-foreground` | `#0f172a` | `#eff6ff` | accent 위 텍스트 |
| `--muted` | `rgba(30, 58, 138, 0.06)` | `rgba(255, 255, 255, 0.06)` | 약한 패널, 비활성 배경 |
| `--muted-foreground` | `#64748b` | `#94a3b8` | 보조 설명 텍스트 |
| `--ring` | `#3b82f6` | `#3b82f6` | 포커스 링 |

### Feedback
| Token | Value | 역할 |
|---|---|---|
| `--success` | `#10b981` | 성공 상태, 완료 계열 |
| `--warning` | `#f59e0b` | 경고 상태, 주의 계열 |
| `--destructive` | `#ef4444` | 오류 상태, 삭제 계열 |

### Sidebar / App Shell
| Token | Light | Dark | 역할 |
|---|---|---|---|
| `--sidebar` | `#0f172a` | `#02040a` | 좌측 네비게이션 배경 |
| `--sidebar-foreground` | `#f8fafc` | `#f8fafc` | 사이드바 텍스트 |
| `--sidebar-primary` | `#60a5fa` | `#ffffff` | 사이드바 강조 요소 |
| `--sidebar-primary-foreground` | `#020617` | `#020617` | 사이드바 강조 텍스트 |
| `--sidebar-accent` | `rgba(255, 255, 255, 0.1)` | `rgba(255, 255, 255, 0.1)` | 사이드바 hover / active 배경 |
| `--sidebar-accent-foreground` | `#ffffff` | `#ffffff` | 사이드바 hover / active 텍스트 |
| `--sidebar-border` | `rgba(255, 255, 255, 0.08)` | `rgba(255, 255, 255, 0.08)` | 사이드바 보더 |
| `--sidebar-ring` | `#3b82f6` | `#60a5fa` | 사이드바 포커스 링 |

### Workspace Tokens
| Token | Value | 역할 |
|---|---|---|
| `--workspace-shell-base` | `#0f1a35` | top bar / sidebar 베이스 색상 |
| `--workspace-shell-gradient` | `radial-gradient(circle at 18% 0%, rgba(74, 121, 255, 0.14), transparent 32%), linear-gradient(180deg, #18284f 0%, #132243 46%, #0f1a35 100%)` | 앱 셸 배경 그라디언트 |

### Chart Tokens
| Token | Value | 역할 |
|---|---|---|
| `--chart-1` | `#3b82f6` | 차트 메인 |
| `--chart-2` | `#60a5fa` | 차트 보조 1 |
| `--chart-3` | `#93c5fd` | 차트 보조 2 |
| `--chart-4` | `#2563eb` | 차트 강조 |
| `--chart-5` | `#1d4ed8` | 차트 깊이 단계 |

## Typography Tokens
| Token | Value | 역할 |
|---|---|---|
| `--font-display-family` | `"Pretendard Variable", "Pretendard", -apple-system, BlinkMacSystemFont, "Apple SD Gothic Neo", "Segoe UI", sans-serif` | 기본 UI / 본문 폰트 |
| `--font-mono-family` | `"IBM Plex Mono", monospace` | 코드, 숫자, 식별자 계열 |

## Radius Tokens
| Token | Value | 역할 |
|---|---|---|
| `--radius` | `0.5rem` | 기본 radius 기준값 |
| `--radius-sm` | `calc(var(--radius) - 6px)` | 작은 입력, 작은 컨트롤 |
| `--radius-md` | `calc(var(--radius) - 2px)` | 중간 크기 입력, 보조 버튼 |
| `--radius-lg` | `var(--radius)` | 기본 버튼, 카드 |
| `--radius-xl` | `calc(var(--radius) + 6px)` | 큰 패널, 강조 카드 |

## Shadow Tokens
| Token | Light | Dark | 역할 |
|---|---|---|---|
| `--shadow-panel` | `0 24px 70px -36px rgba(34, 17, 42, 0.4)` | `0 32px 90px -52px rgba(0, 0, 0, 0.78)` | 기본 패널 그림자 |
| `--shadow-panel-strong` | `0 30px 80px -38px rgba(15, 6, 19, 0.48)` | `0 40px 110px -60px rgba(0, 0, 0, 0.92)` | 강조 패널 그림자 |
| `--shadow-inset` | `inset 0 1px 0 rgba(255, 255, 255, 0.08)` | `inset 0 1px 0 rgba(255, 255, 255, 0.04)` | 내부 하이라이트 |

## Token Usage Rules

### 1. 페이지 배경
- 일반 업무 화면은 `background` 기반으로 처리한다.
- `workspace` 톤은 `workspace-shell-base`, `workspace-shell-gradient`로만 처리한다.
- 별도 브랜딩 연출이 필요한 화면이 아니면 페이지에서 임의의 그라디언트를 직접 만들지 않는다.

### 2. 텍스트
- 기본 텍스트는 `foreground`를 사용한다.
- 설명 텍스트, 보조 텍스트는 `muted-foreground`를 사용한다.
- 카드, 팝오버 내부 텍스트는 각각 `card-foreground`, `popover-foreground`를 우선한다.

### 3. 액션 / 상태
- 주요 액션은 `primary`, 보조 액션은 `secondary`, hover / active 성격은 `accent`를 사용한다.
- 성공 / 경고 / 오류는 각각 `success`, `warning`, `destructive`만 사용한다.
- 상태별 색은 도메인 컴포넌트에서 매핑하되, 새로운 raw hex를 추가하지 않는다.

### 4. 보더 / 입력창
- 기본 보더는 `border`, 입력 배경은 `input`을 사용한다.
- 포커스 스타일은 `ring`으로 통일한다.
- 페이지 단에서 보더 opacity를 새로 계산하기보다, 필요 시 토큰 조합만 허용한다.

### 5. 예외 화면
- 로그인 화면은 와이어프레임상 raw hex와 전용 그라디언트가 많아 기본 업무 화면 토큰과 분리되어 있다.
- 따라서 인증 화면은 1차 토큰 표준화 대상에서 제외하고, 추후 `auth surface` 토큰 묶음으로 별도 정리한다.

## 우선 적용 순서
1. `Button`, `Input`, `Select`, `Card`, `Badge`에 토큰 사용을 강제한다.
2. `PageHeader`, `Pagination`, `StatCard`, `SearchBar`, `SearchFilters` 같은 공통 패턴에서 raw color 사용을 줄인다.
3. 로그인 화면은 별도 surface로 분리 정리한다.

## 제외 범위
- 컴포넌트 variant 설계
- 패턴 컴포넌트 구조
- 도메인별 배지 색상 세부 규칙
- 인증 화면 전용 색 체계
