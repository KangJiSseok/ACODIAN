package com.ibank.axwms.domain.worklog.repository.jooq.projection;

import static com.ibank.axwms.global.jooq.Tables.TB_FILE;

import org.jooq.Record;

/** 업무 상세에 노출할 첨부 파일 한 행. */
public record WorklogFileProjection(
        Long fileId,
        String originalName,
        String storedPath,
        String fileExtension,
        Long fileSizeBytes
) {

    public static WorklogFileProjection from(Record record) {
        return new WorklogFileProjection(
                record.get(TB_FILE.FILE_ID),
                record.get(TB_FILE.ORIGINAL_NAME),
                record.get(TB_FILE.STORED_PATH),
                record.get(TB_FILE.FILE_EXTENSION),
                record.get(TB_FILE.FILE_SIZE_BYTES)
        );
    }
}
