package com.ibank.axwms.domain.notification.repository;

import com.ibank.axwms.domain.notification.entity.Notification;
import com.ibank.axwms.domain.notification.repository.jooq.NotificationJooqRepository;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long>, NotificationJooqRepository {

    /**
     * 현재 사용자의 알림만 변경 대상으로 노출해 다른 사용자의 알림 존재 여부가 응답 차이로 새지 않게 한다.
     */
    Optional<Notification> findByIdAndUserId(Long id, Long userId);

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
