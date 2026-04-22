package com.ibank.axwms.domain.organization.team.entity;

import com.ibank.axwms.domain.organization.team.TeamRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_user_team", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "team_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserTeam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_team_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Enumerated(EnumType.STRING)
    @Column(name = "team_role", nullable = false, length = 20)
    private TeamRole teamRole;

    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary;

    @Column(name = "joined_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime joinedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 사용자-팀 소속 관계를 생성한다.
     * userId/teamId 쌍의 중복 여부는 호출측이 사전에 검증하고, 주 소속 여부는 사용자별 한 건만 true 가 되도록 관리한다.
     */
    public static UserTeam create(Long userId, Long teamId, TeamRole teamRole, boolean isPrimary) {
        UserTeam userTeam = new UserTeam();
        userTeam.userId = userId;
        userTeam.teamId = teamId;
        userTeam.teamRole = teamRole;
        userTeam.isPrimary = isPrimary;
        return userTeam;
    }
}
