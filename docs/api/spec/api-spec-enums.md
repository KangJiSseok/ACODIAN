# API Enum 레퍼런스

## 1. Enum 상세

### 1.1 `DepartmentStatus`

| 값 | 설명 |
|---|---|
| `ACTIVE` | 활성 부서 상태 |
| `INACTIVE` | 비활성 부서 상태 |

### 1.2 `TeamStatus`

| 값 | 설명 |
|---|---|
| `ACTIVE` | 운영 중인 활성 팀 상태 |
| `INACTIVE` | 운영을 중단한 비활성 팀 상태 |

### 1.3 `UserTeamStatus`

| 값 | 설명 |
|---|---|
| `ACTIVE` | 사용자가 현재 팀에 소속된 상태 |
| `LEFT` | 사용자가 팀 소속에서 제외된 상태 |

### 1.4 `EmploymentStatus`

| 값 | 설명 |
|---|---|
| `ACTIVE` | 현재 재직 중인 상태 |
| `LEAVE` | 휴직 상태 |
| `RETIRED` | 퇴사 상태 |

### 1.5 `UserRole`

| 값 | 설명 |
|---|---|
| `DIRECTOR` | 최상위 조직 관리자 권한 |
| `DEPT_HEAD` | 부서장 권한 |
| `TEAM_LEAD` | 팀 리더 권한 |
| `MEMBER` | 일반 구성원 권한 |

### 1.6 `WorklogImportance`

| 값 | 설명 |
|---|---|
| `URGENT` | 가장 높은 우선순위의 긴급 업무 |
| `HIGH` | 우선순위가 높은 업무 |
| `NORMAL` | 일반 우선순위 업무 |
| `LOW` | 우선순위가 낮은 업무 |

### 1.7 `WorklogStatus`

| 값 | 설명 |
|---|---|
| `PENDING` | 아직 시작하지 않은 대기 상태 |
| `IN_PROGRESS` | 현재 진행 중인 상태 |
| `COMPLETED` | 작업이 완료된 상태 |
| `ON_HOLD` | 일시적으로 보류된 상태 |
| `CANCELLED` | 작업이 취소된 상태 |

### 1.8 `AiProcessingStatus`

| 값 | 설명 |
|---|---|
| `PENDING` | AI 처리가 아직 시작되지 않은 상태 |
| `PROCESSING` | AI 처리가 진행 중인 상태 |
| `COMPLETED` | AI 처리가 정상 완료된 상태 |
| `FAILED` | AI 처리 중 오류가 발생한 상태 |

