package com.ibank.axwms.domain.worklog.repository.jooq;

import com.ibank.axwms.domain.worklog.policy.WorklogVisibilityScope;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.WorklogDetailProjection;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.WorklogListProjection;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.WorklogSearchProjection;
import com.ibank.axwms.domain.worklog.repository.jooq.query.WorklogPageQuery;
import com.ibank.axwms.domain.worklog.repository.jooq.query.WorklogSearchQuery;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Select;
import org.jooq.impl.DSL;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static com.ibank.axwms.global.jooq.Tables.TB_TEAM;
import static com.ibank.axwms.global.jooq.Tables.TB_TEAM_ADMIN;
import static com.ibank.axwms.global.jooq.Tables.TB_USER;
import static com.ibank.axwms.global.jooq.Tables.TB_USER_TEAM;
import static com.ibank.axwms.global.jooq.Tables.TB_WORKLOG;
import static com.ibank.axwms.global.jooq.Tables.TB_WORKLOG_DEPENDENCY;
import static com.ibank.axwms.global.jooq.Tables.TB_WORKLOG_TAG;

@Repository
@RequiredArgsConstructor
public class WorklogJooqRepositoryImpl implements WorklogJooqRepository {

    private static final String ACTIVE_USER_TEAM_STATUS = "ACTIVE";

    private final DSLContext dsl;

    /**
     * 사용자가 접근 가능한 팀의 업무 목록을 페이지로 조회한다.
     */
    @Override
    public Page<WorklogListProjection> findWorklogPage(Long userId, WorklogPageQuery query) {
        int pageIndex = query.pageIndex();
        int pageSize = query.pageSize();
        PageRequest pageRequest = PageRequest.of(pageIndex, pageSize);

        Condition condition = TB_WORKLOG.IS_DELETED.isFalse().and(visibleTeamCondition(userId));
        long total = dsl.selectCount()
                .from(TB_WORKLOG)
                .join(TB_TEAM).on(TB_WORKLOG.TEAM_ID.eq(TB_TEAM.TEAM_ID))
                .where(condition)
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
                .where(condition)
                .orderBy(TB_WORKLOG.CREATED_AT.desc(), TB_WORKLOG.WORKLOG_ID.desc())
                .limit(pageSize)
                .offset((long) pageIndex * pageSize)
                .fetch(WorklogListProjection::from);

        return new PageImpl<>(items, pageRequest, total);
    }

    /**
     * 사용자가 접근 가능한 팀의 업무 한 건 본문을 조회한다. 권한 밖이거나 삭제된 행이면 empty.
     */
    @Override
    public Optional<WorklogDetailProjection> findWorklogDetail(Long userId, Long worklogId) {
        Condition condition = TB_WORKLOG.WORKLOG_ID.eq(worklogId)
                .and(TB_WORKLOG.IS_DELETED.isFalse())
                .and(visibleTeamCondition(userId));

        return dsl.select(
                        TB_WORKLOG.WORKLOG_ID,
                        TB_WORKLOG.TEAM_ID,
                        TB_TEAM.TEAM_NAME,
                        TB_WORKLOG.AUTHOR_ID,
                        TB_USER.USER_NAME,
                        TB_WORKLOG.TITLE,
                        TB_WORKLOG.REQUEST_CONTENT,
                        TB_WORKLOG.WORK_CONTENT,
                        TB_WORKLOG.STATUS_CODE,
                        TB_WORKLOG.ACTUAL_HOURS,
                        TB_WORKLOG.INSTRUCTION_DATE,
                        TB_WORKLOG.DUE_DATE
                )
                .from(TB_WORKLOG)
                .join(TB_TEAM).on(TB_WORKLOG.TEAM_ID.eq(TB_TEAM.TEAM_ID))
                .join(TB_USER).on(TB_WORKLOG.AUTHOR_ID.eq(TB_USER.USER_ID))
                .where(condition)
                .fetchOptional(WorklogDetailProjection::from);
    }

