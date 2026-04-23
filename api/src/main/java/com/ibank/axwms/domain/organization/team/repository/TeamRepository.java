package com.ibank.axwms.domain.organization.team.repository;

import com.ibank.axwms.domain.organization.team.entity.Team;
import com.ibank.axwms.domain.organization.team.repository.jooq.TeamJooqRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, Long>, TeamJooqRepository {

    /** 부서 내 팀명 단건 탐색. (department_id, team_name) UNIQUE 제약에 대응한다. */
    Optional<Team> findByDepartmentIdAndTeamName(Long departmentId, String teamName);
}
