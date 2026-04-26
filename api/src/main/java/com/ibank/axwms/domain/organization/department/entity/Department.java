package com.ibank.axwms.domain.organization.department.entity;

import com.ibank.axwms.domain.organization.department.DepartmentStatus;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "status_code", nullable = false, length = 20)
    private DepartmentStatus statusCode;

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
        department.statusCode = DepartmentStatus.ACTIVE;
        return department;
    }

    /** 로컬 시드 재실행이나 기본 정보 수정 시 부서 식별 문맥과 상태를 목표값으로 동기화한다. */
    public void synchronizeSeedProfile(String departmentName,
                                       String description,
                                       Long departmentHeadUserId,
                                       DepartmentStatus statusCode) {
        this.departmentName = departmentName;
        this.description = description;
        assignHeadUserId(departmentHeadUserId);
        changeStatus(statusCode);
    }

    /** API 수정 유스케이스에서 부서명과 설명을 함께 갱신한다. */
    public void updateBasicInfo(String departmentName, String description) {
        this.departmentName = departmentName;
        this.description = description;
    }

    /** 부서장 지정 상태를 바꾼다. head 가 없으면 null 을 허용한다. */
    public void assignHeadUserId(Long departmentHeadUserId) {
        this.departmentHeadUserId = departmentHeadUserId;
    }

    /** soft-delete 수명주기를 위해 부서 상태를 변경한다. */
    public void changeStatus(DepartmentStatus statusCode) {
        this.statusCode = statusCode;
    }
}
