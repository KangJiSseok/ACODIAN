package com.ibank.axwms.domain.worklog.repository.jooq;

import static com.ibank.axwms.global.jooq.Tables.TB_TEAM;
import static com.ibank.axwms.global.jooq.Tables.TB_USER;
import static com.ibank.axwms.global.jooq.Tables.TB_USER_TEAM;
import static com.ibank.axwms.global.jooq.Tables.TB_WORKLOG;
import static com.ibank.axwms.global.jooq.Tables.TB_WORKLOG_DEPENDENCY;

import com.ibank.axwms.domain.worklog.dto.GetWorklogsApiDto;
import com.ibank.axwms.domain.worklog.policy.WorklogVisibilityScope;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.WorklogListProjection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class WorklogJooqRepositoryImpl implements WorklogJooqRepository {

    private static final String ACTIVE_USER_TEAM_STATUS = "ACTIVE";

    private final DSLContext dsl;

    /** 가시 범위와 페이지 요청을 받아 업무 목록을 조회한다. */
    @Override
    public Page<WorklogListProjection> findWorklogPage(WorklogVisibilityScope scope, GetWorklogsApiDto.Request request) {
        int pageIndex = request.pageOrDefault() - 1;
        int pageSize = request.pageSizeOrDefault();
        PageRequest pageRequest = PageRequest.of(pageIndex, pageSize);

        Condition baseCondition = TB_WORKLOG.IS_DELETED.isFalse().and(toCondition(scope));

        long total = dsl.selectCount()
                .from(TB_WORKLOG)
                .where(baseCondition)
                .fetchSingle(0, Integer.class)
                .longValue();

        if (total == 0) {
            return new PageImpl<>(List.of(), pageRequest, 0);
        }

        List<WorklogListProjection> items = dsl.select(
                        TB_WORKLOG.WORKLOG_ID,
                        TB_WORKLOG.TITLE,
                        TB_WORKLOG.STATUS_CODE,
                        TB_WORKLOG.WORK_CONTENT,
                        TB_WORKLOG.ACTUAL_HOURS,
                        TB_WORKLOG.IMPORTANCE_CODE,
                        TB_WORKLOG.AI_SUMMARY,
                        TB_WORKLOG.AI_PROCESSING_STATUS,
                        TB_WORKLOG.AI_SUMMARY_EDITED,
                        TB_WORKLOG.TEAM_ID,
                        TB_TEAM.TEAM_NAME,
                        TB_WORKLOG.AUTHOR_ID,
                        TB_USER.USER_NAME,
                        TB_WORKLOG.INSTRUCTION_DATE,
                        TB_WORKLOG.DUE_DATE
                )
                .from(TB_WORKLOG)
                .join(TB_TEAM).on(TB_WORKLOG.TEAM_ID.eq(TB_TEAM.TEAM_ID))
                .join(TB_USER).on(TB_WORKLOG.AUTHOR_ID.eq(TB_USER.USER_ID))
                .where(baseCondition)
                .orderBy(TB_WORKLOG.CREATED_AT.desc(), TB_WORKLOG.WORKLOG_ID.desc())
                .limit(pageSize)
                .offset((long) pageIndex * pageSize)
                .fetch(record -> new WorklogListProjection(
                        record.get(TB_WORKLOG.WORKLOG_ID),
                        record.get(TB_WORKLOG.TITLE),
                        record.get(TB_WORKLOG.STATUS_CODE),
                        record.get(TB_WORKLOG.WORK_CONTENT),
                        record.get(TB_WORKLOG.ACTUAL_HOURS),
                        record.get(TB_WORKLOG.IMPORTANCE_CODE),
                        record.get(TB_WORKLOG.AI_SUMMARY),
                        record.get(TB_WORKLOG.AI_PROCESSING_STATUS),
                        record.get(TB_WORKLOG.AI_SUMMARY_EDITED),
                        record.get(TB_WORKLOG.TEAM_ID),
                        record.get(TB_TEAM.TEAM_NAME),
                        record.get(TB_WORKLOG.AUTHOR_ID),
                        record.get(TB_USER.USER_NAME),
                        record.get(TB_WORKLOG.INSTRUCTION_DATE),
                        record.get(TB_WORKLOG.DUE_DATE),
                        (Integer) record.get(0)
                ));

        return new PageImpl<>(items, pageRequest, total);
    }

    /** 가시 범위 값을 JOOQ Condition 으로 매핑한다. */
    private Condition toCondition(WorklogVisibilityScope scope) {
        return switch (scope) {
            case WorklogVisibilityScope.All ignored -> DSL.noCondition();
            case WorklogVisibilityScope.Department department -> TB_WORKLOG.TEAM_ID.in(
                    DSL.select(TB_TEAM.TEAM_ID)
                            .from(TB_TEAM)
                            .where(TB_TEAM.DEPARTMENT_ID.eq(department.departmentId()))
                            .and(TB_TEAM.DELETED_AT.isNull())
            );
            case WorklogVisibilityScope.MyTeams myTeams -> TB_WORKLOG.TEAM_ID.in(
                    DSL.select(TB_USER_TEAM.TEAM_ID)
                            .from(TB_USER_TEAM)
                            .where(TB_USER_TEAM.USER_ID.eq(myTeams.userId()))
                            .and(TB_USER_TEAM.STATUS_CODE.eq(ACTIVE_USER_TEAM_STATUS))
                            .and(TB_TEAM.DELETED_AT.isNull())
            );
        };
    }
}
