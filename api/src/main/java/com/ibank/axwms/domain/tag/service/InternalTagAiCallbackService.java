package com.ibank.axwms.domain.tag.service;

import com.ibank.axwms.domain.tag.dto.GetTagsAiApiDto;
import com.ibank.axwms.domain.tag.repository.TagRepository;
import com.ibank.axwms.domain.worklog.dto.ApplyWorklogAiTagsApiDto;
import com.ibank.axwms.domain.worklog.entity.Worklog;
import com.ibank.axwms.domain.worklog.entity.WorklogTag;
import com.ibank.axwms.domain.worklog.repository.WorklogRepository;
import com.ibank.axwms.domain.worklog.repository.WorklogTagRepository;
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
public class InternalTagAiCallbackService {

    private final TagRepository tagRepository;
    private final WorklogRepository worklogRepository;
    private final WorklogTagRepository worklogTagRepository;
    private final TagService tagService;

    /**
     * FastAPI 태그 생성 프롬프트에 전달할 내부용 태그 목록을 조회한다.
     */
    @Transactional(readOnly = true)
    public GetTagsAiApiDto.Response getTags() {
        return GetTagsAiApiDto.Response.from(tagRepository.findAllTagInfo());
    }

    /**
     * AI가 선택한 기존 태그와 새 태그를 업무일지에 연결하고 신규 연결분만 사용 횟수에 반영한다.
     */
    @Transactional
    public void applyAiGeneratedTags(Long worklogId, ApplyWorklogAiTagsApiDto.Request request) {
        getWorklogOrThrow(worklogId);

        List<Long> normalizedExistingTagIds = tagService.normalizeExistingTagIds(request.existingTagIds());
        List<Long> newTagIds = createMissingTags(request.newTagNames());
        List<Long> tagIds = Stream.concat(normalizedExistingTagIds.stream(), newTagIds.stream())
                .distinct()
                .toList();

        List<Long> newlyLinkedTagIds = linkTagsToWorklog(worklogId, tagIds);

        tagService.incrementUsageCountByIds(newlyLinkedTagIds);
    }

    /**
     * 잘못된 업무일지 ID로 AI 태그와 사용 횟수가 오염되지 않도록 대상 존재를 먼저 확정한다.
     */
    private Worklog getWorklogOrThrow(Long worklogId) {
        return worklogRepository.findById(worklogId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKLOG_NOT_FOUND));
    }

    /**
     * AI가 새로 제안한 태그명 중 아직 태그 풀에 없는 값만 멱등 생성하고 적용 대상 ID를 반환한다.
     */
    private List<Long> createMissingTags(List<String> newTagNames) {
        List<String> tagNames = normalizeTagNames(newTagNames);
        if (tagNames.isEmpty()) {
            return List.of();
        }

        tagRepository.insertAiGeneratedTagNamesIgnoreDuplicates(tagNames);
        return tagRepository.findAllByTagNameIn(tagNames).stream()
                .map(tag -> tag.getId())
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
                .map(tagId -> WorklogTag.create(worklogId, tagId))
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
