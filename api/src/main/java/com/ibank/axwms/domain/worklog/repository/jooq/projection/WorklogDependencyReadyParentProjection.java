package com.ibank.axwms.domain.worklog.repository.jooq.projection;

import org.jooq.Field;
import org.jooq.Record;

/**
 * 커밋 후 알림 판정이 notification 모듈 조회 없이 필요한 수신자와 조직 범위를 그대로 사용하게 한다.
 */
public record WorklogDependencyReadyParentProjection(
        Long parentWorklogId,
        Long parentAuthorId,
        Long parentTeamId,
        Long parentDepartmentId,
        String teamName,
        String parentTitle
) {

    /**
     * alias 된 JOOQ 필드를 명시적으로 받아 부모/팀 alias 변경이 projection 계약을 깨지 않게 한다.
     */
    public static WorklogDependencyReadyParentProjection from(Record record,
                                                              Field<Long> parentWorklogId,
                                                              Field<Long> parentAuthorId,
                                                              Field<Long> parentTeamId,
                                                              Field<Long> parentDepartmentId,
                                                              Field<String> teamName,
                                                              Field<String> parentTitle) {
        return new WorklogDependencyReadyParentProjection(
                record.get(parentWorklogId),
                record.get(parentAuthorId),
                record.get(parentTeamId),
                record.get(parentDepartmentId),
                record.get(teamName),
                record.get(parentTitle)
        );
    }
}
