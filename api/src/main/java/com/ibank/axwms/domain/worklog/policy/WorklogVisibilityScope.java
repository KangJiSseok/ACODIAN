package com.ibank.axwms.domain.worklog.policy;

/**
 * 업무 목록 조회에서 현재 사용자가 볼 수 있는 범위를 표현하는 값 타입.
 * 역할 매핑(WorklogVisibilityPolicy) 과 SQL 변환(WorklogJooqRepository) 이
 * 같은 어휘를 공유할 수 있도록 sealed type 으로 한정한다.
 */
public sealed interface WorklogVisibilityScope {

    /** 전체 업무를 조회한다. DIRECTOR 에게만 부여한다. */
    record All() implements WorklogVisibilityScope {
    }

    /** 지정한 부서 산하 ACTIVE 팀의 업무만 조회한다. DEPT_HEAD 에게 부여한다. */
    record Department(Long departmentId) implements WorklogVisibilityScope {
    }

    /** 지정한 사용자가 ACTIVE 멤버인 팀들의 업무만 조회한다. TEAM_LEAD 에게 부여한다. */
    record MyTeams(Long userId) implements WorklogVisibilityScope {
    }

}
