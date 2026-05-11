package com.ibank.axwms.domain.notification.repository.jooq;

import com.ibank.axwms.domain.notification.repository.jooq.projection.WorklogDueSoonReminderCandidateProjection;

import java.time.LocalDate;
import java.util.List;

import com.ibank.axwms.domain.notification.repository.jooq.projection.NotificationSearchProjection;
import com.ibank.axwms.domain.notification.repository.jooq.query.NotificationSearchQuery;
import org.springframework.data.domain.Page;

public interface NotificationJooqRepository {

    /**
     * 하루 1회 실행 전제의 스케줄러/저장 단계로 넘기기 전, 지정 마감일의 업무 후보를 반환한다.
     */
    List<WorklogDueSoonReminderCandidateProjection> findWorklogDueSoonReminderCandidates(LocalDate targetDueDate);

    /** 현재 사용자가 수신자인 알림 목록을 필터와 pagination 조건에 맞춰 조회한다. */
    Page<NotificationSearchProjection> searchNotifications(Long userId, NotificationSearchQuery query);
}
