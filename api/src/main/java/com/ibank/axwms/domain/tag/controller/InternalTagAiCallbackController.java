package com.ibank.axwms.domain.tag.controller;

import com.ibank.axwms.domain.tag.dto.GetTagsAiApiDto;
import com.ibank.axwms.domain.tag.service.InternalTagAiCallbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/tags")
@RequiredArgsConstructor
public class InternalTagAiCallbackController implements InternalTagAiCallbackControllerDocs {

    private final InternalTagAiCallbackService internalTagAiCallbackService;

    @Override
    @GetMapping
    public GetTagsAiApiDto.Response getTags() {
        return internalTagAiCallbackService.getTags();
    }
}
