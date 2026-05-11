package com.ibank.axwms.domain.notification.entity;

import com.ibank.axwms.domain.notification.NotificationReferenceType;
import com.ibank.axwms.domain.notification.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_notification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "team_id")
    private Long teamId;

    @Column(name = "notification_type", nullable = false, length = 50)
    private String notificationType;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * D-3 업무 알림의 중복 방지 identity 가 엔티티 생성 시점부터 같은 값으로 저장되게 한다.
     */
    public static Notification createWorklogDueSoonReminder(Long userId,
                                                            Long departmentId,
                                                            Long teamId,
                                                            Long worklogId,
                                                            String title,
                                                            String content) {
        Notification notification = new Notification();
        notification.userId = userId;
        notification.departmentId = departmentId;
        notification.teamId = teamId;
        notification.notificationType = NotificationType.WORKLOG_DUE_SOON.name();
        notification.title = title;
        notification.content = content;
        notification.referenceType = NotificationReferenceType.WORKLOG.name();
        notification.referenceId = worklogId;
        notification.isRead = Boolean.FALSE;
        notification.readAt = null;
        return notification;
    }

    /**
     * 신규 알림을 생성한다.
     * 읽음 상태가 생략되면 DB 기본값과 같은 false 로 보정하고, 읽지 않은 알림이면 readAt 은 null 로 전달한다.
     */
    public static Notification create(Long userId,
                                      Long departmentId,
                                      Long teamId,
                                      String notificationType,
                                      String title,
                                      String content,
                                      String referenceType,
                                      Long referenceId,
                                      Boolean isRead,
                                      LocalDateTime readAt) {
        Notification notification = new Notification();
        notification.userId = userId;
        notification.departmentId = departmentId;
        notification.teamId = teamId;
        notification.notificationType = notificationType;
        notification.title = title;
        notification.content = content;
        notification.referenceType = referenceType;
        notification.referenceId = referenceId;
        notification.isRead = Boolean.TRUE.equals(isRead);
        notification.readAt = readAt;
        return notification;
    }
}
