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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
import static com.ibank.axwms.domain.tag.dto.TagMergeCandidateApiDto.MIN_MERGE_CANDIDATE_TAG_COUNT;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TagMergeCandidateService {

    private final TagMergeCandidateRepository tagMergeCandidateRepository;
    private final TagMergeCandidateItemRepository tagMergeCandidateItemRepository;
    private final TagRepository tagRepository;
    private final WorklogTagRepository worklogTagRepository;
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
     * 상태 필터가 있으면 해당 후보만, 없으면 전체 후보를 서버 오늘 날짜 기준 최신순으로 조회한다.
     */
    public TagMergeCandidateApiDto.Response getCandidates(TagMergeCandidateStatus statusCode) {
        LocalDate today = LocalDate.now();
        LocalDateTime from = today.atStartOfDay();
        LocalDateTime to = today.plusDays(1).atStartOfDay();
        List<TagMergeCandidate> candidates = statusCode == null
                ? tagMergeCandidateRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(from, to)
                : tagMergeCandidateRepository.findByStatusCodeAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(statusCode, from, to);
        return toResponse(candidates);
    }

    /**
     * 운영자가 확정한 병합 방향을 PENDING 후보 snapshot 에 반영한다.
     */
    @Transactional
    public TagMergeCandidateApiDto.Item updateCandidate(Long mergeCandidateId,
                                                        TagMergeCandidateApiDto.UpdateRequest request) {
        TagMergeCandidate candidate = getCandidateOrThrow(mergeCandidateId);
        validatePending(candidate);

        List<Long> sourceTagIds = normalizeSourceTagIds(request.mergeTargetTagId(), request.mergeCandidateTagIds());
        if (sourceTagIds.size() < MIN_MERGE_CANDIDATE_TAG_COUNT) {
            throw new BusinessException(ErrorCode.COMMON_VALIDATION_ERROR);
        }

        Map<Long, MetaTag> tagsById = getActiveTagsByIdOrThrow(request.mergeTargetTagId(), sourceTagIds);
        MetaTag targetTag = tagsById.get(request.mergeTargetTagId());
        candidate.updateSnapshot(
                targetTag.getId(),
                targetTag.getTagName(),
                normalizeDescription(request.resultDescription())
        );

        tagMergeCandidateItemRepository.deleteByMergeCandidateId(mergeCandidateId);
        tagMergeCandidateItemRepository.saveAll(sourceTagIds.stream()
                .map(sourceTagId -> {
                    MetaTag sourceTag = tagsById.get(sourceTagId);
                    return TagMergeCandidateItem.create(mergeCandidateId, sourceTag.getId(), sourceTag.getTagName());
                })
                .toList());

        return toResponse(List.of(candidate)).items().getFirst();
    }

    /**
     * 저장된 후보를 승인해 source 태그들을 target 태그로 병합하고 source 태그는 soft-delete 처리한다.
     */
    @Transactional
    public void mergeCandidate(Long mergeCandidateId) {
        TagMergeCandidate candidate = getCandidateOrThrow(mergeCandidateId);
        validatePending(candidate);
        claimPendingCandidate(mergeCandidateId);

        List<TagMergeCandidateItem> items = tagMergeCandidateItemRepository.findByMergeCandidateId(candidate.getId());
        if (items.isEmpty()) {
            throw new BusinessException(ErrorCode.TAG_MERGE_CANDIDATE_NOT_FOUND);
        }

        List<Long> sourceTagIds = getActiveSourceTagIdsForMergeOrThrow(candidate, items);

        worklogTagRepository.replaceSourceTagsWithTarget(candidate.getTargetTagId(), sourceTagIds);
        tagRepository.softDeleteByIds(sourceTagIds);
        tagMergeCandidateItemRepository.deleteSourceItemsFromOtherCandidates(
                candidate.getId(),
                sourceTagIds,
                TagMergeCandidateStatus.PENDING
        );
        int usageCount = worklogTagRepository.countDistinctWorklogsByTagId(candidate.getTargetTagId());
        tagRepository.updateDescriptionAndUsageCount(
                candidate.getTargetTagId(),
                candidate.getResultDescription(),
                usageCount
        );
    }

    /**
     * source 후보가 최소 병합 단위보다 작으면 저장 대상에서 제외해 작은 후보 그룹 노출을 막는다.
     */
    private Optional<TagMergeCandidate> saveCandidateIfValid(TagMergeCandidateApiDto.CandidateItem item) {
        List<TagMergeCandidateApiDto.TagItem> sourceTags = normalizeSourceTags(
                item.mergeTargetTag().tagId(),
                item.mergeCandidateTags()
        );
        if (sourceTags.size() < MIN_MERGE_CANDIDATE_TAG_COUNT) {
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
     * 수정 요청의 source ID 는 중복을 제거하되 입력 순서를 유지해 운영자 선택 순서를 보존한다.
     */
    private List<Long> normalizeSourceTagIds(Long targetTagId, List<Long> sourceTagIds) {
        return sourceTagIds.stream()
                .filter(sourceTagId -> !sourceTagId.equals(targetTagId))
                .distinct()
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
        Map<Long, MetaTag> tagsById = getTagsById(candidates, itemsByCandidateId);
        Map<Long, List<TagMergeCandidateItem>> activeItemsByCandidateId =
                filterActiveSourceItems(itemsByCandidateId, tagsById);
        List<TagMergeCandidate> displayableCandidates =
                filterCandidatesWithEnoughActiveSources(candidates, activeItemsByCandidateId);
        Map<Long, Integer> usageCountByTagId = getUsageCountByTagId(tagsById);
        return TagMergeCandidateApiDto.Response.of(displayableCandidates, activeItemsByCandidateId, usageCountByTagId);
    }

    /**
     * 후보 snapshot 에 포함된 태그들의 현재 상태와 usageCount 를 한 번에 조회한다.
     */
    private Map<Long, MetaTag> getTagsById(Collection<TagMergeCandidate> candidates,
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
                .collect(Collectors.toMap(MetaTag::getId, Function.identity()));
    }

    /**
     * 이미 다른 병합에서 삭제된 source 태그는 후속 후보 조회에서 제외해 화면과 새로고침 결과를 맞춘다.
     */
    private Map<Long, List<TagMergeCandidateItem>> filterActiveSourceItems(
            Map<Long, List<TagMergeCandidateItem>> itemsByCandidateId,
            Map<Long, MetaTag> tagsById
    ) {
        return itemsByCandidateId.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .filter(item -> {
                                    MetaTag tag = tagsById.get(item.getSourceTagId());
                                    return tag != null && !tag.getIsDeleted();
                                })
                                .toList()
                ));
    }

    /**
     * 다른 후보 병합으로 source 가 빠진 뒤 최소 병합 단위가 깨진 후보는 목록 응답에서 제외한다.
     */
    private List<TagMergeCandidate> filterCandidatesWithEnoughActiveSources(
            List<TagMergeCandidate> candidates,
            Map<Long, List<TagMergeCandidateItem>> activeItemsByCandidateId
    ) {
        return candidates.stream()
                .filter(candidate -> activeItemsByCandidateId
                        .getOrDefault(candidate.getId(), List.of())
                        .stream()
                        .map(TagMergeCandidateItem::getSourceTagId)
                        .distinct()
                        .count() >= MIN_MERGE_CANDIDATE_TAG_COUNT)
                .toList();
    }

    /**
     * 병합 직전에 target 활성 여부와 남은 활성 source 개수를 재검증해 직접 API 호출도 차단한다.
     */
    private List<Long> getActiveSourceTagIdsForMergeOrThrow(TagMergeCandidate candidate,
                                                            List<TagMergeCandidateItem> items) {
        Map<Long, List<TagMergeCandidateItem>> itemsByCandidateId = Map.of(candidate.getId(), items);
        Map<Long, MetaTag> tagsById = getTagsById(List.of(candidate), itemsByCandidateId);
        MetaTag targetTag = tagsById.get(candidate.getTargetTagId());
        if (targetTag == null || targetTag.getIsDeleted()) {
            throw new BusinessException(ErrorCode.TAG_NOT_FOUND);
        }

        List<Long> sourceTagIds = filterActiveSourceItems(itemsByCandidateId, tagsById)
                .getOrDefault(candidate.getId(), List.of())
                .stream()
                .map(TagMergeCandidateItem::getSourceTagId)
                .distinct()
                .toList();
        if (sourceTagIds.size() < MIN_MERGE_CANDIDATE_TAG_COUNT) {
            throw new BusinessException(ErrorCode.COMMON_VALIDATION_ERROR);
        }
        return sourceTagIds;
    }

    /**
     * 응답 DTO 는 삭제 필터링 이후에도 target/source 의 최신 사용 횟수를 같은 기준으로 사용한다.
     */
    private Map<Long, Integer> getUsageCountByTagId(Map<Long, MetaTag> tagsById) {
        return tagsById.values().stream()
                .collect(Collectors.toMap(MetaTag::getId, MetaTag::getUsageCount));
    }

    /**
     * 병합 적용 요청은 저장된 후보 그룹에만 허용한다.
     */
    private TagMergeCandidate getCandidateOrThrow(Long mergeCandidateId) {
        return tagMergeCandidateRepository.findById(mergeCandidateId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TAG_MERGE_CANDIDATE_NOT_FOUND));
    }

    /**
     * 이미 처리된 후보의 중복 적용은 관계 치환 결과를 예측하기 어렵기 때문에 차단한다.
     */
    private void validatePending(TagMergeCandidate candidate) {
        if (candidate.getStatusCode() != TagMergeCandidateStatus.PENDING) {
            throw new BusinessException(ErrorCode.TAG_MERGE_CANDIDATE_STATUS_INVALID);
        }
    }

    /**
     * 상태 변경 row lock 을 이용해 같은 후보의 중복 병합 요청 중 하나만 후속 변경을 수행하게 한다.
     */
    private void claimPendingCandidate(Long mergeCandidateId) {
        if (tagMergeCandidateRepository.markAppliedIfPending(mergeCandidateId) != 1) {
            throw new BusinessException(ErrorCode.TAG_MERGE_CANDIDATE_STATUS_INVALID);
        }
    }

    /**
     * 후보 생성 이후 삭제된 태그가 있으면 운영자가 본 후보와 적용 결과가 달라지므로 전체 적용을 차단한다.
     */
    private void validateActiveTags(Long targetTagId, List<Long> sourceTagIds) {
        getActiveTagsByIdOrThrow(targetTagId, sourceTagIds);
    }

    /**
     * 후보에 포함된 모든 태그가 병합 적용 가능한 활성 태그인지 확인하고 ID 조회 맵으로 돌려준다.
     */
    private Map<Long, MetaTag> getActiveTagsByIdOrThrow(Long targetTagId, List<Long> sourceTagIds) {
        List<Long> tagIds = Stream.concat(Stream.of(targetTagId), sourceTagIds.stream())
                .distinct()
                .toList();
        List<MetaTag> tags = tagRepository.findAllById(tagIds);
        if (tags.size() != tagIds.size() || tags.stream().anyMatch(MetaTag::getIsDeleted)) {
            throw new BusinessException(ErrorCode.TAG_NOT_FOUND);
        }
        return tags.stream()
                .collect(Collectors.toMap(MetaTag::getId, Function.identity()));
    }
}
