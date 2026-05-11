package com.ibank.axwms.domain.file.repository.jooq.query;

import com.ibank.axwms.domain.file.dto.GetFilesApiDto;

public record FilePageQuery(
        int page,
        int pageSize
) {
    public static FilePageQuery from(GetFilesApiDto.Request request) {
        return new FilePageQuery(request.pageOrDefault(), request.pageSizeOrDefault());
    }

    public int pageIndex() {
        return page - 1;
    }
}
