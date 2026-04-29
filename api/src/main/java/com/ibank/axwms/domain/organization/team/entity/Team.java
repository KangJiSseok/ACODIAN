package com.ibank.axwms.domain.organization.team.entity;

import com.ibank.axwms.domain.organization.team.TeamStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "tb_team")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_id")
    private Long id;

    @Column(name = "department_id", nullable = false)
    private Long departmentId;

    @Column(name = "team_name", nullable = false, length = 100)
    private String teamName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_code", nullable = false, length = 20)
    private TeamStatus statusCode;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "expected_end_date")
    private LocalDate expectedEndDate;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 신규 팀을 생성한다.
     * soft-delete lifecycle 은 deletedAt 으로 별도 관리하므로 생성 시점에는 null 로 둔다.
     */
    public static Team create(Long departmentId,
                              String teamName,
                              TeamStatus statusCode,
                              String description,
                              LocalDate startDate,
                              LocalDate expectedEndDate) {
        Team team = new Team();
        team.departmentId = departmentId;
        team.teamName = teamName;
        team.statusCode = statusCode;
        team.description = description;
        team.startDate = startDate;
        team.expectedEndDate = expectedEndDate;
        team.deletedAt = null;
        return team;
    }

    /** 로컬 시드 재실행 시 팀의 식별 문맥과 운영 상태를 목표값으로 맞추고 soft-delete 는 해제한다. */
    public void synchronizeSeedProfile(Long departmentId,
                                       String teamName,
                                       TeamStatus statusCode,
                                       String description,
                                       LocalDate startDate,
                                       LocalDate expectedEndDate) {
        updateProfile(departmentId, teamName, statusCode, description, startDate, expectedEndDate);
        this.deletedAt = null;
    }

    /** 팀 기본 정보를 최신 요청값으로 갱신한다. */
    public void updateProfile(Long departmentId,
                              String teamName,
                              TeamStatus statusCode,
                              String description,
                              LocalDate startDate,
                              LocalDate expectedEndDate) {
        this.departmentId = departmentId;
        this.teamName = teamName;
        this.description = description;
        this.startDate = startDate;
        this.expectedEndDate = expectedEndDate;
        changeStatus(statusCode);
    }

    /** 운영 상태를 바꾼다. soft-delete 와는 별개다. */
    public void changeStatus(TeamStatus statusCode) {
        this.statusCode = statusCode;
    }

    /** 팀을 soft-delete 처리한다. */
    public void markDeleted(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}
