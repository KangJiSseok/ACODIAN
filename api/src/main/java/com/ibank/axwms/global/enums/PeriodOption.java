package com.ibank.axwms.global.enums;

import java.time.LocalDate;

/**
 * 검색/조회 API 의 기간 필터 옵션.
 * 기준 컬럼은 호출 측이 created_at 등 도메인 컬럼으로 지정하고,
 * 본 enum 은 today 기준 from 일자 계산만 책임진다.
 */
public enum PeriodOption {
    LAST_7(7),
    LAST_30(30),
    LAST_90(90);

    private final int days;

    PeriodOption(int days) {
        this.days = days;
    }

    /** today 기준 (today - days) 일자를 반환한다. 검색 from 경계 값으로 사용한다. */
    public LocalDate fromDate(LocalDate today) {
        return today.minusDays(days);
    }
}
