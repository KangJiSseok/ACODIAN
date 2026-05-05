package com.ibank.axwms.domain.organization.user.controller;

import com.ibank.axwms.domain.organization.user.dto.GetAdminCandidatesApiDto;
import com.ibank.axwms.domain.organization.user.dto.GetDepartmentCandidatesApiDto;
import com.ibank.axwms.domain.organization.user.dto.GetMyProfileApiDto;
import com.ibank.axwms.domain.organization.user.dto.GetUserApiDto;
import com.ibank.axwms.domain.organization.user.dto.GetUsersApiDto;
import com.ibank.axwms.domain.organization.user.dto.UpdateUserApiDto;
import com.ibank.axwms.domain.organization.user.service.UserService;
import com.ibank.axwms.global.response.EmptyResponse;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController implements UserControllerDocs {

    private final UserService userService;

    @Override
    @GetMapping("/me")
    public GetMyProfileApiDto.Response getMyProfile(@AuthenticationPrincipal CustomUserPrincipal principal) {
        return userService.getMyProfile(principal);
    }

    @Override
    @GetMapping("/{id}")
    public GetUserApiDto.Response getUser(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long id
    ) {
        return userService.getUser(id);
    }

    @Override
    @GetMapping("/admin-candidates")
    @PreAuthorize("hasAnyRole('DIRECTOR','DEPT_HEAD')")
    public List<GetAdminCandidatesApiDto.Response> getAdminCandidates(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return userService.getAdminCandidates(principal);
    }

    @Override
    @GetMapping("/department-candidates")
    @PreAuthorize("hasRole('DIRECTOR')")
    public List<GetDepartmentCandidatesApiDto.Response> getDepartmentCandidates(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return userService.getDepartmentCandidates();
    }

    @Override
    @GetMapping
    @PreAuthorize("hasAnyRole('DIRECTOR','DEPT_HEAD')")
    public List<GetUsersApiDto.Response> getUsers(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @ModelAttribute GetUsersApiDto.Request request
    ) {
        return userService.getUsers(request);
    }

    @Override
    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRECTOR','DEPT_HEAD')")
    public EmptyResponse updateUser(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserApiDto.Request request
    ) {
        userService.updateUser(principal, id, request);
        return EmptyResponse.INSTANCE;
    }
}
