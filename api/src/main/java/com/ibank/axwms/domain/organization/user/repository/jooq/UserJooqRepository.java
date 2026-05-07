package com.ibank.axwms.domain.organization.user.repository.jooq;

import com.ibank.axwms.domain.organization.user.repository.jooq.projection.UserSummaryProjection;
import com.ibank.axwms.domain.organization.user.repository.jooq.query.UserListQuery;
import org.springframework.data.domain.Page;

public interface UserJooqRepository {

    /** 사용자 목록을 필터 조건, role 고정 정렬 기준, 페이지 요청 기준으로 조회한다. */
    Page<UserSummaryProjection> findUsers(UserListQuery query);
}
