package com.ibank.axwms.domain.organization.team.service;

import com.ibank.axwms.domain.organization.team.UserTeamStatus;
import com.ibank.axwms.domain.organization.team.dto.CreateTeamApiDto;
import com.ibank.axwms.domain.organization.team.dto.GetTeamApiDto;
import com.ibank.axwms.domain.organization.team.dto.GetTeamSummaryApiDto;
import com.ibank.axwms.domain.organization.team.dto.GetTeamUsersApiDto;
import com.ibank.axwms.domain.organization.team.dto.GetTeamsApiDto;
import com.ibank.axwms.domain.organization.team.entity.Team;
import com.ibank.axwms.domain.organization.team.entity.TeamAdmin;
import com.ibank.axwms.domain.organization.team.entity.UserTeam;
import com.ibank.axwms.domain.organization.team.repository.TeamAdminRepository;
import com.ibank.axwms.domain.organization.team.repository.TeamRepository;
import com.ibank.axwms.domain.organization.team.repository.UserTeamRepository;
import com.ibank.axwms.domain.organization.team.repository.jooq.query.TeamPageQuery;
import com.ibank.axwms.domain.organization.user.UserRole;
import com.ibank.axwms.domain.organization.user.entity.User;
import com.ibank.axwms.domain.organization.user.repository.UserRepository;
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import com.ibank.axwms.global.response.PageResponse;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamService {
    private final TeamRepository teamRepository;
    private final TeamAdminRepository teamAdminRepository;
    private final UserTeamRepository userTeamRepository;
    private final UserRepository userRepository;

    /**
     * 로그인 사용자가 볼 수 있는 팀 목록을 페이지로 조회한다.
     * admin grant 팀과 ACTIVE membership 팀의 DISTINCT 합집합만 반환한다.
     *
     * @param principal 현재 로그인 사용자
     * @param request 페이지/사이즈 요청 DTO. null 이면 기본값을 사용한다.
     * @return 팀 목록 페이지 응답
     */
    public PageResponse<GetTeamsApiDto.Response> getTeams(CustomUserPrincipal principal, GetTeamsApiDto.Request request) {
        TeamPageQuery query = TeamPageQuery.from(request);
        return GetTeamsApiDto.Response.fromPage(teamRepository.findTeamPage(principal.userId(), query));
    }

    /**
     * 로그인 사용자가 볼 수 있는 팀의 ACTIVE/INACTIVE/전체 개수를 조회한다.
     *
     * @param principal 현재 로그인 사용자
     * @return 팀 상태 요약 응답
     */
    public GetTeamSummaryApiDto.Response getTeamSummary(CustomUserPrincipal principal) {
        return GetTeamSummaryApiDto.Response.from(teamRepository.countTeamSummary(principal.userId()));
    }

    /**
     * 로그인 사용자가 볼 수 있는 단일 팀 상세와 업무일지 집계를 조회한다.
     *
     * @param principal 현재 로그인 사용자
     * @param teamId 조회 대상 팀 ID
     * @return 팀 상세 응답
     * @throws BusinessException TEAM_NOT_FOUND 팀이 없거나 soft-delete 되었을 때
     * @throws BusinessException AUTH_ACCESS_DENIED 대상 팀이 visible scope 에 없을 때
     */
    public GetTeamApiDto.Response getTeam(CustomUserPrincipal principal, Long teamId) {
        return teamRepository.findTeamDetail(principal.userId(), teamId)
                .map(GetTeamApiDto.Response::from)
                .orElseThrow(() -> getTeamAccessError(teamId));
    }

    /**
     * 로그인 사용자가 볼 수 있는 팀의 ACTIVE 사용자 목록을 조회한다.
     *
     * @param principal 현재 로그인 사용자
     * @param teamId 조회 대상 팀 ID
     * @return 팀 사용자 목록 응답
     * @throws BusinessException TEAM_NOT_FOUND 팀이 없거나 soft-delete 되었을 때
     * @throws BusinessException AUTH_ACCESS_DENIED 대상 팀이 visible scope 에 없을 때
     */
    public GetTeamUsersApiDto.Response getTeamUsers(CustomUserPrincipal principal, Long teamId) {
        return teamRepository.findTeamUsers(principal.userId(), teamId)
                .map(GetTeamUsersApiDto.Response::from)
                .orElseThrow(() -> getTeamAccessError(teamId));
    }

    /**
     * 새 팀과 팀 관리 grant, ACTIVE membership 을 함께 생성한다.
     *
     * @param request 팀 생성 요청
     * @throws BusinessException TEAM_DUPLICATE_NAME soft-delete 되지 않은 동일 팀명이 이미 존재할 때
     * @throws BusinessException USER_NOT_FOUND 요청에 포함된 사용자 ID가 존재하지 않을 때
     */
    @Transactional
    public void createTeam(CreateTeamApiDto.Request request) {
        validateCreateTeamRequest(request);
        validateTeamNameUnique(request.teamName());

        Set<Long> requestedUserIds = requestedUserIds(request);
        ensureUsersExist(requestedUserIds);

        Team team = teamRepository.save(Team.create(
                request.teamName(),
                request.statusCode(),
                request.description(),
                request.startDate(),
                request.expectedEndDate()
        ));
        Long teamId = team.getId();

        teamAdminRepository.saveAll(adminGrants(teamId, request.addAdmin()));
        userTeamRepository.saveAll(userMemberships(teamId, request.addUsers()));
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
     * 사용자가 해당 팀에 ACTIVE membership 으로 소속되어 있는지 여부를 반환한다.
     * 호출측(예: 업무 등록) 이 "대상 팀에 대한 쓰기 권한" 을 Principal 기반으로 판단할 때 사용한다.
     * 소속 미존재 시 던질 도메인 에러 코드는 호출측 유스케이스가 결정한다.
     *
     * @param userId 검증 대상 사용자 ID
     * @param teamId 검증 대상 팀 ID
     * @return 사용자가 해당 팀에 ACTIVE 상태로 속해 있으면 true
     */
    public boolean isMember(Long userId, Long teamId) {
        return userTeamRepository.existsByUserIdAndTeamIdAndStatusCode(userId, teamId, UserTeamStatus.ACTIVE);
    }

    /** soft-delete 되지 않은 동일 팀명이 이미 존재하면 중복 오류를 던진다. */
    private void validateTeamNameUnique(String teamName) {
        if (teamRepository.existsByTeamNameAndDeletedAtIsNull(teamName)) {
            throw new BusinessException(ErrorCode.TEAM_DUPLICATE_NAME);
        }
    }

    /** 생성 요청 내부 중복과 리더 정확히 한 명 규칙 위반을 비즈니스 오류로 정규화한다. */
    private void validateCreateTeamRequest(CreateTeamApiDto.Request request) {
        Set<Long> memberUserIds = new HashSet<>();
        long leaderCount = 0;
        for (CreateTeamApiDto.AddUser addUser : request.addUsers()) {
            if (!memberUserIds.add(addUser.userId())) {
                throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST);
            }
            if (Boolean.TRUE.equals(addUser.isLeader())) {
                leaderCount++;
            }
        }
        if (leaderCount != 1) {
            throw new BusinessException(ErrorCode.TEAM_LEADER_COUNT_INVALID);
        }
    }

    /** addAdmin 과 addUsers 의 참조 사용자 ID 집합을 만든다. */
    private Set<Long> requestedUserIds(CreateTeamApiDto.Request request) {
        Set<Long> userIds = new HashSet<>();
        userIds.add(request.addAdmin());
        request.addUsers().forEach(addUser -> userIds.add(addUser.userId()));
        return userIds;
    }

    /** 요청 사용자 ID가 모두 존재하는지 확인한다. */
    private void ensureUsersExist(Set<Long> requestedUserIds) {
        Set<Long> existingUserIds = userRepository.findAllById(requestedUserIds).stream()
                .map(User::getId)
                .collect(Collectors.toSet());
        if (!existingUserIds.containsAll(requestedUserIds)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
    }

    /** 요청 admin 과 모든 DIRECTOR 사용자에게 생성 팀의 관리 grant 를 부여한다. */
    private List<TeamAdmin> adminGrants(Long teamId, Long requestedAdminUserId) {
        Set<Long> adminUserIds = new HashSet<>();
        adminUserIds.add(requestedAdminUserId);
        userRepository.findAllByRoleCode(UserRole.DIRECTOR)
                .forEach(director -> adminUserIds.add(director.getId()));
        return adminUserIds.stream()
                .map(userId -> TeamAdmin.grant(userId, teamId))
                .toList();
    }

    /** 요청 팀원들을 ACTIVE membership 으로 생성한다. */
    private List<UserTeam> userMemberships(Long teamId, List<CreateTeamApiDto.AddUser> addUsers) {
        return addUsers.stream()
                .map(addUser -> UserTeam.create(
                        addUser.userId(),
                        teamId,
                        addUser.isLeader(),
                        addUser.teamRole(),
                        null,
                        false,
                        UserTeamStatus.ACTIVE
                ))
                .toList();
    }

    /** 팀 존재 여부와 visible scope 접근 가능 여부를 분리해 접근 실패 오류 코드를 결정한다. */
    private BusinessException getTeamAccessError(Long teamId) {
        if (!teamRepository.existsByIdAndDeletedAtIsNull(teamId)) {
            return new BusinessException(ErrorCode.TEAM_NOT_FOUND);
        }
        return new BusinessException(ErrorCode.AUTH_ACCESS_DENIED);
    }
}
