package com.ibank.axwms.domain.tag.service;

import com.ibank.axwms.domain.tag.dto.GetTagsAiApiDto;
import com.ibank.axwms.domain.tag.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InternalTagAiCallbackService {

    private final TagRepository tagRepository;

    /**
     * FastAPI 태그 생성 프롬프트에 전달할 내부용 태그 목록을 조회한다.
     */
    @Transactional(readOnly = true)
    public GetTagsAiApiDto.Response getTags() {
        return GetTagsAiApiDto.Response.from(tagRepository.findAllTagInfo());
    }
}
