package com.ibank.axwms.domain.organization.team.repository;

import com.ibank.axwms.domain.organization.team.entity.Team;
import com.ibank.axwms.domain.organization.team.repository.jooq.TeamJooqRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, Long>, TeamJooqRepository {

    /**
     * 로컬 시드가 현재/legacy 팀명을 찾아 재사용할 때 사용한다.
     */
    Optional<Team> findFirstByTeamNameOrderByDeletedAtDesc(String teamName);

    /** soft-delete 되지 않은 팀 ID가 존재하는지 확인한다. */
    boolean existsByIdAndDeletedAtIsNull(Long id);
}
