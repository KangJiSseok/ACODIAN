# 회원가입 화면 연동과 로컬 실행 확인

## 배경

로그인 화면에서 회원가입 흐름을 추가하기 전에, 최신 백엔드 브랜치에 회원가입 API가 실제로 존재하는지 다시 확인했다.

처음에는 현재 작업 브랜치 기준으로 `/auth/signup`이 보이지 않아 `/users/signup` 문서 기준으로 생각했지만, `origin/dev`를 최신으로 가져오니 백엔드에 `/auth/signup`이 반영되어 있었다.

## 확인한 API 계약

현재 `origin/dev` 기준 회원가입 API는 다음 형태다.

```text
POST /api/auth/signup
Content-Type: multipart/form-data
```

요청 파트는 두 개다.

```text
request       JSON part, 필수
profile_image File part, 선택
```

`request` JSON에는 다음 값이 들어간다.

```json
{
  "department_id": 1,
  "user_name": "홍길동",
  "email": "user@example.com",
  "password": "password1!",
  "position_name": "대리",
  "title_name": "팀원",
  "join_date": "2026-04-30",
  "phone": "010-0000-0000",
  "employment_status": "ACTIVE"
}
```

중요한 점은 회원가입 API가 일반 JSON 요청이 아니라 `multipart/form-data` 요청이라는 것이다. 프로필 이미지를 선택하지 않더라도 `request`는 JSON part로 보내야 한다.

## 프론트 작업 방향

로그인 페이지에는 두 가지 패널을 둔다.

- 기본 상태: 로그인 패널 표시
- 회원가입 클릭: 로그인 패널은 아래로 내려가고 회원가입 패널이 올라옴
- 회원가입 성공: 로그인 패널로 돌아오고 가입한 이메일을 로그인 이메일 입력값에 채움

회원가입 입력값은 다음 기준으로 구성한다.

- 부서
- 이름
- 이메일
- 비밀번호
- 직급
- 직책
- 입사일
- 연락처
- 프로필 이미지 파일

프론트 서비스 함수는 `/auth/signup`으로 요청해야 하며, `FormData`에 `request` Blob과 선택 `profile_image` 파일을 담는다.

## 로컬 bootRun 실패 원인

`./gradlew build`와 CI의 `./gradlew --no-daemon clean test bootJar`는 통과했다. 실패는 빌드가 아니라 `./gradlew bootRun` 실행 단계에서 발생했다.

첫 번째 원인은 S3 환경변수 누락이다.

```text
Configured region (${AWS_REGION}) ... invalid URI
```

프로필 이미지 업로드 기능이 들어오면서 `S3Client` 빈을 만들 때 다음 환경변수들이 필요해졌다.

```text
AWS_S3_BUCKET
AWS_REGION
AWS_S3_BASE_PREFIX
AWS_S3_PUBLIC_BASE_URL
AWS_ACCESS_KEY_ID
AWS_SECRET_ACCESS_KEY
```

단, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` 같은 민감정보는 절대 `application.yml`이나 문서에 실제 값으로 남기면 안 된다. 로컬에서는 쉘 환경변수나 Git에 올라가지 않는 개인 환경 파일로 관리한다.

두 번째 원인은 local profile의 seed 데이터와 기존 로컬 DB 데이터 충돌이다.

```text
duplicate key value violates unique constraint "tb_department_department_head_user_id_key"
```

S3 더미 환경변수로 첫 번째 문제를 넘기면, `LocalSeedRunner`가 기존 DB에 있는 부서장 데이터와 충돌하면서 위 오류가 발생했다.

## 당장 로컬에서 띄우는 방법

기존 DB를 지우지 않고 서버만 띄우려면 `local` profile의 seed runner를 피해서 실행할 수 있다.

```bash
SPRING_PROFILES_ACTIVE=dev \
AUTH_CORS_ALLOWED_ORIGIN_PATTERNS='http://localhost:*' \
AUTH_REFRESH_COOKIE_SECURE=false \
AUTH_REFRESH_COOKIE_SAMESITE=Lax \
AWS_S3_BUCKET=로컬용_버킷 \
AWS_REGION=ap-northeast-2 \
AWS_S3_BASE_PREFIX=로컬용_prefix \
AWS_S3_PUBLIC_BASE_URL=로컬용_public_base_url \
AWS_ACCESS_KEY_ID=로컬_환경변수로만_관리 \
AWS_SECRET_ACCESS_KEY=로컬_환경변수로만_관리 \
./gradlew bootRun
```

이 방식으로 백엔드가 `http://localhost:8100/api`에서 정상 기동되는 것을 확인했다.

## 기억할 점

- `/auth/signup`은 최신 `origin/dev`에 존재한다.
- 회원가입 요청은 `multipart/form-data`다.
- 프로필 이미지는 URL 문자열이 아니라 선택 파일로 보내야 한다.
- S3 키는 코드/문서/커밋에 넣지 않는다.
- `bootRun` 실패는 컴파일 문제가 아니라 실행 환경 변수와 로컬 seed 데이터 충돌 문제였다.
