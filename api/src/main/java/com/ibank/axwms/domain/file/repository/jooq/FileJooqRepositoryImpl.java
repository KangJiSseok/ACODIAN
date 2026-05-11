package com.ibank.axwms.domain.file.repository.jooq;

import static com.ibank.axwms.global.jooq.Tables.TB_FILE;

import com.ibank.axwms.domain.file.repository.jooq.projection.FileSummaryProjection;
import com.ibank.axwms.domain.file.repository.jooq.query.FilePageQuery;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.WorklogFileProjection;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class FileJooqRepositoryImpl implements FileJooqRepository {

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
    public Page<FileSummaryProjection> findFilePage(FilePageQuery query) {
        int pageIndex = query.pageIndex();
        int pageSize = query.pageSize();
        PageRequest pageRequest = PageRequest.of(pageIndex, pageSize);

        long total = dsl.selectCount()
                .from(TB_FILE)
                .where(TB_FILE.IS_DELETED.isFalse())
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
                .where(TB_FILE.IS_DELETED.isFalse())
                .orderBy(TB_FILE.CREATED_AT.desc(), TB_FILE.FILE_ID.desc())
                .limit(pageSize)
                .offset((long) pageIndex * pageSize)
                .fetch(FileSummaryProjection::from);

        return new PageImpl<>(items, pageRequest, total);
    }
}
