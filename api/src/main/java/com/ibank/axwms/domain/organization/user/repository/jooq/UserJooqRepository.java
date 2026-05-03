package com.ibank.axwms.domain.organization.user.repository.jooq;

import com.ibank.axwms.domain.organization.user.repository.jooq.projection.UserSummaryProjection;
import com.ibank.axwms.domain.organization.user.repository.jooq.query.UserListQuery;
import java.util.List;

public interface UserJooqRepository {

    /** 사용자 목록을 필터 조건과 role 고정 정렬 기준으로 조회한다. */
    List<UserSummaryProjection> findUsers(UserListQuery query);
}
