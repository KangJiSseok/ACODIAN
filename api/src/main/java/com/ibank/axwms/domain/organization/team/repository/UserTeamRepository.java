package com.ibank.axwms.domain.organization.team.repository;

import com.ibank.axwms.domain.organization.team.UserTeamStatus;
import com.ibank.axwms.domain.organization.team.entity.UserTeam;
import com.ibank.axwms.domain.organization.team.repository.jooq.UserTeamJooqRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTeamRepository extends JpaRepository<UserTeam, Long>, UserTeamJooqRepository {

    /** 현재 사용자에 연결된 ACTIVE 팀 관계를 주 소속 우선으로 조회한다. */
    List<UserTeam> findAllByUserIdAndStatusCodeOrderByIsPrimaryDesc(Long userId, UserTeamStatus statusCode);

    /** 읽기 ownership 검증에서 현재 사용자의 대표 ACTIVE 팀을 빠르게 복원한다. */
    Optional<UserTeam> findFirstByUserIdAndStatusCodeOrderByIsPrimaryDesc(Long userId, UserTeamStatus statusCode);

    /** 로컬 시드가 같은 사용자-팀 관계를 중복 삽입하지 않도록 단건 탐색한다. */
    Optional<UserTeam> findByUserIdAndTeamId(Long userId, Long teamId);
}
