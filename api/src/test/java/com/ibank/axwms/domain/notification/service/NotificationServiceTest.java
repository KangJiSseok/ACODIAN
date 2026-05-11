package com.ibank.axwms.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.ibank.axwms.domain.notification.dto.SearchNotificationsApiDto;
import com.ibank.axwms.domain.notification.repository.NotificationRepository;
import com.ibank.axwms.domain.notification.repository.jooq.projection.NotificationSearchProjection;
import com.ibank.axwms.domain.notification.repository.jooq.query.NotificationSearchQuery;
import com.ibank.axwms.global.response.PageResponse;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

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

    private CustomUserPrincipal principal() {
        return new CustomUserPrincipal(101L, "user@ibank.com", "MEMBER");
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