    private Condition visibleTeamCondition(Long userId) {
        return TB_TEAM.DELETED_AT.isNull()
                .and(TB_TEAM.TEAM_ID.in(visibleTeamIds(userId)));
    }

    private Select<Record1<Long>> visibleTeamIds(Long userId) {
        return DSL.select(TB_TEAM_ADMIN.TEAM_ID)
                .from(TB_TEAM_ADMIN)
                .where(TB_TEAM_ADMIN.USER_ID.eq(userId))
                .union(DSL.select(TB_USER_TEAM.TEAM_ID)
                        .from(TB_USER_TEAM)
                        .where(TB_USER_TEAM.USER_ID.eq(userId))
                        .and(TB_USER_TEAM.STATUS_CODE.eq(ACTIVE_USER_TEAM_STATUS)));
    }

    @Override
    public Page<WorklogSearchProjection> searchWorklogPage(WorklogVisibilityScope scope, WorklogSearchQuery query) {
        int pageIndex = query.pageIndex();
        int pageSize = query.pageSize();
        PageRequest pageRequest = PageRequest.of(pageIndex, pageSize);

        Condition condition = applySearchFilters(
                TB_WORKLOG.IS_DELETED.isFalse().and(toCondition(scope)),
                query);

        long total = dsl.selectCount()
                .from(TB_WORKLOG)
                .where(condition)
                .fetchSingle(0, Integer.class)
                .longValue();

        if (total == 0) {
            return new PageImpl<>(List.of(), pageRequest, 0);
        }

        Field<Integer> predecessorCountField = DSL.selectCount()
                .from(TB_WORKLOG_DEPENDENCY)
                .where(TB_WORKLOG_DEPENDENCY.WORKLOG_ID.eq(TB_WORKLOG.WORKLOG_ID))
                .asField("predecessor_count");

        List<WorklogSearchProjection> items = dsl.select(
                        TB_WORKLOG.WORKLOG_ID,
                        TB_WORKLOG.TITLE,
                        TB_WORKLOG.AI_SUMMARY,
                        TB_WORKLOG.STATUS_CODE,
                        TB_WORKLOG.IMPORTANCE_CODE,
                        TB_WORKLOG.AI_PROCESSING_STATUS,
                        predecessorCountField,
                        TB_WORKLOG.TEAM_ID,
                        TB_TEAM.TEAM_NAME,
                        TB_WORKLOG.AUTHOR_ID,
                        TB_USER.USER_NAME,
                        TB_USER.PROFILE_IMAGE_URL,
                        TB_WORKLOG.INSTRUCTION_DATE,
                        TB_WORKLOG.DUE_DATE
                )
                .from(TB_WORKLOG)
                .join(TB_TEAM).on(TB_WORKLOG.TEAM_ID.eq(TB_TEAM.TEAM_ID))
                .join(TB_USER).on(TB_WORKLOG.AUTHOR_ID.eq(TB_USER.USER_ID))
                .where(condition)
                .orderBy(TB_WORKLOG.CREATED_AT.desc(), TB_WORKLOG.WORKLOG_ID.desc())
                .limit(pageSize)
                .offset((long) pageIndex * pageSize)
                .fetch(record -> WorklogSearchProjection.from(record, predecessorCountField));

        return new PageImpl<>(items, pageRequest, total);
    }

