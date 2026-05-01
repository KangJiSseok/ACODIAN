package com.ibank.axwms.domain.organization.team.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * 팀 관리 권한 grant 의 entity-side 모델이다.
 * 실제 테이블 migration 이 추가되기 전까지는 JPA managed entity 로 활성화하지 않는다.
 */
@MappedSuperclass
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamAdmin {

    @Id
    @Column(name = "team_admin_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "granted_at", nullable = false)
    private LocalDateTime grantedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 업무 소속과 분리된 팀 관리 권한 grant 를 생성한다.
     * 같은 사용자와 팀 조합은 후속 migration 의 unique 제약으로 한 번만 부여된다.
     */
    public static TeamAdmin grant(Long userId, Long teamId) {
        TeamAdmin teamAdmin = new TeamAdmin();
        teamAdmin.userId = Objects.requireNonNull(userId, "userId must not be null");
        teamAdmin.teamId = Objects.requireNonNull(teamId, "teamId must not be null");
        teamAdmin.grantedAt = LocalDateTime.now();
        return teamAdmin;
    }
}
