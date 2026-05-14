package com.ibank.axwms.domain.file.repository.jooq.query;

import com.ibank.axwms.domain.file.dto.GetFilesApiDto;

import java.time.LocalDateTime;

public record FilePageQuery(
        int page,
        int pageSize,
        String fileExtension,
        LocalDateTime createdFrom
) {
    /**
     * API 요청 DTO 의 기본값과 공개 필터 표현을 repository 가 비교할 수 있는 내부 query 값으로 변환한다.
     */
    public static FilePageQuery from(GetFilesApiDto.Request request) {
        GetFilesApiDto.Request query = request == null
                ? new GetFilesApiDto.Request(null, null, null, null)
                : request;
        String fileExtension = query.fileType() == null ? null : query.fileType().dbExtension();
        LocalDateTime createdFrom = query.period() == null ? null : LocalDateTime.now().minusDays(query.period());
        return of(
                query.pageOrDefault(),
                query.pageSizeOrDefault(),
                fileExtension,
                createdFrom
        );
    }

    /**
     * HTTP 요청 DTO 를 repository 밖에 두기 위해 service 가 정규화한 조회 값만 내부 query 로 고정한다.
     */
    public static FilePageQuery of(int page, int pageSize, String fileExtension, LocalDateTime createdFrom) {
        return new FilePageQuery(page, pageSize, fileExtension, createdFrom);
    }

    /**
     * API 의 1-indexed page 계약을 Spring/JOOQ limit-offset 계산용 0-indexed 값으로 바꾼다.
     */
    public int pageIndex() {
        return page - 1;
    }
}
