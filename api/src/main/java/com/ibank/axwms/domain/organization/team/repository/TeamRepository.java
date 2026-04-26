package com.ibank.axwms.domain.organization.team.repository;

import com.ibank.axwms.domain.organization.team.TeamStatus;
import com.ibank.axwms.domain.organization.team.entity.Team;
import com.ibank.axwms.domain.organization.team.repository.jooq.TeamJooqRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, Long>, TeamJooqRepository {

    /** 부서 내 팀명 단건 탐색. (department_id, team_name) UNIQUE 제약에 대응한다. */
    Optional<Team> findByDepartmentIdAndTeamName(Long departmentId, String teamName);

    /** 부서 비활성화 전 ACTIVE 팀이 남아 있는지 확인한다. */
    boolean existsByDepartmentIdAndStatusCode(Long departmentId, TeamStatus statusCode);

    /** 활성 팀 기준으로만 부서 내 팀명 단건 탐색을 수행한다. */
    Optional<Team> findByDepartmentIdAndTeamNameAndDeletedAtIsNull(Long departmentId, String teamName);

    /** 로컬 시드 보정에서 soft-delete 된 동일 팀명을 복구할 수 있도록 최신 팀 row 를 조회한다. */
    Optional<Team> findFirstByDepartmentIdAndTeamNameOrderByDeletedAtDesc(Long departmentId, String teamName);
}
