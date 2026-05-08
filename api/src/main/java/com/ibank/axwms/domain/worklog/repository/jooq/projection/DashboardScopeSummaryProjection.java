package com.ibank.axwms.domain.worklog.repository.jooq.projection;

import org.jooq.Field;
import org.jooq.Record;

/**
 * 한 scope (전사/부서/팀) 의 dashboard 스칼라 집계 5종을 한 번의 SELECT 로 묶은 projection.
 * 같은 (is_deleted=false [+ scope JOIN]) base 위에서 CASE WHEN 으로 갈라 sum — round-trip 3회 → 1회.
 *
 * <p>기존 ProgressProjection / AiOutcomeProjection 과 호환되도록 progress(), aiOutcome() 추출 메서드를 제공해
 * DTO factory 시그니처는 변경하지 않는다.
 */
public record DashboardScopeSummaryProjection(
        int completedTotal,
        int total,
        int weeklyCompleted,
        int aiSuccess,
        int aiFailed
) {

    /**
     * 5개 필드 모두 repository 쿼리에서 만든 계산 Field 라 record 외 추가 인자로 받는다.
     * 같은 Field 인스턴스를 select(...) 와 from(record, ...) 에 함께 전달해야 record.get(field) 가 매칭된다.
     */
    public static DashboardScopeSummaryProjection from(
            Record record,
            Field<Integer> completed,
            Field<Integer> total,
            Field<Integer> weeklyCompleted,
            Field<Integer> aiSuccess,
            Field<Integer> aiFailed
    ) {
        return new DashboardScopeSummaryProjection(
                record.get(completed),
                record.get(total),
                record.get(weeklyCompleted),
                record.get(aiSuccess),
                record.get(aiFailed)
        );
    }

    /** 통합 집계에서 ProgressProjection 부분만 추출 — 기존 Progress.from(...) 호환. */
    public ProgressProjection progress() {
        return new ProgressProjection(completedTotal, total);
    }

    /** 통합 집계에서 AiOutcomeProjection 부분만 추출 — 기존 GetDashboardApiDto.aiSuccessRate(...) 호환. */
    public AiOutcomeProjection aiOutcome() {
        return new AiOutcomeProjection(aiSuccess, aiFailed);
    }
}
