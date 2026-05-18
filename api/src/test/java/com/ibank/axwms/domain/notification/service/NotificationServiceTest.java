package com.ibank.axwms.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.ibank.axwms.domain.notification.dto.MarkAllNotificationsReadApiDto;
import com.ibank.axwms.domain.notification.dto.SearchNotificationsApiDto;
import com.ibank.axwms.domain.notification.entity.Notification;
import com.ibank.axwms.domain.notification.repository.NotificationRepository;
import com.ibank.axwms.domain.notification.repository.jooq.projection.NotificationSearchProjection;
import com.ibank.axwms.domain.notification.repository.jooq.query.NotificationSearchQuery;
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import com.ibank.axwms.global.response.PageResponse;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    @DisplayName("searchNotifications 는 principal userId 와 필터 query 로 알림 페이지를 조회한다")
    void searchNotifications_는_principal_userId와_필터_query로_알림_페이지를_조회한다() {
        CustomUserPrincipal principal = principal();
        SearchNotificationsApiDto.Request request = new SearchNotificationsApiDto.Request(false, 3L, 21L, 2, 10);
        NotificationSearchQuery expectedQuery = NotificationSearchQuery.from(request);
        given(notificationRepository.searchNotifications(101L, expectedQuery)).willReturn(new PageImpl<>(
                List.of(projection()),
                PageRequest.of(1, 10),
                11
        ));

        PageResponse<SearchNotificationsApiDto.Response.Item> response =
                notificationService.searchNotifications(principal, request);

        assertThat(response.page()).isEqualTo(2);
        assertThat(response.pageSize()).isEqualTo(10);
        assertThat(response.totalCount()).isEqualTo(11);
        assertThat(response.items()).singleElement()
                .extracting(
                        SearchNotificationsApiDto.Response.Item::notificationId,
                        SearchNotificationsApiDto.Response.Item::title,
                        SearchNotificationsApiDto.Response.Item::isRead,
                        SearchNotificationsApiDto.Response.Item::departmentId,
                        SearchNotificationsApiDto.Response.Item::teamId
                )
                .containsExactly(1001L, "새 업무 등록", false, 3L, 21L);
        then(notificationRepository).should().searchNotifications(101L, expectedQuery);
    }

    @Test
    @DisplayName("searchNotifications 는 null 요청이면 전체 읽음 상태와 기본 페이지 값으로 조회한다")
    void searchNotifications_는_null_요청이면_전체_읽음_상태와_기본_페이지_값으로_조회한다() {
        CustomUserPrincipal principal = principal();
        given(notificationRepository.searchNotifications(eq(101L), any()))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        notificationService.searchNotifications(principal, null);

        ArgumentCaptor<NotificationSearchQuery> queryCaptor = ArgumentCaptor.forClass(NotificationSearchQuery.class);
        then(notificationRepository).should().searchNotifications(eq(101L), queryCaptor.capture());
        assertThat(queryCaptor.getValue().isRead()).isNull();
        assertThat(queryCaptor.getValue().departmentId()).isNull();
        assertThat(queryCaptor.getValue().teamId()).isNull();
        assertThat(queryCaptor.getValue().page()).isEqualTo(1);
        assertThat(queryCaptor.getValue().pageSize()).isEqualTo(20);
        assertThat(queryCaptor.getValue().pageIndex()).isZero();
    }

    @Test
    @DisplayName("markNotificationAsRead 는 현재 사용자 알림을 읽음 상태로 변경한다")
    void markNotificationAsRead_는_현재_사용자_알림을_읽음_상태로_변경한다() {
        CustomUserPrincipal principal = principal();
        Notification notification = unreadNotification();
        given(notificationRepository.findByIdAndUserId(1001L, 101L)).willReturn(Optional.of(notification));

        notificationService.markNotificationAsRead(principal, 1001L);

        assertThat(notification.getIsRead()).isTrue();
        assertThat(notification.getReadAt()).isNotNull();
        then(notificationRepository).should().findByIdAndUserId(1001L, 101L);
    }

    @Test
    @DisplayName("markNotificationAsRead 는 이미 읽은 알림의 readAt 을 유지한다")
    void markNotificationAsRead_는_이미_읽은_알림의_readAt을_유지한다() {
        CustomUserPrincipal principal = principal();
        LocalDateTime firstReadAt = LocalDateTime.of(2026, 5, 11, 9, 30);
        Notification notification = readNotification(firstReadAt);
        given(notificationRepository.findByIdAndUserId(1001L, 101L)).willReturn(Optional.of(notification));

        notificationService.markNotificationAsRead(principal, 1001L);

        assertThat(notification.getIsRead()).isTrue();
        assertThat(notification.getReadAt()).isEqualTo(firstReadAt);
    }

    @Test
    @DisplayName("markNotificationAsRead 는 알림이 없거나 소유자가 다르면 NOTIFICATION_NOT_FOUND 예외를 던진다")
    void markNotificationAsRead_는_알림이_없거나_소유자가_다르면_NOTIFICATION_NOT_FOUND_예외를_던진다() {
        CustomUserPrincipal principal = principal();
        given(notificationRepository.findByIdAndUserId(1001L, 101L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markNotificationAsRead(principal, 1001L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND);
    }

    @Test
    @DisplayName("markAllNotificationsRead 는 principal userId 로 안읽은 알림 전체 읽음 처리를 위임한다")
    void markAllNotificationsRead_는_principal_userId로_안읽은_알림_전체_읽음_처리를_위임한다() {
        CustomUserPrincipal principal = principal();
        given(notificationRepository.markUnreadAsReadByUserId(eq(101L), any(LocalDateTime.class))).willReturn(3);

        MarkAllNotificationsReadApiDto.Response response = notificationService.markAllNotificationsRead(principal);

        ArgumentCaptor<LocalDateTime> readAtCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        then(notificationRepository).should().markUnreadAsReadByUserId(eq(101L), readAtCaptor.capture());
        assertThat(response.updatedCount()).isEqualTo(3);
        assertThat(readAtCaptor.getValue()).isNotNull();
    }

    @Test
    @DisplayName("deleteExpiredReadNotifications 는 기준 시각 3일 전까지 읽은 알림 삭제를 위임한다")
    void deleteExpiredReadNotifications_는_기준_시각_3일_전까지_읽은_알림_삭제를_위임한다() {
        LocalDateTime now = LocalDateTime.of(2026, 5, 18, 9, 30);
        given(notificationRepository.deleteReadNotificationsReadAtBeforeOrEqual(
                LocalDateTime.of(2026, 5, 15, 9, 30)
        )).willReturn(2);

        int deletedCount = notificationService.deleteExpiredReadNotifications(now);

        assertThat(deletedCount).isEqualTo(2);
        then(notificationRepository).should()
                .deleteReadNotificationsReadAtBeforeOrEqual(LocalDateTime.of(2026, 5, 15, 9, 30));
    }

    private CustomUserPrincipal principal() {
        return new CustomUserPrincipal(101L, "user@ibank.com", "MEMBER");
    }

    private Notification unreadNotification() {
        return Notification.create(
                101L,
                3L,
                21L,
                "WORKLOG_CREATED",
                "새 업무 등록",
                "확인할 업무가 있습니다.",
                "WORKLOG",
                501L,
                false,
                null
        );
    }

    private Notification readNotification(LocalDateTime readAt) {
        return Notification.create(
                101L,
                3L,
                21L,
                "WORKLOG_CREATED",
                "새 업무 등록",
                "확인할 업무가 있습니다.",
                "WORKLOG",
                501L,
                true,
                readAt
        );
    }

    private NotificationSearchProjection projection() {
        return new NotificationSearchProjection(
                1001L,
                "WORKLOG_CREATED",
                "새 업무 등록",
                "확인할 업무가 있습니다.",
                "WORKLOG",
                501L,
                3L,
                21L,
                false,
                null,
                LocalDateTime.of(2026, 5, 11, 9, 0)
        );
    }
}
