package com.ibank.axwms.domain.worklog;

public enum WorklogStatus {
    PENDING,        // 작성 예정 또는 아직 처리 전
    IN_PROGRESS,    // 현재 작업 중
    COMPLETED,      // 작업 끝남
    ON_HOLD,        // 잠시 보류됨
    CANCELLED       // 작업 취소됨
}
