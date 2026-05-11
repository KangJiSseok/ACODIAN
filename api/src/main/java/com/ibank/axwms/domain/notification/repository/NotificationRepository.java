package com.ibank.axwms.domain.notification.repository;

import com.ibank.axwms.domain.notification.entity.Notification;
import com.ibank.axwms.domain.notification.repository.jooq.NotificationJooqRepository;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long>, NotificationJooqRepository {

    /**
     * bulk update 가 영속성 컨텍스트를 우회하므로 변경 직후 조회가 stale entity 를 보지 않도록 자동 flush/clear 한다.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Notification notification
               set notification.isRead = true,
                   notification.readAt = :readAt,
                   notification.updatedAt = :readAt
             where notification.userId = :userId
               and notification.isRead = false
            """)
    int markUnreadAsReadByUserId(@Param("userId") Long userId, @Param("readAt") LocalDateTime readAt);
}
