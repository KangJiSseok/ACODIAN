package com.ibank.axwms.domain.worklog.repository.jooq;

import com.ibank.axwms.domain.worklog.WorklogStatus;
import com.ibank.axwms.domain.worklog.policy.WorklogVisibilityScope;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.AiOutcomeProjection;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.AuthorCountSummaryProjection;
import com.ibank.axwms.global.enums.AiProcessingStatus;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.DepartmentLoadProjection;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.DepartmentProgressProjection;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.ProgressProjection;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.TeamLoadProjection;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.TeamProgressProjection;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.WorklogBriefProjection;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.ibank.axwms.global.jooq.Tables.TB_DEPARTMENT;
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
                .join(TB_TEAM).on(TB_WORKLOG.TEAM_ID.eq(TB_TEAM.TEAM_ID))
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
     * AI ranking 결과의 업무 ID 목록을 최신 업무/팀/작성자 정보로 다시 조회한다.
     */
    @Override
    public List<WorklogSearchProjection> findSearchWorklogsByIds(WorklogVisibilityScope scope, List<Long> worklogIds) {
        if (worklogIds == null || worklogIds.isEmpty()) {
            return List.of();
        }

        Field<Integer> predecessorCountField = DSL.selectCount()
                .from(TB_WORKLOG_DEPENDENCY)
                .where(TB_WORKLOG_DEPENDENCY.WORKLOG_ID.eq(TB_WORKLOG.WORKLOG_ID))
                .asField("predecessor_count");

        Condition condition = TB_WORKLOG.WORKLOG_ID.in(worklogIds)
                .and(TB_WORKLOG.IS_DELETED.isFalse())
                .and(TB_TEAM.DELETED_AT.isNull())
                .and(toCondition(scope));

        return dsl.select(
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
                .fetch(record -> WorklogSearchProjection.from(record, predecessorCountField));
    }

    /**
     * WorklogVisibilityScope 를 AI 서버 권한 필터용 팀 ID 목록으로 풀어낸다.
     */
    @Override
    public List<Long> findVisibleTeamIds(WorklogVisibilityScope scope) {
        Condition condition = TB_TEAM.DELETED_AT.isNull().and(toTeamCondition(scope));
        return dsl.select(TB_TEAM.TEAM_ID)
                .from(TB_TEAM)
                .where(condition)
                .fetch(TB_TEAM.TEAM_ID);
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
            case WorklogVisibilityScope.Department department ->
                    TB_WORKLOG.TEAM_ID.in(visibleNonDeletedTeamIds(department.userId()));
            case WorklogVisibilityScope.MyTeams myTeams ->
                    TB_WORKLOG.TEAM_ID.in(visibleNonDeletedTeamIds(myTeams.userId()));
        };
    }

    // ===== 대시보드 ME 위젯 =====

    // enum.name() 으로 정의해 enum 이 리네임/삭제되면 컴파일 에러로 잡히도록 한다 (매직 문자열 제거).
    private static final String STATUS_IN_PROGRESS = WorklogStatus.IN_PROGRESS.name();
    private static final String STATUS_COMPLETED = WorklogStatus.COMPLETED.name();
    private static final String AI_STATUS_FAILED = AiProcessingStatus.FAILED.name();

    /**
     * 한 번의 SELECT 로 ME 카운트 3종 동시 집계 — 3개 별도 쿼리 → 1번으로 round-trip 절감.
     * 같은 (author + team + is_deleted=false) 위에서 CASE WHEN 으로 갈라 sum 한다.
     */
    @Override
    public AuthorCountSummaryProjection aggregateAuthorCounts(Long authorId, Long teamId, LocalDate completedFrom) {
        Field<Integer> inProgress = DSL.coalesce(
                DSL.sum(DSL.when(TB_WORKLOG.STATUS_CODE.eq(STATUS_IN_PROGRESS), 1).otherwise(0)),
                0
        ).cast(Integer.class).as("in_progress_count");
        Field<Integer> completedSince = DSL.coalesce(
                DSL.sum(DSL.when(TB_WORKLOG.STATUS_CODE.eq(STATUS_COMPLETED)
                        .and(TB_WORKLOG.COMPLETION_DATE.ge(completedFrom)), 1).otherwise(0)),
                0
        ).cast(Integer.class).as("completed_since_count");
        Field<Integer> aiFailed = DSL.coalesce(
                DSL.sum(DSL.when(TB_WORKLOG.AI_PROCESSING_STATUS.eq(AI_STATUS_FAILED), 1).otherwise(0)),
                0
        ).cast(Integer.class).as("ai_failed_count");

        return dsl.select(inProgress, completedSince, aiFailed)
                .from(TB_WORKLOG)
                .where(TB_WORKLOG.AUTHOR_ID.eq(authorId))
                .and(TB_WORKLOG.TEAM_ID.eq(teamId))
                .and(TB_WORKLOG.IS_DELETED.isFalse())
                .fetchSingle(record -> AuthorCountSummaryProjection.from(
                        record, inProgress, completedSince, aiFailed));
    }

    @Override
    public List<WorklogBriefProjection> findAuthorThisWeekDue(Long authorId, Long teamId, LocalDate today, int limit) {
        return dsl.select(TB_WORKLOG.WORKLOG_ID, TB_WORKLOG.TITLE, TB_WORKLOG.STATUS_CODE, TB_WORKLOG.DUE_DATE)
                .from(TB_WORKLOG)
                .where(TB_WORKLOG.AUTHOR_ID.eq(authorId))
                .and(TB_WORKLOG.TEAM_ID.eq(teamId))
                .and(TB_WORKLOG.IS_DELETED.isFalse())
                .and(TB_WORKLOG.STATUS_CODE.ne(STATUS_COMPLETED))
                .and(TB_WORKLOG.DUE_DATE.between(today, today.plusDays(7)))
                .orderBy(TB_WORKLOG.DUE_DATE.asc().nullsLast(), TB_WORKLOG.WORKLOG_ID.asc())
                .limit(limit)
                .fetch(WorklogBriefProjection::from);
    }

    @Override
    public List<WorklogBriefProjection> findAuthorTodayItems(Long authorId, Long teamId, int limit) {
        Field<Integer> inProgressPriority =
                DSL.when(TB_WORKLOG.STATUS_CODE.eq(STATUS_IN_PROGRESS), 0).otherwise(1);
        return dsl.select(TB_WORKLOG.WORKLOG_ID, TB_WORKLOG.TITLE, TB_WORKLOG.STATUS_CODE, TB_WORKLOG.DUE_DATE)
                .from(TB_WORKLOG)
                .where(TB_WORKLOG.AUTHOR_ID.eq(authorId))
                .and(TB_WORKLOG.TEAM_ID.eq(teamId))
                .and(TB_WORKLOG.IS_DELETED.isFalse())
                .and(TB_WORKLOG.STATUS_CODE.ne(STATUS_COMPLETED))
                .orderBy(inProgressPriority.asc(),
                        TB_WORKLOG.DUE_DATE.asc().nullsLast(),
                        TB_WORKLOG.WORKLOG_ID.asc())
                .limit(limit)
                .fetch(WorklogBriefProjection::from);
    }

    @Override
    public List<WorklogBriefProjection> findAuthorImminentAndOverdue(Long authorId, Long teamId, LocalDate today, int limit) {
        return dsl.select(TB_WORKLOG.WORKLOG_ID, TB_WORKLOG.TITLE, TB_WORKLOG.STATUS_CODE, TB_WORKLOG.DUE_DATE)
                .from(TB_WORKLOG)
                .where(TB_WORKLOG.AUTHOR_ID.eq(authorId))
                .and(TB_WORKLOG.TEAM_ID.eq(teamId))
                .and(TB_WORKLOG.IS_DELETED.isFalse())
                .and(TB_WORKLOG.STATUS_CODE.ne(STATUS_COMPLETED))
                .and(TB_WORKLOG.DUE_DATE.le(today.plusDays(3)))
                .orderBy(TB_WORKLOG.DUE_DATE.asc().nullsLast(), TB_WORKLOG.WORKLOG_ID.asc())
                .limit(limit)
                .fetch(WorklogBriefProjection::from);
    }

    @Override
    public List<Long> findIncompleteAuthorWorklogIdsBlockedByPredecessor(Long authorId, Long teamId, int limit) {
        var pred = TB_WORKLOG.as("pred");
        return dsl.select(TB_WORKLOG.WORKLOG_ID)
                .from(TB_WORKLOG)
                .where(TB_WORKLOG.AUTHOR_ID.eq(authorId))
                .and(TB_WORKLOG.TEAM_ID.eq(teamId))
                .and(TB_WORKLOG.IS_DELETED.isFalse())
                .and(TB_WORKLOG.STATUS_CODE.ne(STATUS_COMPLETED))
                .and(DSL.exists(
                        DSL.selectOne()
                                .from(TB_WORKLOG_DEPENDENCY)
                                .join(pred).on(pred.WORKLOG_ID.eq(TB_WORKLOG_DEPENDENCY.DEPENDS_ON_WORKLOG_ID))
                                .where(TB_WORKLOG_DEPENDENCY.WORKLOG_ID.eq(TB_WORKLOG.WORKLOG_ID))
                                .and(pred.IS_DELETED.isFalse())
                                .and(pred.STATUS_CODE.ne(STATUS_COMPLETED))
                ))
                .orderBy(TB_WORKLOG.DUE_DATE.asc().nullsLast(), TB_WORKLOG.WORKLOG_ID.asc())
                .limit(limit)
                .fetch(record -> record.get(TB_WORKLOG.WORKLOG_ID));
    }

    // ===== 대시보드 위젯 — 전사(ORG)/부서(DEPT) 집계 =====

    // STATUS_COMPLETED 와 같은 문자열이지만 컬럼이 다름 (ai_processing_status). enum 분리되어 있어 별도 상수.
    private static final String AI_STATUS_COMPLETED = AiProcessingStatus.COMPLETED.name();

    /**
     * 부서 소속 팀의 worklog 만 잡기 위한 공통 JOIN 조건.
     * worklog → team 으로 join 하고 team.department_id 로 부서를 좁힌다 (삭제 팀 제외).
     */
    private static Condition deptScope(Long departmentId) {
        return TB_TEAM.TEAM_ID.eq(TB_WORKLOG.TEAM_ID)
                .and(TB_TEAM.DEPARTMENT_ID.eq(departmentId))
                .and(TB_TEAM.DELETED_AT.isNull());
    }

    /**
     * 진행 현황 (완료, 전체) 동시 집계. coalesce(sum(case when …), 0) 으로 매칭 0건 시 NULL 대신 0 보장.
     * teamJoinCondition 이 null 이면 전사 (team JOIN 없음), 아니면 부서로 좁힘.
     */
    private ProgressProjection aggregateProgress(Condition teamJoinCondition) {
        Field<Integer> completed = DSL.coalesce(
                DSL.sum(DSL.when(TB_WORKLOG.STATUS_CODE.eq(STATUS_COMPLETED), 1).otherwise(0)),
                0
        ).cast(Integer.class).as("completed_count");
        Field<Integer> total = DSL.count(TB_WORKLOG.WORKLOG_ID).as("total_count");

        var step = dsl.select(completed, total).from(TB_WORKLOG);
        if (teamJoinCondition != null) {
            step = step.join(TB_TEAM).on(teamJoinCondition);
        }
        return step.where(TB_WORKLOG.IS_DELETED.isFalse())
                .fetchSingle(record -> ProgressProjection.from(record, completed, total));
    }

    /** "최근 N일 완료" 카운트. teamJoinCondition 이 null 이면 전사, 아니면 부서. */
    private int countCompletedSince(Condition teamJoinCondition, LocalDate from) {
        var step = dsl.selectCount().from(TB_WORKLOG);
        if (teamJoinCondition != null) {
            step = step.join(TB_TEAM).on(teamJoinCondition);
        }
        return step.where(TB_WORKLOG.IS_DELETED.isFalse())
                .and(TB_WORKLOG.STATUS_CODE.eq(STATUS_COMPLETED))
                .and(TB_WORKLOG.COMPLETION_DATE.ge(from))
                .fetchSingle(0, Integer.class);
    }

    /**
     * AI 처리 결과 (success, failed) raw 카운트. 성공률 계산은 DTO 책임 —
     * PENDING/PROCESSING 을 분모에서 뺄지 등 분모 정의가 도메인 정책이라 repository 가 결정하지 않는다.
     */
    private AiOutcomeProjection aggregateAiOutcome(Condition teamJoinCondition) {
        Field<Integer> success = DSL.coalesce(
                DSL.sum(DSL.when(TB_WORKLOG.AI_PROCESSING_STATUS.eq(AI_STATUS_COMPLETED), 1).otherwise(0)),
                0
        ).cast(Integer.class).as("success_count");
        Field<Integer> failed = DSL.coalesce(
                DSL.sum(DSL.when(TB_WORKLOG.AI_PROCESSING_STATUS.eq(AI_STATUS_FAILED), 1).otherwise(0)),
                0
        ).cast(Integer.class).as("failed_count");

        var step = dsl.select(success, failed).from(TB_WORKLOG);
        if (teamJoinCondition != null) {
            step = step.join(TB_TEAM).on(teamJoinCondition);
        }
        return step.where(TB_WORKLOG.IS_DELETED.isFalse())
                .fetchSingle(record -> AiOutcomeProjection.from(record, success, failed));
    }

    /**
     * 마감 임박/지연 (D-3 이내, 미완료) worklog. team JOIN 은 항상 있고 (team_name 표시용),
     * teamJoinCondition 으로 scope 결정 — 전사면 단순 매칭, 부서면 dept 필터까지.
     * 부서 표시는 team.department_id 기준 — 부서별 집계 위젯과 같은 매핑이라 합이 맞는다.
     * DUE_DATE.le(today+3) 는 NULL due_date 를 자연스럽게 제외하므로 nullsLast() 정렬은 안전 가드용.
     */
    private List<WorklogBriefProjection> findImminentAndOverdue(Condition teamJoinCondition,
                                                                LocalDate today, int limit) {
        return dsl.select(
                        TB_WORKLOG.WORKLOG_ID,
                        TB_WORKLOG.TITLE,
                        TB_WORKLOG.STATUS_CODE,
                        TB_WORKLOG.DUE_DATE,
                        TB_WORKLOG.AUTHOR_ID,
                        TB_USER.USER_NAME,
                        TB_WORKLOG.TEAM_ID,
                        TB_TEAM.TEAM_NAME,
                        TB_DEPARTMENT.DEPARTMENT_ID,
                        TB_DEPARTMENT.DEPARTMENT_NAME
                )
                .from(TB_WORKLOG)
                .join(TB_USER).on(TB_USER.USER_ID.eq(TB_WORKLOG.AUTHOR_ID))
                .join(TB_TEAM).on(teamJoinCondition)
                .leftJoin(TB_DEPARTMENT).on(TB_DEPARTMENT.DEPARTMENT_ID.eq(TB_TEAM.DEPARTMENT_ID))
                .where(TB_WORKLOG.IS_DELETED.isFalse())
                .and(TB_WORKLOG.STATUS_CODE.ne(STATUS_COMPLETED))
                .and(TB_WORKLOG.DUE_DATE.le(today.plusDays(3)))
                .orderBy(TB_WORKLOG.DUE_DATE.asc().nullsLast(), TB_WORKLOG.WORKLOG_ID.asc())
                .limit(limit)
                .fetch(WorklogBriefProjection::fromOrg);
    }

    // ----- 공개 API: 전사 (DEPARTMENT_COMPARISON) -----

    @Override
    public ProgressProjection aggregateOrgProgress() {
        return aggregateProgress(null);
    }

    @Override
    public int countOrgCompletedSince(LocalDate from) {
        return countCompletedSince(null, from);
    }

    @Override
    public AiOutcomeProjection aggregateOrgAiOutcome() {
        return aggregateAiOutcome(null);
    }

    @Override
    public List<WorklogBriefProjection> findOrgImminentAndOverdue(LocalDate today, int limit) {
        // 전사 위젯은 모든 worklog 포함 — team JOIN 은 team_name 표시용으로만, 추가 필터 없음.
        return findImminentAndOverdue(TB_TEAM.TEAM_ID.eq(TB_WORKLOG.TEAM_ID), today, limit);
    }

    // ----- 공개 API: 단일 부서 (DEPARTMENT_DETAIL) -----

    @Override
    public ProgressProjection aggregateDeptProgress(Long departmentId) {
        return aggregateProgress(deptScope(departmentId));
    }

    @Override
    public int countDeptCompletedSince(Long departmentId, LocalDate from) {
        return countCompletedSince(deptScope(departmentId), from);
    }

    @Override
    public AiOutcomeProjection aggregateDeptAiOutcome(Long departmentId) {
        return aggregateAiOutcome(deptScope(departmentId));
    }

    @Override
    public List<WorklogBriefProjection> findDeptImminentAndOverdue(Long departmentId, LocalDate today, int limit) {
        return findImminentAndOverdue(deptScope(departmentId), today, limit);
    }

    // ----- 공개 API: 부서별 그래프 (DEPARTMENT_COMPARISON 전용, 부서 baseline) -----

    /**
     * 부서 baseline 으로 LEFT JOIN — worklog 0 건 부서도 (completed=0, total=0) 행으로 포함.
     * 부서 매핑은 worklog → tb_team → tb_team.department_id 기준 (DEPARTMENT_DETAIL 과 동일).
     * 즉 "이 부서가 소유한 팀들의 worklog" — 작성자가 다른 부서 소속이어도 팀이 이 부서면 포함된다.
     * 삭제된 팀은 매핑에서 제외.
     */
    @Override
    public List<DepartmentProgressProjection> findDepartmentCompletionRates() {
        Field<Integer> completed = DSL.coalesce(
                DSL.sum(DSL.when(TB_WORKLOG.STATUS_CODE.eq(STATUS_COMPLETED), 1).otherwise(0)),
                0
        ).cast(Integer.class).as("completed_count");
        Field<Integer> total = DSL.count(TB_WORKLOG.WORKLOG_ID).as("total_count");

        return dsl.select(TB_DEPARTMENT.DEPARTMENT_ID, TB_DEPARTMENT.DEPARTMENT_NAME, completed, total)
                .from(TB_DEPARTMENT)
                .leftJoin(TB_TEAM).on(TB_TEAM.DEPARTMENT_ID.eq(TB_DEPARTMENT.DEPARTMENT_ID)
                        .and(TB_TEAM.DELETED_AT.isNull()))
                .leftJoin(TB_WORKLOG).on(TB_WORKLOG.TEAM_ID.eq(TB_TEAM.TEAM_ID)
                        .and(TB_WORKLOG.IS_DELETED.isFalse()))
                .groupBy(TB_DEPARTMENT.DEPARTMENT_ID, TB_DEPARTMENT.DEPARTMENT_NAME)
                .orderBy(TB_DEPARTMENT.DEPARTMENT_NAME.asc(), TB_DEPARTMENT.DEPARTMENT_ID.asc())
                .fetch(record -> DepartmentProgressProjection.from(record, completed, total));
    }

    /**
     * 부서별 활성(미완료) worklog 수. 부서 baseline LEFT JOIN.
     * 부서 매핑은 worklog → tb_team → tb_team.department_id 기준.
     */
    @Override
    public List<DepartmentLoadProjection> findDepartmentWorkloads() {
        Field<Integer> activeCount = DSL.count(TB_WORKLOG.WORKLOG_ID).as("active_count");
        return dsl.select(TB_DEPARTMENT.DEPARTMENT_ID, TB_DEPARTMENT.DEPARTMENT_NAME, activeCount)
                .from(TB_DEPARTMENT)
                .leftJoin(TB_TEAM).on(TB_TEAM.DEPARTMENT_ID.eq(TB_DEPARTMENT.DEPARTMENT_ID)
                        .and(TB_TEAM.DELETED_AT.isNull()))
                .leftJoin(TB_WORKLOG).on(TB_WORKLOG.TEAM_ID.eq(TB_TEAM.TEAM_ID)
                        .and(TB_WORKLOG.IS_DELETED.isFalse())
                        .and(TB_WORKLOG.STATUS_CODE.ne(STATUS_COMPLETED)))
                .groupBy(TB_DEPARTMENT.DEPARTMENT_ID, TB_DEPARTMENT.DEPARTMENT_NAME)
                .orderBy(TB_DEPARTMENT.DEPARTMENT_NAME.asc(), TB_DEPARTMENT.DEPARTMENT_ID.asc())
                .fetch(record -> DepartmentLoadProjection.from(record, activeCount));
    }

    // ----- 공개 API: 팀별 그래프 (DEPARTMENT_DETAIL 전용, 팀 baseline) -----

    /** 팀 baseline LEFT JOIN — 부서 소속이지만 worklog 0 건인 팀도 (completed=0, total=0) 행으로 포함. */
    @Override
    public List<TeamProgressProjection> findTeamCompletionRatesInDept(Long departmentId) {
        Field<Integer> completed = DSL.coalesce(
                DSL.sum(DSL.when(TB_WORKLOG.STATUS_CODE.eq(STATUS_COMPLETED), 1).otherwise(0)),
                0
        ).cast(Integer.class).as("completed_count");
        Field<Integer> total = DSL.count(TB_WORKLOG.WORKLOG_ID).as("total_count");

        return dsl.select(TB_TEAM.TEAM_ID, TB_TEAM.TEAM_NAME, completed, total)
                .from(TB_TEAM)
                .leftJoin(TB_WORKLOG).on(TB_WORKLOG.TEAM_ID.eq(TB_TEAM.TEAM_ID)
                        .and(TB_WORKLOG.IS_DELETED.isFalse()))
                .where(TB_TEAM.DEPARTMENT_ID.eq(departmentId))
                .and(TB_TEAM.DELETED_AT.isNull())
                .groupBy(TB_TEAM.TEAM_ID, TB_TEAM.TEAM_NAME)
                .orderBy(TB_TEAM.TEAM_NAME.asc(), TB_TEAM.TEAM_ID.asc())
                .fetch(record -> TeamProgressProjection.from(record, completed, total));
    }

    /** 팀 baseline LEFT JOIN — 부서 소속이지만 활성 worklog 0 건인 팀도 행에 포함. */
    @Override
    public List<TeamLoadProjection> findTeamWorkloadsInDept(Long departmentId) {
        Field<Integer> activeCount = DSL.count(TB_WORKLOG.WORKLOG_ID).as("active_count");
        return dsl.select(TB_TEAM.TEAM_ID, TB_TEAM.TEAM_NAME, activeCount)
                .from(TB_TEAM)
                .leftJoin(TB_WORKLOG).on(TB_WORKLOG.TEAM_ID.eq(TB_TEAM.TEAM_ID)
                        .and(TB_WORKLOG.IS_DELETED.isFalse())
                        .and(TB_WORKLOG.STATUS_CODE.ne(STATUS_COMPLETED)))
                .where(TB_TEAM.DEPARTMENT_ID.eq(departmentId))
                .and(TB_TEAM.DELETED_AT.isNull())
                .groupBy(TB_TEAM.TEAM_ID, TB_TEAM.TEAM_NAME)
                .orderBy(TB_TEAM.TEAM_NAME.asc(), TB_TEAM.TEAM_ID.asc())
                .fetch(record -> TeamLoadProjection.from(record, activeCount));
    }

    /**
     * 팀 테이블을 기준으로 같은 visible scope 를 적용할 수 있게 변환한다.
     */
    private Condition toTeamCondition(WorklogVisibilityScope scope) {
        return switch (scope) {
            case WorklogVisibilityScope.All ignored -> DSL.noCondition();
            case WorklogVisibilityScope.Department department ->
                    TB_TEAM.TEAM_ID.in(visibleTeamIds(department.userId()));
            case WorklogVisibilityScope.MyTeams myTeams -> TB_TEAM.TEAM_ID.in(visibleTeamIds(myTeams.userId()));
        };
    }

    /**
     * 업무 검색 scope 에서 삭제된 팀의 업무가 함께 노출되지 않도록 팀 삭제 조건을 subquery 안에 묶는다.
     */
    private Select<Record1<Long>> visibleNonDeletedTeamIds(Long userId) {
        return DSL.select(TB_TEAM.TEAM_ID)
                .from(TB_TEAM)
                .where(TB_TEAM.DELETED_AT.isNull())
                .and(TB_TEAM.TEAM_ID.in(visibleTeamIds(userId)));
    }
}
