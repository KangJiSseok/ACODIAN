package com.ibank.axwms.domain.worklog.service;

import com.ibank.axwms.domain.tag.entity.MetaTag;
import com.ibank.axwms.domain.tag.repository.TagRepository;
import com.ibank.axwms.domain.tag.service.TagService;
import com.ibank.axwms.domain.worklog.dto.ApplyWorklogTagsAiApiDto;
import com.ibank.axwms.domain.worklog.dto.UpdateWorklogAiApiDto;
import com.ibank.axwms.domain.worklog.entity.Worklog;
import com.ibank.axwms.domain.worklog.entity.WorklogTag;
import com.ibank.axwms.domain.worklog.repository.WorklogRepository;
import com.ibank.axwms.domain.worklog.repository.WorklogTagRepository;
import com.ibank.axwms.global.enums.AiProcessingStatus;
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InternalWorklogAiCallbackService {

    private final WorklogRepository worklogRepository;
    private final WorklogTagRepository worklogTagRepository;
    private final TagRepository tagRepository;
    private final TagService tagService;

    /**
     * AI 요약 처리 결과를 업무일지에 반영하고 성공/실패 상태를 갱신한다.
     */
    @Transactional
    public void updateAiResult(Long worklogId, UpdateWorklogAiApiDto.Request request) {

        Worklog worklog = getWorklogOrThrow(worklogId);

        if (request.aiProcessingStatus() == AiProcessingStatus.COMPLETED) {
            worklog.changeAiSummary(request.aiSummary());
            worklog.completeAiSummaryProcessing();
        } else if (request.aiProcessingStatus() == AiProcessingStatus.FAILED) {
            worklog.failAiSummaryProcessing();
        }

    }

    /**
     * AI가 선택한 기존 태그와 새 태그를 업무일지에 연결하고 신규 연결분만 사용 횟수에 반영한다.
     */
    @Transactional
    public void applyAiGeneratedTags(Long worklogId, ApplyWorklogTagsAiApiDto.Request request) {
        getWorklogOrThrow(worklogId);

        List<Long> existingTagIds = request.existingTagIds();
        List<Long> newTagIds = createNewTags(request.newTagNames());

        List<Long> tagIds = mergeTagIds(existingTagIds, newTagIds);

        List<Long> newlyLinkedTagIds = linkTagsToWorklog(worklogId, tagIds);

        tagService.incrementUsageCountByIds(newlyLinkedTagIds);
    }

    /**
     * 잘못된 업무일지 ID로 AI 결과와 태그 사용 횟수가 오염되지 않도록 대상 존재를 먼저 확정한다.
     */
    private Worklog getWorklogOrThrow(Long worklogId) {
        return worklogRepository.findById(worklogId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKLOG_NOT_FOUND));
    }

    /**
     * AI가 새로 제안한 태그명 중 아직 태그 풀에 없는 값만 생성하고 적용 대상 ID를 반환한다.
     */
    private List<Long> createNewTags(List<String> newTagNames) {
        List<String> tagNames = normalizeTagNames(newTagNames);
        if (tagNames.isEmpty()) {
            return List.of();
        }

        List<MetaTag> existingTags = tagRepository.findAllByTagNameIn(tagNames);
        List<String> existingTagNames = existingTags.stream()
                .map(MetaTag::getTagName)
                .toList();
        List<MetaTag> newTags = tagNames.stream()
                .filter(tagName -> !existingTagNames.contains(tagName))
                .map(MetaTag::create)
                .toList();

        List<Long> existingTagIds = existingTags.stream()
                .map(MetaTag::getId)
                .toList();
        List<Long> createdTagIds = saveNewTags(newTags).stream()
                .map(MetaTag::getId)
                .toList();

        return mergeTagIds(existingTagIds, createdTagIds);
    }

    /**
     * 생성할 태그가 없을 때 JPA 호출을 생략해 빈 입력 콜백을 부작용 없이 처리한다.
     */
    private List<MetaTag> saveNewTags(List<MetaTag> newTags) {
        if (newTags.isEmpty()) {
            return List.of();
        }

        return tagRepository.saveAll(newTags);
    }

    /**
     * 기존 태그와 신규 태그를 하나의 적용 목록으로 합치며 재시도 입력의 중복 ID를 제거한다.
     */
    private List<Long> mergeTagIds(List<Long> existingTagIds, List<Long> newTagIds) {
        return Stream.concat(existingTagIds.stream(), newTagIds.stream())
                .distinct()
                .toList();
    }

    /**
     * 이미 연결된 태그는 건너뛰어 AI 콜백 재시도 시 사용 횟수 중복 증가를 차단한다.
     */
    private List<Long> linkTagsToWorklog(Long worklogId, List<Long> tagIds) {
        if (tagIds.isEmpty()) {
            return List.of();
        }

        Set<Long> linkedTagIds = worklogTagRepository.findByWorklogIdAndTagIdIn(worklogId, tagIds).stream()
                .map(WorklogTag::getTagId)
                .collect(Collectors.toSet());
        List<WorklogTag> newLinks = tagIds.stream()
                .filter(tagId -> !linkedTagIds.contains(tagId))
                .map(tagId -> WorklogTag.createAiGenerated(worklogId, tagId))
                .toList();

        if (newLinks.isEmpty()) {
            return List.of();
        }

        return worklogTagRepository.saveAll(newLinks).stream()
                .map(WorklogTag::getTagId)
                .toList();
    }

    /**
     * AI가 반환한 태그명에서 공백 값과 중복 값을 제거해 DB 처리 단위를 고정한다.
     */
    private List<String> normalizeTagNames(List<String> tagNames) {
        return tagNames.stream()
                .map(String::trim)
                .filter(tagName -> !tagName.isBlank())
                .distinct()
                .limit(5)
                .toList();
    }

}
