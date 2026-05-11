package com.ibank.axwms.domain.notification.service;

import com.ibank.axwms.domain.notification.dto.SearchNotificationsApiDto;
import com.ibank.axwms.domain.notification.repository.NotificationRepository;
import com.ibank.axwms.domain.notification.repository.jooq.query.NotificationSearchQuery;
import com.ibank.axwms.global.response.PageResponse;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;

    /**
     * 현재 로그인 사용자가 수신자인 알림만 조회한다.
     * isRead 가 null 이면 읽음 상태 조건을 걸지 않고, true/false 일 때만 해당 상태로 좁힌다.
     */
    public PageResponse<SearchNotificationsApiDto.Response.Item> searchNotifications(CustomUserPrincipal principal, SearchNotificationsApiDto.Request request) {
        NotificationSearchQuery query = NotificationSearchQuery.from(request);
        return SearchNotificationsApiDto.Response.fromPage(
                notificationRepository.searchNotifications(principal.userId(), query)
        );
    }
}
