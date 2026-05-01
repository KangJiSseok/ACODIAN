package com.ibank.axwms.domain.organization.team.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ibank.axwms.domain.organization.team.TeamStatus;
import com.ibank.axwms.domain.organization.team.entity.Team;
import com.ibank.axwms.testsupport.IntegrationTestSupport;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TeamRepositoryIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private TeamRepository teamRepository;

    @BeforeEach
    void setUp() {
        teamRepository.deleteAll();
    }

    @Test
    @DisplayName("팀명으로 로컬 시드 재사용 대상 팀을 조회한다")
    void 팀명으로_로컬_시드_재사용_대상_팀을_조회한다() {
        Team team = teamRepository.save(createTeam("물류혁신TF"));

        assertThat(teamRepository.findFirstByTeamNameOrderByDeletedAtDesc("물류혁신TF"))
                .get()
                .extracting(Team::getId)
                .isEqualTo(team.getId());
    }

    @Test
    @DisplayName("팀명이 일치하지 않으면 빈 Optional 을 반환한다")
    void 팀명이_일치하지_않으면_빈_Optional을_반환한다() {
        teamRepository.save(createTeam("물류혁신TF"));

        assertThat(teamRepository.findFirstByTeamNameOrderByDeletedAtDesc("운영지원TF"))
                .isEmpty();
    }

    private Team createTeam(String teamName) {
        return Team.create(
                teamName,
                TeamStatus.ACTIVE,
                teamName + " 설명",
                LocalDate.of(2026, 4, 1),
                null
        );
    }
}
