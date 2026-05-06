package com.ibank.axwms.domain.dashboard.controller;

import com.ibank.axwms.domain.dashboard.dto.GetDashboardApiDto;
import com.ibank.axwms.domain.dashboard.service.DashboardService;
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
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController implements DashboardControllerDocs {

    private final DashboardService dashboardService;

    @Override
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public GetDashboardApiDto.Response getDashboard(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @ModelAttribute GetDashboardApiDto.Request request
    ) {
        return dashboardService.getDashboard(principal, request);
    }
}
