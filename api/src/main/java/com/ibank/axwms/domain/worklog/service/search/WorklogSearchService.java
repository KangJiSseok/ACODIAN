package com.ibank.axwms.domain.worklog.service.search;

import com.ibank.axwms.domain.worklog.dto.GetWorklogFilterOptionsApiDto;
import com.ibank.axwms.domain.worklog.dto.SearchWorklogsApiDto;
import com.ibank.axwms.global.response.PageResponse;
import com.ibank.axwms.global.security.CustomUserPrincipal;

/**
 * 업무일지 키워드 검색 유스케이스 인터페이스.
 * LightRAG 기반 시맨틱 검색은 별도 서비스와 endpoint 로 분리해 fallback 없이 호출한다.
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

    /**
     * 업무일지 검색 화면 진입 시 필요한 필터 옵션(가시 팀+멤버, 전체 태그)을 한 번에 반환한다.
     *
     * @param principal 현재 로그인 사용자
     * @return 필터 옵션 응답
     */
    GetWorklogFilterOptionsApiDto.Response getFilterOptions(CustomUserPrincipal principal);
}
