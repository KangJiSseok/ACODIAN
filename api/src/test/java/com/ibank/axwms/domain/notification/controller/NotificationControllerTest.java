package com.ibank.axwms.domain.notification.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.ibank.axwms.domain.notification.dto.MarkAllNotificationsReadApiDto;
import com.ibank.axwms.domain.notification.dto.SearchNotificationsApiDto;
import com.ibank.axwms.domain.notification.service.NotificationService;
import com.ibank.axwms.domain.notification.sse.NotificationSseEmitterRegistry;
import com.ibank.axwms.global.response.EmptyResponse;
import com.ibank.axwms.global.response.PageResponse;
import com.ibank.axwms.global.response.ResponseEnvelope;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private NotificationSseEmitterRegistry notificationSseEmitterRegistry;

    @InjectMocks
    private NotificationController notificationController;

    @Test
    @DisplayName("알림 SSE 구독 메서드는 /stream text/event-stream 매핑을 사용한다")
    void 알림_SSE_구독_메서드는_stream_text_event_stream_매핑을_사용한다() throws NoSuchMethodException {
        Method method = NotificationController.class.getMethod("subscribeNotificationStream", CustomUserPrincipal.class);
        GetMapping getMapping = method.getAnnotation(GetMapping.class);

        assertThat(getMapping).isNotNull();
        assertThat(getMapping.value()).containsExactly("/stream");
        assertThat(getMapping.produces()).containsExactly(MediaType.TEXT_EVENT_STREAM_VALUE);
    }

    @Test
    @DisplayName("알림 SSE 구독 메서드는 인증 사용자 role gate 를 선언한다")
    void 알림_SSE_구독_메서드는_인증_사용자_role_gate를_선언한다() throws NoSuchMethodException {
        Method method = NotificationController.class.getMethod("subscribeNotificationStream", CustomUserPrincipal.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).contains("MEMBER");
    }

    @Test
    @DisplayName("알림 SSE 구독 응답은 공통 JSON 봉투 래핑 대상에서 제외된다")
    void 알림_SSE_구독_응답은_공통_JSON_봉투_래핑_대상에서_제외된다() throws NoSuchMethodException {
        Method method = NotificationController.class.getMethod("subscribeNotificationStream", CustomUserPrincipal.class);

        boolean shouldWrap = ResponseEnvelope.shouldWrap(new MethodParameter(method, -1));

        assertThat(shouldWrap).isFalse();
    }

    @Test
    @DisplayName("알림 SSE 구독 메서드는 인증 사용자 id 로 registry 에 연결을 등록한다")
    void 알림_SSE_구독_메서드는_인증_사용자_id로_registry에_연결을_등록한다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(101L, "user@ibank.com", "MEMBER");
        SseEmitter emitter = new SseEmitter(30_000L);
        given(notificationSseEmitterRegistry.register(101L)).willReturn(emitter);

        SseEmitter response = notificationController.subscribeNotificationStream(principal);

        then(notificationSseEmitterRegistry).should().register(101L);
        assertThat(response).isSameAs(emitter);
    }

    @Test
    @DisplayName("알림 읽음 처리 메서드는 /{id}/read PATCH 매핑을 사용한다")
    void 알림_읽음_처리_메서드는_id_read_PATCH_매핑을_사용한다() throws NoSuchMethodException {
        Method method = NotificationController.class.getMethod(
                "markNotificationAsRead",
                CustomUserPrincipal.class,
                Long.class
        );
        PatchMapping patchMapping = method.getAnnotation(PatchMapping.class);

        assertThat(patchMapping).isNotNull();
        assertThat(patchMapping.value()).containsExactly("/{id}/read");
    }

    @Test
    @DisplayName("알림 읽음 처리 메서드는 인증 사용자 role gate 를 선언한다")
    void 알림_읽음_처리_메서드는_인증_사용자_role_gate를_선언한다() throws NoSuchMethodException {
        Method method = NotificationController.class.getMethod(
                "markNotificationAsRead",
                CustomUserPrincipal.class,
                Long.class
        );
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).contains("MEMBER");
    }

    @Test
    @DisplayName("알림 읽음 처리 메서드는 서비스를 호출하고 EmptyResponse 를 반환한다")
    void 알림_읽음_처리_메서드는_서비스를_호출하고_EmptyResponse를_반환한다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(101L, "user@ibank.com", "MEMBER");

        EmptyResponse response = notificationController.markNotificationAsRead(principal, 1001L);

        then(notificationService).should().markNotificationAsRead(principal, 1001L);
        assertThat(response).isSameAs(EmptyResponse.INSTANCE);
    }

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
