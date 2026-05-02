package com.ibank.axwms.domain.organization.team.service;

import com.ibank.axwms.domain.organization.team.UserTeamStatus;
import com.ibank.axwms.domain.organization.team.dto.GetTeamsApiDto;
import com.ibank.axwms.domain.organization.team.entity.Team;
import com.ibank.axwms.domain.organization.team.repository.TeamRepository;
import com.ibank.axwms.domain.organization.team.repository.UserTeamRepository;
import com.ibank.axwms.domain.organization.team.repository.jooq.query.TeamPageQuery;
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import com.ibank.axwms.global.response.PageResponse;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamService {
    private final TeamRepository teamRepository;
    private final UserTeamRepository userTeamRepository;

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
}
