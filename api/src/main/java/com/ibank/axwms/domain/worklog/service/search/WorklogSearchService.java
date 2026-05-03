package com.ibank.axwms.domain.worklog.service.search;

import com.ibank.axwms.domain.worklog.dto.SearchWorklogsApiDto;
import com.ibank.axwms.global.response.PageResponse;
import com.ibank.axwms.global.security.CustomUserPrincipal;

/**
 * 업무일지 검색 유스케이스 인터페이스.
 * 1단계는 키워드(LIKE)+필터 기반 단일 구현체만 존재하며,
 * 시맨틱 검색/그래프 확장 단계에서 별도 구현체 또는 데코레이터로 진입한다.
 */
public interface WorklogSearchService {

    /**
     * 로그인 사용자의 가시 범위 내에서 업무일지를 검색한다.
     *
     * @param principal 현재 로그인 사용자
     * @param request 검색 요청 DTO. null 이면 기본값 페이지 응답을 돌려준다.
     * @return 페이지네이션 응답
     */
    PageResponse<SearchWorklogsApiDto.Response.Item> searchWorklogs(
            CustomUserPrincipal principal,
            SearchWorklogsApiDto.Request request
    );
}
