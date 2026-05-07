package com.ibank.axwms.domain.worklog.repository.jooq.projection;

import org.jooq.Field;
import org.jooq.Record;

/** AI 처리 결과 집계 — COMPLETED 와 FAILED 의 카운트. service 가 success rate 를 계산한다. */
public record AiOutcomeProjection(
        int success,
        int failed
) {

    /**
     * success/failed 는 repository 쿼리에서 만든 계산 Field 라 record 외 추가 인자로 받는다.
     * 같은 Field 인스턴스를 select(...) 와 from(record, ...) 에 함께 전달해야 record.get(field) 가 매칭된다.
     */
    public static AiOutcomeProjection from(Record record,
                                           Field<Integer> success,
                                           Field<Integer> failed) {
        return new AiOutcomeProjection(
                record.get(success),
                record.get(failed)
        );
    }
}
