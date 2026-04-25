package com.ibank.axwms.domain.organization.team.service;

import com.ibank.axwms.domain.organization.team.UserTeamStatus;
import com.ibank.axwms.domain.organization.team.dto.BulkUpsertTeamUsersApiDto;
import com.ibank.axwms.domain.organization.team.dto.CreateTeamApiDto;
import com.ibank.axwms.domain.organization.team.dto.DeleteTeamApiDto;
import com.ibank.axwms.domain.organization.team.dto.GetTeamDetailApiDto;
import com.ibank.axwms.domain.organization.team.dto.GetTeamUsersApiDto;
import com.ibank.axwms.domain.organization.team.dto.GetTeamWorklogsApiDto;
import com.ibank.axwms.domain.organization.team.dto.GetTeamsApiDto;
import com.ibank.axwms.domain.organization.team.dto.UpdateTeamApiDto;
import com.ibank.axwms.domain.organization.team.dto.UpdateTeamStatusApiDto;
import com.ibank.axwms.domain.organization.team.entity.Team;
import com.ibank.axwms.domain.organization.team.entity.UserTeam;
import com.ibank.axwms.domain.organization.team.repository.TeamRepository;
import com.ibank.axwms.domain.organization.team.repository.UserTeamRepository;
import com.ibank.axwms.domain.organization.user.UserRole;
import com.ibank.axwms.domain.organization.user.entity.User;
import com.ibank.axwms.domain.organization.user.repository.UserRepository;
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import com.ibank.axwms.global.response.PageResponse;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamService {

    private final TeamAccessPolicy teamAccessPolicy;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final UserTeamRepository userTeamRepository;

    /** team 목록 skeleton 시그니처를 유지하면서 repository projection 을 API 응답으로 조립한다. */
    public PageResponse<GetTeamsApiDto.Response.Item> getTeams(CustomUserPrincipal principal, GetTeamsApiDto.Request request) {
        teamAccessPolicy.assertReadable(principal);
        GetTeamsApiDto.Request normalizedRequest = request == null
                ? new GetTeamsApiDto.Request(null, null, null, null, null, null, null)
                : request;
        if (normalizedRequest.departmentId() != null) {
            User principalUser = getRequiredPrincipalUser(principal);
            teamAccessPolicy.assertDepartmentOwnership(principal, principalUser.getDepartmentId(), normalizedRequest.departmentId());
        }
        return GetTeamsApiDto.Response.fromPage(teamRepository.findTeamPage(normalizedRequest));
    }

    /** team 상세 skeleton 시그니처를 유지하면서 projection 이 있으면 API 응답으로 조립한다. */
    public GetTeamDetailApiDto.Response getTeamDetail(CustomUserPrincipal principal, Long teamId) {
        assertReadableTeamOwnership(principal, teamId);
        return teamRepository.findTeamDetail(teamId)
                .map(GetTeamDetailApiDto.Response::from)
                .orElseGet(() -> GetTeamDetailApiDto.Response.placeholder(teamId));
    }

    /** team users skeleton 시그니처를 유지하면서 projection 페이지를 API 응답으로 조립한다. */
    public PageResponse<GetTeamUsersApiDto.Response.Item> getTeamUsers(CustomUserPrincipal principal,
                                                                       Long teamId,
                                                                       GetTeamUsersApiDto.Request request) {
        assertReadableTeamOwnership(principal, teamId);
        GetTeamUsersApiDto.Request normalizedRequest = request == null
                ? new GetTeamUsersApiDto.Request(null, null, null, null, null, null)
                : request;
        return GetTeamUsersApiDto.Response.fromPage(userTeamRepository.findTeamUserPage(teamId, normalizedRequest));
    }

    /** team worklogs skeleton 시그니처를 유지하면서 projection 페이지를 API 응답으로 조립한다. */
    public PageResponse<GetTeamWorklogsApiDto.Response.Item> getTeamWorklogs(CustomUserPrincipal principal,
                                                                             Long teamId,
                                                                             GetTeamWorklogsApiDto.Request request) {
        assertReadableTeamOwnership(principal, teamId);
        GetTeamWorklogsApiDto.Request normalizedRequest = request == null
                ? new GetTeamWorklogsApiDto.Request(null, null, null, null, null, null)
                : request;
        return GetTeamWorklogsApiDto.Response.fromPage(teamRepository.findTeamWorklogPage(teamId, normalizedRequest));
    }

    /** 생성 계열은 skeleton 단계에서 response shape 만 고정한다. */
    @Transactional
    public CreateTeamApiDto.Response createTeam(CustomUserPrincipal principal, CreateTeamApiDto.Request request) {
        teamAccessPolicy.assertWritable(principal);
        User principalUser = getRequiredPrincipalUser(principal);
        teamAccessPolicy.assertDepartmentOwnership(principal, principalUser.getDepartmentId(), request.departmentId());
        return CreateTeamApiDto.Response.placeholder();
    }

    /** 수정 계열은 skeleton 단계에서 response shape 만 고정한다. */
    @Transactional
    public UpdateTeamApiDto.Response updateTeam(CustomUserPrincipal principal, Long teamId, UpdateTeamApiDto.Request request) {
        assertWritableTeamOwnership(principal, teamId);
        return UpdateTeamApiDto.Response.of(teamId);
    }

    /** 상태 전환은 운영 상태와 soft-delete lifecycle 이 분리된 계약만 먼저 고정한다. */
    @Transactional
    public UpdateTeamStatusApiDto.Response updateTeamStatus(CustomUserPrincipal principal,
                                                            Long teamId,
                                                            UpdateTeamStatusApiDto.Request request) {
        assertWritableTeamOwnership(principal, teamId);
        return UpdateTeamStatusApiDto.Response.of(teamId, request.statusCode());
    }

    /** users/bulk endpoint 는 legacy members/bulk 를 대체하는 canonical 경로로 고정한다. */
    @Transactional
    public BulkUpsertTeamUsersApiDto.Response bulkUpsertTeamUsers(CustomUserPrincipal principal,
                                                                  Long teamId,
                                                                  BulkUpsertTeamUsersApiDto.Request request) {
        assertWritableTeamOwnership(principal, teamId);
        return BulkUpsertTeamUsersApiDto.Response.of(teamId, request.items().size());
    }

    /** 삭제는 hard delete 가 아니라 deletedAt 마킹 계약만 먼저 고정한다. */
    @Transactional
    public DeleteTeamApiDto.Response deleteTeam(CustomUserPrincipal principal, Long teamId) {
        assertWritableTeamOwnership(principal, teamId);
        return DeleteTeamApiDto.Response.of(teamId, LocalDateTime.now());
    }

    /** 현재 인증 주체의 조직 문맥을 복원해 부서 ownership 검증의 기준값으로 사용한다. */
    private User getRequiredPrincipalUser(CustomUserPrincipal principal) {
        return userRepository.findById(principal.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    /** team-id 기반 skeleton endpoint 가 soft-delete 된 팀이나 존재하지 않는 팀에 성공 응답을 주지 않도록 보호한다. */
    private Team getReadableTargetTeam(Long teamId) {
        return teamRepository.findById(teamId)
                .filter(team -> team.getDeletedAt() == null)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_ACCESS_DENIED));
    }

    /** 읽기 endpoint 는 역할 계층과 현재 사용자 부서/팀 문맥에 맞는 ownership 검증까지 수행한다. */
    private void assertReadableTeamOwnership(CustomUserPrincipal principal, Long teamId) {
        teamAccessPolicy.assertReadable(principal);
        User principalUser = getRequiredPrincipalUser(principal);
        Team targetTeam = getReadableTargetTeam(teamId);
        UserRole role = getRequiredRole(principal);
        if (role == UserRole.DIRECTOR) {
            return;
        }
        if (role == UserRole.DEPT_HEAD) {
            teamAccessPolicy.assertDepartmentOwnership(principal, principalUser.getDepartmentId(), targetTeam.getDepartmentId());
            return;
        }
        teamAccessPolicy.assertTeamOwnership(principal, getRequiredPrincipalTeamId(principal.userId()), targetTeam.getId());
    }

    /** 쓰기 endpoint 는 최소 role gate 이후에도 대상 팀이 같은 부서 ownership 안에 있는지 검증한다. */
    private void assertWritableTeamOwnership(CustomUserPrincipal principal, Long teamId) {
        teamAccessPolicy.assertWritable(principal);
        User principalUser = getRequiredPrincipalUser(principal);
        Team targetTeam = getReadableTargetTeam(teamId);
        teamAccessPolicy.assertDepartmentOwnership(principal, principalUser.getDepartmentId(), targetTeam.getDepartmentId());
    }

    /** TEAM_LEAD/MEMBER 읽기 검증에 사용할 현재 사용자의 대표 ACTIVE 팀 id 를 복원한다. */
    private Long getRequiredPrincipalTeamId(Long userId) {
        Optional<UserTeam> principalTeam = userTeamRepository.findFirstByUserIdAndStatusCodeOrderByIsPrimaryDesc(userId, UserTeamStatus.ACTIVE);
        return principalTeam
                .map(UserTeam::getTeamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_ACCESS_DENIED));
    }

    /** 서비스 ownership 분기에서 사용할 현재 사용자 역할 코드를 비즈니스 예외로 정규화한다. */
    private UserRole getRequiredRole(CustomUserPrincipal principal) {
        try {
            return UserRole.valueOf(principal.roleCode());
        } catch (RuntimeException ex) {
            throw new BusinessException(ErrorCode.AUTH_ACCESS_DENIED);
        }
    }
}
