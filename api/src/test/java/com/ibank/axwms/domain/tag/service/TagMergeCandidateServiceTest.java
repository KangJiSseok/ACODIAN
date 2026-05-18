package com.ibank.axwms.domain.tag.service;

import com.ibank.axwms.domain.tag.TagMergeCandidateStatus;
import com.ibank.axwms.domain.tag.dto.TagMergeCandidateApiDto;
import com.ibank.axwms.domain.tag.entity.MetaTag;
import com.ibank.axwms.domain.tag.entity.TagMergeCandidate;
import com.ibank.axwms.domain.tag.entity.TagMergeCandidateItem;
import com.ibank.axwms.domain.tag.external.TagMergeAiClient;
import com.ibank.axwms.domain.tag.repository.TagMergeCandidateItemRepository;
import com.ibank.axwms.domain.tag.repository.TagMergeCandidateRepository;
import com.ibank.axwms.domain.tag.repository.TagRepository;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TagMergeCandidateServiceTest {

    private static final Long MERGE_CANDIDATE_ID = 10L;
    private static final Long TARGET_TAG_ID = 1L;
    private static final Long SOURCE_TAG_ID = 2L;
    private static final Long OTHER_SOURCE_TAG_ID = 3L;

    @Mock
    private TagMergeCandidateRepository tagMergeCandidateRepository;

    @Mock
    private TagMergeCandidateItemRepository tagMergeCandidateItemRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private WorklogTagRepository worklogTagRepository;

    @Mock
    private TagMergeAiClient tagMergeAiClient;

    @InjectMocks
    private TagMergeCandidateService tagMergeCandidateService;

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("AI 병합 후보 저장 시 활성 태그만 snapshot 으로 저장한다")
    void AI_병합_후보_저장_시_활성_태그만_snapshot_으로_저장한다() {
        TagMergeCandidateApiDto.Request request = new TagMergeCandidateApiDto.Request(List.of(
                new TagMergeCandidateApiDto.CandidateItem(
                        new TagMergeCandidateApiDto.TagItem(TARGET_TAG_ID, " 기준정보 ", 10),
                        "기준 관련 태그 설명",
                        List.of(
                                new TagMergeCandidateApiDto.TagItem(SOURCE_TAG_ID, "기준 보정", 4),
                                new TagMergeCandidateApiDto.TagItem(SOURCE_TAG_ID, "기준 보정 중복", 4),
                                new TagMergeCandidateApiDto.TagItem(OTHER_SOURCE_TAG_ID, "운영 기준 보강", 3),
                                new TagMergeCandidateApiDto.TagItem(TARGET_TAG_ID, "기준정보", 10)
                        )
                )
        ));
        given(tagRepository.findAllById(anyCollection()))
                .willReturn(List.of(
                        metaTag(TARGET_TAG_ID, "기준정보", 10, false),
                        metaTag(SOURCE_TAG_ID, "기준 보정", 4, false),
                        metaTag(OTHER_SOURCE_TAG_ID, "운영 기준 보강", 3, false)
                ));
        given(tagMergeCandidateRepository.save(any(TagMergeCandidate.class)))
                .willAnswer(invocation -> {
                    TagMergeCandidate candidate = invocation.getArgument(0);
                    ReflectionTestUtils.setField(candidate, "id", MERGE_CANDIDATE_ID);
                    return candidate;
                });
        given(tagMergeCandidateItemRepository.findByMergeCandidateIdIn(List.of(MERGE_CANDIDATE_ID)))
                .willReturn(List.of(
                        TagMergeCandidateItem.create(MERGE_CANDIDATE_ID, SOURCE_TAG_ID, "기준 보정"),
                        TagMergeCandidateItem.create(MERGE_CANDIDATE_ID, OTHER_SOURCE_TAG_ID, "운영 기준 보강")
                ));
        ArgumentCaptor<Iterable<TagMergeCandidateItem>> itemCaptor = ArgumentCaptor.forClass(Iterable.class);

        TagMergeCandidateApiDto.Response response = tagMergeCandidateService.createCandidates(request);

        verify(tagMergeCandidateItemRepository).saveAll(itemCaptor.capture());
        assertThat(itemCaptor.getValue())
                .extracting("sourceTagId")
                .containsExactly(SOURCE_TAG_ID, OTHER_SOURCE_TAG_ID);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().mergeTargetTag().tagName()).isEqualTo("기준정보");
        assertThat(response.items().getFirst().resultDescription()).isEqualTo("기준 관련 태그 설명");
    }

    @Test
    @DisplayName("AI 서버에서 생성한 후보를 동일한 저장 정책으로 저장한다")
    void AI_서버에서_생성한_후보를_동일한_저장_정책으로_저장한다() {
        TagMergeCandidateApiDto.GenerateRequest generateRequest =
                new TagMergeCandidateApiDto.GenerateRequest(5, 20, 0, 200);
        TagMergeCandidateApiDto.Request generatedRequest = new TagMergeCandidateApiDto.Request(List.of(
                new TagMergeCandidateApiDto.CandidateItem(
                        new TagMergeCandidateApiDto.TagItem(TARGET_TAG_ID, "기준정보", 10),
                        "기준 관련 태그 설명",
                        List.of(
                                new TagMergeCandidateApiDto.TagItem(SOURCE_TAG_ID, "기준 보정", 4),
                                new TagMergeCandidateApiDto.TagItem(OTHER_SOURCE_TAG_ID, "운영 기준 보강", 3)
                        )
                )
        ));
        given(tagMergeAiClient.generateCandidates(generateRequest)).willReturn(generatedRequest);
        given(tagRepository.findAllById(anyCollection()))
                .willReturn(List.of(
                        metaTag(TARGET_TAG_ID, "기준정보", 10, false),
                        metaTag(SOURCE_TAG_ID, "기준 보정", 4, false),
                        metaTag(OTHER_SOURCE_TAG_ID, "운영 기준 보강", 3, false)
                ));
        given(tagMergeCandidateRepository.save(any(TagMergeCandidate.class)))
                .willAnswer(invocation -> {
                    TagMergeCandidate candidate = invocation.getArgument(0);
                    ReflectionTestUtils.setField(candidate, "id", MERGE_CANDIDATE_ID);
                    return candidate;
                });
        given(tagMergeCandidateItemRepository.findByMergeCandidateIdIn(List.of(MERGE_CANDIDATE_ID)))
                .willReturn(List.of(
                        TagMergeCandidateItem.create(MERGE_CANDIDATE_ID, SOURCE_TAG_ID, "기준 보정"),
                        TagMergeCandidateItem.create(MERGE_CANDIDATE_ID, OTHER_SOURCE_TAG_ID, "운영 기준 보강")
                ));

        TagMergeCandidateApiDto.Response response = tagMergeCandidateService.generateCandidates(generateRequest);

        assertThat(response.items()).hasSize(1);
        verify(tagMergeAiClient).generateCandidates(generateRequest);
        verify(tagMergeCandidateRepository).save(any(TagMergeCandidate.class));
    }

    @Test
    @DisplayName("병합 후보 조회 시 삭제된 source 태그만 제외하고 남은 후보는 유지한다")
    void 병합_후보_조회_시_삭제된_source_태그만_제외하고_남은_후보는_유지한다() {
        TagMergeCandidate candidate = candidate(TagMergeCandidateStatus.PENDING);
        given(tagMergeCandidateRepository.findByStatusCodeOrderByCreatedAtDesc(TagMergeCandidateStatus.PENDING))
                .willReturn(List.of(candidate));
        given(tagMergeCandidateItemRepository.findByMergeCandidateIdIn(List.of(MERGE_CANDIDATE_ID)))
                .willReturn(List.of(
                        TagMergeCandidateItem.create(MERGE_CANDIDATE_ID, SOURCE_TAG_ID, "기준 보정"),
                        TagMergeCandidateItem.create(MERGE_CANDIDATE_ID, OTHER_SOURCE_TAG_ID, "운영 기준 보강")
                ));
        given(tagRepository.findAllById(anyCollection()))
                .willReturn(List.of(
                        metaTag(TARGET_TAG_ID, "기준정보", 10, false),
                        metaTag(SOURCE_TAG_ID, "기준 보정", 4, true),
                        metaTag(OTHER_SOURCE_TAG_ID, "운영 기준 보강", 3, false)
                ));

        TagMergeCandidateApiDto.Response response =
                tagMergeCandidateService.getCandidates(TagMergeCandidateStatus.PENDING);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().mergeCandidateTags())
                .extracting(TagMergeCandidateApiDto.TagItem::tagId)
                .containsExactly(OTHER_SOURCE_TAG_ID);
    }

    @Test
    @DisplayName("태그 병합 시 업무일지 연결을 결과 태그로 치환하고 source 태그를 숨긴다")
    void 태그_병합_시_업무일지_연결을_결과_태그로_치환하고_source_태그를_숨긴다() {
        TagMergeCandidate candidate = candidate(TagMergeCandidateStatus.PENDING);
        List<TagMergeCandidateItem> items = List.of(
                TagMergeCandidateItem.create(MERGE_CANDIDATE_ID, SOURCE_TAG_ID, "기준 보정"),
                TagMergeCandidateItem.create(MERGE_CANDIDATE_ID, OTHER_SOURCE_TAG_ID, "운영 기준 보강")
        );
        given(tagMergeCandidateRepository.findById(MERGE_CANDIDATE_ID)).willReturn(Optional.of(candidate));
        given(tagMergeCandidateItemRepository.findByMergeCandidateId(MERGE_CANDIDATE_ID)).willReturn(items);
        given(tagMergeCandidateRepository.markAppliedIfPending(MERGE_CANDIDATE_ID)).willReturn(1);
        given(tagRepository.findAllById(List.of(TARGET_TAG_ID, SOURCE_TAG_ID, OTHER_SOURCE_TAG_ID)))
                .willReturn(List.of(
                        metaTag(TARGET_TAG_ID, "기준정보", 7, false),
                        metaTag(SOURCE_TAG_ID, "기준 보정", 4, false),
                        metaTag(OTHER_SOURCE_TAG_ID, "운영 기준 보강", 3, false)
                ));
        given(worklogTagRepository.countDistinctWorklogsByTagId(TARGET_TAG_ID)).willReturn(9);

        tagMergeCandidateService.mergeCandidate(MERGE_CANDIDATE_ID);

        verify(worklogTagRepository).replaceSourceTagsWithTarget(TARGET_TAG_ID, List.of(SOURCE_TAG_ID, OTHER_SOURCE_TAG_ID));
        verify(tagRepository).softDeleteByIds(List.of(SOURCE_TAG_ID, OTHER_SOURCE_TAG_ID));
        verify(tagMergeCandidateItemRepository).deleteSourceItemsFromOtherCandidates(
                MERGE_CANDIDATE_ID,
                List.of(SOURCE_TAG_ID, OTHER_SOURCE_TAG_ID),
                TagMergeCandidateStatus.PENDING
        );
        verify(tagRepository).updateDescriptionAndUsageCount(TARGET_TAG_ID, "기준 관련 태그 설명", 9);
        verify(tagMergeCandidateRepository).markAppliedIfPending(MERGE_CANDIDATE_ID);
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("대기 중인 병합 후보이면 결과 태그와 source 목록을 수정한다")
    void 대기_중인_병합_후보이면_결과_태그와_source_목록을_수정한다() {
        TagMergeCandidate candidate = candidate(TagMergeCandidateStatus.PENDING);
        TagMergeCandidateApiDto.UpdateRequest request = new TagMergeCandidateApiDto.UpdateRequest(
                OTHER_SOURCE_TAG_ID,
                "수정된 병합 설명",
                List.of(TARGET_TAG_ID, SOURCE_TAG_ID)
        );
        given(tagMergeCandidateRepository.findById(MERGE_CANDIDATE_ID)).willReturn(Optional.of(candidate));
        given(tagRepository.findAllById(anyCollection()))
                .willReturn(List.of(
                        metaTag(OTHER_SOURCE_TAG_ID, "운영 기준 보강", 3, false),
                        metaTag(TARGET_TAG_ID, "기준정보", 10, false),
                        metaTag(SOURCE_TAG_ID, "기준 보정", 4, false)
                ));
        given(tagMergeCandidateItemRepository.findByMergeCandidateIdIn(List.of(MERGE_CANDIDATE_ID)))
                .willReturn(List.of(
                        TagMergeCandidateItem.create(MERGE_CANDIDATE_ID, TARGET_TAG_ID, "기준정보"),
                        TagMergeCandidateItem.create(MERGE_CANDIDATE_ID, SOURCE_TAG_ID, "기준 보정")
                ));
        ArgumentCaptor<Iterable<TagMergeCandidateItem>> itemCaptor = ArgumentCaptor.forClass(Iterable.class);

        TagMergeCandidateApiDto.Item response =
                tagMergeCandidateService.updateCandidate(MERGE_CANDIDATE_ID, request);

        verify(tagMergeCandidateItemRepository).deleteByMergeCandidateId(MERGE_CANDIDATE_ID);
        verify(tagMergeCandidateItemRepository).saveAll(itemCaptor.capture());
        assertThat(candidate.getTargetTagId()).isEqualTo(OTHER_SOURCE_TAG_ID);
        assertThat(candidate.getTargetTagName()).isEqualTo("운영 기준 보강");
        assertThat(candidate.getResultDescription()).isEqualTo("수정된 병합 설명");
        assertThat(itemCaptor.getValue())
                .extracting("sourceTagId")
                .containsExactly(TARGET_TAG_ID, SOURCE_TAG_ID);
        assertThat(response.mergeCandidateId()).isEqualTo(MERGE_CANDIDATE_ID);
        assertThat(response.mergeTargetTag().tagId()).isEqualTo(OTHER_SOURCE_TAG_ID);
        assertThat(response.mergeCandidateTags())
                .extracting(TagMergeCandidateApiDto.TagItem::tagId)
                .containsExactly(TARGET_TAG_ID, SOURCE_TAG_ID);
    }

    @Test
    @DisplayName("이미 처리된 병합 후보이면 적용을 차단한다")
    void 이미_처리된_병합_후보이면_적용을_차단한다() {
        given(tagMergeCandidateRepository.findById(MERGE_CANDIDATE_ID))
                .willReturn(Optional.of(candidate(TagMergeCandidateStatus.APPLIED)));

        assertThatThrownBy(() -> tagMergeCandidateService.mergeCandidate(MERGE_CANDIDATE_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.TAG_MERGE_CANDIDATE_STATUS_INVALID);

        verify(worklogTagRepository, never()).replaceSourceTagsWithTarget(any(), any());
    }

    private TagMergeCandidate candidate(TagMergeCandidateStatus statusCode) {
        TagMergeCandidate candidate = TagMergeCandidate.create(
                TARGET_TAG_ID,
                "기준정보",
                "기준 관련 태그 설명"
        );
        ReflectionTestUtils.setField(candidate, "id", MERGE_CANDIDATE_ID);
        ReflectionTestUtils.setField(candidate, "statusCode", statusCode);
        return candidate;
    }

    private MetaTag metaTag(Long id, String tagName, Integer usageCount, boolean isDeleted) {
        MetaTag tag = MetaTag.create(tagName);
        ReflectionTestUtils.setField(tag, "id", id);
        ReflectionTestUtils.setField(tag, "usageCount", usageCount);
        ReflectionTestUtils.setField(tag, "isDeleted", isDeleted);
        return tag;
    }
}
