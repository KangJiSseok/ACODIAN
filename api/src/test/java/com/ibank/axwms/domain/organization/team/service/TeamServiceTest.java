package com.ibank.axwms.domain.organization.team.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.ibank.axwms.domain.organization.team.TeamStatus;
import com.ibank.axwms.domain.organization.team.entity.Team;
import com.ibank.axwms.domain.organization.team.repository.TeamRepository;
import com.ibank.axwms.domain.organization.team.repository.UserTeamRepository;
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private UserTeamRepository userTeamRepository;

    @InjectMocks
    private TeamService teamService;

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
    @DisplayName("사용자 팀 소속 여부는 repository 결과를 반환한다")
    void 사용자_팀_소속_여부는_repository_결과를_반환한다() {
        given(userTeamRepository.existsByUserIdAndTeamId(101L, 21L)).willReturn(true);

        boolean result = teamService.isMember(101L, 21L);

        assertThat(result).isTrue();
        then(userTeamRepository).should().existsByUserIdAndTeamId(101L, 21L);
    }

    private Team createTeam(Long id) {
        Team team = Team.create(
                10L,
                "물류혁신TF",
                TeamStatus.ACTIVE,
                "테스트 팀",
                LocalDate.of(2025, 1, 1),
                null
        );
        ReflectionTestUtils.setField(team, "id", id);
        return team;
    }
}
