package com.ibank.axwms.domain.tag.service;

import com.ibank.axwms.domain.tag.dto.SearchTagApiDto;
import com.ibank.axwms.domain.tag.repository.TagRepository;
import com.ibank.axwms.domain.tag.repository.jooq.projection.SearchTagProjection;
import com.ibank.axwms.domain.tag.repository.jooq.query.SearchTagQuery;
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import com.ibank.axwms.global.response.PageResponse;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
     * 업무일지와 연결 해제된 태그의 사용 횟수 캐시를 감소시키되 음수로 내려가지 않게 한다.
     */
    @Transactional
    public void decrementUsageCountByIds(Collection<Long> tagIds) {
        List<Long> normalizedIds = normalizeTagIds(tagIds);
        if (normalizedIds.isEmpty()) {
            return;
        }

        tagRepository.decrementUsageCountByIds(normalizedIds);
    }

    /**
     * 메타 태그를 태그명 LIKE (대소문자 무시) 로 검색해 페이지로 반환한다.
     * worklog 등록/수정 화면의 태그 picker, file 목록의 태그 필터 등 공통 lookup 으로 사용된다.
     */
    public PageResponse<SearchTagApiDto.Response.Item> searchTag(CustomUserPrincipal principal,
                                                                 SearchTagApiDto.Request request) {
        SearchTagQuery query = SearchTagQuery.from(request);
        Page<SearchTagProjection> page = tagRepository.searchTagPage(query);
        return SearchTagApiDto.Response.fromPage(page);
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
