package com.ibank.axwms.domain.tag.service;

import com.ibank.axwms.domain.tag.repository.TagRepository;
import com.ibank.axwms.domain.tag.repository.jooq.projection.MetaTagDetailProjection;
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TagService {

    private final TagRepository tagRepository;

    /**
     * 메타 태그 전체를 5개 컬럼 모두 포함한 상세 형태로 이름순 반환한다.
     * 업무 등록 화면 등 폼 옵션 노출에 사용된다.
     */
    public List<MetaTagDetailProjection> findAllTagDetails() {
        return tagRepository.findAllTagDetails();
    }

    /**
     * 요청에서 넘어온 태그 ID를 중복 제거하고 모두 실제 메타 태그인지 검증한다.
     */
    public List<Long> normalizeExistingTagIds(Collection<Long> tagIds) {
        List<Long> normalizedIds = normalizeTagIds(tagIds);
        if (normalizedIds.isEmpty()) {
            return List.of();
        }

        List<Long> existingIds = tagRepository.findAllById(normalizedIds).stream()
                .map(tag -> tag.getId())
                .toList();
        if (existingIds.size() != normalizedIds.size()) {
            throw new BusinessException(ErrorCode.TAG_NOT_FOUND);
        }
        return normalizedIds;
    }

    /**
     * 업무일지와 새로 연결된 태그의 사용 횟수 캐시를 증가시킨다.
     */
    @Transactional
    public void incrementUsageCountByIds(Collection<Long> tagIds) {
        List<Long> normalizedIds = normalizeTagIds(tagIds);
        if (normalizedIds.isEmpty()) {
            return;
        }

        tagRepository.incrementUsageCountByIds(normalizedIds);
    }

    /**
     * null 과 중복을 제거해 저장 계층에서 안정적으로 사용할 태그 ID 목록을 만든다.
     */
    private List<Long> normalizeTagIds(Collection<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return List.of();
        }

        return tagIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }
}
