package com.ibank.axwms.domain.notification.event;

import com.ibank.axwms.domain.notification.service.NotificationService;
import com.ibank.axwms.domain.worklog.event.WorklogDependencyReadyEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorklogDependencyReadyNotificationEventHandler {

    private final NotificationService notificationService;

    /**
     * worklog 커밋 후 snapshot 이벤트만 소비해 notification 저장 실패가 원 업무 상태 변경을 되돌리지 않게 한다.
     */
    @EventListener
    public void handle(WorklogDependencyReadyEvent event) {
        try {
            notificationService.createWorklogDependencyReadyNotification(
                    event.parentAuthorId(),
                    event.parentDepartmentId(),
                    event.parentTeamId(),
                    event.parentWorklogId(),
                    event.teamName(),
                    event.parentTitle()
            );
        } catch (RuntimeException exception) {
            log.warn(
                    "선행 업무 완료 알림 생성에 실패했습니다. completedWorklogId={}, parentWorklogId={}, recipientUserId={}",
                    event.completedWorklogId(),
                    event.parentWorklogId(),
                    event.parentAuthorId(),
                    exception
            );
        }
    }
}
