package com.ibank.axwms.domain.organization.user.repository.jooq.query;

import com.ibank.axwms.domain.organization.user.EmploymentStatus;
import com.ibank.axwms.domain.organization.user.dto.GetUsersApiDto;

/** 사용자 목록 repository 조회에 필요한 optional filter 값을 정규화한 query 이다. */
public record UserListQuery(
        String userName,
        Long departmentId,
        String positionName,
        EmploymentStatus employmentStatus,
        int page,
        int pageSize
) {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;

    /** API 요청 DTO 의 null 요청을 빈 필터와 기본 페이지 query 로 변환한다. */
    public static UserListQuery from(GetUsersApiDto.Request request) {
        if (request == null) {
            return new UserListQuery(null, null, null, null, DEFAULT_PAGE, DEFAULT_PAGE_SIZE);
        }
        return new UserListQuery(
                blankToNull(request.userName()),
                request.departmentId(),
                blankToNull(request.positionName()),
                request.employmentStatus(),
                request.pageOrDefault(),
                request.pageSizeOrDefault()
        );
    }

    /** API 는 1-based page 를 받지만 repository offset 계산은 0-based index 를 사용한다. */
    public int pageIndex() {
        return page - 1;
    }

    /** 빈 문자열 filter 가 전체 조건으로 동작하도록 repository 경계에서 null 로 통일한다. */
    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
