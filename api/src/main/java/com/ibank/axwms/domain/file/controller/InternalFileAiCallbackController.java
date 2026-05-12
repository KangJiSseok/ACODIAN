package com.ibank.axwms.domain.file.controller;

import com.ibank.axwms.domain.file.dto.UpdateFileAiApiDto;
import com.ibank.axwms.domain.file.service.InternalFileAiCallbackService;
import com.ibank.axwms.global.response.EmptyResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/files")
@RequiredArgsConstructor
public class InternalFileAiCallbackController implements InternalFileAiCallbackControllerDocs {

    private final InternalFileAiCallbackService internalFileAiCallbackService;

    @Override
    @PatchMapping("/{fileId}/ai-result")
    public EmptyResponse updateFileAiResult(@PathVariable Long fileId, @Valid @RequestBody UpdateFileAiApiDto.Request request) {

        internalFileAiCallbackService.updateAiResult(fileId, request);

        return EmptyResponse.INSTANCE;
    }
}
