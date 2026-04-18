package com.ibank.axwms.domain.worklog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_worklog_dependency", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"worklog_id", "depends_on_worklog_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorklogDependency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dependency_id")
    private Long id;

    @Column(name = "worklog_id", nullable = false)
    private Long worklogId;

    @Column(name = "depends_on_worklog_id", nullable = false)
    private Long dependsOnWorklogId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
