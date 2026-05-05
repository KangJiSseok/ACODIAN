package com.ibank.axwms.domain.organization.skill.controller;

import com.ibank.axwms.domain.organization.skill.dto.CreateSkillApiDto;
import com.ibank.axwms.domain.organization.skill.dto.GetSkillsApiDto;
import com.ibank.axwms.domain.organization.skill.dto.UpdateSkillApiDto;
import com.ibank.axwms.domain.organization.skill.service.UserSkillService;
import com.ibank.axwms.global.response.EmptyResponse;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users/{userId}/skills")
@RequiredArgsConstructor
public class UserSkillController implements UserSkillControllerDocs {

    private final UserSkillService userSkillService;

    @Override
    @GetMapping
    @PreAuthorize("hasAnyRole('DIRECTOR','DEPT_HEAD')")
    public GetSkillsApiDto.Response getSkills(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long userId
    ) {
        return userSkillService.getSkills(principal, userId);
    }

    @Override
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('DIRECTOR','DEPT_HEAD')")
    public EmptyResponse createSkill(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long userId,
            @Valid @RequestBody CreateSkillApiDto.Request request
    ) {
        userSkillService.createSkill(principal, userId, request);
        return EmptyResponse.INSTANCE;
    }

    @Override
    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRECTOR','DEPT_HEAD')")
    public EmptyResponse updateSkill(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long userId,
            @PathVariable Long id,
            @Valid @RequestBody UpdateSkillApiDto.Request request
    ) {
        userSkillService.updateSkill(principal, userId, id, request);
        return EmptyResponse.INSTANCE;
    }

    @Override
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRECTOR','DEPT_HEAD')")
    public EmptyResponse deleteSkill(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long userId,
            @PathVariable Long id
    ) {
        userSkillService.deleteSkill(principal, userId, id);
        return EmptyResponse.INSTANCE;
    }
}
