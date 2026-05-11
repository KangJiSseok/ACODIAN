package com.ibank.axwms.domain.notification.repository.jooq.query;

import com.ibank.axwms.domain.notification.dto.SearchNotificationsApiDto;

/** 내 알림 목록 조회에 필요한 필터와 pagination 값을 repository 경계에서 사용하는 query 로 정규화한다. */
public record NotificationSearchQuery(
        Boolean isRead,
        Long departmentId,
        Long teamId,
        int page,
        int pageSize
) {

    /** API 요청 DTO 의 null 요청과 기본 pagination 값을 repository 전용 query 로 변환한다. */
    public static NotificationSearchQuery from(SearchNotificationsApiDto.Request request) {
        SearchNotificationsApiDto.Request query = request == null
                ? new SearchNotificationsApiDto.Request(null, null, null, null, null)
                : request;
        return new NotificationSearchQuery(
                query.isRead(),
                query.departmentId(),
                query.teamId(),
                query.pageOrDefault(),
                query.pageSizeOrDefault()
        );
    }

    /** Spring Data PageRequest 에 맞춰 1-based page 를 0-based index 로 변환한다. */
    public int pageIndex() {
        return page - 1;
    }
}
