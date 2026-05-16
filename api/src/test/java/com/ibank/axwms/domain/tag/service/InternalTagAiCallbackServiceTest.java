package com.ibank.axwms.domain.tag.service;

import com.ibank.axwms.domain.tag.entity.MetaTag;
import com.ibank.axwms.domain.tag.repository.TagRepository;
import com.ibank.axwms.domain.worklog.WorklogImportance;
import com.ibank.axwms.domain.worklog.WorklogStatus;
import com.ibank.axwms.domain.worklog.dto.ApplyWorklogAiTagsApiDto;
import com.ibank.axwms.domain.worklog.entity.Worklog;
import com.ibank.axwms.domain.worklog.entity.WorklogTag;
import com.ibank.axwms.domain.worklog.repository.WorklogRepository;
import com.ibank.axwms.domain.worklog.repository.WorklogTagRepository;
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InternalTagAiCallbackServiceTest {

    private static final Long WORKLOG_ID = 501L;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private WorklogRepository worklogRepository;

    @Mock
    private WorklogTagRepository worklogTagRepository;

    @Mock
    private TagService tagService;

    @InjectMocks
    private InternalTagAiCallbackService internalTagAiCallbackService;

    @Test
    @DisplayName("AI 태그 반영 시 신규 태그를 생성하고 새로 연결된 태그만 사용 횟수를 증가시킨다")
    void AI_태그_반영_시_신규_태그를_생성하고_새로_연결된_태그만_사용_횟수를_증가시킨다() {
        ApplyWorklogAiTagsApiDto.Request request = new ApplyWorklogAiTagsApiDto.Request(
                List.of(1L, 2L),
                List.of(" 재고 ", "재고")
        );
        MetaTag createdTag = createMetaTag(3L, "재고");
        WorklogTag existingLink = createWorklogTag(WORKLOG_ID, 2L);
        WorklogTag savedLink1 = createWorklogTag(WORKLOG_ID, 1L);
        WorklogTag savedLink3 = createWorklogTag(WORKLOG_ID, 3L);
        given(worklogRepository.findById(WORKLOG_ID)).willReturn(Optional.of(createWorklog()));
        given(tagService.normalizeExistingTagIds(List.of(1L, 2L))).willReturn(List.of(1L, 2L));
        given(tagRepository.findAllByTagNameIn(List.of("재고"))).willReturn(List.of(createdTag));
        given(worklogTagRepository.findByWorklogIdAndTagIdIn(WORKLOG_ID, List.of(1L, 2L, 3L)))
                .willReturn(List.of(existingLink));
        given(worklogTagRepository.saveAll(org.mockito.ArgumentMatchers.<List<WorklogTag>>any()))
                .willReturn(List.of(savedLink1, savedLink3));

        internalTagAiCallbackService.applyAiGeneratedTags(WORKLOG_ID, request);

        verify(tagRepository).insertAiGeneratedTagNamesIgnoreDuplicates(List.of("재고"));
        verify(worklogTagRepository).saveAll(org.mockito.ArgumentMatchers.<List<WorklogTag>>any());
        verify(tagService).incrementUsageCountByIds(List.of(1L, 3L));
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("내부 태그 생성 요청이면 신규 태그를 만들고 업무일지에 연결한다")
    void 내부_태그_생성_요청이면_신규_태그를_만들고_업무일지에_연결한다() {
        ApplyWorklogAiTagsApiDto.Request request = new ApplyWorklogAiTagsApiDto.Request(
                List.of(1L),
                List.of(" 배치자동화 ")
        );
        MetaTag createdTag = createMetaTag(3L, "배치자동화");
        WorklogTag savedLink1 = createWorklogTag(WORKLOG_ID, 1L);
        WorklogTag savedLink3 = createWorklogTag(WORKLOG_ID, 3L);
        ArgumentCaptor<List<WorklogTag>> linkCaptor = ArgumentCaptor.forClass(List.class);
        given(worklogRepository.findById(WORKLOG_ID)).willReturn(Optional.of(createWorklog()));
        given(tagService.normalizeExistingTagIds(List.of(1L))).willReturn(List.of(1L));
        given(tagRepository.findAllByTagNameIn(List.of("배치자동화"))).willReturn(List.of(createdTag));
        given(worklogTagRepository.findByWorklogIdAndTagIdIn(WORKLOG_ID, List.of(1L, 3L)))
                .willReturn(List.of());
        given(worklogTagRepository.saveAll(org.mockito.ArgumentMatchers.<List<WorklogTag>>any()))
                .willReturn(List.of(savedLink1, savedLink3));

        internalTagAiCallbackService.applyAiGeneratedTags(WORKLOG_ID, request);

        verify(tagRepository).insertAiGeneratedTagNamesIgnoreDuplicates(List.of("배치자동화"));
        verify(worklogTagRepository).saveAll(linkCaptor.capture());
        assertThat(linkCaptor.getValue())
                .extracting(WorklogTag::getTagId)
                .containsExactly(1L, 3L);
        verify(tagService).incrementUsageCountByIds(List.of(1L, 3L));
    }

    @Test
    @DisplayName("AI 태그 반영 대상 업무일지가 없으면 태그 생성과 연결을 수행하지 않는다")
    void AI_태그_반영_대상_업무일지가_없으면_태그_생성과_연결을_수행하지_않는다() {
        ApplyWorklogAiTagsApiDto.Request request = new ApplyWorklogAiTagsApiDto.Request(
                List.of(1L),
                List.of("재고")
        );
        given(worklogRepository.findById(WORKLOG_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> internalTagAiCallbackService.applyAiGeneratedTags(WORKLOG_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.WORKLOG_NOT_FOUND);

        verify(tagRepository, never()).insertAiGeneratedTagNamesIgnoreDuplicates(org.mockito.ArgumentMatchers.any());
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
        return WorklogTag.create(worklogId, tagId);
    }
}
