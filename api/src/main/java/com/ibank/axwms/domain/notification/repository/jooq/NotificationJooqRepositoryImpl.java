package com.ibank.axwms.domain.notification.repository.jooq;

import static com.ibank.axwms.global.jooq.Tables.TB_NOTIFICATION;

import com.ibank.axwms.domain.notification.repository.jooq.projection.NotificationSearchProjection;
import com.ibank.axwms.domain.notification.repository.jooq.query.NotificationSearchQuery;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class NotificationJooqRepositoryImpl implements NotificationJooqRepository {

    private final DSLContext dsl;

    /** 현재 사용자의 알림 목록을 최신 알림 우선 정렬과 선택 필터로 조회한다. */
    @Override
    public Page<NotificationSearchProjection> searchNotifications(Long userId, NotificationSearchQuery query) {
        int pageIndex = query.pageIndex();
        int pageSize = query.pageSize();
        PageRequest pageRequest = PageRequest.of(pageIndex, pageSize);

        Condition condition = searchCondition(userId, query);
        long total = dsl.selectCount()
                .from(TB_NOTIFICATION)
                .where(condition)
                .fetchSingle(0, Integer.class)
                .longValue();

        if (total == 0) {
            return new PageImpl<>(List.of(), pageRequest, 0);
        }

        List<NotificationSearchProjection> items = dsl.select(
                        TB_NOTIFICATION.NOTIFICATION_ID,
                        TB_NOTIFICATION.NOTIFICATION_TYPE,
                        TB_NOTIFICATION.TITLE,
                        TB_NOTIFICATION.CONTENT,
                        TB_NOTIFICATION.REFERENCE_TYPE,
                        TB_NOTIFICATION.REFERENCE_ID,
                        TB_NOTIFICATION.DEPARTMENT_ID,
                        TB_NOTIFICATION.TEAM_ID,
                        TB_NOTIFICATION.IS_READ,
                        TB_NOTIFICATION.READ_AT,
                        TB_NOTIFICATION.CREATED_AT
                )
                .from(TB_NOTIFICATION)
                .where(condition)
                .orderBy(TB_NOTIFICATION.CREATED_AT.desc(), TB_NOTIFICATION.NOTIFICATION_ID.desc())
                .limit(pageSize)
                .offset((long) pageIndex * pageSize)
                .fetch(NotificationSearchProjection::from);

        return new PageImpl<>(items, pageRequest, total);
    }

    /** 수신자 고정 조건에 선택 필터를 누적해 null 필터는 전체 조회로 둔다. */
    private Condition searchCondition(Long userId, NotificationSearchQuery query) {
        Condition condition = TB_NOTIFICATION.USER_ID.eq(userId);
        if (query.isRead() != null) {
            condition = condition.and(TB_NOTIFICATION.IS_READ.eq(query.isRead()));
        }
        if (query.departmentId() != null) {
            condition = condition.and(TB_NOTIFICATION.DEPARTMENT_ID.eq(query.departmentId()));
        }
        if (query.teamId() != null) {
            condition = condition.and(TB_NOTIFICATION.TEAM_ID.eq(query.teamId()));
        }
        return condition;
    }
}
