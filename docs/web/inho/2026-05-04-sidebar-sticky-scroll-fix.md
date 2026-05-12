# Sidebar 스크롤 잘림 현상 수정 회고

- 작성일: 2026-05-04
- 파일명: `2026-05-04-sidebar-sticky-scroll-fix.md`
- 대상 파일:
  - `web/src/app/_common/components/layout/sidebar.tsx`

## 목적

페이지 본문이 뷰포트보다 길어 스크롤이 발생할 때 사이드바가 함께 위로 밀려 올라가, 상단이 잘려 보이고 하단이 비어 보이는 현상이 발생했다. 원인과 수정 방향을 기록한다.

## 증상

- 마이페이지처럼 본문이 긴 화면에서 마우스로 페이지를 스크롤하면 좌측 사이드바도 같이 위로 끌려 올라간다.
- 그 결과 사이드바 상단(브랜드 영역, 상단 메뉴)이 화면 밖으로 잘려 보이고, 사이드바 하단 아래쪽에는 빈 영역이 생긴다.
- 사이드바는 보호 라우트의 주 내비게이션이므로 어떤 스크롤 위치에서도 항상 같은 자리에 노출되어야 한다.

## 원인

`web/src/app/_common/components/layout/sidebar.tsx`의 `<aside>`가 다음 상태였다.

- 높이는 `h-screen`(100vh)으로 고정되어 있었다.
- 포지셔닝은 `relative` 만 지정되어 있어 사실상 일반 흐름(normal flow)에 놓여 있었다.

부모 레이아웃은 `web/src/app/(protected)/layout.tsx`에서 다음과 같이 구성되어 있다.

```tsx
<div className="flex min-h-screen">
  <Sidebar />
  <div className="flex min-h-screen min-w-0 flex-1 flex-col">
    <Gnb />
    <main className="workspace-main flex-1 px-4 py-6 md:px-8">{children}</main>
  </div>
</div>
```

이 구조에서는 `<main>` 콘텐츠가 길어지면 페이지 자체(루트 스크롤 컨테이너)가 스크롤된다. 그런데 `<aside>`는 일반 흐름 안의 flex 아이템이라, 페이지가 스크롤되면 사이드바도 함께 위로 흘러 올라간다.

내부 `<div>`에 `overflow-y-auto`가 걸려 있긴 했지만, 이는 사이드바 자체가 화면에 고정되어 있을 때만 의미가 있다. 사이드바 자체가 페이지와 함께 스크롤되어버리면 내부 스크롤은 무용지물이었다.

## 해결

`<aside>`의 포지셔닝을 `relative`에서 `sticky top-0`으로 교체했다.

- `position: sticky` + `top: 0`으로 사이드바가 항상 뷰포트 상단에 고정된다.
- `h-screen`은 그대로 유지하여 사이드바 높이가 항상 100vh가 되도록 한다.
- 내부 `overflow-y-auto`가 그대로 살아있어, 메뉴가 길어지면 사이드바 내부에서만 스크롤된다(외부 페이지 스크롤과 분리).
- `z-20`도 유지하여 메인 콘텐츠 위에 올바르게 겹쳐 표시된다.

## sticky가 동작하기 위한 전제 확인

`position: sticky`는 다음 조건이 충족돼야 정상 동작한다.

- 부모 컨테이너에 `overflow: hidden | auto | scroll`이 걸려 있지 않아야 한다.
- sticky 요소에 위치 기준값(`top`, `left` 등)이 지정돼 있어야 한다.

현재 부모 체인은 다음과 같다.

- `<body>` / `<html>`: 별도 overflow 설정 없음
- protected layout의 `<div className="flex min-h-screen">`: overflow 설정 없음
- `<aside>` 자체에 `top-0` 명시

따라서 sticky가 정상적으로 뷰포트 기준으로 동작한다.

## 다음 작업 시 기준

- 페이지 전역 레이아웃에서 "항상 보여야 하는 사이드 영역"은 sticky로 뷰포트에 고정한다. 일반 흐름에 두고 `h-screen`만으로 처리하지 않는다.
- 사이드 영역 자체의 스크롤은 내부 컨테이너의 `overflow-y-auto`로 처리하고, 외부 페이지 스크롤과 분리한다.
- 부모 체인에 `overflow: hidden`을 추가할 일이 생기면 sticky가 깨질 수 있으므로 주의한다. 추가가 필요하다면 sticky 적용 위치를 다시 점검한다.

## 최종 방향

사이드바는 "페이지와 함께 흐르는 컬럼"이 아니라 "뷰포트에 고정된 내비게이션 셸"로 다루는 것이 맞다. 외부 스크롤이 일어나도 항상 같은 위치에 보여야 하며, 사이드바 자체의 메뉴가 길어지는 경우만 내부 스크롤로 처리한다.

- `<aside>`: `sticky top-0 h-screen`으로 뷰포트 고정
- 내부 메뉴 컨테이너: `overflow-y-auto`로 자체 스크롤
- 외부 페이지 스크롤과 사이드바 스크롤은 서로 영향을 주지 않는다.
