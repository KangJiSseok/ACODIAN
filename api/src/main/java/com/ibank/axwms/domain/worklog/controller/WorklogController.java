package com.ibank.axwms.domain.worklog.controller;

import com.ibank.axwms.domain.worklog.dto.CreateWorklogApiDto;
import com.ibank.axwms.domain.worklog.dto.GetWorklogDetailApiDto;
import com.ibank.axwms.domain.worklog.dto.GetWorklogFilterOptionsApiDto;
import com.ibank.axwms.domain.worklog.dto.GetWorklogOptionsApiDto;
import com.ibank.axwms.domain.worklog.dto.GetWorklogsApiDto;
import com.ibank.axwms.domain.worklog.dto.SearchWorklogsApiDto;
import com.ibank.axwms.domain.worklog.service.WorklogService;
import com.ibank.axwms.domain.worklog.service.search.WorklogSearchService;
import com.ibank.axwms.global.response.PageResponse;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/worklogs")
@RequiredArgsConstructor
public class WorklogController implements WorklogControllerDocs {

    private final WorklogService worklogService;
    private final WorklogSearchService worklogSearchService;

    @Override
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateWorklogApiDto.Response createWorklog(
            @Valid @RequestPart CreateWorklogApiDto.Request request,
            @RequestPart List<MultipartFile> files,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return worklogService.createWorklog(principal, request, files);
    }

    @Override
    @GetMapping
    @PreAuthorize("hasAnyRole('DIRECTOR','DEPT_HEAD','TEAM_LEAD','MEMBER')")
    public PageResponse<GetWorklogsApiDto.Response.Item> getWorklogs(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @ModelAttribute GetWorklogsApiDto.Request request
    ) {
        return worklogService.getWorklogs(principal, request);
    }

    @Override
    @GetMapping("/{worklogId}")
    @PreAuthorize("hasAnyRole('DIRECTOR','DEPT_HEAD','TEAM_LEAD','MEMBER')")
    public GetWorklogDetailApiDto.Response getWorklogDetail(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long worklogId
    ) {
        return worklogService.getWorklogDetail(principal, worklogId);
    }

    @Override
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('DIRECTOR','DEPT_HEAD','TEAM_LEAD','MEMBER')")
    public PageResponse<SearchWorklogsApiDto.Response.Item> searchWorklogs(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @ModelAttribute SearchWorklogsApiDto.Request request
    ) {
        return worklogSearchService.searchWorklogs(principal, request);
    }

    @Override
    @GetMapping("/filter-options")
    @PreAuthorize("hasAnyRole('DIRECTOR','DEPT_HEAD','TEAM_LEAD','MEMBER')")
    public GetWorklogFilterOptionsApiDto.Response getFilterOptions(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return worklogSearchService.getFilterOptions(principal);
    }

    @Override
    @GetMapping("/options")
    @PreAuthorize("hasAnyRole('DIRECTOR','DEPT_HEAD','TEAM_LEAD','MEMBER')")
    public GetWorklogOptionsApiDto.Response getWorklogOptions(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return worklogService.getWorklogOptions(principal);
    }
}
