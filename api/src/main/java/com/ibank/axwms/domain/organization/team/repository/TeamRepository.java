package com.ibank.axwms.domain.organization.team.repository;

import com.ibank.axwms.domain.organization.team.TeamStatus;
import com.ibank.axwms.domain.organization.team.entity.Team;
import com.ibank.axwms.domain.organization.team.repository.jooq.TeamJooqRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, Long>, TeamJooqRepository {

    /** 동일 팀명 후보를 soft-delete 최신순으로 조회해 생성 검증 후속 확인에 사용한다. */
    Optional<Team> findFirstByTeamNameOrderByDeletedAtDesc(String teamName);

    /** soft-delete 되지 않은 팀을 ID로 조회한다. */
    Optional<Team> findByIdAndDeletedAtIsNull(Long id);

    /** soft-delete 되지 않았고 지정 상태에 해당하는 팀을 ID로 조회한다 (예: dashboard 진입 게이트의 ACTIVE 필터). */
    Optional<Team> findByIdAndDeletedAtIsNullAndStatusCode(Long id, TeamStatus statusCode);

    /** soft-delete 되지 않은 팀 ID가 존재하는지 확인한다. */
    boolean existsByIdAndDeletedAtIsNull(Long id);

    /** soft-delete 되지 않았고 지정 상태에 해당하는 팀이 존재하는지 확인한다. */
    boolean existsByIdAndDeletedAtIsNullAndStatusCode(Long id, TeamStatus statusCode);

    /** soft-delete 되지 않은 동일 팀명 존재 여부를 확인한다. */
    boolean existsByTeamNameAndDeletedAtIsNull(String teamName);

    /** 자기 자신을 제외하고 soft-delete 되지 않은 동일 팀명이 존재하는지 확인한다. */
    boolean existsByTeamNameAndDeletedAtIsNullAndIdNot(String teamName, Long id);
}
