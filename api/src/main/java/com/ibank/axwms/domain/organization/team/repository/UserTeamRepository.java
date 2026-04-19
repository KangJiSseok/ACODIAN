package com.ibank.axwms.domain.organization.team.repository;

import com.ibank.axwms.domain.organization.team.entity.UserTeam;
import com.ibank.axwms.domain.organization.team.repository.jooq.UserTeamJooqRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTeamRepository extends JpaRepository<UserTeam, Long>, UserTeamJooqRepository {
}
