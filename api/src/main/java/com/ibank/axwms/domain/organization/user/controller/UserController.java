package com.ibank.axwms.domain.organization.user.controller;

import com.ibank.axwms.domain.organization.user.dto.GetManagerCandidatesApiDto;
import com.ibank.axwms.domain.organization.user.dto.GetMyProfileApiDto;
import com.ibank.axwms.domain.organization.user.service.UserService;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
    @GetMapping("/manager-candidates")
    @PreAuthorize("hasAnyRole('DIRECTOR','DEPT_HEAD')")
    public List<GetManagerCandidatesApiDto.Response> getManagerCandidates(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return userService.getManagerCandidates(principal);
    }
}
