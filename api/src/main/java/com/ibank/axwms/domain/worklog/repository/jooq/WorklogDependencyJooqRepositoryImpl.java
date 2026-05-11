package com.ibank.axwms.domain.worklog.repository.jooq;

import com.ibank.axwms.domain.worklog.WorklogStatus;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.BlockedPredecessorRowProjection;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.WorklogDependencyProjection;
import lombok.RequiredArgsConstructor;
import org.jooq.CommonTableExpression;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.ibank.axwms.global.jooq.Tables.TB_TEAM;
import static com.ibank.axwms.global.jooq.Tables.TB_WORKLOG;
import static com.ibank.axwms.global.jooq.Tables.TB_WORKLOG_DEPENDENCY;

@Repository
@RequiredArgsConstructor
public class WorklogDependencyJooqRepositoryImpl implements WorklogDependencyJooqRepository {

    private final DSLContext dsl;

    @Override
    public List<WorklogDependencyProjection> findDirectDependencies(Long worklogId) {
        return dsl.select(
                        TB_WORKLOG.WORKLOG_ID,
                        TB_WORKLOG.TITLE,
                        TB_WORKLOG.STATUS_CODE
                )
                .from(TB_WORKLOG_DEPENDENCY)
                .join(TB_WORKLOG).on(TB_WORKLOG.WORKLOG_ID.eq(TB_WORKLOG_DEPENDENCY.DEPENDS_ON_WORKLOG_ID))
                .where(TB_WORKLOG_DEPENDENCY.WORKLOG_ID.eq(worklogId))
                .and(TB_WORKLOG.IS_DELETED.isFalse())
                .orderBy(TB_WORKLOG_DEPENDENCY.CREATED_AT.asc(), TB_WORKLOG_DEPENDENCY.DEPENDENCY_ID.asc())
                .fetch(WorklogDependencyProjection::from);
    }

    @Override
    public Map<Long, Long> countByWorklogIds(Collection<Long> worklogIds) {
        if (worklogIds == null || worklogIds.isEmpty()) {
            return Map.of();
        }
        return dsl.select(TB_WORKLOG_DEPENDENCY.WORKLOG_ID, DSL.count())
                .from(TB_WORKLOG_DEPENDENCY)
                .where(TB_WORKLOG_DEPENDENCY.WORKLOG_ID.in(worklogIds))
                .groupBy(TB_WORKLOG_DEPENDENCY.WORKLOG_ID)
                .fetchMap(TB_WORKLOG_DEPENDENCY.WORKLOG_ID, record -> record.get(DSL.count()).longValue());
    }

    @Override
    public Set<Long> findDependsOnWorklogIdsByWorklogId(Long worklogId) {
        return new HashSet<>(
                dsl.select(TB_WORKLOG_DEPENDENCY.DEPENDS_ON_WORKLOG_ID)
                        .from(TB_WORKLOG_DEPENDENCY)
                        .where(TB_WORKLOG_DEPENDENCY.WORKLOG_ID.eq(worklogId))
                        .fetch(TB_WORKLOG_DEPENDENCY.DEPENDS_ON_WORKLOG_ID)
        );
    }

    /**
     * 새 선행 후보들의 의존 그래프를 재귀적으로 따라가며 candidateWorklogId 에 도달하는 시작점을 찾는다.
     * candidateWorklogId 의 선행으로 X 를 추가했을 때 X → ... → candidateWorklogId 경로가 있으면 사이클이 발생하므로,
     * 그런 X 를 사전에 식별해 호출 측이 검증 실패로 처리한다.
     */
    @Override
    public Set<Long> findPredecessorsCausingCycle(Long candidateWorklogId, Collection<Long> newPredecessorIds) {
        if (newPredecessorIds == null || newPredecessorIds.isEmpty()) {
            return Set.of();
        }

        // CTE 내부에서 자기 자신을 다시 참조할 때 사용할 unqualified 컬럼/테이블 핸들.
        Field<Long> cteOrigin = DSL.field(DSL.name("origin_id"), Long.class);
        Field<Long> cteWorklog = DSL.field(DSL.name("worklog_id"), Long.class);
        Table<?> cteTable = DSL.table(DSL.name("reachable"));

        CommonTableExpression<?> reachable = DSL.name("reachable")
                .fields("origin_id", "worklog_id")
                .as(
                        // anchor: 새 선행 후보 자체를 origin 으로 두고, 그 후보가 의존하는 worklog 들을 첫 hop 으로 잡는다.
                        DSL.select(
                                        TB_WORKLOG_DEPENDENCY.WORKLOG_ID,
                                        TB_WORKLOG_DEPENDENCY.DEPENDS_ON_WORKLOG_ID)
                                .from(TB_WORKLOG_DEPENDENCY)
                                .where(TB_WORKLOG_DEPENDENCY.WORKLOG_ID.in(newPredecessorIds))
                                // recursive: 누적된 worklog 마다 한 단계 더 depends_on 을 따라간다.
                                // UNION (not UNION ALL) — 이미 본 (origin, worklog) 쌍은 dedup 해
                                // 데이터에 의도치 않은 사이클이 존재해도 무한 루프를 방지한다.
                                .union(
                                        DSL.select(cteOrigin, TB_WORKLOG_DEPENDENCY.DEPENDS_ON_WORKLOG_ID)
                                                .from(cteTable)
                                                .join(TB_WORKLOG_DEPENDENCY)
                                                .on(TB_WORKLOG_DEPENDENCY.WORKLOG_ID.eq(cteWorklog))
                                )
                );

        return new HashSet<>(
                dsl.withRecursive(reachable)
                        .selectDistinct(cteOrigin)
                        .from(reachable)
                        .where(cteWorklog.eq(candidateWorklogId))
                        .fetch(cteOrigin)
        );
    }

    @Override
    public List<BlockedPredecessorRowProjection> findIncompletePredecessorsByWorklogIds(Collection<Long> worklogIds) {
        if (worklogIds == null || worklogIds.isEmpty()) {
            return List.of();
        }
        var my = TB_WORKLOG.as("my");
        var pred = TB_WORKLOG.as("pred");
        // 선행 worklog 의 팀이 삭제된 경우 dashboard 정책상 제외 — pred 의 team JOIN 으로 가드.
        var predTeam = TB_TEAM.as("pred_team");

        return dsl.select(
                        my.WORKLOG_ID,
                        my.TITLE,
                        pred.WORKLOG_ID,
                        pred.TITLE,
                        pred.STATUS_CODE
                )
                .from(my)
                .join(TB_WORKLOG_DEPENDENCY).on(TB_WORKLOG_DEPENDENCY.WORKLOG_ID.eq(my.WORKLOG_ID))
                .join(pred).on(pred.WORKLOG_ID.eq(TB_WORKLOG_DEPENDENCY.DEPENDS_ON_WORKLOG_ID))
                .join(predTeam).on(predTeam.TEAM_ID.eq(pred.TEAM_ID)
                        .and(predTeam.DELETED_AT.isNull()))
                .where(my.WORKLOG_ID.in(worklogIds))
                .and(pred.IS_DELETED.isFalse())
                .and(pred.STATUS_CODE.ne(WorklogStatus.COMPLETED.name()))
                .orderBy(my.WORKLOG_ID.asc(), pred.WORKLOG_ID.asc())
                .fetch(record -> BlockedPredecessorRowProjection.from(record, my, pred));
    }
}
