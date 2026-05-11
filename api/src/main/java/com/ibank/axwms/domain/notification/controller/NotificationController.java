package com.ibank.axwms.domain.notification.controller;

import com.ibank.axwms.domain.notification.dto.MarkAllNotificationsReadApiDto;
import com.ibank.axwms.domain.notification.dto.SearchNotificationsApiDto;
import com.ibank.axwms.domain.notification.service.NotificationService;
import com.ibank.axwms.domain.notification.sse.NotificationSseEmitterRegistry;
import com.ibank.axwms.global.response.PageResponse;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController implements NotificationControllerDocs {

    private final NotificationService notificationService;
    private final NotificationSseEmitterRegistry notificationSseEmitterRegistry;

    @Override
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('DIRECTOR','DEPT_HEAD','TEAM_LEAD','MEMBER')")
    public SseEmitter subscribeNotificationStream(@AuthenticationPrincipal CustomUserPrincipal principal) {
        return notificationSseEmitterRegistry.register(principal.userId());
    }

    @Override
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('DIRECTOR','DEPT_HEAD','TEAM_LEAD','MEMBER')")
    public PageResponse<SearchNotificationsApiDto.Response.Item> searchNotifications(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @ModelAttribute SearchNotificationsApiDto.Request request
    ) {
        return notificationService.searchNotifications(principal, request);
    }

    @Override
    @PatchMapping("/me/read-all")
    @PreAuthorize("hasAnyRole('DIRECTOR','DEPT_HEAD','TEAM_LEAD','MEMBER')")
    public MarkAllNotificationsReadApiDto.Response markAllNotificationsRead(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return notificationService.markAllNotificationsRead(principal);
    }
}
