package com.ibank.axwms.domain.tag.repository;

import com.ibank.axwms.domain.tag.entity.MetaTag;
import com.ibank.axwms.domain.tag.repository.jooq.MetaTagJooqRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<MetaTag, Long>, MetaTagJooqRepository {
}
