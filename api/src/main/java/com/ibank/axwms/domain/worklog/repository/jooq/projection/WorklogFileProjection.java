package com.ibank.axwms.domain.worklog.repository.jooq.projection;

import static com.ibank.axwms.global.jooq.Tables.TB_FILE;

import com.ibank.axwms.global.enums.AiProcessingStatus;
import org.jooq.Record;

/** 업무 상세에 노출할 첨부 파일 한 행. AI 요약과 처리 상태를 같이 실어 보낸다. */
public record WorklogFileProjection(
        Long fileId,
        String originalName,
        String storedPath,
        String fileExtension,
        Long fileSizeBytes,
        String aiSummary,
        AiProcessingStatus aiProcessingStatus
) {

    public static WorklogFileProjection from(Record record) {
        return new WorklogFileProjection(
                record.get(TB_FILE.FILE_ID),
                record.get(TB_FILE.ORIGINAL_NAME),
                record.get(TB_FILE.STORED_PATH),
                record.get(TB_FILE.FILE_EXTENSION),
                record.get(TB_FILE.FILE_SIZE_BYTES),
                record.get(TB_FILE.AI_SUMMARY),
                AiProcessingStatus.valueOf(record.get(TB_FILE.AI_PROCESSING_STATUS))
        );
    }
}
