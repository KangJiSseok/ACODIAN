package com.ibank.axwms.domain.worklog.repository.jooq;

import com.ibank.axwms.domain.worklog.WorklogStatus;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.BlockedPredecessorRowProjection;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.WorklogDependencyReadyParentProjection;
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

        Table<?> reachableStep = DSL.table(DSL.name("reachable")).as("r");
        Field<Long> stepOrigin = DSL.field(DSL.name("r", "origin_id"), Long.class);
        Field<Long> stepWorklog = DSL.field(DSL.name("r", "worklog_id"), Long.class);
        Table<?> reachableResult = DSL.table(DSL.name("reachable")).as("result");
        Field<Long> resultOrigin = DSL.field(DSL.name("result", "origin_id"), Long.class);
        Field<Long> resultWorklog = DSL.field(DSL.name("result", "worklog_id"), Long.class);

        CommonTableExpression<?> reachable = DSL.name("reachable")
                .fields("origin_id", "worklog_id")
                .as(
                        DSL.select(
                                        TB_WORKLOG_DEPENDENCY.WORKLOG_ID,
                                        TB_WORKLOG_DEPENDENCY.DEPENDS_ON_WORKLOG_ID)
                                .from(TB_WORKLOG_DEPENDENCY)
                                .where(TB_WORKLOG_DEPENDENCY.WORKLOG_ID.in(newPredecessorIds))
                                .union(
                                        DSL.select(stepOrigin, TB_WORKLOG_DEPENDENCY.DEPENDS_ON_WORKLOG_ID)
                                                .from(reachableStep)
                                                .join(TB_WORKLOG_DEPENDENCY)
                                                .on(TB_WORKLOG_DEPENDENCY.WORKLOG_ID.eq(stepWorklog))
                                )
                );

        return new HashSet<>(
                dsl.withRecursive(reachable)
                        .selectDistinct(resultOrigin)
                        .from(reachableResult)
                        .where(resultWorklog.eq(candidateWorklogId))
                        .fetch(resultOrigin)
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

    /**
     * 완료된 선행 업무를 anchor 로 잡고 직접 부모의 다른 미삭제 선행이 모두 완료됐을 때만 후속 알림 후보로 노출한다.
     */
    @Override
    public List<WorklogDependencyReadyParentProjection> findReadyParentsByCompletedPredecessorId(Long completedWorklogId) {
        if (completedWorklogId == null) {
            return List.of();
        }

        var anchorDependency = TB_WORKLOG_DEPENDENCY.as("anchor_dependency");
        var parent = TB_WORKLOG.as("parent_worklog");
        var parentTeam = TB_TEAM.as("parent_team");
        var allDependency = TB_WORKLOG_DEPENDENCY.as("all_dependency");
        var predecessor = TB_WORKLOG.as("predecessor_worklog");

        return dsl.selectDistinct(
                        parent.WORKLOG_ID,
                        parent.AUTHOR_ID,
                        parent.TEAM_ID,
                        parentTeam.DEPARTMENT_ID,
                        parentTeam.TEAM_NAME,
                        parent.TITLE
                )
                .from(anchorDependency)
                .join(parent).on(parent.WORKLOG_ID.eq(anchorDependency.WORKLOG_ID))
                .join(parentTeam).on(parentTeam.TEAM_ID.eq(parent.TEAM_ID)
                        .and(parentTeam.DELETED_AT.isNull()))
                .where(anchorDependency.DEPENDS_ON_WORKLOG_ID.eq(completedWorklogId))
                .and(parent.IS_DELETED.isFalse())
                .and(DSL.notExists(
                        DSL.selectOne()
                                .from(allDependency)
                                .join(predecessor)
                                .on(predecessor.WORKLOG_ID.eq(allDependency.DEPENDS_ON_WORKLOG_ID))
                                .where(allDependency.WORKLOG_ID.eq(parent.WORKLOG_ID))
                                .and(predecessor.IS_DELETED.isFalse())
                                .and(predecessor.STATUS_CODE.ne(WorklogStatus.COMPLETED.name()))
                ))
                .orderBy(parent.WORKLOG_ID.asc())
                .fetch(record -> WorklogDependencyReadyParentProjection.from(
                        record,
                        parent.WORKLOG_ID,
                        parent.AUTHOR_ID,
                        parent.TEAM_ID,
                        parentTeam.DEPARTMENT_ID,
                        parentTeam.TEAM_NAME,
                        parent.TITLE
                ));
    }
}
