package com.ibank.axwms.domain.organization.team.repository;

import com.ibank.axwms.domain.organization.team.entity.TeamAdmin;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamAdminRepository extends JpaRepository<TeamAdmin, Long> {

    /** 팀 관리 권한 grant를 보유하는지 확인한다 */
    boolean existsByUserIdAndTeamId(Long userId, Long teamId);

    /** 팀 관리 권한 grant 를 회수한다. 대상 grant 가 없으면 아무 것도 삭제하지 않는다. */
    void deleteByUserIdAndTeamId(Long userId, Long teamId);
}
