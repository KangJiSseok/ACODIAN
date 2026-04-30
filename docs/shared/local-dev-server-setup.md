# 로컬 개발 서버 실행 가이드

이 문서는 AX-WMS를 로컬에서 실행할 때 필요한 DB, Redis, API, Web 서버 실행 순서를 정리한다.

## 전체 구조

로컬 개발 환경에서는 DB와 Redis만 Docker로 실행하고, API와 Web은 로컬 프로세스로 실행한다.

| 구성 요소 | 실행 방식 | 기본 주소 |
|---|---|---|
| PostgreSQL | Docker Compose | `localhost:5434` |
| Redis | Docker Compose | `localhost:6379` |
| API | `./gradlew bootRun` | `http://localhost:8100/api` |
| Web | `pnpm dev` | `http://localhost:3000` |

S3는 로컬에서 따로 켜는 서비스가 아니다. API 서버가 AWS S3에 직접 접근하므로 S3 관련 환경변수를 API 실행 전에 주입해야 한다.

## 1. DB와 Redis 실행

루트 디렉터리에서 실행한다.

```powershell
docker compose -f infra\compose.local.yml up -d
```

상태 확인:

```powershell
docker ps
```

`axwms-postgres`, `axwms-redis`가 `healthy` 상태면 정상이다.

## 2. API 서버 실행

API는 `api` 디렉터리에서 `bootRun`으로 실행한다. 회원가입 프로필 이미지 업로드 기능 때문에 S3 환경변수가 필요하다.

```powershell
$env:AWS_S3_BUCKET="ibank-s209team-s3"
$env:AWS_REGION="ap-northeast-2"
$env:AWS_S3_BASE_PREFIX="local/본인이름"
$env:AWS_S3_PUBLIC_BASE_URL="https://d1sif143wcm5pd.cloudfront.net"
$env:AWS_ACCESS_KEY_ID="<AWS_ACCESS_KEY_ID>"
$env:AWS_SECRET_ACCESS_KEY="<AWS_SECRET_ACCESS_KEY>"

cd api
.\gradlew.bat bootRun
```

Git Bash나 WSL에서는 다음처럼 실행한다.

```bash
export AWS_S3_BUCKET="ibank-s209team-s3"
export AWS_REGION="ap-northeast-2"
export AWS_S3_BASE_PREFIX="local/본인이름"
export AWS_S3_PUBLIC_BASE_URL="https://d1sif143wcm5pd.cloudfront.net"
export AWS_ACCESS_KEY_ID="<AWS_ACCESS_KEY_ID>"
export AWS_SECRET_ACCESS_KEY="<AWS_SECRET_ACCESS_KEY>"

cd api
./gradlew bootRun
```

정상 실행 확인:

```powershell
Invoke-WebRequest -UseBasicParsing http://localhost:8100/api/api-docs
```

Swagger UI:

```text
http://localhost:8100/api/swagger-ui.html
```

## 3. Web 서버 실행

루트 디렉터리에서 실행한다.

```powershell
pnpm dev:web
```

또는 `web` 디렉터리에서 실행한다.

```powershell
cd web
pnpm dev
```

브라우저에서 다음 주소로 접속한다.

```text
http://localhost:3000
```

## 4. S3 설정 의미

API의 S3 설정은 다음 환경변수로 주입한다.

| 환경변수 | 의미 |
|---|---|
| `AWS_S3_BUCKET` | 파일을 저장할 S3 버킷 이름 |
| `AWS_REGION` | S3 버킷 리전 |
| `AWS_S3_BASE_PREFIX` | 버킷 내부에서 로컬 개발자별 파일을 구분할 prefix |
| `AWS_S3_PUBLIC_BASE_URL` | 저장된 파일을 조회할 때 사용할 공개 base URL, 보통 CloudFront 주소 |
| `AWS_ACCESS_KEY_ID` | AWS API 호출용 access key |
| `AWS_SECRET_ACCESS_KEY` | AWS API 호출용 secret key |

예를 들어 `AWS_S3_BASE_PREFIX`를 `local/정인호`로 두면, 로컬 테스트 업로드 파일은 버킷 내부에서 해당 prefix 아래에 저장된다.

## 5. 주의사항

- AWS access key와 secret key는 코드, 문서, 커밋, MR 본문에 넣지 않는다.
- 채팅이나 문서에 키가 노출됐다면 AWS에서 해당 키를 폐기하고 새 키를 발급한다.
- 이미지 없이 회원가입을 테스트할 때도 API 서버의 S3 설정값은 필요할 수 있다.
- 실제 이미지를 첨부해 회원가입하면 API가 S3 업로드를 수행하므로 유효한 AWS 키가 필요하다.
- `bootRun` 첫 실행 시 jOOQ codegen이 먼저 돌 수 있어 기동까지 시간이 걸릴 수 있다.
- Next dev 서버 실행 중 `web/next-env.d.ts`가 갱신될 수 있다.

## 6. 포트 확인

Windows PowerShell:

```powershell
netstat -ano | Select-String ":3000|:8100|:5434|:6379"
```

정상이라면 다음 포트가 `LISTENING` 상태로 보인다.

- `3000`: Web
- `8100`: API
- `5434`: PostgreSQL
- `6379`: Redis

## 7. 종료 방법

API와 Web은 실행 중인 터미널에서 `Ctrl+C`로 종료한다.

DB와 Redis는 루트 디렉터리에서 다음 명령으로 종료한다.

```powershell
docker compose -f infra\compose.local.yml down
```

데이터 볼륨까지 삭제하려면 아래 명령을 사용한다. 로컬 DB 데이터가 사라지므로 주의한다.

```powershell
docker compose -f infra\compose.local.yml down -v
```
