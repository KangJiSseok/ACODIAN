package com.ibank.axwms.domain.notification.repository.jooq;

import com.ibank.axwms.domain.notification.repository.jooq.projection.NotificationSearchProjection;
import com.ibank.axwms.domain.notification.repository.jooq.query.NotificationSearchQuery;
import org.springframework.data.domain.Page;

public interface NotificationJooqRepository {

    /** 현재 사용자가 수신자인 알림 목록을 필터와 pagination 조건에 맞춰 조회한다. */
    Page<NotificationSearchProjection> searchNotifications(Long userId, NotificationSearchQuery query);
}
