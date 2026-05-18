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
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.ibank.axwms.domain.tag.dto.TagMergeCandidateApiDto.DESCRIPTION_MAX_LENGTH;
import static com.ibank.axwms.domain.tag.dto.TagMergeCandidateApiDto.MAX_MERGE_CANDIDATE_TAG_COUNT;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TagMergeCandidateService {

    private final TagMergeCandidateRepository tagMergeCandidateRepository;
    private final TagMergeCandidateItemRepository tagMergeCandidateItemRepository;
    private final TagRepository tagRepository;
    private final TagMergeAiClient tagMergeAiClient;

    /**
     * AI가 추천한 후보를 운영자가 검토할 수 있도록 snapshot 형태로 저장한다.
     */
    @Transactional
    public TagMergeCandidateApiDto.Response createCandidates(TagMergeCandidateApiDto.Request request) {
        List<TagMergeCandidate> savedCandidates = request.items().stream()
                .map(this::saveCandidateIfValid)
                .flatMap(Optional::stream)
                .toList();

        return toResponse(savedCandidates);
    }

    /**
     * Spring 권한 경계 안에서 AI 후보를 생성하고, 생성 결과를 같은 저장 정책으로 snapshot 처리한다.
     */
    @Transactional
    public TagMergeCandidateApiDto.Response generateCandidates(TagMergeCandidateApiDto.GenerateRequest request) {
        try {
            TagMergeCandidateApiDto.Request generated = tagMergeAiClient.generateCandidates(request);
            return createCandidates(generated);
        } catch (RestClientException ex) {
            throw new BusinessException(ErrorCode.TAG_MERGE_CANDIDATE_GENERATION_FAILED, ex);
        }
    }

    /**
     * 상태 필터가 있으면 해당 후보만, 없으면 전체 후보를 최신순으로 조회한다.
     */
    public TagMergeCandidateApiDto.Response getCandidates(TagMergeCandidateStatus statusCode) {
        List<TagMergeCandidate> candidates = statusCode == null
                ? tagMergeCandidateRepository.findAllByOrderByCreatedAtDesc()
                : tagMergeCandidateRepository.findByStatusCodeOrderByCreatedAtDesc(statusCode);
        return toResponse(candidates);
    }

    /**
     * source 후보가 비어 있으면 저장 대상에서 제외해 빈 후보 그룹 노출을 막는다.
     */
    private Optional<TagMergeCandidate> saveCandidateIfValid(TagMergeCandidateApiDto.CandidateItem item) {
        List<TagMergeCandidateApiDto.TagItem> sourceTags = normalizeSourceTags(
                item.mergeTargetTag().tagId(),
                item.mergeCandidateTags()
        );
        if (sourceTags.isEmpty()) {
            return Optional.empty();
        }
        validateActiveTags(
                item.mergeTargetTag().tagId(),
                sourceTags.stream()
                        .map(TagMergeCandidateApiDto.TagItem::tagId)
                        .toList()
        );

        TagMergeCandidate candidate = tagMergeCandidateRepository.save(
                TagMergeCandidate.create(
                        item.mergeTargetTag().tagId(),
                        item.mergeTargetTag().tagName().trim(),
                        normalizeDescription(item.resultDescription())
                )
        );
        List<TagMergeCandidateItem> candidateItems = sourceTags.stream()
                .map(sourceTag -> TagMergeCandidateItem.create(
                        candidate.getId(),
                        sourceTag.tagId(),
                        sourceTag.tagName().trim()
                ))
                .toList();
        tagMergeCandidateItemRepository.saveAll(candidateItems);
        return Optional.of(candidate);
    }

    /**
     * target 과 중복되는 source 를 제거하고 같은 source 태그는 첫 번째 snapshot 만 유지한다.
     */
    private List<TagMergeCandidateApiDto.TagItem> normalizeSourceTags(Long targetTagId,
                                                                      List<TagMergeCandidateApiDto.TagItem> sourceTags) {
        return sourceTags.stream()
                .filter(sourceTag -> !sourceTag.tagId().equals(targetTagId))
                .collect(Collectors.toMap(
                        TagMergeCandidateApiDto.TagItem::tagId,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ))
                .values()
                .stream()
                .limit(MAX_MERGE_CANDIDATE_TAG_COUNT)
                .toList();
    }

    /**
     * description 은 생성 정책상 100자 미만을 지향하되 DB 하드 제한 150자만 강제한다.
     */
    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        String trimmed = description.trim();
        return trimmed.length() > DESCRIPTION_MAX_LENGTH
                ? trimmed.substring(0, DESCRIPTION_MAX_LENGTH)
                : trimmed;
    }

    /**
     * 저장된 후보와 아이템을 모아 현재 usageCount 기준 응답으로 변환한다.
     */
    private TagMergeCandidateApiDto.Response toResponse(List<TagMergeCandidate> candidates) {
        if (candidates.isEmpty()) {
            return new TagMergeCandidateApiDto.Response(List.of());
        }

        List<Long> candidateIds = candidates.stream()
                .map(TagMergeCandidate::getId)
                .toList();
        Map<Long, List<TagMergeCandidateItem>> itemsByCandidateId = tagMergeCandidateItemRepository
                .findByMergeCandidateIdIn(candidateIds)
                .stream()
                .collect(Collectors.groupingBy(TagMergeCandidateItem::getMergeCandidateId));
        Map<Long, Integer> usageCountByTagId = getUsageCountByTagId(candidates, itemsByCandidateId);
        return TagMergeCandidateApiDto.Response.of(candidates, itemsByCandidateId, usageCountByTagId);
    }

    /**
     * 저장된 snapshot 이름은 유지하되 현재 태그 사용 횟수만 ID 기준으로 보강한다.
     */
    private Map<Long, Integer> getUsageCountByTagId(Collection<TagMergeCandidate> candidates,
                                                    Map<Long, List<TagMergeCandidateItem>> itemsByCandidateId) {
        Set<Long> tagIds = Stream.concat(
                        candidates.stream().map(TagMergeCandidate::getTargetTagId),
                        itemsByCandidateId.values().stream()
                                .flatMap(Collection::stream)
                                .map(TagMergeCandidateItem::getSourceTagId)
                )
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return tagRepository.findAllById(tagIds).stream()
                .collect(Collectors.toMap(MetaTag::getId, MetaTag::getUsageCount));
    }

    /**
     * 후보 생성 이후 삭제된 태그가 있으면 운영자가 본 후보와 적용 결과가 달라지므로 전체 적용을 차단한다.
     */
    private void validateActiveTags(Long targetTagId, List<Long> sourceTagIds) {
        List<Long> tagIds = Stream.concat(Stream.of(targetTagId), sourceTagIds.stream())
                .distinct()
                .toList();
        List<MetaTag> tags = tagRepository.findAllById(tagIds);
        if (tags.size() != tagIds.size() || tags.stream().anyMatch(MetaTag::getIsDeleted)) {
            throw new BusinessException(ErrorCode.TAG_NOT_FOUND);
        }
    }
}
