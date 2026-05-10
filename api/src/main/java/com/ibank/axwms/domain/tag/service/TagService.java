package com.ibank.axwms.domain.tag.service;

import com.ibank.axwms.domain.tag.repository.TagRepository;
import com.ibank.axwms.domain.tag.repository.jooq.projection.MetaTagDetailProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
}
