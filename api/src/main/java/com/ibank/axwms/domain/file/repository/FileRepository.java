package com.ibank.axwms.domain.file.repository;

import com.ibank.axwms.domain.file.entity.File;
import com.ibank.axwms.domain.file.repository.jooq.FileJooqRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface FileRepository extends JpaRepository<File, Long>, FileJooqRepository {

    /** 삭제 검증용: 요청된 fileId 들이 해당 worklog 에 속하면서 아직 살아있는 (isDeleted=false) 행만 조회한다. */
    List<File> findAllByIdInAndWorklogIdAndIsDeletedFalse(Collection<Long> ids, Long worklogId);
}
