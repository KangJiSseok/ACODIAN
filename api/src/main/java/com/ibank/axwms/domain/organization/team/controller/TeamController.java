package com.ibank.axwms.domain.organization.team.controller;

import com.ibank.axwms.domain.organization.team.dto.CreateTeamApiDto;
import com.ibank.axwms.domain.organization.team.dto.GetTeamApiDto;
import com.ibank.axwms.domain.organization.team.dto.GetTeamSummaryApiDto;
import com.ibank.axwms.domain.organization.team.dto.GetTeamUsersApiDto;
import com.ibank.axwms.domain.organization.team.dto.GetTeamsApiDto;
import com.ibank.axwms.domain.organization.team.dto.UpdateTeamApiDto;
import com.ibank.axwms.domain.organization.team.service.TeamService;
import com.ibank.axwms.global.response.EmptyResponse;
import com.ibank.axwms.global.response.PageResponse;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/teams")
@RequiredArgsConstructor
public class TeamController implements TeamControllerDocs {

    private final TeamService teamService;

    @Override
    @GetMapping
    @PreAuthorize("hasAnyRole('DIRECTOR','DEPT_HEAD','TEAM_LEAD','MEMBER')")
    public PageResponse<GetTeamsApiDto.Response> getTeams(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @ModelAttribute GetTeamsApiDto.Request request
    ) {
        return teamService.getTeams(principal, request);
    }

    @Override
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('DIRECTOR','DEPT_HEAD','TEAM_LEAD','MEMBER')")
    public GetTeamSummaryApiDto.Response getTeamSummary(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return teamService.getTeamSummary(principal);
    }

    @Override
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRECTOR','DEPT_HEAD','TEAM_LEAD','MEMBER')")
    public GetTeamApiDto.Response getTeam(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long id
    ) {
        return teamService.getTeam(principal, id);
    }

    @Override
    @GetMapping("/{id}/users")
    @PreAuthorize("hasAnyRole('DIRECTOR','DEPT_HEAD','TEAM_LEAD','MEMBER')")
    public GetTeamUsersApiDto.Response getTeamUsers(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long id
    ) {
        return teamService.getTeamUsers(principal, id);
    }

    @Override
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('DIRECTOR','DEPT_HEAD')")
    public EmptyResponse createTeam(@Valid @RequestBody CreateTeamApiDto.Request request) {
        teamService.createTeam(request);
        return EmptyResponse.INSTANCE;
    }

    @Override
    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRECTOR','DEPT_HEAD')")
    public EmptyResponse updateTeam(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateTeamApiDto.Request request
    ) {
        teamService.updateTeam(principal, id, request);
        return EmptyResponse.INSTANCE;
    }
}
