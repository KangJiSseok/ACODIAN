package com.ibank.axwms.domain.notification.repository;

import com.ibank.axwms.domain.notification.entity.Notification;
import com.ibank.axwms.domain.notification.repository.jooq.NotificationJooqRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long>, NotificationJooqRepository {

    /** 같은 업무 reminder 계열 알림 중 가장 먼저 생성된 row 를 갱신 대상으로 선택해 중복 insert 를 막는다. */
    Optional<Notification> findFirstByUserIdAndReferenceTypeAndReferenceIdAndNotificationTypeInOrderByIdAsc(
            Long userId,
            String referenceType,
            Long referenceId,
            List<String> notificationTypes
    );

    /**
     * 같은 부모 업무 준비 알림의 row 와 SSE 재발행을 막기 위해 저장 전 semantic identity 존재 여부를 확인한다.
     */
    boolean existsByUserIdAndReferenceTypeAndReferenceIdAndNotificationType(
            Long userId,
            String referenceType,
            Long referenceId,
            String notificationType
    );

    /**
     * unique race 도 예외 대신 no-op 으로 수렴시켜 같은 부모 준비 알림이 중복 저장되거나 SSE 로 재발행되지 않게 한다.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            insert into tb_notification (
                user_id,
                department_id,
                team_id,
                notification_type,
                title,
                content,
                reference_type,
                reference_id,
                is_read,
                read_at,
                created_at,
                updated_at
            )
            values (
                :userId,
                :departmentId,
                :teamId,
                :notificationType,
                :title,
                :content,
                :referenceType,
                :referenceId,
                false,
                null,
                current_timestamp,
                current_timestamp
            )
            on conflict (user_id, reference_type, reference_id, notification_type)
            where notification_type = 'WORKLOG_DEPENDENCY_READY'
            do nothing
            """, nativeQuery = true)
    int insertWorklogDependencyReadyNotificationIfAbsent(@Param("userId") Long userId,
                                                         @Param("departmentId") Long departmentId,
                                                         @Param("teamId") Long teamId,
                                                         @Param("notificationType") String notificationType,
                                                         @Param("title") String title,
                                                         @Param("content") String content,
                                                         @Param("referenceType") String referenceType,
                                                         @Param("referenceId") Long referenceId);

    /**
     * insert-on-conflict 결과로 실제 생성된 row 의 ID/생성시각을 조회해 SSE snapshot 을 정확한 DB 값으로 만든다.
     */
    Optional<Notification> findFirstByUserIdAndReferenceTypeAndReferenceIdAndNotificationTypeOrderByIdAsc(
            Long userId,
            String referenceType,
            Long referenceId,
            String notificationType
    );

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

    /**
     * 읽은 시점으로부터 보관 기간이 지난 알림만 제거해 안읽은 알림과 readAt 없는 비정상 row 를 보존한다.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            delete from Notification notification
             where notification.isRead = true
               and notification.readAt is not null
               and notification.readAt <= :expiredAt
            """)
    int deleteReadNotificationsReadAtBeforeOrEqual(@Param("expiredAt") LocalDateTime expiredAt);
}
