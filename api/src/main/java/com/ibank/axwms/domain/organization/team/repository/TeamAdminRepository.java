package com.ibank.axwms.domain.organization.team.repository;

import com.ibank.axwms.domain.organization.team.entity.TeamAdmin;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamAdminRepository extends JpaRepository<TeamAdmin, Long> {

    /** 팀 관리 권한 grant 중복 생성을 방지한다. */
    boolean existsByUserIdAndTeamId(Long userId, Long teamId);
}
