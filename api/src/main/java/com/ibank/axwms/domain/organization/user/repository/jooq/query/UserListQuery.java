package com.ibank.axwms.domain.organization.user.repository.jooq.query;

import com.ibank.axwms.domain.organization.user.EmploymentStatus;
import com.ibank.axwms.domain.organization.user.dto.GetUsersApiDto;

/** 사용자 목록 repository 조회에 필요한 optional filter 값을 정규화한 query 이다. */
public record UserListQuery(
        String userName,
        Long departmentId,
        String positionName,
        EmploymentStatus employmentStatus
) {

    /** API 요청 DTO 의 null 요청을 빈 필터 query 로 변환한다. */
    public static UserListQuery from(GetUsersApiDto.Request request) {
        if (request == null) {
            return new UserListQuery(null, null, null, null);
        }
        return new UserListQuery(
                blankToNull(request.userName()),
                request.departmentId(),
                blankToNull(request.positionName()),
                request.employmentStatus()
        );
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
