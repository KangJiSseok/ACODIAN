package com.ibank.axwms.domain.organization.team.entity;

import com.ibank.axwms.domain.organization.team.UserTeamStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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

    @Column(name = "team_leader", nullable = false)
    private Boolean teamLeader;

    @Column(name = "team_role", nullable = false, length = 50)
    private String teamRole;

    @Column(name = "allocation", length = 50)
    private String allocation;

    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_code", nullable = false, length = 20)
    private UserTeamStatus statusCode;

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
     * 탈퇴/재가입은 별도 row 누적이 아니라 statusCode 전환으로 표현한다.
     */
    public static UserTeam create(Long userId,
                                  Long teamId,
                                  boolean teamLeader,
                                  String teamRole,
                                  String allocation,
                                  boolean isPrimary,
                                  UserTeamStatus statusCode) {
        UserTeam userTeam = new UserTeam();
        userTeam.userId = userId;
        userTeam.teamId = teamId;
        userTeam.teamLeader = teamLeader;
        userTeam.teamRole = teamRole;
        userTeam.allocation = allocation;
        userTeam.isPrimary = isPrimary;
        userTeam.statusCode = statusCode;
        return userTeam;
    }

    /** 로컬 시드 재실행 시 사용자-팀 관계의 역할/주소속/상태를 목표값으로 맞춘다. */
    public void synchronizeSeedProfile(boolean teamLeader,
                                       String teamRole,
                                       String allocation,
                                       boolean isPrimary,
                                       UserTeamStatus statusCode) {
        this.teamLeader = teamLeader;
        this.teamRole = teamRole;
        this.allocation = allocation;
        this.isPrimary = isPrimary;
        changeStatus(statusCode);
    }

    /** membership 단일 row 의 현재 상태를 바꾼다. */
    public void changeStatus(UserTeamStatus statusCode) {
        this.statusCode = statusCode;
    }
}
