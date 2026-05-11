package com.ibank.axwms.domain.file.repository.jooq;

import com.ibank.axwms.domain.file.repository.jooq.projection.FileSummaryProjection;
import com.ibank.axwms.domain.file.repository.jooq.projection.FileWorklogProjection;
import com.ibank.axwms.domain.file.repository.jooq.query.FilePageQuery;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.WorklogFileProjection;
import org.springframework.data.domain.Page;

import java.util.Collection;
import java.util.List;

public interface FileJooqRepository {

    /** 주어진 worklog 에 첨부된, 삭제되지 않은 파일 목록을 조회한다. */
    List<WorklogFileProjection> findByWorklogId(Long worklogId);

    /**
     * 사용자가 접근 가능한 worklog 의 첨부 파일을 최신 등록순으로 페이지 조회한다.
     * 가시성 룰: file 의 worklog 가 속한 team 이 사용자의 admin grant 또는 ACTIVE membership 에 포함되고,
     * worklog 와 team 모두 미삭제일 때만 노출된다.
     */
    Page<FileSummaryProjection> findFilePage(Long userId, FilePageQuery query);

    /**
     * 파일 목록 행에 함께 박아넣을 업무 요약을 worklog ID 목록으로 batch 조회한다.
     * 1 쿼리로 worklog + team + author + 선행 업무 개수까지 한 번에 가져온다.
     * 가시성 룰은 findFilePage 와 동일 — 권한 밖이거나 삭제된 행은 결과에 포함되지 않는다.
     */
    List<FileWorklogProjection> findFileWorklogsByIds(Long userId, Collection<Long> worklogIds);
}
