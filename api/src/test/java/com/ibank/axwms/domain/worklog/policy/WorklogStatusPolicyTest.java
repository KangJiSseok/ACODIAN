package com.ibank.axwms.domain.worklog.policy;

import com.ibank.axwms.domain.worklog.WorklogStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WorklogStatusPolicyTest {

    private final WorklogStatusPolicy policy = new WorklogStatusPolicy();

    @Test
    @DisplayName("프론트 수정 화면에서 제공하는 상태 전이를 서버에서도 허용한다")
    void allows_frontend_status_transitions() {
        assertThat(policy.canTransition(WorklogStatus.PENDING, WorklogStatus.IN_PROGRESS)).isTrue();
        assertThat(policy.canTransition(WorklogStatus.IN_PROGRESS, WorklogStatus.COMPLETED)).isTrue();
        assertThat(policy.canTransition(WorklogStatus.IN_PROGRESS, WorklogStatus.ON_HOLD)).isTrue();
        assertThat(policy.canTransition(WorklogStatus.IN_PROGRESS, WorklogStatus.CANCELLED)).isTrue();
        assertThat(policy.canTransition(WorklogStatus.ON_HOLD, WorklogStatus.IN_PROGRESS)).isTrue();
    }

    @Test
    @DisplayName("완료 또는 취소 상태와 건너뛰는 전이는 서버에서 거부한다")
    void rejects_terminal_or_skipped_status_transitions() {
        assertThat(policy.canTransition(WorklogStatus.PENDING, WorklogStatus.COMPLETED)).isFalse();
        assertThat(policy.canTransition(WorklogStatus.COMPLETED, WorklogStatus.IN_PROGRESS)).isFalse();
        assertThat(policy.canTransition(WorklogStatus.CANCELLED, WorklogStatus.IN_PROGRESS)).isFalse();
    }
}
