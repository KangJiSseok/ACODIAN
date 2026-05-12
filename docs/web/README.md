# Web 협업 문서

`apps/web`는 실행 코드 영역이고, `docs/web`는 프론트엔드 협업 문서 영역이다.

프론트엔드 개발자 2명이 각자 브랜치를 나눠 작업할 때, 오래 유지해야 하는 기준만 이 폴더에 둔다.

## 폴더 구조

```text
docs/web/
├── README.md
├── collaboration-rules.md
└── ownership.md
```

## 사용 원칙

- 담당 화면/기능 소유권은 `ownership.md`에 정리한다.
- 일시적인 진행 상황, 작업 메모, 인수인계 내용은 저장소 문서보다 이슈/PR 설명으로 우선 공유한다.
- 이 폴더에는 오래 유지할 규칙과 담당 범위만 남긴다.
- 실행 코드와 직접 관련 없는 협업 메모는 `apps/web/src`에 두지 않는다.

## 빠른 기준

- 코드 공유: `apps/web/src/app/_common`
- 협업 문서 공유: `docs/web`
- 최우선 규칙 문서: [collaboration-rules.md](/C:/Users/SSAFY/S14P31S209/docs/web/collaboration-rules.md)
