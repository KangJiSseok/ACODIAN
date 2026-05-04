package com.ibank.axwms.domain.organization.skill.controller;

import com.ibank.axwms.domain.organization.skill.dto.GetSkillsApiDto;
import com.ibank.axwms.domain.organization.skill.service.UserSkillService;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
