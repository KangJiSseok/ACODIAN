package com.ibank.axwms.domain.worklog.controller;

import com.ibank.axwms.domain.worklog.dto.UpdateWorklogAiApiDto;
import com.ibank.axwms.domain.worklog.service.InternalWorklogAiCallbackService;
import com.ibank.axwms.global.response.EmptyResponse;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/worklogs")
@RequiredArgsConstructor
public class InternalWorklogAiCallbackController implements InternalWorklogAiCallbackControllerDocs {

    private final InternalWorklogAiCallbackService internalWorklogAiCallbackService;

    @Override
    @PatchMapping("/{worklogId}/ai-result")
    public EmptyResponse updateWorklogAiResult(@PathVariable Long worklogId, @Valid @RequestBody UpdateWorklogAiApiDto.Request request) {

        internalWorklogAiCallbackService.updateAiResult(worklogId, request);

        return EmptyResponse.INSTANCE;
    }
}
