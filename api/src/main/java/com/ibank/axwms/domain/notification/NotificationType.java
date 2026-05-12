package com.ibank.axwms.domain.notification;

/**
 * 알림 유형 코드를 한 곳에 모아 DB 문자열 값의 오타와 임의 확장을 컴파일 단계에서 줄인다.
 */
public enum NotificationType {
    WORKLOG_DUE_SOON,
    WORKLOG_DUE_TODAY,
    WORKLOG_OVERDUE
}
