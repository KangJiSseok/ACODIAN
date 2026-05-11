package com.ibank.axwms.domain.notification.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.ibank.axwms.domain.notification.dto.MarkAllNotificationsReadApiDto;
import com.ibank.axwms.domain.notification.dto.SearchNotificationsApiDto;
import com.ibank.axwms.domain.notification.service.NotificationService;
import com.ibank.axwms.global.response.PageResponse;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    @Test
    @DisplayName("내 알림 목록 조회 메서드는 인증 사용자와 요청을 서비스에 위임한다")
    void 내_알림_목록_조회_메서드는_인증_사용자와_요청을_서비스에_위임한다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(101L, "user@ibank.com", "MEMBER");
        SearchNotificationsApiDto.Request request = new SearchNotificationsApiDto.Request(null, null, null, 1, 20);
        PageResponse<SearchNotificationsApiDto.Response.Item> serviceResponse = response();
        given(notificationService.searchNotifications(principal, request)).willReturn(serviceResponse);

        PageResponse<SearchNotificationsApiDto.Response.Item> response =
                notificationController.searchNotifications(principal, request);

        then(notificationService).should().searchNotifications(principal, request);
        assertThat(response).isSameAs(serviceResponse);
    }

    @Test
    @DisplayName("내 알림 전체 읽음 처리 메서드는 인증 사용자를 서비스에 위임한다")
    void 내_알림_전체_읽음_처리_메서드는_인증_사용자를_서비스에_위임한다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(101L, "user@ibank.com", "MEMBER");
        MarkAllNotificationsReadApiDto.Response serviceResponse = new MarkAllNotificationsReadApiDto.Response(3);
        given(notificationService.markAllNotificationsRead(principal)).willReturn(serviceResponse);

        MarkAllNotificationsReadApiDto.Response response = notificationController.markAllNotificationsRead(principal);

        then(notificationService).should().markAllNotificationsRead(principal);
        assertThat(response).isSameAs(serviceResponse);
    }

    private PageResponse<SearchNotificationsApiDto.Response.Item> response() {
        return new PageResponse<>(
                List.of(new SearchNotificationsApiDto.Response.Item(
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
                )),
                1,
                20,
                1,
                1,
                true,
                true,
                false,
                false
        );
    }
}
