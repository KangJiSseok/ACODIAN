package com.ibank.axwms.domain.file.repository.jooq;

import static com.ibank.axwms.global.jooq.Tables.TB_FILE;
import static com.ibank.axwms.global.jooq.Tables.TB_TEAM;
import static com.ibank.axwms.global.jooq.Tables.TB_TEAM_ADMIN;
import static com.ibank.axwms.global.jooq.Tables.TB_USER;
import static com.ibank.axwms.global.jooq.Tables.TB_USER_TEAM;
import static com.ibank.axwms.global.jooq.Tables.TB_WORKLOG;
import static com.ibank.axwms.global.jooq.Tables.TB_WORKLOG_DEPENDENCY;

import com.ibank.axwms.domain.file.repository.jooq.projection.FileSummaryProjection;
import com.ibank.axwms.domain.file.repository.jooq.projection.FileWorklogProjection;
import com.ibank.axwms.domain.file.repository.jooq.query.FilePageQuery;
import com.ibank.axwms.domain.organization.team.UserTeamStatus;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.WorklogFileProjection;
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

import java.util.Collection;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class FileJooqRepositoryImpl implements FileJooqRepository {

    // worklog 도메인의 가시성 룰과 동일 — admin grant ∪ ACTIVE membership 의 (미삭제) team 만 노출.
    private static final String ACTIVE_USER_TEAM_STATUS = UserTeamStatus.ACTIVE.name();

    private final DSLContext dsl;

    @Override
    public List<WorklogFileProjection> findByWorklogId(Long worklogId) {
        return dsl.select(
                        TB_FILE.FILE_ID,
                        TB_FILE.ORIGINAL_NAME,
                        TB_FILE.STORED_PATH,
                        TB_FILE.FILE_EXTENSION,
                        TB_FILE.FILE_SIZE_BYTES
                )
                .from(TB_FILE)
                .where(TB_FILE.WORKLOG_ID.eq(worklogId))
                .and(TB_FILE.IS_DELETED.isFalse())
                .orderBy(TB_FILE.CREATED_AT.asc(), TB_FILE.FILE_ID.asc())
                .fetch(WorklogFileProjection::from);
    }

    @Override
    public Page<FileSummaryProjection> findFilePage(Long userId, FilePageQuery query) {
        int pageIndex = query.pageIndex();
        int pageSize = query.pageSize();
        PageRequest pageRequest = PageRequest.of(pageIndex, pageSize);

        Condition condition = filePageCondition(userId, query);

        long total = dsl.selectCount()
                .from(TB_FILE)
                .join(TB_WORKLOG).on(TB_WORKLOG.WORKLOG_ID.eq(TB_FILE.WORKLOG_ID))
                .join(TB_TEAM).on(TB_TEAM.TEAM_ID.eq(TB_WORKLOG.TEAM_ID))
                .where(condition)
                .fetchSingle(0, Integer.class)
                .longValue();

        if (total == 0) {
            return new PageImpl<>(List.of(), pageRequest, 0);
        }

        List<FileSummaryProjection> items = dsl.select(
                        TB_FILE.FILE_ID,
                        TB_FILE.WORKLOG_ID,
                        TB_FILE.ORIGINAL_NAME,
                        TB_FILE.STORED_PATH,
                        TB_FILE.FILE_EXTENSION,
                        TB_FILE.FILE_SIZE_BYTES,
                        TB_FILE.AI_SUMMARY,
                        TB_FILE.AI_PROCESSING_STATUS,
                        TB_FILE.CREATED_AT
                )
                .from(TB_FILE)
                .join(TB_WORKLOG).on(TB_WORKLOG.WORKLOG_ID.eq(TB_FILE.WORKLOG_ID))
                .join(TB_TEAM).on(TB_TEAM.TEAM_ID.eq(TB_WORKLOG.TEAM_ID))
                .where(condition)
                .orderBy(TB_FILE.CREATED_AT.desc(), TB_FILE.FILE_ID.desc())
                .limit(pageSize)
                .offset((long) pageIndex * pageSize)
                .fetch(FileSummaryProjection::from);

        return new PageImpl<>(items, pageRequest, total);
    }

    /**
     * count/items 쿼리가 같은 visible scope 와 파일 필터 조건을 공유하도록 단일 Condition 으로 조립한다.
     */
    private Condition filePageCondition(Long userId, FilePageQuery query) {
        Condition condition = TB_FILE.IS_DELETED.isFalse()
                .and(TB_WORKLOG.IS_DELETED.isFalse())
                .and(visibleTeamCondition(userId));
        if (query.fileExtension() != null) {
            condition = condition.and(TB_FILE.FILE_EXTENSION.eq(query.fileExtension()));
        }
        if (query.createdFrom() != null) {
            condition = condition.and(TB_FILE.CREATED_AT.ge(query.createdFrom()));
        }
        return condition;
    }

    /**
     * worklog ID 목록에 대해 업무 요약을 1 쿼리로 batch 조회한다.
     * - 선행 업무 개수는 TB_WORKLOG_DEPENDENCY 서브쿼리(상관 SELECT COUNT)로 같은 행에 박는다.
     * - 가시성/미삭제 조건은 findFilePage 와 동일하게 다시 적용해 권한 밖 worklog 가 새어나가지 않도록 한다.
     */
    @Override
    public List<FileWorklogProjection> findFileWorklogsByIds(Long userId, Collection<Long> worklogIds) {
        if (worklogIds == null || worklogIds.isEmpty()) {
            return List.of();
        }

        Field<Integer> dependencyCount = DSL.selectCount()
                .from(TB_WORKLOG_DEPENDENCY)
                .where(TB_WORKLOG_DEPENDENCY.WORKLOG_ID.eq(TB_WORKLOG.WORKLOG_ID))
                .asField("dependency_count");

        Condition condition = TB_WORKLOG.WORKLOG_ID.in(worklogIds)
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
                        TB_WORKLOG.AI_SUMMARY,
                        TB_WORKLOG.AI_PROCESSING_STATUS,
                        TB_WORKLOG.DUE_DATE,
                        TB_WORKLOG.ACTUAL_HOURS,
                        dependencyCount
                )
                .from(TB_WORKLOG)
                .join(TB_TEAM).on(TB_TEAM.TEAM_ID.eq(TB_WORKLOG.TEAM_ID))
                .join(TB_USER).on(TB_USER.USER_ID.eq(TB_WORKLOG.AUTHOR_ID))
                .where(condition)
                .fetch(record -> FileWorklogProjection.from(record, dependencyCount));
    }

    /** team.deleted_at IS NULL AND team_id IN (admin grant ∪ ACTIVE membership). */
    private Condition visibleTeamCondition(Long userId) {
        return TB_TEAM.DELETED_AT.isNull()
                .and(TB_TEAM.TEAM_ID.in(visibleTeamIds(userId)));
    }

    /**
     * 파일 목록과 업무 요약 조회가 같은 팀 가시성 집합을 사용하도록 grant 와 ACTIVE membership 을 합친다.
     */
    private Select<Record1<Long>> visibleTeamIds(Long userId) {
        return DSL.select(TB_TEAM_ADMIN.TEAM_ID)
                .from(TB_TEAM_ADMIN)
                .where(TB_TEAM_ADMIN.USER_ID.eq(userId))
                .union(DSL.select(TB_USER_TEAM.TEAM_ID)
                        .from(TB_USER_TEAM)
                        .where(TB_USER_TEAM.USER_ID.eq(userId))
                        .and(TB_USER_TEAM.STATUS_CODE.eq(ACTIVE_USER_TEAM_STATUS)));
    }
}
