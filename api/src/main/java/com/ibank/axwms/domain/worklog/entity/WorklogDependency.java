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

    /**
     * 선행 업무 등록을 위한 신규 WorklogDependency 엔티티를 만든다.
     *
     * @param worklogId 의존을 갖는 (자식) worklog ID
     * @param dependsOnWorklogId 선행으로 지정된 worklog ID
     * @return 저장 전 WorklogDependency 엔티티
     */
    public static WorklogDependency create(Long worklogId, Long dependsOnWorklogId) {
        WorklogDependency dependency = new WorklogDependency();
        dependency.worklogId = worklogId;
        dependency.dependsOnWorklogId = dependsOnWorklogId;
        return dependency;
    }
}
