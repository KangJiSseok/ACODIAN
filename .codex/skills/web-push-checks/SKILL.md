---
name: web-push-checks
description: AX-WMS web 변경사항을 push하기 전에 프론트엔드 lint를 확인하도록 안내합니다. Codex가 web 영역 변경을 커밋하거나 push하려고 할 때, 또는 사용자가 "푸시해줘", "push 해줘", "MR 올려줘"처럼 원격 반영을 요청할 때 사용합니다.
---

# Web Push Checks

## Push 전 확인

`web` 영역 변경을 원격에 push하기 전에는 항상 프론트엔드 lint를 먼저 실행한다.

```bash
pnpm lint:web
```

## 판정 기준

- lint가 통과하면 push를 진행한다.
- lint가 실패하면 push 전에 실패 파일과 원인을 사용자에게 보고한다.
- 실패가 이번 변경과 무관한 기존 이슈로 보이면, 기존 이슈임을 명확히 말하고 사용자의 push 의사를 확인한다.
- `web`을 건드리지 않은 변경이라면 이 skill은 적용하지 않는다.

## 보고 방식

push 전 검증 결과를 최종 응답에 짧게 포함한다.

예:

```text
push 전 `pnpm lint:web`를 실행했고 통과했습니다.
```

또는:

```text
push 전 `pnpm lint:web`에서 기존 lint 에러가 확인되어 push는 보류했습니다.
```
