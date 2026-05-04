package com.ibank.axwms.domain.organization.team.repository;

import com.ibank.axwms.domain.organization.team.UserTeamStatus;
import com.ibank.axwms.domain.organization.team.entity.UserTeam;
import com.ibank.axwms.domain.organization.team.repository.jooq.UserTeamJooqRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTeamRepository extends JpaRepository<UserTeam, Long>, UserTeamJooqRepository {

    /**
     * 해당 사용자가 주어진 상태의 팀 소속인지 여부를 반환한다.
     * 업무 작성 같은 리소스 접근 판단은 ACTIVE membership 만 인정한다.
     */
    boolean existsByUserIdAndTeamIdAndStatusCode(Long userId, Long teamId, UserTeamStatus statusCode);

    /** 로컬 시드가 기존 사용자-팀 관계를 찾아 멱등 보정할 때 사용한다. */
    Optional<UserTeam> findByUserIdAndTeamId(Long userId, Long teamId);

    /** ACTIVE 리더 membership 을 조회한다. 리더 변경 시 기존 리더 강등에 사용한다. */
    List<UserTeam> findAllByTeamIdAndStatusCodeAndIsLeader(Long teamId, UserTeamStatus statusCode, boolean isLeader);

}
