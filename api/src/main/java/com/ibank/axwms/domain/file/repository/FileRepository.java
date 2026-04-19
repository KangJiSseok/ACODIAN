package com.ibank.axwms.domain.file.repository;

import com.ibank.axwms.domain.file.entity.File;
import com.ibank.axwms.domain.file.repository.jooq.FileJooqRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileRepository extends JpaRepository<File, Long>, FileJooqRepository {
}
