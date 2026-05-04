package com.ibank.axwms.domain.file.repository.jooq;

import static com.ibank.axwms.global.jooq.Tables.TB_FILE;

import com.ibank.axwms.domain.worklog.repository.jooq.projection.WorklogFileProjection;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
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
}
