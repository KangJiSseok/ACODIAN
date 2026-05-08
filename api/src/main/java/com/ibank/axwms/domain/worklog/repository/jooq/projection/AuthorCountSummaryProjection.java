package com.ibank.axwms.domain.worklog.repository.jooq.projection;

import org.jooq.Field;
import org.jooq.Record;

/**
 * ME 대시보드의 3개 카운트 위젯 (in_progress / 기간내 완료 / AI 실패) raw 집계.
 * 같은 author + is_deleted=false 조건 위에서 한 번의 SELECT 로 동시 카운트해 round-trip 비용을 줄인다.
 * service 가 CompletedInPeriod 조립 등 응답 가공을 담당.
 */
public record AuthorCountSummaryProjection(
        int inProgressCount,
        int completedSinceCount,
        int aiFailedCount
) {

    /**
     * 3개 필드 모두 repository 쿼리에서 만든 계산 Field 라 record 외 추가 인자로 받는다.
     * 같은 Field 인스턴스를 select(...) 와 from(record, ...) 에 함께 전달해야 record.get(field) 가 매칭된다.
     */
    public static AuthorCountSummaryProjection from(Record record,
                                                    Field<Integer> inProgress,
                                                    Field<Integer> completedSince,
                                                    Field<Integer> aiFailed) {
        return new AuthorCountSummaryProjection(
                record.get(inProgress),
                record.get(completedSince),
                record.get(aiFailed)
        );
    }
}
