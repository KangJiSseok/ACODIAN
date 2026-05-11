package com.ibank.axwms.domain.worklog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.ibank.axwms.domain.tag.entity.MetaTag;
import com.ibank.axwms.domain.tag.repository.TagRepository;
import com.ibank.axwms.domain.tag.service.TagService;
import com.ibank.axwms.domain.worklog.WorklogImportance;
import com.ibank.axwms.domain.worklog.WorklogStatus;
import com.ibank.axwms.domain.worklog.dto.ApplyWorklogTagsAiApiDto;
import com.ibank.axwms.domain.worklog.dto.UpdateWorklogAiApiDto;
import com.ibank.axwms.domain.worklog.entity.Worklog;
import com.ibank.axwms.domain.worklog.entity.WorklogTag;
import com.ibank.axwms.domain.worklog.repository.WorklogRepository;
import com.ibank.axwms.domain.worklog.repository.WorklogTagRepository;
import com.ibank.axwms.global.enums.AiProcessingStatus;
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class InternalWorklogAiCallbackServiceTest {

    private static final Long WORKLOG_ID = 501L;
    private static final String AI_SUMMARY = "AI가 업무 내용을 요약한 결과";

    @Mock
    private WorklogRepository worklogRepository;

    @Mock
    private WorklogTagRepository worklogTagRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private TagService tagService;

    @InjectMocks
    private InternalWorklogAiCallbackService internalWorklogAiCallbackService;

    @Test
    @DisplayName("AI 상태가 COMPLETED 이면 업무일지에 요약문과 완료 상태를 반영한다")
    void AI_상태가_completed_이면_업무일지에_요약문과_완료_상태를_반영한다() {
        Worklog worklog = createWorklog();
        UpdateWorklogAiApiDto.Request request = new UpdateWorklogAiApiDto.Request(
                AI_SUMMARY,
                AiProcessingStatus.COMPLETED
        );
        given(worklogRepository.findById(WORKLOG_ID)).willReturn(Optional.of(worklog));

        internalWorklogAiCallbackService.updateAiResult(WORKLOG_ID, request);

        assertThat(worklog.getAiSummary()).isEqualTo(AI_SUMMARY);
        assertThat(worklog.getAiProcessingStatus()).isEqualTo(AiProcessingStatus.COMPLETED);
        assertThat(worklog.getAiSummaryEdited()).isFalse();
    }

    @Test
    @DisplayName("AI 상태가 FAILED 이면 업무일지에 실패 상태만 반영한다")
    void AI_상태가_failed_이면_업무일지에_실패_상태만_반영한다() {
        Worklog worklog = createWorklog();
        UpdateWorklogAiApiDto.Request request = new UpdateWorklogAiApiDto.Request(
                AI_SUMMARY,
                AiProcessingStatus.FAILED
        );
        given(worklogRepository.findById(WORKLOG_ID)).willReturn(Optional.of(worklog));

        internalWorklogAiCallbackService.updateAiResult(WORKLOG_ID, request);

        assertThat(worklog.getAiSummary()).isNull();
        assertThat(worklog.getAiProcessingStatus()).isEqualTo(AiProcessingStatus.FAILED);
        assertThat(worklog.getAiSummaryEdited()).isFalse();
    }

    @Test
    @DisplayName("업무일지가 없으면 WORKLOG_NOT_FOUND 를 던진다")
    void 업무일지가_없으면_worklog_not_found_를_던진다() {
        UpdateWorklogAiApiDto.Request request = new UpdateWorklogAiApiDto.Request(
                AI_SUMMARY,
                AiProcessingStatus.COMPLETED
        );
        given(worklogRepository.findById(WORKLOG_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> internalWorklogAiCallbackService.updateAiResult(WORKLOG_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.WORKLOG_NOT_FOUND);

        verify(worklogRepository).findById(WORKLOG_ID);
        verifyNoMoreInteractions(worklogRepository);
    }

    @Test
    @DisplayName("AI 태그 반영 시 신규 태그를 생성하고 새로 연결된 태그만 사용 횟수를 증가시킨다")
    void AI_태그_반영_시_신규_태그를_생성하고_새로_연결된_태그만_사용_횟수를_증가시킨다() {
        ApplyWorklogTagsAiApiDto.Request request = new ApplyWorklogTagsAiApiDto.Request(
                List.of(1L, 2L),
                List.of(" 재고 ", "재고")
        );
        MetaTag createdTag = createMetaTag(3L, "재고");
        WorklogTag existingLink = createWorklogTag(WORKLOG_ID, 2L);
        WorklogTag savedLink1 = createWorklogTag(WORKLOG_ID, 1L);
        WorklogTag savedLink3 = createWorklogTag(WORKLOG_ID, 3L);
        given(worklogRepository.findById(WORKLOG_ID)).willReturn(Optional.of(createWorklog()));
        given(tagRepository.findAllByTagNameIn(List.of("재고"))).willReturn(List.of());
        given(tagRepository.saveAll(org.mockito.ArgumentMatchers.<List<MetaTag>>any())).willReturn(List.of(createdTag));
        given(worklogTagRepository.findByWorklogIdAndTagIdIn(WORKLOG_ID, List.of(1L, 2L, 3L)))
                .willReturn(List.of(existingLink));
        given(worklogTagRepository.saveAll(org.mockito.ArgumentMatchers.<List<WorklogTag>>any()))
                .willReturn(List.of(savedLink1, savedLink3));

        internalWorklogAiCallbackService.applyAiGeneratedTags(WORKLOG_ID, request);

        verify(tagRepository).saveAll(org.mockito.ArgumentMatchers.<List<MetaTag>>any());
        verify(worklogTagRepository).saveAll(org.mockito.ArgumentMatchers.<List<WorklogTag>>any());
        verify(tagService).incrementUsageCountByIds(List.of(1L, 3L));
    }

    @Test
    @DisplayName("AI 태그 반영 대상 업무일지가 없으면 태그 생성과 연결을 수행하지 않는다")
    void AI_태그_반영_대상_업무일지가_없으면_태그_생성과_연결을_수행하지_않는다() {
        ApplyWorklogTagsAiApiDto.Request request = new ApplyWorklogTagsAiApiDto.Request(
                List.of(1L),
                List.of("재고")
        );
        given(worklogRepository.findById(WORKLOG_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> internalWorklogAiCallbackService.applyAiGeneratedTags(WORKLOG_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.WORKLOG_NOT_FOUND);

        verify(tagRepository, never()).saveAll(org.mockito.ArgumentMatchers.<List<MetaTag>>any());
        verify(worklogTagRepository, never()).saveAll(org.mockito.ArgumentMatchers.<List<WorklogTag>>any());
        verify(tagService, never()).incrementUsageCountByIds(org.mockito.ArgumentMatchers.any());
    }

    private Worklog createWorklog() {
        return Worklog.create(
                101L,
                21L,
                "결산 보고서 작성",
                "재무팀 요청사항 반영",
                "데이터 집계와 초안 작성",
                WorklogStatus.PENDING,
                WorklogImportance.HIGH,
                java.math.BigDecimal.ONE,
                LocalDate.of(2026, 4, 22),
                LocalDate.of(2026, 4, 25)
        );
    }

    private MetaTag createMetaTag(Long tagId, String tagName) {
        MetaTag metaTag = MetaTag.create(tagName);
        ReflectionTestUtils.setField(metaTag, "id", tagId);
        return metaTag;
    }

    private WorklogTag createWorklogTag(Long worklogId, Long tagId) {
        return WorklogTag.createAiGenerated(worklogId, tagId);
    }
}
