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

    /**
     * 해당 사용자가 주어진 팀 소속인지 여부를 반환한다.
     * 탈퇴/이동으로 매핑이 제거되면 false 를 반환한다.
     */
    boolean existsByUserIdAndTeamId(Long userId, Long teamId);

    /** 현재 사용자에 연결된 팀 관계를 주 소속 우선으로 조회한다. */
    List<UserTeam> findAllByUserIdOrderByIsPrimaryDesc(Long userId);

    /** 로컬 시드가 같은 사용자-팀 관계를 중복 삽입하지 않도록 단건 탐색한다. */
    Optional<UserTeam> findByUserIdAndTeamId(Long userId, Long teamId);
}
