package com.ibank.axwms.domain.organization.team.repository;

import com.ibank.axwms.domain.organization.team.TeamStatus;
import com.ibank.axwms.domain.organization.team.entity.Team;
import com.ibank.axwms.domain.organization.team.repository.jooq.TeamJooqRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, Long>, TeamJooqRepository {

    /** 부서 비활성화 전 ACTIVE 팀이 남아 있는지 확인한다. */
    boolean existsByDepartmentIdAndStatusCode(Long departmentId, TeamStatus statusCode);

    /**
     * 로컬 시드가 같은 부서 안의 현재/legacy 팀명을 찾아 재사용할 때 사용한다.
     */
    Optional<Team> findFirstByDepartmentIdAndTeamNameOrderByDeletedAtDesc(Long departmentId, String teamName);
}
