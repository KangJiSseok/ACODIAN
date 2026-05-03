package com.ibank.axwms.domain.organization.team.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;

import com.ibank.axwms.domain.organization.team.TeamStatus;
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
import com.ibank.axwms.domain.organization.team.repository.jooq.projection.TeamDetailProjection;
import com.ibank.axwms.domain.organization.team.repository.TeamRepository;
import com.ibank.axwms.domain.organization.team.repository.UserTeamRepository;
import com.ibank.axwms.domain.organization.team.repository.jooq.projection.TeamStatusSummaryProjection;
import com.ibank.axwms.domain.organization.team.repository.jooq.projection.TeamSummaryProjection;
import com.ibank.axwms.domain.organization.team.repository.jooq.projection.TeamUserSummaryProjection;
import com.ibank.axwms.domain.organization.team.repository.jooq.query.TeamPageQuery;
import com.ibank.axwms.domain.organization.user.EmploymentStatus;
import com.ibank.axwms.domain.organization.user.UserRole;
import com.ibank.axwms.domain.organization.user.entity.User;
import com.ibank.axwms.domain.organization.user.repository.UserRepository;
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import com.ibank.axwms.global.response.PageResponse;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamAdminRepository teamAdminRepository;

    @Mock
    private UserTeamRepository userTeamRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TeamService teamService;

    @Test
    @DisplayName("getTeams 는 principal userId 로 repository 조회 결과를 PageResponse 로 변환한다")
    void getTeams_는_principal_userId로_repository_조회_결과를_PageResponse로_변환한다() {
        CustomUserPrincipal principal = principal();
        GetTeamsApiDto.Request request = new GetTeamsApiDto.Request(2, 10);
        given(teamRepository.findTeamPage(101L, TeamPageQuery.from(request))).willReturn(new PageImpl<>(
                List.of(projection()),
                PageRequest.of(1, 10),
                11
        ));

        PageResponse<GetTeamsApiDto.Response> response = teamService.getTeams(principal, request);

        assertThat(response.page()).isEqualTo(2);
        assertThat(response.pageSize()).isEqualTo(10);
        assertThat(response.totalCount()).isEqualTo(11);
        assertThat(response.items()).singleElement()
                .extracting(
                        GetTeamsApiDto.Response::teamId,
                        GetTeamsApiDto.Response::teamName,
                        GetTeamsApiDto.Response::memberCount,
                        GetTeamsApiDto.Response::myIsLeader,
                        GetTeamsApiDto.Response::allocation
                )
                .containsExactly(21L, "물류혁신TF", 2L, true, "주담당");
        then(teamRepository).should().findTeamPage(101L, TeamPageQuery.from(request));
    }

    @Test
    @DisplayName("getTeams 는 null 요청에도 기본 페이지 값을 가진 요청으로 repository 에 위임한다")
    void getTeams_는_null_요청에도_기본_페이지_값을_가진_요청으로_repository에_위임한다() {
        CustomUserPrincipal principal = principal();
        given(teamRepository.findTeamPage(eq(101L), any()))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        teamService.getTeams(principal, null);

        ArgumentCaptor<TeamPageQuery> requestCaptor = ArgumentCaptor.forClass(TeamPageQuery.class);
        then(teamRepository).should().findTeamPage(eq(101L), requestCaptor.capture());
        assertThat(requestCaptor.getValue().page()).isEqualTo(1);
        assertThat(requestCaptor.getValue().pageSize()).isEqualTo(20);
        assertThat(requestCaptor.getValue().pageIndex()).isZero();
    }

    @Test
    @DisplayName("getTeamSummary 는 principal userId 로 repository 조회 결과를 요약 응답으로 변환한다")
    void getTeamSummary_는_principal_userId로_repository_조회_결과를_요약_응답으로_변환한다() {
        CustomUserPrincipal principal = principal();
        given(teamRepository.countTeamSummary(101L)).willReturn(new TeamStatusSummaryProjection(3L, 1L, 4L));

        GetTeamSummaryApiDto.Response response = teamService.getTeamSummary(principal);

        assertThat(response)
                .extracting(
                        GetTeamSummaryApiDto.Response::activeTeamCount,
                        GetTeamSummaryApiDto.Response::inactiveTeamCount,
                        GetTeamSummaryApiDto.Response::totalTeamCount
                )
                .containsExactly(3L, 1L, 4L);
        then(teamRepository).should().countTeamSummary(101L);
    }

    @Test
    @DisplayName("getTeam 은 principal userId 와 teamId 로 repository 조회 결과를 상세 응답으로 변환한다")
    void getTeam_은_principal_userId와_teamId로_repository_조회_결과를_상세_응답으로_변환한다() {
        CustomUserPrincipal principal = principal();
        given(teamRepository.findTeamDetail(101L, 21L)).willReturn(Optional.of(detailProjection()));

        GetTeamApiDto.Response response = teamService.getTeam(principal, 21L);

        assertThat(response)
                .extracting(
                        GetTeamApiDto.Response::teamId,
                        GetTeamApiDto.Response::teamName,
                        GetTeamApiDto.Response::teamLeaderId,
                        GetTeamApiDto.Response::teamLeaderName,
                        GetTeamApiDto.Response::deptHeadAdminUserId,
                        GetTeamApiDto.Response::deptHeadAdminUsername
                )
                .containsExactly(21L, "물류혁신TF", 101L, "홍길동", 202L, "김사업부장");
        then(teamRepository).should().findTeamDetail(101L, 21L);
        then(teamRepository).should(never()).existsByIdAndDeletedAtIsNull(21L);
    }

    @Test
    @DisplayName("getTeam 대상 팀이 없으면 TEAM_NOT_FOUND 예외를 던진다")
    void getTeam_대상_팀이_없으면_TEAM_NOT_FOUND_예외를_던진다() {
        CustomUserPrincipal principal = principal();
        given(teamRepository.findTeamDetail(101L, 999L)).willReturn(Optional.empty());
        given(teamRepository.existsByIdAndDeletedAtIsNull(999L)).willReturn(false);

        assertThatThrownBy(() -> teamService.getTeam(principal, 999L))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.TEAM_NOT_FOUND);
    }

    @Test
    @DisplayName("getTeam 대상 팀이 visible scope 밖이면 AUTH_ACCESS_DENIED 예외를 던진다")
    void getTeam_대상_팀이_visible_scope_밖이면_AUTH_ACCESS_DENIED_예외를_던진다() {
        CustomUserPrincipal principal = principal();
        given(teamRepository.findTeamDetail(101L, 21L)).willReturn(Optional.empty());
        given(teamRepository.existsByIdAndDeletedAtIsNull(21L)).willReturn(true);

        assertThatThrownBy(() -> teamService.getTeam(principal, 21L))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_ACCESS_DENIED);
    }

    @Test
    @DisplayName("getTeamUsers 는 visible team 의 ACTIVE 사용자 목록을 응답으로 변환한다")
    void getTeamUsers_는_visible_team의_ACTIVE_사용자_목록을_응답으로_변환한다() {
        CustomUserPrincipal principal = principal();
        given(teamRepository.findTeamUsers(101L, 21L)).willReturn(Optional.of(List.of(teamUserProjection())));

        GetTeamUsersApiDto.Response response = teamService.getTeamUsers(principal, 21L);

        assertThat(response.items()).singleElement()
                .extracting(
                        GetTeamUsersApiDto.Item::isLeader,
                        GetTeamUsersApiDto.Item::userId,
                        GetTeamUsersApiDto.Item::userName,
                        GetTeamUsersApiDto.Item::positionName,
                        GetTeamUsersApiDto.Item::teamRole
                )
                .containsExactly(true, 101L, "홍길동", "과장", "플랫폼 총괄");
        then(teamRepository).should().findTeamUsers(101L, 21L);
    }

    @Test
    @DisplayName("getTeamUsers 대상 팀이 없으면 TEAM_NOT_FOUND 예외를 던진다")
    void getTeamUsers_대상_팀이_없으면_TEAM_NOT_FOUND_예외를_던진다() {
        CustomUserPrincipal principal = principal();
        given(teamRepository.findTeamUsers(101L, 999L)).willReturn(Optional.empty());
        given(teamRepository.existsByIdAndDeletedAtIsNull(999L)).willReturn(false);

        assertThatThrownBy(() -> teamService.getTeamUsers(principal, 999L))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.TEAM_NOT_FOUND);
    }

    @Test
    @DisplayName("getTeamUsers 대상 팀이 visible scope 밖이면 AUTH_ACCESS_DENIED 예외를 던진다")
    void getTeamUsers_대상_팀이_visible_scope_밖이면_AUTH_ACCESS_DENIED_예외를_던진다() {
        CustomUserPrincipal principal = principal();
        given(teamRepository.findTeamUsers(101L, 21L)).willReturn(Optional.empty());
        given(teamRepository.existsByIdAndDeletedAtIsNull(21L)).willReturn(true);

        assertThatThrownBy(() -> teamService.getTeamUsers(principal, 21L))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_ACCESS_DENIED);
    }

    @Test
    @DisplayName("팀 ID가 존재하면 팀 엔티티를 반환한다")
    void 팀_ID가_존재하면_팀_엔티티를_반환한다() {
        Team team = createTeam(21L);
        given(teamRepository.findById(21L)).willReturn(Optional.of(team));

        Team result = teamService.getTeamOrThrow(21L);

        assertThat(result).isSameAs(team);
    }

    @Test
    @DisplayName("팀 ID가 존재하지 않으면 TEAM_NOT_FOUND 예외를 던진다")
    void 팀_ID가_존재하지_않으면_TEAM_NOT_FOUND_예외를_던진다() {
        given(teamRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> teamService.getTeamOrThrow(999L))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.TEAM_NOT_FOUND);
    }

    @Test
    @DisplayName("사용자 팀 소속 여부는 ACTIVE membership 기준 repository 결과를 반환한다")
    void 사용자_팀_소속_여부는_ACTIVE_membership_기준_repository_결과를_반환한다() {
        given(userTeamRepository.existsByUserIdAndTeamIdAndStatusCode(101L, 21L, UserTeamStatus.ACTIVE))
                .willReturn(true);

        boolean result = teamService.isMember(101L, 21L);

        assertThat(result).isTrue();
        then(userTeamRepository).should()
                .existsByUserIdAndTeamIdAndStatusCode(101L, 21L, UserTeamStatus.ACTIVE);
    }

    @Test
    @DisplayName("createTeam 은 팀과 요청 관리자 및 DIRECTOR grant 와 ACTIVE membership 을 생성한다")
    void createTeam_은_팀과_요청_관리자_및_director_grant와_active_membership을_생성한다() {
        CreateTeamApiDto.Request request = createTeamRequest();
        Team savedTeam = createTeam(501L);
        User requestedAdmin = createUser(201L, UserRole.MEMBER);
        User leader = createUser(202L, UserRole.MEMBER);
        User member = createUser(203L, UserRole.MEMBER);
        User director = createUser(301L, UserRole.DIRECTOR);
        given(teamRepository.existsByTeamNameAndDeletedAtIsNull("물류혁신TF")).willReturn(false);
        given(userRepository.findAllById(any())).willReturn(List.of(requestedAdmin, leader, member));
        given(userRepository.findAllByRoleCode(UserRole.DIRECTOR)).willReturn(List.of(director));
        given(teamRepository.save(any(Team.class))).willReturn(savedTeam);

        teamService.createTeam(request);

        ArgumentCaptor<Team> teamCaptor = ArgumentCaptor.forClass(Team.class);
        then(teamRepository).should().save(teamCaptor.capture());
        assertThat(teamCaptor.getValue())
                .extracting(Team::getTeamName, Team::getStatusCode, Team::getDescription, Team::getStartDate, Team::getExpectedEndDate)
                .containsExactly(
                        "물류혁신TF",
                        TeamStatus.ACTIVE,
                        "창고 자동화 및 운영 고도화",
                        LocalDate.of(2026, 4, 1),
                        LocalDate.of(2026, 12, 31)
                );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TeamAdmin>> adminCaptor = ArgumentCaptor.forClass(List.class);
        then(teamAdminRepository).should().saveAll(adminCaptor.capture());
        assertThat(adminCaptor.getValue())
                .extracting(TeamAdmin::getUserId, TeamAdmin::getTeamId)
                .containsExactlyInAnyOrder(
                        tuple(201L, 501L),
                        tuple(301L, 501L)
                );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<UserTeam>> userTeamCaptor = ArgumentCaptor.forClass(List.class);
        then(userTeamRepository).should().saveAll(userTeamCaptor.capture());
        assertThat(userTeamCaptor.getValue())
                .extracting(
                        UserTeam::getUserId,
                        UserTeam::getTeamId,
                        UserTeam::getIsLeader,
                        UserTeam::getTeamRole,
                        UserTeam::getStatusCode
                )
                .containsExactly(
                        tuple(202L, 501L, true, "WMS 운영", UserTeamStatus.ACTIVE),
                        tuple(203L, 501L, false, "현장 총괄", UserTeamStatus.ACTIVE)
                );
    }

    @Test
    @DisplayName("updateTeam 은 admin grant 보유자 요청의 null 이 아닌 필드와 grant membership 변경만 반영한다")
    void updateTeam_은_admin_grant_보유자_요청의_null이_아닌_필드와_grant_membership_변경만_반영한다() {
        CustomUserPrincipal principal = principal();
        Team team = createTeam(21L);
        User newAdmin = createUser(204L, UserRole.MEMBER);
        User removeAdmin = createUser(205L, UserRole.MEMBER);
        User rejoinUser = createUser(202L, UserRole.MEMBER);
        User removeUser = createUser(203L, UserRole.MEMBER);
        User editUser = createUser(206L, UserRole.MEMBER);
        UserTeam currentLeader = UserTeam.create(201L, 21L, true, "기존 리더", "주담당", true, UserTeamStatus.ACTIVE);
        UserTeam rejoinMembership = UserTeam.create(202L, 21L, false, "이탈", "겸임", false, UserTeamStatus.LEFT);
        UserTeam removeMembership = UserTeam.create(203L, 21L, false, "제거 대상", "겸임", false, UserTeamStatus.ACTIVE);
        UserTeam editMembership = UserTeam.create(206L, 21L, false, "이전 역할", "겸임", false, UserTeamStatus.ACTIVE);
        UpdateTeamApiDto.Request request = updateTeamRequest();
        given(teamRepository.findById(21L)).willReturn(Optional.of(team));
        given(teamAdminRepository.existsByUserIdAndTeamId(101L, 21L)).willReturn(true);
        given(teamRepository.existsByTeamNameAndDeletedAtIsNullAndIdNot("수정팀", 21L)).willReturn(false);
        given(userRepository.findAllById(any())).willReturn(List.of(newAdmin, removeAdmin, rejoinUser, removeUser, editUser));
        given(teamAdminRepository.existsByUserIdAndTeamId(204L, 21L)).willReturn(false);
        given(userTeamRepository.findAllByTeamIdAndStatusCodeAndIsLeader(21L, UserTeamStatus.ACTIVE, true))
                .willReturn(List.of(currentLeader));
        given(userTeamRepository.findByUserIdAndTeamId(202L, 21L)).willReturn(Optional.of(rejoinMembership));
        given(userTeamRepository.findByUserIdAndTeamId(203L, 21L)).willReturn(Optional.of(removeMembership));
        given(userTeamRepository.findByUserIdAndTeamId(206L, 21L)).willReturn(Optional.of(editMembership));

        teamService.updateTeam(principal, 21L, request);

        assertThat(team)
                .extracting(Team::getTeamName, Team::getStatusCode, Team::getDescription, Team::getStartDate, Team::getExpectedEndDate)
                .containsExactly(
                        "수정팀",
                        TeamStatus.INACTIVE,
                        "수정 설명",
                        LocalDate.of(2026, 5, 1),
                        LocalDate.of(2026, 11, 30)
                );
        assertThat(currentLeader.getIsLeader()).isFalse();
        assertThat(rejoinMembership)
                .extracting(UserTeam::getIsLeader, UserTeam::getTeamRole, UserTeam::getStatusCode)
                .containsExactly(true, "신규 리더", UserTeamStatus.ACTIVE);
        assertThat(removeMembership.getStatusCode()).isEqualTo(UserTeamStatus.LEFT);
        assertThat(editMembership.getTeamRole()).isEqualTo("수정 역할");
        then(teamAdminRepository).should().save(any(TeamAdmin.class));
        then(teamAdminRepository).should().deleteByUserIdAndTeamId(205L, 21L);
    }

    @Test
    @DisplayName("updateTeam 은 대상 팀 admin grant 가 없으면 AUTH_ACCESS_DENIED 예외를 던진다")
    void updateTeam_은_대상_팀_admin_grant가_없으면_auth_access_denied_예외를_던진다() {
        Team team = createTeam(21L);
        given(teamRepository.findById(21L)).willReturn(Optional.of(team));
        given(teamAdminRepository.existsByUserIdAndTeamId(101L, 21L)).willReturn(false);

        assertThatThrownBy(() -> teamService.updateTeam(principal(), 21L, updateTeamRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_ACCESS_DENIED);
        then(userRepository).should(never()).findAllById(any());
    }

    @Test
    @DisplayName("updateTeam 은 다른 활성 팀과 팀명이 중복되면 TEAM_DUPLICATE_NAME 예외를 던진다")
    void updateTeam_은_다른_활성_팀과_팀명이_중복되면_team_duplicate_name_예외를_던진다() {
        Team team = createTeam(21L);
        given(teamRepository.findById(21L)).willReturn(Optional.of(team));
        given(teamAdminRepository.existsByUserIdAndTeamId(101L, 21L)).willReturn(true);
        given(teamRepository.existsByTeamNameAndDeletedAtIsNullAndIdNot("수정팀", 21L)).willReturn(true);

        assertThatThrownBy(() -> teamService.updateTeam(principal(), 21L, updateTeamRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.TEAM_DUPLICATE_NAME);
        then(userRepository).should(never()).findAllById(any());
    }

    @Test
    @DisplayName("createTeam 은 동일 팀명이 존재하면 TEAM_DUPLICATE_NAME 예외를 던진다")
    void createTeam_은_동일_팀명이_존재하면_team_duplicate_name_예외를_던진다() {
        CreateTeamApiDto.Request request = createTeamRequest();
        given(teamRepository.existsByTeamNameAndDeletedAtIsNull("물류혁신TF")).willReturn(true);

        assertThatThrownBy(() -> teamService.createTeam(request))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.TEAM_DUPLICATE_NAME);
        then(teamRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("createTeam 은 요청 사용자 중 존재하지 않는 사용자가 있으면 USER_NOT_FOUND 예외를 던진다")
    void createTeam_은_요청_사용자_중_존재하지_않는_사용자가_있으면_user_not_found_예외를_던진다() {
        CreateTeamApiDto.Request request = createTeamRequest();
        given(teamRepository.existsByTeamNameAndDeletedAtIsNull("물류혁신TF")).willReturn(false);
        given(userRepository.findAllById(any())).willReturn(List.of(createUser(201L, UserRole.MEMBER)));

        assertThatThrownBy(() -> teamService.createTeam(request))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
        then(teamRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("createTeam 은 팀 리더가 둘 이상이면 TEAM_LEADER_COUNT_INVALID 예외를 던진다")
    void createTeam_은_팀_리더가_둘_이상이면_team_leader_count_invalid_예외를_던진다() {
        CreateTeamApiDto.Request request = new CreateTeamApiDto.Request(
                "물류혁신TF",
                "창고 자동화 및 운영 고도화",
                201L,
                List.of(
                        new CreateTeamApiDto.AddUser(202L, true, "WMS 운영"),
                        new CreateTeamApiDto.AddUser(203L, true, "현장 총괄")
                ),
                TeamStatus.ACTIVE,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 12, 31)
        );

        assertThatThrownBy(() -> teamService.createTeam(request))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.TEAM_LEADER_COUNT_INVALID);
        then(teamRepository).should(never()).existsByTeamNameAndDeletedAtIsNull(any());
    }

    @Test
    @DisplayName("createTeam 은 팀 리더가 없으면 TEAM_LEADER_COUNT_INVALID 예외를 던진다")
    void createTeam_은_팀_리더가_없으면_team_leader_count_invalid_예외를_던진다() {
        CreateTeamApiDto.Request request = new CreateTeamApiDto.Request(
                "물류혁신TF",
                "창고 자동화 및 운영 고도화",
                201L,
                List.of(
                        new CreateTeamApiDto.AddUser(202L, false, "WMS 운영"),
                        new CreateTeamApiDto.AddUser(203L, false, "현장 총괄")
                ),
                TeamStatus.ACTIVE,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 12, 31)
        );

        assertThatThrownBy(() -> teamService.createTeam(request))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.TEAM_LEADER_COUNT_INVALID);
        then(teamRepository).should(never()).existsByTeamNameAndDeletedAtIsNull(any());
    }

    private CustomUserPrincipal principal() {
        return new CustomUserPrincipal(101L, "user@ibank.com", "MEMBER");
    }

    private TeamSummaryProjection projection() {
        return new TeamSummaryProjection(
                21L,
                "물류혁신TF",
                "ACTIVE",
                "테스트 팀",
                101L,
                "홍길동",
                2L,
                true,
                "플랫폼 총괄",
                "주담당",
                true,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 12, 31)
        );
    }

    private TeamDetailProjection detailProjection() {
        return new TeamDetailProjection(
                21L,
                "물류혁신TF",
                "ACTIVE",
                "테스트 팀",
                101L,
                "홍길동",
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 12, 31),
                202L,
                "김사업부장"
        );
    }

    private TeamUserSummaryProjection teamUserProjection() {
        return new TeamUserSummaryProjection(
                true,
                101L,
                "홍길동",
                "과장",
                "플랫폼 총괄"
        );
    }

    private Team createTeam(Long id) {
        Team team = Team.create(
                "물류혁신TF",
                TeamStatus.ACTIVE,
                "테스트 팀",
                LocalDate.of(2025, 1, 1),
                null
        );
        ReflectionTestUtils.setField(team, "id", id);
        return team;
    }

    private CreateTeamApiDto.Request createTeamRequest() {
        return new CreateTeamApiDto.Request(
                "물류혁신TF",
                "창고 자동화 및 운영 고도화",
                201L,
                List.of(
                        new CreateTeamApiDto.AddUser(202L, true, "WMS 운영"),
                        new CreateTeamApiDto.AddUser(203L, false, "현장 총괄")
                ),
                TeamStatus.ACTIVE,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 12, 31)
        );
    }

    private UpdateTeamApiDto.Request updateTeamRequest() {
        return new UpdateTeamApiDto.Request(
                "수정팀",
                "수정 설명",
                204L,
                205L,
                List.of(new UpdateTeamApiDto.AddUser(202L, true, "신규 리더")),
                List.of(203L),
                List.of(new UpdateTeamApiDto.EditUser(206L, "수정 역할")),
                TeamStatus.INACTIVE,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 11, 30)
        );
    }

    private User createUser(Long id, UserRole role) {
        User user = User.create(
                1L,
                "테스트사용자",
                "user-" + id + "@ibank.com",
                "$2a$10$abcdefghijklmnopqrstuv",
                role,
                EmploymentStatus.ACTIVE,
                "사원",
                null,
                LocalDate.of(2025, 1, 1),
                null,
                null
        );
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
