package com.ibank.axwms.domain.organization.department.repository.jooq.projection;

import java.time.LocalDateTime;

/** 활성 부서 목록 한 행에 필요한 필드만 담는 읽기 전용 projection 이다. */
public record DepartmentListItemProjection(
        Long departmentId,
        String departmentName,
        String description,
        Long departmentHeadUserId,
        String departmentHeadUserName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
