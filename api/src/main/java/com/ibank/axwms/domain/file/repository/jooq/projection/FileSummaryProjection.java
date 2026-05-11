package com.ibank.axwms.domain.file.repository.jooq.projection;

import com.ibank.axwms.global.enums.AiProcessingStatus;
import org.jooq.Record;

import java.time.LocalDateTime;

import static com.ibank.axwms.global.jooq.Tables.TB_FILE;

/** 파일 목록 한 행에 필요한 메타데이터를 담는 read 전용 projection. */
public record FileSummaryProjection(
        Long id,
        Long worklogId,
        String originalName,
        String storedPath,
        String fileExtension,
        Long fileSizeBytes,
        String aiSummary,
        AiProcessingStatus aiProcessingStatus,
        LocalDateTime createdAt
) {
    public static FileSummaryProjection from(Record record) {
        return new FileSummaryProjection(
                record.get(TB_FILE.FILE_ID),
                record.get(TB_FILE.WORKLOG_ID),
                record.get(TB_FILE.ORIGINAL_NAME),
                record.get(TB_FILE.STORED_PATH),
                record.get(TB_FILE.FILE_EXTENSION),
                record.get(TB_FILE.FILE_SIZE_BYTES),
                record.get(TB_FILE.AI_SUMMARY),
                AiProcessingStatus.valueOf(record.get(TB_FILE.AI_PROCESSING_STATUS)),
                record.get(TB_FILE.CREATED_AT)
        );
    }
}
