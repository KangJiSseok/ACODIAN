package com.ibank.axwms.domain.organization.team.service;

import com.ibank.axwms.domain.organization.team.UserTeamStatus;
import com.ibank.axwms.domain.organization.team.dto.BulkUpsertTeamUsersApiDto;
import com.ibank.axwms.domain.organization.team.dto.CreateTeamApiDto;
import com.ibank.axwms.domain.organization.team.dto.GetTeamDetailApiDto;
import com.ibank.axwms.domain.organization.team.dto.GetTeamUsersApiDto;
import com.ibank.axwms.domain.organization.team.dto.GetTeamWorklogsApiDto;
import com.ibank.axwms.domain.organization.team.dto.GetTeamsApiDto;
import com.ibank.axwms.domain.organization.team.dto.GetTeamsSummaryApiDto;
import com.ibank.axwms.domain.organization.team.dto.UpdateTeamApiDto;
import com.ibank.axwms.domain.organization.team.dto.UpdateTeamStatusApiDto;
import com.ibank.axwms.domain.organization.team.entity.Team;
import com.ibank.axwms.domain.organization.team.entity.UserTeam;
import com.ibank.axwms.domain.organization.team.repository.TeamRepository;
import com.ibank.axwms.domain.organization.team.repository.UserTeamRepository;
import com.ibank.axwms.domain.organization.team.repository.jooq.query.TeamPageQuery;
import com.ibank.axwms.domain.organization.team.repository.jooq.query.TeamSummaryQuery;
import com.ibank.axwms.domain.organization.team.repository.jooq.query.TeamUsersQuery;
import com.ibank.axwms.domain.organization.team.repository.jooq.query.TeamWorklogsQuery;
import com.ibank.axwms.domain.organization.user.UserRole;
import com.ibank.axwms.domain.organization.user.entity.User;
import com.ibank.axwms.domain.organization.user.repository.UserRepository;
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import com.ibank.axwms.global.response.EmptyResponse;
import com.ibank.axwms.global.response.PageResponse;
import com.ibank.axwms.global.security.CustomUserPrincipal;
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

    /** 조회 query 를 정규화한 뒤 repository projection 을 API 응답으로 조립한다. */
    public PageResponse<GetTeamsApiDto.Response.Item> getTeams(CustomUserPrincipal principal, GetTeamsApiDto.Request request) {
        return GetTeamsApiDto.Response.fromPage(teamRepository.findTeamPage(normalizeTeamPageQuery(principal, request)));
    }

    /** 팀 목록과 분리된 상단 집계를 최신 spec 응답으로 반환한다. */
    public GetTeamsSummaryApiDto.Response getTeamSummary(CustomUserPrincipal principal, GetTeamsSummaryApiDto.Request request) {
        teamAccessPolicy.assertReadable(principal);
        return GetTeamsSummaryApiDto.Response.from(teamRepository.findTeamSummary(normalizeTeamSummaryQuery(principal, request)));
    }

    /** team 상세 skeleton 시그니처를 유지하면서 projection 이 있으면 API 응답으로 조립한다. */
    public GetTeamDetailApiDto.Response getTeamDetail(CustomUserPrincipal principal, Long teamId) {
        assertReadableTeamOwnership(principal, teamId);
        return teamRepository.findTeamDetail(teamId)
                .map(GetTeamDetailApiDto.Response::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON_INTERNAL_SERVER_ERROR));
    }

    /** 최신 spec 의 page/pageSize 계약만 남기고 repository query 로 변환한다. */
    public PageResponse<GetTeamUsersApiDto.Response.Item> getTeamUsers(CustomUserPrincipal principal,
                                                                       Long teamId,
                                                                       GetTeamUsersApiDto.Request request) {
        assertReadableTeamOwnership(principal, teamId);
        return GetTeamUsersApiDto.Response.fromPage(userTeamRepository.findTeamUserPage(teamId, normalizeTeamUsersQuery(request)));
    }

    /** 최신 spec 의 page/pageSize 계약만 남기고 repository query 로 변환한다. */
    public PageResponse<GetTeamWorklogsApiDto.Response.Item> getTeamWorklogs(CustomUserPrincipal principal,
                                                                             Long teamId,
                                                                             GetTeamWorklogsApiDto.Request request) {
        assertReadableTeamOwnership(principal, teamId);
        return GetTeamWorklogsApiDto.Response.fromPage(teamRepository.findTeamWorklogPage(teamId, normalizeTeamWorklogsQuery(request)));
    }

    /** 생성 계열은 최신 spec 의 EmptyResponse/201 Created 계약을 유지한다. */
    @Transactional
    public EmptyResponse createTeam(CustomUserPrincipal principal, CreateTeamApiDto.Request request) {
        teamAccessPolicy.assertWritable(principal);
        User principalUser = getRequiredPrincipalUser(principal);
        teamAccessPolicy.assertDepartmentOwnership(principal, principalUser.getDepartmentId(), request.departmentId());
        return EmptyResponse.INSTANCE;
    }

    /** 수정 계열은 최신 spec 의 EmptyResponse 계약을 유지한다. */
    @Transactional
    public EmptyResponse updateTeam(CustomUserPrincipal principal, Long teamId, UpdateTeamApiDto.Request request) {
        assertWritableTeamOwnership(principal, teamId);
        return EmptyResponse.INSTANCE;
    }

    /** 상태 전환은 운영 상태와 soft-delete lifecycle 이 분리된 최신 계약만 고정한다. */
    @Transactional
    public EmptyResponse updateTeamStatus(CustomUserPrincipal principal,
                                          Long teamId,
                                          UpdateTeamStatusApiDto.Request request) {
        assertWritableTeamOwnership(principal, teamId);
        return EmptyResponse.INSTANCE;
    }

    /** users/bulk endpoint 는 add/remove body shape 을 받되 성공 응답은 EmptyResponse 로 통일한다. */
    @Transactional
    public EmptyResponse bulkUpsertTeamUsers(CustomUserPrincipal principal,
                                             Long teamId,
                                             BulkUpsertTeamUsersApiDto.Request request) {
        assertWritableTeamOwnership(principal, teamId);
        return EmptyResponse.INSTANCE;
    }

    /** 삭제는 hard delete 가 아니라 deletedAt 마킹 계약만 먼저 고정한다. */
    @Transactional
    public EmptyResponse deleteTeam(CustomUserPrincipal principal, Long teamId) {
        assertWritableTeamOwnership(principal, teamId);
        return EmptyResponse.INSTANCE;
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

    /**
     * 팀 ID로 팀을 조회하고 없으면 도메인 오류를 던진다.
     *
     * @param teamId 조회할 팀 ID
     * @return 존재하는 팀 엔티티
     * @throws BusinessException TEAM_NOT_FOUND 팀이 존재하지 않을 때
     */
    public Team getTeamOrThrow(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
    }

    /**
     * 사용자가 해당 팀 소속인지 여부를 반환한다.
     * 호출측(예: 업무 등록) 이 "대상 팀에 대한 쓰기 권한" 을 Principal 기반으로 판단할 때 사용한다.
     * 소속 미존재 시 던질 도메인 에러 코드는 호출측 유스케이스가 결정한다.
     *
     * @param userId 검증 대상 사용자 ID
     * @param teamId 검증 대상 팀 ID
     * @return 사용자가 해당 팀에 속해 있으면 true
     */
    public boolean isMember(Long userId, Long teamId) {
        return userTeamRepository.existsByUserIdAndTeamId(userId, teamId);
    }

    /**  role에 따라 repository에 넘길 query DTO를 각 각 다르게 채움. */
    private TeamPageQuery normalizeTeamPageQuery(CustomUserPrincipal principal, GetTeamsApiDto.Request request) {
        GetTeamsApiDto.Request normalizedRequest = request == null
                ? new GetTeamsApiDto.Request(null, null, null)
                : request;
        UserRole role = getRequiredRole(principal);

        //
        Long principalDepartmentId = role == UserRole.DEPT_HEAD
                ? getRequiredPrincipalUser(principal).getDepartmentId()
                : null;

        return new TeamPageQuery(
                normalizedRequest.pageOrDefault(),
                normalizedRequest.pageSizeOrDefault(),
                normalizedRequest.departmentId(),
                principalDepartmentId,
                principal.userId(),
                role
        );
    }

    private TeamSummaryQuery normalizeTeamSummaryQuery(CustomUserPrincipal principal, GetTeamsSummaryApiDto.Request request) {
        GetTeamsSummaryApiDto.Request normalizedRequest = request == null
                ? new GetTeamsSummaryApiDto.Request(null)
                : request;
        UserRole role = getRequiredRole(principal);
        Long requestedDepartmentId = normalizedRequest.departmentId();
        if (role == UserRole.DEPT_HEAD) {
            User principalUser = getRequiredPrincipalUser(principal);
            Long effectiveDepartmentId = requestedDepartmentId == null ? principalUser.getDepartmentId() : requestedDepartmentId;
            teamAccessPolicy.assertDepartmentOwnership(principal, principalUser.getDepartmentId(), effectiveDepartmentId);
            return new TeamSummaryQuery(effectiveDepartmentId, null);
        }
        if (role == UserRole.TEAM_LEAD || role == UserRole.MEMBER) {
            return new TeamSummaryQuery(requestedDepartmentId, getRequiredPrincipalTeamId(principal.userId()));
        }
        return new TeamSummaryQuery(requestedDepartmentId, null);
    }

    private TeamUsersQuery normalizeTeamUsersQuery(GetTeamUsersApiDto.Request request) {
        GetTeamUsersApiDto.Request normalizedRequest = request == null
                ? new GetTeamUsersApiDto.Request(null, null)
                : request;
        return new TeamUsersQuery(normalizedRequest.pageOrDefault(), normalizedRequest.pageSizeOrDefault());
    }

    private TeamWorklogsQuery normalizeTeamWorklogsQuery(GetTeamWorklogsApiDto.Request request) {
        GetTeamWorklogsApiDto.Request normalizedRequest = request == null
                ? new GetTeamWorklogsApiDto.Request(null, null)
                : request;
        return new TeamWorklogsQuery(normalizedRequest.pageOrDefault(), normalizedRequest.pageSizeOrDefault());
    }
}