    /**
     * 검색 query 의 각 필터를 base condition 에 누적한다.
     * keyword 는 제목 ILIKE substring 매칭, 상태/중요도는 enum name() 으로 String 비교한다.
     * tagId 는 worklog_tag exists 서브쿼리로 매핑한다.
     */
    private Condition applySearchFilters(Condition base, WorklogSearchQuery query) {
        Condition condition = base;
        if (query.keyword() != null) {
            condition = condition.and(TB_WORKLOG.TITLE.containsIgnoreCase(query.keyword()));
        }
        if (query.teamId() != null) {
            condition = condition.and(TB_WORKLOG.TEAM_ID.eq(query.teamId()));
        }
        if (query.teamStatus() != null) {
            condition = condition.and(TB_TEAM.STATUS_CODE.eq(query.teamStatus().name()));
        }
        if (query.statusCode() != null) {
            condition = condition.and(TB_WORKLOG.STATUS_CODE.eq(query.statusCode().name()));
        }
        if (query.importanceCode() != null) {
            condition = condition.and(TB_WORKLOG.IMPORTANCE_CODE.eq(query.importanceCode().name()));
        }
        if (query.authorId() != null) {
            condition = condition.and(TB_WORKLOG.AUTHOR_ID.eq(query.authorId()));
        }
        if (query.tagId() != null) {
            condition = condition.and(DSL.exists(
                    DSL.selectOne()
                            .from(TB_WORKLOG_TAG)
                            .where(TB_WORKLOG_TAG.WORKLOG_ID.eq(TB_WORKLOG.WORKLOG_ID))
                            .and(TB_WORKLOG_TAG.TAG_ID.eq(query.tagId()))
            ));
        }
        if (query.createdFrom() != null) {
            condition = condition.and(TB_WORKLOG.CREATED_AT.ge(query.createdFrom().atStartOfDay()));
        }
        return condition;
    }

    /**
     * 가시 범위 값을 JOOQ Condition 으로 매핑한다.
     */
    private Condition toCondition(WorklogVisibilityScope scope) {
        return switch (scope) {
            case WorklogVisibilityScope.All ignored -> DSL.noCondition();
            case WorklogVisibilityScope.Department department -> TB_WORKLOG.TEAM_ID.in(
                            DSL.select(TB_USER_TEAM.TEAM_ID)
                                    .from(TB_USER_TEAM)
                                    .join(TB_TEAM).on(TB_USER_TEAM.TEAM_ID.eq(TB_TEAM.TEAM_ID))
                                    .where(TB_USER_TEAM.USER_ID.eq(department.userId()))
                                    .and(TB_USER_TEAM.STATUS_CODE.eq(ACTIVE_USER_TEAM_STATUS))
                                    .and(TB_TEAM.DELETED_AT.isNull())
                    )
                    .or(TB_WORKLOG.TEAM_ID.in(
                            DSL.select(TB_TEAM_ADMIN.TEAM_ID)
                                    .from(TB_TEAM_ADMIN)
                                    .join(TB_TEAM).on(TB_TEAM_ADMIN.TEAM_ID.eq(TB_TEAM.TEAM_ID))
                                    .where(TB_TEAM_ADMIN.USER_ID.eq(department.userId()))
                                    .and(TB_TEAM.DELETED_AT.isNull())
                    ));
            case WorklogVisibilityScope.MyTeams myTeams -> TB_WORKLOG.TEAM_ID.in(
                            DSL.select(TB_USER_TEAM.TEAM_ID)
                                    .from(TB_USER_TEAM)
                                    .join(TB_TEAM).on(TB_USER_TEAM.TEAM_ID.eq(TB_TEAM.TEAM_ID))
                                    .where(TB_USER_TEAM.USER_ID.eq(myTeams.userId()))
                                    .and(TB_USER_TEAM.STATUS_CODE.eq(ACTIVE_USER_TEAM_STATUS))
                                    .and(TB_TEAM.DELETED_AT.isNull())
                    )
                    .or(TB_WORKLOG.TEAM_ID.in(
                            DSL.select(TB_TEAM_ADMIN.TEAM_ID)
                                    .from(TB_TEAM_ADMIN)
                                    .join(TB_TEAM).on(TB_TEAM_ADMIN.TEAM_ID.eq(TB_TEAM.TEAM_ID))
                                    .where(TB_TEAM_ADMIN.USER_ID.eq(myTeams.userId()))
                                    .and(TB_TEAM.DELETED_AT.isNull())
                    ));
        };
    }
}
