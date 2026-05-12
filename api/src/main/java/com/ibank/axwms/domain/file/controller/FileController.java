package com.ibank.axwms.domain.file.controller;

import com.ibank.axwms.domain.file.dto.GetFileTypesApiDto;
import com.ibank.axwms.domain.file.dto.GetFilesApiDto;
import com.ibank.axwms.domain.file.service.FileService;
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

import java.util.List;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController implements FileControllerDocs {

    private final FileService fileService;

    @GetMapping
    @PreAuthorize("hasAnyRole('DIRECTOR','DEPT_HEAD','TEAM_LEAD','MEMBER')")
    public PageResponse<GetFilesApiDto.Response.Item> getFiles(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @ModelAttribute GetFilesApiDto.Request request
    ) {
        return fileService.getFiles(principal, request);
    }

    @Override
    @GetMapping("/types")
    @PreAuthorize("hasAnyRole('DIRECTOR','DEPT_HEAD','TEAM_LEAD','MEMBER')")
    public List<GetFileTypesApiDto.Response> getFileTypes() {
        return fileService.getFileTypes();
    }
}
