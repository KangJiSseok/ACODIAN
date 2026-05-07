package com.ibank.axwms.domain.worklog.repository.jooq.projection;

import org.jooq.Field;
import org.jooq.Record;

/**
 * 단일 진행 현황 (완료 / 전체) 카운트. service 가 rate 를 계산한다.
 * 전사 / 부서 / 팀 등 scope 의 단일 진행률 집계에 공통 사용된다.
 */
public record ProgressProjection(
        int completed,
        int total
) {

    /**
     * completed/total 은 repository 쿼리에서 만든 계산 Field 라 record 외 추가 인자로 받는다.
     * 같은 Field 인스턴스를 select(...) 와 from(record, ...) 에 함께 전달해야 record.get(field) 가 매칭된다.
     */
    public static ProgressProjection from(Record record,
                                          Field<Integer> completed,
                                          Field<Integer> total) {
        return new ProgressProjection(
                record.get(completed),
                record.get(total)
        );
    }
}
