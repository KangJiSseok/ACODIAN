package com.ibank.axwms.domain.notification.controller;

import com.ibank.axwms.domain.notification.dto.MarkAllNotificationsReadApiDto;
import com.ibank.axwms.domain.notification.dto.SearchNotificationsApiDto;
import com.ibank.axwms.global.response.PageResponse;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "Notification", description = "알림 API")
public interface NotificationControllerDocs {

    @Operation(summary = "내 알림 SSE 구독",
            description = "현재 로그인 사용자의 새 알림을 Server-Sent Events 로 실시간 수신한다. "
                    + "재접속 replay 와 누락 복구는 이 엔드포인트의 책임이 아니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "SSE 연결이 열린다."),
            @ApiResponse(responseCode = "401", description = "인증이 필요하다.", content = @Content),
            @ApiResponse(responseCode = "403", description = "알림 구독 권한이 없다.", content = @Content)
    })
    SseEmitter subscribeNotificationStream(@Parameter(hidden = true) CustomUserPrincipal principal);

    @Operation(summary = "내 알림 목록 조회",
            description = "현재 로그인 사용자의 알림을 읽음 상태, 부서 ID, 팀 ID 조건으로 필터링해 페이지네이션으로 조회한다. "
                    + "isRead 가 null 이면 읽음/안읽음 전체를 반환한다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "내 알림 목록을 반환한다."),
            @ApiResponse(responseCode = "400", description = "요청 값이 올바르지 않다.", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증이 필요하다.", content = @Content),
            @ApiResponse(responseCode = "403", description = "알림 조회 권한이 없다.", content = @Content)
    })
    PageResponse<SearchNotificationsApiDto.Response.Item> searchNotifications(
            @Parameter(hidden = true) CustomUserPrincipal principal,
            @ParameterObject SearchNotificationsApiDto.Request request
    );

    @Operation(summary = "내 알림 전체 읽음 처리",
            description = "현재 로그인 사용자의 안읽은 알림을 모두 읽음 상태로 변경하고, 실제 변경된 건수를 반환한다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "안읽은 알림 전체 읽음 처리를 완료한다."),
            @ApiResponse(responseCode = "401", description = "인증이 필요하다.", content = @Content),
            @ApiResponse(responseCode = "403", description = "알림 변경 권한이 없다.", content = @Content)
    })
    MarkAllNotificationsReadApiDto.Response markAllNotificationsRead(
            @Parameter(hidden = true) CustomUserPrincipal principal
    );
}
