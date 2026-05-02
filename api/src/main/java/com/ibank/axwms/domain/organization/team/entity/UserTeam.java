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
import java.time.LocalDate;
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

    @Column(name = "is_leader", nullable = false)
    private Boolean isLeader;

    @Column(name = "team_role", nullable = false, length = 50)
    private String teamRole;

    @Column(name = "allocation", length = 50)
    private String allocation;

    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_code", nullable = false, length = 20)
    private UserTeamStatus statusCode;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** 사용자-팀 소속 관계를 생성한다. */
    public static UserTeam create(Long userId,
                                  Long teamId,
                                  boolean isLeader,
                                  String teamRole,
                                  String allocation,
                                  boolean isPrimary,
                                  UserTeamStatus statusCode) {
        return create(userId, teamId, isLeader, teamRole, allocation, isPrimary, statusCode, null);
    }

    /** 신규 membership 생성 시 joinedAt 을 명시적으로 주입할 수 있다. */
    public static UserTeam create(Long userId,
                                  Long teamId,
                                  boolean isLeader,
                                  String teamRole,
                                  String allocation,
                                  boolean isPrimary,
                                  UserTeamStatus statusCode,
                                  LocalDateTime joinedAt) {
        UserTeam userTeam = new UserTeam();
        userTeam.userId = userId;
        userTeam.teamId = teamId;
        userTeam.isLeader = isLeader;
        userTeam.teamRole = teamRole;
        userTeam.allocation = allocation;
        userTeam.isPrimary = isPrimary;
        userTeam.statusCode = statusCode;
        userTeam.joinedAt = joinedAt == null ? LocalDateTime.now() : joinedAt;
        return userTeam;
    }

    /** 로컬 시드 재실행 시 사용자-팀 관계의 권한/역할/주소속/상태를 목표값으로 맞춘다. */
    public void synchronizeSeedProfile(boolean isLeader,
                                       String teamRole,
                                       String allocation,
                                       boolean isPrimary,
                                       UserTeamStatus statusCode) {
        synchronizeMembershipProfile(isLeader, teamRole, allocation, isPrimary, statusCode);
    }

    /** membership 리더 여부/역할/상태를 한 번에 최신값으로 맞춘다. */
    public void synchronizeMembershipProfile(boolean isLeader,
                                             String teamRole,
                                             String allocation,
                                             boolean isPrimary,
                                             UserTeamStatus statusCode) {
        this.isLeader = isLeader;
        this.teamRole = teamRole;
        this.allocation = allocation;
        this.isPrimary = isPrimary;
        changeStatus(statusCode);
    }

    /** membership 단일 row 의 현재 상태를 바꾼다. */
    public void changeStatus(UserTeamStatus statusCode) {
        this.statusCode = statusCode;
    }

    /** create/update leader reassignment 시 현재 membership 을 LEADER 로 승격한다. */
    public void promoteToLeader(String teamRole, String allocation, boolean isPrimary) {
        synchronizeMembershipProfile(true, teamRole, allocation, isPrimary, UserTeamStatus.ACTIVE);
    }

    /** 같은 팀의 기존 대표자를 일반 멤버로 강등한다. */
    public void demoteToMember() {
        this.isLeader = false;
    }

    /** LEFT 처리된 membership 을 다시 활성화한다. */
    public void reactivate(boolean isLeader, String teamRole, String allocation, boolean isPrimary) {
        synchronizeMembershipProfile(isLeader, teamRole, allocation, isPrimary, UserTeamStatus.ACTIVE);
    }

    /** bulk write 에서 joinedAt 을 요청값으로 맞출 필요가 있을 때만 사용한다. */
    public void synchronizeJoinedAt(LocalDate joinedAt) {
        if (joinedAt != null) {
            this.joinedAt = joinedAt.atStartOfDay();
        }
    }
}
