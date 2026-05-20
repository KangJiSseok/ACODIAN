package com.ibank.axwms.domain.worklog.event;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;

import com.ibank.axwms.domain.worklog.repository.jooq.projection.WorklogDependencyReadyParentProjection;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class WorklogCompletionEventHandlerTest {

    @Mock
    private WorklogDependencyReadyParentFinder readyParentFinder;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private WorklogCompletionEventHandler handler;

    @Test
    @DisplayName("완료 이벤트의 ready parent snapshot 을 notification 이벤트로 발행한다")
    void 완료_이벤트의_ready_parent_snapshot을_notification_이벤트로_발행한다() {
        // given
        given(readyParentFinder.findReadyParents(501L)).willReturn(List.of(new WorklogDependencyReadyParentProjection(
                701L,
                101L,
                21L,
                3L,
                "물류혁신TF",
                "부모 업무"
        )));

        // when
        handler.handle(new WorklogCompletedEvent(501L));

        // then
        verify(applicationEventPublisher).publishEvent(new WorklogDependencyReadyEvent(
                501L,
                701L,
                101L,
                21L,
                3L,
                "물류혁신TF",
                "부모 업무"
        ));
    }

    @Test
    @DisplayName("ready parent 조회 실패는 업무 완료 트랜잭션 후속 흐름으로 전파하지 않는다")
    void ready_parent_조회_실패는_업무_완료_트랜잭션_후속_흐름으로_전파하지_않는다() {
        // given
        given(readyParentFinder.findReadyParents(501L)).willThrow(new RuntimeException("query failed"));

        // when & then
        assertThatCode(() -> handler.handle(new WorklogCompletedEvent(501L))).doesNotThrowAnyException();
        verify(applicationEventPublisher, never()).publishEvent(any(Object.class));
    }
}
