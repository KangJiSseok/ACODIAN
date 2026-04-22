# OCI VM Spring API 1회성 배포 스모크 테스트 runbook

GitLab CI/CD로 정식화하기 전에, OCI VM에서 `git clone → build → docker compose up`
흐름으로 Spring Boot API가 기동하는지 한 번 확인하기 위한 절차다.
산출물은 `api/Dockerfile`, `infra/compose.deploy.yml`, `infra/.env.example`이며
본 runbook은 이들을 VM에서 어떻게 연결해 돌리는지를 정리한다.

## 대상 환경
- Oracle Cloud Infrastructure (OCI) Compute Instance
- Ubuntu 22.04 LTS 기준
- 최소 스펙: 1 OCPU / 6 GB RAM / 50 GB 스토리지
- 아키텍처: x86_64 또는 aarch64(OCI Ampere) 모두 지원 (멀티아키 이미지 사용)

## 1. OCI 방화벽 열기
VM 내부 방화벽과 OCI 콘솔 측 네트워크 정책은 별개이므로 **둘 다** 열어야 외부에서 8080에 접근할 수 있다.

- OCI 콘솔 → Networking → Virtual Cloud Networks → 대상 VCN → Security List(또는 NSG)
  → Ingress Rule 추가: Source `0.0.0.0/0`(또는 본인 IP/32), Protocol `TCP`, Destination Port `8080`
- VM 내부 (ufw 활성 시):
  ```bash
  sudo ufw allow 8080/tcp
  ```

## 2. 필수 패키지 설치
```bash
sudo apt update
sudo apt install -y git openjdk-21-jdk curl

# Docker Engine + Compose plugin
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker "$USER"
```
`docker` 그룹 반영을 위해 **로그아웃 후 재로그인**한다.
jOOQ codegen이 빌드 중 `docker run`을 직접 호출하므로 sudo 없이 docker가 동작해야 한다.

확인:
```bash
java -version       # openjdk 21.x
docker version      # Client/Server 둘 다 나와야 함
docker compose version
```

## 3. 리포지토리 clone & 빌드
```bash
git clone <repo-url> S14P31S209
cd S14P31S209/api
./gradlew clean bootJar
```
- `clean`을 붙이는 이유: `build/libs/`에 이전 빌드의 plain jar 등이 남아 있으면 `Dockerfile`의 `COPY build/libs/*.jar`가 실패할 수 있다.
- 최초 실행은 pgvector 이미지 pull + Flyway migrate + jOOQ 생성 + 컴파일로 수 분 소요된다.

## 4. 환경변수 파일 준비
```bash
cd ../infra
cp .env.example .env
# 필요 시 vim .env 로 JWT_SECRET 값을 원하는 문자열로 교체
```
`.env`는 최상위 `.gitignore`의 `.env` 패턴으로 이미 무시된다 (`.env.example`만 커밋됨).

## 5. 컨테이너 기동
```bash
docker compose -f compose.deploy.yml --env-file .env up -d --build
```

## 6. 검증 (Acceptance Criteria)
```bash
# 6-1. 3개 서비스가 모두 running/healthy
docker compose -f compose.deploy.yml ps

# 6-2. Flyway migration 성공 로그
docker compose -f compose.deploy.yml logs api | grep "Successfully applied"

# 6-3. Swagger 엔드포인트 200 OK
curl -I http://localhost:8080/swagger-ui/index.html
curl -I http://localhost:8080/v3/api-docs
```
브라우저에서 `http://<OCI_PUBLIC_IP>:8080/swagger-ui.html` 접속 시,
인증 프롬프트 없이 Swagger UI가 렌더링되고 API 그룹 목록이 보이면 스모크 테스트 통과다
(`global/config/SecurityConfig`가 해당 경로를 `permitAll`로 허용).

## 7. 정리
```bash
# 컨테이너만 종료 (postgres 데이터 named volume 유지)
docker compose -f compose.deploy.yml down

# 데이터 볼륨까지 완전 삭제
docker compose -f compose.deploy.yml down -v
```

## 알려진 Caveat
1. **Security 화이트리스트**: `SecurityConfig.PUBLIC_PATHS` 외 모든 엔드포인트는 인증 필요 (`.authenticated()`). 스모크 테스트 범위에선 Swagger 경로만 확인하고, 실제 비즈니스 API는 이후 인증 구현이 붙은 뒤에 검증한다.
2. **Flyway baseline**: `application.yml`에 `baseline-on-migrate: true`가 있어 빈 DB 최초 기동에서 자동 baseline이 생성된다.
3. **JPA `ddl-auto: validate`**: 마이그레이션 스키마와 엔티티 매핑이 어긋나면 api 컨테이너가 기동 실패한다. 재시작 루프가 관측되면 `docker compose logs api`에서 Hibernate schema validation 에러를 확인한다.
4. **jOOQ codegen 캐시**: 최초 `./gradlew bootJar`만 느리고, 이후 migration/`build.gradle`/`jooq-codegen.gradle` 변경이 없으면 fingerprint 캐시로 skip된다.
5. **OCI 방화벽 이중 구조**: VM 내부 ufw만 열어서는 외부 접근 불가. OCI 콘솔 Security List/NSG도 반드시 함께 열어야 한다.
6. **메모리**: 1 OCPU / 6 GB 환경에서 빌드(피크 ≈ 1.5 GB) + 런타임(피크 ≈ 2 GB)이 동시에 돌면 OOM 위험이 있다. 권장 순서는 "빌드 완료 → compose up"으로 시차를 둔다.

## 문제 해결 힌트
| 증상 | 의심 원인 | 확인/대응 |
|---|---|---|
| `./gradlew bootJar` 중 `docker: command not found` | Docker 미설치 또는 `docker` 그룹 미적용 | 재로그인, `docker ps` 동작 확인 |
| `bootJar` 중 pgvector 컨테이너 생성 실패 | Docker 권한 / 네트워크 / 디스크 부족 | `docker ps -a`, `df -h`, `docker pull pgvector/pgvector:pg17` 수동 실행 |
| api 컨테이너 재시작 루프 | Flyway/JPA 실패 또는 DB 연결 실패 | `docker compose logs api` 에서 스택 확인, postgres health 상태 확인 |
| Swagger 접속 `Connection refused` | 8080 미노출 또는 컨테이너 비정상 | `docker compose ps`에서 포트 매핑 확인, OCI Security List 확인 |
| Swagger 접속 `401` | `permitAll` 매칭 실패(경로 변경?) | `SecurityConfig.PUBLIC_PATHS`와 실제 URL 비교 |

## 이 뒤에 할 일
- GitLab Runner 기반 `.gitlab-ci.yml` 작성 (clone → `./gradlew bootJar` → `docker compose -f compose.deploy.yml up -d --build`)
- `.env` 값을 GitLab CI/CD Variable로 이관
- nginx 리버스 프록시 + HTTPS
- `web`, `ai` 서비스까지 포함하는 풀 스택 compose 분리 설계
