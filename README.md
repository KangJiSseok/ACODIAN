<div align="center">

<img src="exec/image/00-login.png" width="100%" alt="ACODIAN"/>

# ACODIAN (AX-WMS)

**본부·사업부·팀 단위 조직과 업무일지를 관리하고, AI가 요약·태그·시맨틱 검색까지 이어서 처리하는 업무 관리 플랫폼**

업무일지를 저장하는 순간 사용자는 대기 없이 다음 화면으로 이동하고, 요약·태그·청킹·임베딩·색인은 백그라운드에서 비동기로 처리됩니다.

[![Next.js](https://img.shields.io/badge/Next.js-16.2-000000?logo=next.js&logoColor=white)](https://nextjs.org)
[![React](https://img.shields.io/badge/React-19.2-61DAFB?logo=react&logoColor=black)](https://react.dev)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.9-3178C6?logo=typescript&logoColor=white)](https://www.typescriptlang.org)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.5-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org)
[![FastAPI](https://img.shields.io/badge/FastAPI-AI%20Service-009688?logo=fastapi&logoColor=white)](https://fastapi.tiangolo.com)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-pgvector-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)](https://www.docker.com)

</div>

---

## 목차

1. [서비스 소개](#서비스-소개)
2. [핵심 기능](#핵심-기능)
3. [시스템 아키텍처](#시스템-아키텍처)
4. [업무일지 처리 흐름](#업무일지-처리-흐름)
5. [기술 스택](#기술-스택)
6. [로컬 실행 및 DB 구성 검증](#로컬-실행-및-db-구성-검증)
7. [프로젝트 구조](#프로젝트-구조)

---

## 서비스 소개

**AX-WMS(ACODIAN)** 는 본부 → 사업부 → 팀으로 이어지는 조직 구조와 업무일지를 하나의 시스템에서 관리하고, AI가 요약·태그·시맨틱 검색을 담당하는 조직·업무 관리 플랫폼입니다.

업무 원본과 조직/권한 규칙은 Spring Boot API가 기준 시스템으로 소유하고, 업무일지 등록 이후의 요약·태그 생성·텍스트 청킹·임베딩·시맨틱 검색은 FastAPI 기반 AI 서비스가 Celery 워커로 비동기 처리합니다. 사용자는 저장 버튼을 누른 즉시 다음 화면으로 이동하며, AI 처리 결과는 완료되는 대로 알림과 상세 화면에 반영됩니다. 키워드가 정확히 기억나지 않을 때는 자연어로 질문하는 **AI 모드(시맨틱 검색)** 로 과거 업무일지를 다시 찾아 재활용할 수 있습니다.

> 개발 기간: 2026.04.11 ~ 2026.05.25 (커밋 1,196개)

<p align="center">
  <img src="exec/image/09-dashboard-director.png" width="90%" alt="관리자 대시보드"/>
</p>

---

## 핵심 기능

### 1. 조직·업무일지 관리

부서·팀·사용자 구조를 등록하고, 업무일지를 상태·중요도·선행 업무 의존 관계와 함께 관리합니다. 팀 대표자는 별도 컬럼이 아니라 팀 멤버십의 `isLeader` 플래그로 표현되어, 한 사용자가 여러 팀에 속하면서 특정 팀의 리더를 겸할 수 있습니다.

<table>
<tr>
<td><img src="exec/image/10-department-list.png" width="100%"/></td>
<td><img src="exec/image/11-department-detail.png" width="100%"/></td>
</tr>
<tr>
<td><img src="exec/image/12-team-list.png" width="100%"/></td>
<td><img src="exec/image/13-team-detail-acodian.png" width="100%"/></td>
</tr>
</table>

### 2. 업무일지 등록과 AI 비동기 후처리

업무일지를 저장하면 사용자는 기다리지 않고 바로 상세 화면으로 이동합니다. 이후 AI 서비스가 업무 내용을 요약하고, 관련 태그를 추천하며, 시맨틱 검색을 위한 청킹·임베딩까지 백그라운드에서 처리합니다. 처리 결과(성공/실패)는 알림으로 도착하고, 실패 시 상세 화면에서 재시도할 수 있습니다.

<table>
<tr>
<td><img src="exec/image/04-worklog-create-form.png" width="100%"/></td>
<td><img src="exec/image/05-worklog-create-result.png" width="100%"/></td>
</tr>
</table>

<p align="center"><img src="exec/image/03-worklog-detail-ai.png" width="90%" alt="AI 요약이 반영된 업무일지 상세"/></p>

### 3. AI 모드(시맨틱 검색 · GraphRAG)

일반 키워드 검색은 제목에 정확한 단어가 포함된 업무만 찾지만, **AI 모드**는 자연어 질문을 **GraphRAG**로 넘겨 의미가 가까운 과거 업무일지를 찾아 답변으로 정리해 줍니다. 예를 들어 `"pgVector로 하려 했으나, Qdrant로 바꾼 이유가 생각이 안나 찾아줘."` 라는 문장으로도 관련 업무일지를 검색할 수 있습니다. 답변에는 근거가 된 업무일지로 바로 이동하는 링크가 함께 포함됩니다.

<table>
<tr>
<td><img src="exec/image/02-worklog-list.png" width="100%"/><p align="center"><sub>업무일지 조회 — AI 모드 진입 전</sub></p></td>
<td><img src="exec/image/07-worklog-ai-search-empty.png" width="100%"/><p align="center"><sub>AI 모드 진입 직후 (질문 입력 대기)</sub></p></td>
</tr>
<tr>
<td><img src="exec/image/08a-worklog-ai-search-loading.png" width="100%"/><p align="center"><sub>질문 전송 · GraphRAG 응답 생성 중</sub></p></td>
<td><img src="exec/image/08-worklog-ai-search-result.png" width="100%"/><p align="center"><sub>GraphRAG 응답 결과 (근거 업무일지 링크 포함)</sub></p></td>
</tr>
</table>


### 4. 관리자 대시보드 · 알림 · 태그 관리

일반 팀원은 본인 업무 중심의 대시보드를, 본부장/사업부장은 전체 부서 진행률·부하 편중 지수·AI 파이프라인 성공률을 비교하는 대시보드를 확인합니다. 업무 마감 임박·지연·AI 처리 실패는 실시간 알림(Redis Stream + SSE)으로 전달되며, AI가 생성한 태그는 관리자가 중복 태그 병합 화면에서 정리할 수 있습니다.

<table>
<tr>
<td><img src="exec/image/01-dashboard-member.png" width="100%"/></td>
<td><img src="exec/image/06-notification.png" width="100%"/></td>
</tr>
<tr>
<td><img src="exec/image/14-user-list.png" width="100%"/></td>
<td><img src="exec/image/16-tag-list.png" width="100%"/></td>
</tr>
</table>

---

## 시스템 아키텍처

Next.js Web이 사용자 접점, Spring Boot API가 조직/업무/알림의 기준 시스템, FastAPI AI 서비스가 요약·태그·청킹·임베딩·시맨틱 검색을 전담하는 구조입니다. 배포 환경에서는 Nginx가 단일 게이트웨이로 세 서비스를 라우팅합니다.

```mermaid
flowchart LR
    subgraph Client
        FE["Web<br/>Next.js 16 + React 19"]
    end

    subgraph Gateway
        NG["Nginx<br/>단일 진입 게이트웨이"]
    end

    subgraph Core["Core API"]
        BE["API<br/>Spring Boot 4 / Java 21"]
    end

    subgraph AI["AI Service (FastAPI)"]
        SUM["요약 · 태그 Chain<br/>LangChain + Gemini"]
        TASK["Celery Worker<br/>청킹 · 임베딩 · 재시도"]
        GraphRAG["GraphRAG v3<br/>시맨틱 검색"]
    end

    subgraph Data
        DB[("PostgreSQL 17<br/>+ pgvector")]
        REDIS[("Redis<br/>Celery Broker / SSE Stream")]
        QDRANT[("Qdrant<br/>GraphRAG Vector Store")]
        NEO4J[("Neo4j<br/>GraphRAG Graph Store")]
    end

    subgraph Storage
        S3[("Object Storage(S3)<br/>첨부파일 원본")]
    end

    FE -->|REST API| NG --> BE
    NG --> FE
    BE --> DB
    BE --> REDIS
    BE --> S3
    BE -->|업무일지 등록 이벤트| AI
    SUM --> DB
    TASK --> DB
    TASK --> QDRANT
    LIGHT --> QDRANT
    LIGHT --> NEO4J
    AI -->|요약/태그 결과 callback| BE
    BE -->|알림 스트림| REDIS -->|SSE| FE
```

---

## 업무일지 처리 흐름

**① 등록 (동기)**

```
Web 저장 요청 → API가 업무일지 원본을 PostgreSQL에 반영 → 사용자는 즉시 상세 화면으로 이동
```

**② AI 후처리 (비동기)**

```
API → AI 파이프라인 트리거 → Celery Worker가 요약(LangChain+Gemini) · 태그 추천 · 텍스트 청킹 · 임베딩 생성 실행
   → 처리 결과를 API 내부 콜백으로 반영 → 완료/실패를 사용자에게 알림(Redis Stream → SSE)
```

**③ 시맨틱 검색**

```
자연어 질문 입력 → AI 서비스가 질의 임베딩 생성 → pgvector/Qdrant 유사도 검색 + 메타데이터 필터(작성자/팀/부서/상태/태그)
   → 관련 업무일지로 역추적 가능한 결과 반환
```

이 구조에서 업무 원본의 기준은 항상 `api`(PostgreSQL)이며, `ai`가 생성하는 요약·태그·임베딩은 검색 성능을 위한 보조 계층으로 취급됩니다. 자세한 서비스 경계와 데이터 소유권 정책은 [`docs/AX-WMS_기획서_아키텍처가이드.md`](docs/AX-WMS_기획서_아키텍처가이드.md)에 정리되어 있습니다.

---

## 기술 스택

<table>
<tr>
<th width="18%">영역</th>
<th>스택</th>
</tr>
<tr>
<td><b>Web</b></td>
<td>
Next.js 16.2 · React 19.2 · TypeScript · Tailwind CSS 4 · TanStack Query · Zustand · Axios · Playwright(E2E)
</td>
</tr>
<tr>
<td><b>API</b></td>
<td>
Java 21 · Spring Boot 4.0.5 · Spring Data JPA · jOOQ · Spring Security · Flyway · QueryDSL 없이 jOOQ 조합 · AWS S3 SDK · Springdoc(Swagger) · Redis Stream 기반 알림(SSE)
</td>
</tr>
<tr>
<td><b>AI Service</b></td>
<td>
Python 3.12 · FastAPI · Uvicorn · Celery(+Redis broker) · LangChain / LangGraph · Google Gemini(google-genai) · GraphRAG(시맨틱 검색) · python-docx/pptx/pyhwp(문서 파싱)
</td>
</tr>
<tr>
<td><b>Data</b></td>
<td>
PostgreSQL 17 + <code>pgvector</code>(기준 데이터 · 임베딩) · Redis 7(Celery 브로커 · 알림 스트림) · Qdrant 1.17(GraphRAG 벡터 저장) · Neo4j 5.26(GraphRAG 그래프 저장)
</td>
</tr>
<tr>
<td><b>Infra / DevOps</b></td>
<td>
Docker Compose · GitLab CI/CD(영역별 변경 감지 조건부 빌드: web/api/ai/infra) · GHCR 이미지 배포 · EC2 + systemd Nginx 게이트웨이
</td>
</tr>
</table>

---