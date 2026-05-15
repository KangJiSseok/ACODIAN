package com.ibank.axwms.domain.tag.controller;

import com.ibank.axwms.domain.tag.dto.SearchTagApiDto;
import com.ibank.axwms.domain.tag.service.TagService;
import com.ibank.axwms.global.response.PageResponse;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tags")
@RequiredArgsConstructor
public class TagController implements TagControllerDocs {

    private final TagService tagService;

    @Override
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('DIRECTOR','DEPT_HEAD','TEAM_LEAD','MEMBER')")
    public PageResponse<SearchTagApiDto.Response.Item> searchTag(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @ModelAttribute SearchTagApiDto.Request request
    ) {
        return tagService.searchTag(principal, request);
    }
}
