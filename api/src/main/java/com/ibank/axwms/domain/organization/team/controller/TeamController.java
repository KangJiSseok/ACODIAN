package com.ibank.axwms.domain.organization.team.controller;

import com.ibank.axwms.domain.organization.team.dto.BulkUpsertTeamUsersApiDto;
import com.ibank.axwms.domain.organization.team.dto.CreateTeamApiDto;
import com.ibank.axwms.domain.organization.team.dto.GetTeamDetailApiDto;
import com.ibank.axwms.domain.organization.team.dto.GetTeamUsersApiDto;
import com.ibank.axwms.domain.organization.team.dto.GetTeamWorklogsApiDto;
import com.ibank.axwms.domain.organization.team.dto.GetTeamsApiDto;
import com.ibank.axwms.domain.organization.team.dto.GetTeamsSummaryApiDto;
import com.ibank.axwms.domain.organization.team.dto.UpdateTeamApiDto;
import com.ibank.axwms.domain.organization.team.dto.UpdateTeamStatusApiDto;
import com.ibank.axwms.domain.organization.team.service.TeamService;
import com.ibank.axwms.global.response.EmptyResponse;
import com.ibank.axwms.global.response.PageResponse;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
    public PageResponse<GetTeamsApiDto.Response.Item> getTeams(@AuthenticationPrincipal CustomUserPrincipal principal,
                                                               @Valid @ModelAttribute GetTeamsApiDto.Request request) {
        return teamService.getTeams(principal, request);
    }

    @Override
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('DIRECTOR','DEPT_HEAD','TEAM_LEAD','MEMBER')")
    public GetTeamsSummaryApiDto.Response getTeamSummary(@AuthenticationPrincipal CustomUserPrincipal principal,
                                                         @Valid @ModelAttribute GetTeamsSummaryApiDto.Request request) {
        return teamService.getTeamSummary(principal, request);
    }

    @Override
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRECTOR','DEPT_HEAD','TEAM_LEAD','MEMBER')")
    public GetTeamDetailApiDto.Response getTeamDetail(@AuthenticationPrincipal CustomUserPrincipal principal,
                                                      @PathVariable Long id) {
        return teamService.getTeamDetail(principal, id);
    }

    @Override
    @GetMapping("/{id}/users")
    @PreAuthorize("hasAnyRole('DIRECTOR','DEPT_HEAD','TEAM_LEAD','MEMBER')")
    public PageResponse<GetTeamUsersApiDto.Response.Item> getTeamUsers(@AuthenticationPrincipal CustomUserPrincipal principal,
                                                                       @PathVariable Long id,
                                                                       @Valid @ModelAttribute GetTeamUsersApiDto.Request request) {
        return teamService.getTeamUsers(principal, id, request);
    }

    @Override
    @GetMapping("/{id}/worklogs")
    @PreAuthorize("hasAnyRole('DIRECTOR','DEPT_HEAD','TEAM_LEAD','MEMBER')")
    public PageResponse<GetTeamWorklogsApiDto.Response.Item> getTeamWorklogs(@AuthenticationPrincipal CustomUserPrincipal principal,
                                                                             @PathVariable Long id,
                                                                             @Valid @ModelAttribute GetTeamWorklogsApiDto.Request request) {
        return teamService.getTeamWorklogs(principal, id, request);
    }

    @Override
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('DIRECTOR','DEPT_HEAD')")
    public EmptyResponse createTeam(@AuthenticationPrincipal CustomUserPrincipal principal,
                                    @Valid @RequestBody CreateTeamApiDto.Request request) {
        return teamService.createTeam(principal, request);
    }

    @Override
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRECTOR','DEPT_HEAD')")
    public EmptyResponse updateTeam(@AuthenticationPrincipal CustomUserPrincipal principal,
                                    @PathVariable Long id,
                                    @Valid @RequestBody UpdateTeamApiDto.Request request) {
        return teamService.updateTeam(principal, id, request);
    }

    @Override
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('DIRECTOR','DEPT_HEAD')")
    public EmptyResponse updateTeamStatus(@AuthenticationPrincipal CustomUserPrincipal principal,
                                          @PathVariable Long id,
                                          @Valid @RequestBody UpdateTeamStatusApiDto.Request request) {
        return teamService.updateTeamStatus(principal, id, request);
    }

    @Override
    @PostMapping("/{id}/users/bulk")
    @PreAuthorize("hasAnyRole('DIRECTOR','DEPT_HEAD')")
    public EmptyResponse bulkUpsertTeamUsers(@AuthenticationPrincipal CustomUserPrincipal principal,
                                             @PathVariable Long id,
                                             @Valid @RequestBody BulkUpsertTeamUsersApiDto.Request request) {
        return teamService.bulkUpsertTeamUsers(principal, id, request);
    }

    @Override
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRECTOR','DEPT_HEAD')")
    public EmptyResponse deleteTeam(@AuthenticationPrincipal CustomUserPrincipal principal,
                                    @PathVariable Long id) {
        teamService.deleteTeam(principal, id);
        return EmptyResponse.INSTANCE;
    }
}
