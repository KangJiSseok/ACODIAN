package com.ibank.axwms.domain.organization.team.service;

import com.ibank.axwms.domain.organization.team.UserTeamStatus;
import com.ibank.axwms.domain.organization.team.dto.CreateTeamApiDto;
import com.ibank.axwms.domain.organization.team.dto.GetTeamApiDto;
import com.ibank.axwms.domain.organization.team.dto.GetTeamSummaryApiDto;
import com.ibank.axwms.domain.organization.team.dto.GetTeamUsersApiDto;
import com.ibank.axwms.domain.organization.team.dto.GetTeamsApiDto;
import com.ibank.axwms.domain.organization.team.dto.UpdateTeamApiDto;
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
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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

        User requestedAdmin = getUserOrThrow(request.addAdmin());
        ensureUsersExist(requestedMemberUserIdsForCreate(request));
        Long departmentId = requestedAdmin.getDepartmentId();

        Team team = teamRepository.save(Team.create(
                departmentId,
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
     * 대상 팀의 기본 정보, 관리 grant, membership 을 null 이 아닌 요청 필드 기준으로 부분 수정한다.
     *
     * @param principal 현재 로그인 사용자
     * @param teamId 수정 대상 팀 ID
     * @param request 부분 수정 요청
     * @throws BusinessException TEAM_NOT_FOUND 팀이 없거나 soft-delete 되었을 때
     * @throws BusinessException AUTH_ACCESS_DENIED 호출자가 대상 팀의 admin grant 를 보유하지 않을 때
     * @throws BusinessException TEAM_DUPLICATE_NAME soft-delete 되지 않은 동일 팀명이 이미 존재할 때
     * @throws BusinessException USER_NOT_FOUND 요청에 포함된 사용자 ID가 존재하지 않을 때
     */
    @Transactional
    public void updateTeam(CustomUserPrincipal principal, Long teamId, UpdateTeamApiDto.Request request) {

        //검증
        validateUpdateTeamRequest(request);
        Team team = getActiveTeamOrThrow(teamId);
        validateTeamAdminGrant(principal.userId(), teamId);
        validateRemoveAdminAllowed(principal, request.removeAdmin());
        validateTeamNameUniqueForUpdate(team, request.teamName());
        ensureUsersExist(requestedUserIdsForUpdate(request));

        //검증이 끝났으므로 모두 update
        team.updatePartial(
                request.teamName(),
                request.statusCode(),
                request.description(),
                request.startDate(),
                request.expectedEndDate()
        );
        //Admin 추가, 삭제
        addAdminGrant(teamId, request.addAdmin());
        removeAdminGrant(teamId, request.removeAdmin());

        //팀 멤버 추가,삭제,수정
        List<UpdateTeamApiDto.AddUser> addUsers = nonNullAddUsers(request);
        reassignLeaderIfRequested(teamId, addUsers);
        addOrReactivateUsers(teamId, addUsers);
        removeUsers(teamId, nonNullRemoveUsers(request));
        editUsers(teamId, nonNullEditUsers(request));

        //최종 리더가 1명이면 예외
        validateAtLeastOneActiveLeader(teamId);
    }

    /**
     * 대상 팀의 admin grant 를 보유한 사용자가 팀을 soft-delete 한다.
     *
     * @param principal 현재 로그인 사용자
     * @param teamId 삭제 대상 팀 ID
     * @throws BusinessException TEAM_NOT_FOUND 팀이 없거나 이미 soft-delete 되었을 때
     * @throws BusinessException AUTH_ACCESS_DENIED 호출자가 대상 팀의 admin grant 를 보유하지 않을 때
     */
    @Transactional
    public void deleteTeam(CustomUserPrincipal principal, Long teamId) {
        Team team = getActiveTeamOrThrow(teamId);
        validateTeamAdminGrant(principal.userId(), teamId);

        team.markDeleted(LocalDateTime.now());
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

    /** 수정 대상 팀명을 자기 자신을 제외한 soft-delete 되지 않은 팀명과 비교한다. */
    private void validateTeamNameUniqueForUpdate(Team team, String requestedTeamName) {
        if (requestedTeamName != null // 팀명 변경 요청이 있을 때만 검사
                && !Objects.equals(team.getTeamName(), requestedTeamName) // 현재 팀명과 다를 때만 검사
                && teamRepository.existsByTeamNameAndDeletedAtIsNullAndIdNot(requestedTeamName, team.getId())) {
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

    /** 부분 수정 요청의 중복 사용자와 리더 변경 충돌을 비즈니스 오류로 정규화한다. */
    private void validateUpdateTeamRequest(UpdateTeamApiDto.Request request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST);
        }
        validateNoDuplicateUsers(nonNullAddUsers(request));
        validateNoDuplicateIds(nonNullRemoveUsers(request));
        validateNoDuplicateEditUsers(nonNullEditUsers(request));
        validateLeaderCount(request);
        validateTeamNameNotBlank(request);
        validateNoUserListConflicts(request);
    }

    /** addUsers 에 리더가 2명 이상이면 오류를 던진다. */
    private void validateLeaderCount(UpdateTeamApiDto.Request request) {
        if (countRequestedLeaders(nonNullAddUsers(request)) > 1) {
            throw new BusinessException(ErrorCode.TEAM_LEADER_COUNT_INVALID);
        }
    }

    /** 모든 membership 변경 적용 후 팀에 ACTIVE 리더가 한 명도 없으면 오류를 던진다. */
    private void validateAtLeastOneActiveLeader(Long teamId) {
        if (userTeamRepository.findAllByTeamIdAndStatusCodeAndIsLeader(teamId, UserTeamStatus.ACTIVE, true).isEmpty()) {
            throw new BusinessException(ErrorCode.TEAM_LEADER_COUNT_INVALID);
        }
    }

    /** teamName 이 null 이 아닌데 빈 문자열이면 오류를 던진다. */
    private void validateTeamNameNotBlank(UpdateTeamApiDto.Request request) {
        if (request.teamName() != null && request.teamName().isBlank()) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST);
        }
    }

    /** addUsers/removeUsers 교집합과 editUsers/removeUsers 교집합을 검사한다. */
    private void validateNoUserListConflicts(UpdateTeamApiDto.Request request) {
        Set<Long> addUserIds = nonNullAddUsers(request).stream()
                .map(UpdateTeamApiDto.AddUser::userId)
                .collect(Collectors.toSet());
        if (nonNullRemoveUsers(request).stream().anyMatch(addUserIds::contains)) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST);
        }
        Set<Long> removeUserIds = new HashSet<>(nonNullRemoveUsers(request));
        if (nonNullEditUsers(request).stream()
                .map(UpdateTeamApiDto.EditUser::userId)
                .anyMatch(removeUserIds::contains)) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST);
        }
    }

    /** 팀 생성 요청의 member 사용자 ID만 모아 admin 단건 조회와 membership 검증 책임을 분리한다. */
    private Set<Long> requestedMemberUserIdsForCreate(CreateTeamApiDto.Request request) {
        Set<Long> userIds = new HashSet<>();
        request.addUsers().forEach(addUser -> userIds.add(addUser.userId()));
        return userIds;
    }

    /** 부분 수정 요청이 참조하는 모든 사용자 ID 집합을 만든다. */
    private Set<Long> requestedUserIdsForUpdate(UpdateTeamApiDto.Request request) {
        Set<Long> userIds = new HashSet<>();
        addIfNotNull(userIds, request.addAdmin());
        addIfNotNull(userIds, request.removeAdmin());
        nonNullAddUsers(request).forEach(addUser -> addIfNotNull(userIds, addUser.userId()));
        nonNullRemoveUsers(request).forEach(userId -> addIfNotNull(userIds, userId));
        nonNullEditUsers(request).forEach(editUser -> addIfNotNull(userIds, editUser.userId()));
        return userIds;
    }

    /** 요청 사용자 ID가 모두 존재하는지 확인한다. */
    private void ensureUsersExist(Set<Long> requestedUserIds) {
        if (requestedUserIds.isEmpty()) {
            return;
        }
        Set<Long> existingUserIds = userRepository.findAllById(requestedUserIds).stream()
                .map(User::getId)
                .collect(Collectors.toSet());
        if (!existingUserIds.containsAll(requestedUserIds)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
    }

    /** 생성 팀 ownership 기준이 되는 요청 admin 을 단건 조회하고 누락 시 참조 오류로 변환한다. */
    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
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

    /** teamId 로 soft-delete 되지 않은 팀을 조회한다. */
    private Team getActiveTeamOrThrow(Long teamId) {
        return teamRepository.findByIdAndDeletedAtIsNull(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
    }

    /** 호출자가 대상 팀 admin grant 를 보유하는지 검증한다. */
    private void validateTeamAdminGrant(Long userId, Long teamId) {
        if (!teamAdminRepository.existsByUserIdAndTeamId(userId, teamId)) {
            throw new BusinessException(ErrorCode.AUTH_ACCESS_DENIED);
        }
    }

    /** 팀 관리 grant 회수는 DIRECTOR 만 수행할 수 있다. */
    private void validateRemoveAdminAllowed(CustomUserPrincipal principal, Long removeAdminUserId) {
        if (removeAdminUserId == null || UserRole.DIRECTOR.name().equals(principal.roleCode())) {
            return;
        }
        throw new BusinessException(ErrorCode.AUTH_ACCESS_DENIED);
    }

    /** 요청 사용자에게 팀 관리 grant 를 추가한다. 이미 있으면 멱등 처리한다. */
    private void addAdminGrant(Long teamId, Long userId) {
        if (userId != null && !teamAdminRepository.existsByUserIdAndTeamId(userId, teamId)) {
            teamAdminRepository.save(TeamAdmin.grant(userId, teamId));
        }
    }

    /** 요청 사용자의 팀 관리 grant 를 회수한다. */
    private void removeAdminGrant(Long teamId, Long userId) {
        if (userId != null) {
            teamAdminRepository.deleteByUserIdAndTeamId(userId, teamId);
        }
    }

    /** addUsers 중 isLeader=true 가 있으면 해당 사용자 외 기존 ACTIVE 리더를 강등한다. */
    private void reassignLeaderIfRequested(Long teamId, List<UpdateTeamApiDto.AddUser> addUsers) {
        addUsers.stream()
                .filter(u -> Boolean.TRUE.equals(u.isLeader()))
                .findFirst()
                .ifPresent(newLeader -> demoteCurrentLeaders(teamId, newLeader.userId()));
    }

    /** 사용자 추가 요청을 신규 ACTIVE membership 생성 또는 기존 row 재활성화로 반영한다. */
    private void addOrReactivateUsers(Long teamId, List<UpdateTeamApiDto.AddUser> addUsers) {
        for (UpdateTeamApiDto.AddUser addUser : addUsers) {
            Optional<UserTeam> existing = userTeamRepository.findByUserIdAndTeamId(addUser.userId(), teamId);
            if (existing.isPresent()) {
                existing.get().reactivate(addUser.isLeader(), addUser.teamRole(), null, false);
            } else {
                userTeamRepository.save(UserTeam.create(
                        addUser.userId(),
                        teamId,
                        addUser.isLeader(),
                        addUser.teamRole(),
                        null,
                        false,
                        UserTeamStatus.ACTIVE
                ));
            }
        }
    }

    /** 지정 사용자 외 기존 ACTIVE 리더를 일반 멤버로 강등한다. */
    private void demoteCurrentLeaders(Long teamId, Long newLeaderUserId) {
        userTeamRepository.findAllByTeamIdAndStatusCodeAndIsLeader(teamId, UserTeamStatus.ACTIVE, true).stream()
                .filter(userTeam -> !Objects.equals(userTeam.getUserId(), newLeaderUserId))
                .forEach(UserTeam::demoteToMember);
    }

    /** 요청된 사용자 membership 을 LEFT 상태로 전환한다. */
    private void removeUsers(Long teamId, List<Long> userIds) {
        for (Long userId : userIds) {
            userTeamRepository.findByUserIdAndTeamId(userId, teamId)
                    .ifPresent(userTeam -> userTeam.changeStatus(UserTeamStatus.LEFT));
        }
    }

    /** 요청된 ACTIVE membership 의 팀 내 업무 역할을 수정한다. */
    private void editUsers(Long teamId, List<UpdateTeamApiDto.EditUser> editUsers) {
        for (UpdateTeamApiDto.EditUser editUser : editUsers) {
            UserTeam userTeam = userTeamRepository.findByUserIdAndTeamId(editUser.userId(), teamId)
                    .filter(membership -> membership.getStatusCode() == UserTeamStatus.ACTIVE)
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
            userTeam.updateTeamRole(editUser.teamRole());
        }
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

    /** null 이면 빈 리스트를 반환한다. addUsers 필드는 부분 수정 요청에서 생략 가능하다. */
    private List<UpdateTeamApiDto.AddUser> nonNullAddUsers(UpdateTeamApiDto.Request request) {
        return request.addUsers() == null ? List.of() : request.addUsers();
    }

    /** null 이면 빈 리스트를 반환한다. removeUsers 필드는 부분 수정 요청에서 생략 가능하다. */
    private List<Long> nonNullRemoveUsers(UpdateTeamApiDto.Request request) {
        return request.removeUsers() == null ? List.of() : request.removeUsers();
    }

    /** null 이면 빈 리스트를 반환한다. editUsers 필드는 부분 수정 요청에서 생략 가능하다. */
    private List<UpdateTeamApiDto.EditUser> nonNullEditUsers(UpdateTeamApiDto.Request request) {
        return request.editUsers() == null ? List.of() : request.editUsers();
    }

    /** addUsers 목록에 동일 userId 가 중복되면 오류를 던진다. */
    private void validateNoDuplicateUsers(List<UpdateTeamApiDto.AddUser> addUsers) {
        validateNoDuplicateIds(addUsers.stream()
                .map(UpdateTeamApiDto.AddUser::userId)
                .toList());
    }

    /** editUsers 목록에 동일 userId 가 중복되면 오류를 던진다. */
    private void validateNoDuplicateEditUsers(List<UpdateTeamApiDto.EditUser> editUsers) {
        validateNoDuplicateIds(editUsers.stream()
                .map(UpdateTeamApiDto.EditUser::userId)
                .toList());
    }

    /** userId 목록에 중복이 있으면 오류를 던진다. null userId 는 중복 검사에서 제외한다. */
    private void validateNoDuplicateIds(List<Long> userIds) {
        Set<Long> unique = new HashSet<>();
        for (Long userId : userIds) {
            if (userId != null && !unique.add(userId)) {
                throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST);
            }
        }
    }

    /** addUsers 중 isLeader=true 인 항목 수를 반환한다. */
    private long countRequestedLeaders(List<UpdateTeamApiDto.AddUser> addUsers) {
        return addUsers.stream()
                .filter(addUser -> Boolean.TRUE.equals(addUser.isLeader()))
                .count();
    }

    /** 값이 null 이 아닌 경우에만 집합에 추가한다. */
    private void addIfNotNull(Set<Long> userIds, Long userId) {
        if (userId != null) {
            userIds.add(userId);
        }
    }
}
