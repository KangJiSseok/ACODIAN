package com.ibank.axwms.domain.worklog.policy;

import com.ibank.axwms.domain.worklog.WorklogStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class WorklogStatusPolicy {

    private static final Map<WorklogStatus, Set<WorklogStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(WorklogStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(WorklogStatus.PENDING, EnumSet.of(WorklogStatus.IN_PROGRESS));
        ALLOWED_TRANSITIONS.put(WorklogStatus.IN_PROGRESS, EnumSet.of(
                WorklogStatus.COMPLETED,
                WorklogStatus.ON_HOLD,
                WorklogStatus.CANCELLED
        ));
        ALLOWED_TRANSITIONS.put(WorklogStatus.ON_HOLD, EnumSet.of(WorklogStatus.IN_PROGRESS));
        ALLOWED_TRANSITIONS.put(WorklogStatus.COMPLETED, EnumSet.noneOf(WorklogStatus.class));
        ALLOWED_TRANSITIONS.put(WorklogStatus.CANCELLED, EnumSet.noneOf(WorklogStatus.class));
    }

    /**
     * 수정 API 가 프론트 상태 옵션과 같은 서버 측 gate 를 적용할 수 있도록 현재 상태에서 다음 상태가 가능한지 판단한다.
     */
    public boolean canTransition(WorklogStatus currentStatus, WorklogStatus nextStatus) {
        if (currentStatus == null || nextStatus == null) {
            return true;
        }
        if (currentStatus == nextStatus) {
            return true;
        }
        return ALLOWED_TRANSITIONS
                .getOrDefault(currentStatus, Set.of())
                .contains(nextStatus);
    }
}
