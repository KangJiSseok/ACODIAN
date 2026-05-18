package com.ibank.axwms.domain.tag.controller;

import com.ibank.axwms.domain.tag.TagMergeCandidateStatus;
import com.ibank.axwms.domain.tag.dto.TagMergeCandidateApiDto;
import com.ibank.axwms.domain.tag.service.TagMergeCandidateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tags/merge-candidates")
@RequiredArgsConstructor
public class TagMergeCandidateController implements TagMergeCandidateControllerDocs {

    private final TagMergeCandidateService tagMergeCandidateService;

    @Override
    @PostMapping
    @PreAuthorize("hasAnyRole('DIRECTOR','DEPT_HEAD')")
    public TagMergeCandidateApiDto.Response createCandidates(
            @Valid @RequestBody TagMergeCandidateApiDto.Request request
    ) {
        return tagMergeCandidateService.createCandidates(request);
    }

    @Override
    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('DIRECTOR','DEPT_HEAD')")
    public TagMergeCandidateApiDto.Response generateCandidates(
            @Valid @RequestBody TagMergeCandidateApiDto.GenerateRequest request
    ) {
        return tagMergeCandidateService.generateCandidates(request);
    }

    @Override
    @GetMapping
    @PreAuthorize("hasAnyRole('DIRECTOR','DEPT_HEAD')")
    public TagMergeCandidateApiDto.Response getCandidates(
            @RequestParam(required = false) TagMergeCandidateStatus statusCode
    ) {
        return tagMergeCandidateService.getCandidates(statusCode);
    }

    @Override
    @PostMapping("/{mergeCandidateId}/merge")
    @PreAuthorize("hasAnyRole('DIRECTOR','DEPT_HEAD')")
    public void mergeCandidate(@PathVariable Long mergeCandidateId) {
        tagMergeCandidateService.mergeCandidate(mergeCandidateId);
    }
}
