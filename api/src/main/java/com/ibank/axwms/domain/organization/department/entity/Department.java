package com.ibank.axwms.domain.organization.department.entity;

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
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_department", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"department_name"}),
        @UniqueConstraint(columnNames = {"department_head_user_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "department_id")
    private Long id;

    @Column(name = "department_name", nullable = false, length = 100)
    private String departmentName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "department_head_user_id")
    private Long departmentHeadUserId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 신규 부서를 생성한다.
     * departmentName 은 tb_department 의 UNIQUE key 이므로 중복 여부는 호출측이 사전 검증한다.
     * departmentHeadUserId 는 부서장 지정 유스케이스에서 별도로 주입되므로 생성 시점에는 null 로 둔다.
     */
    public static Department create(String departmentName, String description) {
        Department department = new Department();
        department.departmentName = departmentName;
        department.description = description;
        return department;
    }
}
