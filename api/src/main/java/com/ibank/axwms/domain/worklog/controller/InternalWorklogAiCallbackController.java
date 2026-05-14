package com.ibank.axwms.domain.worklog.controller;

import com.ibank.axwms.domain.tag.service.InternalTagAiCallbackService;
import com.ibank.axwms.domain.worklog.dto.ApplyWorklogAiTagsApiDto;
import com.ibank.axwms.domain.worklog.dto.UpdateWorklogAiApiDto;
import com.ibank.axwms.domain.worklog.service.InternalWorklogAiCallbackService;
import com.ibank.axwms.global.response.EmptyResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/worklogs")
@RequiredArgsConstructor
public class InternalWorklogAiCallbackController implements InternalWorklogAiCallbackControllerDocs {

    private final InternalWorklogAiCallbackService internalWorklogAiCallbackService;
    private final InternalTagAiCallbackService internalTagAiCallbackService;

    @Override
    @PatchMapping("/{worklogId}/ai-result")
    public EmptyResponse updateWorklogAiResult(@PathVariable Long worklogId, @Valid @RequestBody UpdateWorklogAiApiDto.Request request) {

        internalWorklogAiCallbackService.updateAiResult(worklogId, request);

        return EmptyResponse.INSTANCE;
    }

    @Override
    @PostMapping("/{worklogId}/ai-tags")
    public EmptyResponse applyAiGeneratedTags(
            @PathVariable Long worklogId,
            @Valid @RequestBody ApplyWorklogAiTagsApiDto.Request request
    ) {
        internalTagAiCallbackService.applyAiGeneratedTags(worklogId, request);
        return EmptyResponse.INSTANCE;
    }
}
