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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_team", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"department_id", "team_name"})
})
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

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 신규 팀을 생성한다.
     * departmentId 는 유효한 부서를 가리켜야 하며, 같은 부서 안의 teamName 중복 여부는 호출측이 사전 검증한다.
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
        return team;
    }
}
