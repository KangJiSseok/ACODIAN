package com.ibank.axwms.domain.organization.user.entity;

import com.ibank.axwms.domain.organization.user.EmploymentStatus;
import com.ibank.axwms.domain.organization.user.UserRole;
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
@Table(name = "tb_user", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"email"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "department_id", nullable = false)
    private Long departmentId;

    @Column(name = "user_name", nullable = false, length = 50)
    private String userName;

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "position_name", length = 50)
    private String positionName;

    @Column(name = "title_name", length = 50)
    private String titleName;

    @Column(name = "join_date")
    private LocalDate joinDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_code", nullable = false, length = 20)
    private UserRole roleCode;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Column(name = "phone", length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_status", nullable = false, length = 20)
    private EmploymentStatus employmentStatus;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 신규 사용자를 생성한다.
     * email 은 tb_user 의 UNIQUE key 이므로 중복 여부는 호출측이 사전 검증한다.
     * passwordHash 는 BCrypt 로 해시된 값이어야 하며 평문을 전달하면 인증이 성립하지 않는다.
     * departmentId 는 tb_department 의 유효한 id 여야 한다(FK, NOT NULL).
     */
    public static User create(Long departmentId,
                              String userName,
                              String email,
                              String passwordHash,
                              UserRole roleCode,
                              EmploymentStatus employmentStatus,
                              String positionName,
                              String titleName,
                              LocalDate joinDate,
                              String phone,
                              String profileImageUrl) {
        User user = new User();
        user.departmentId = departmentId;
        user.userName = userName;
        user.email = email;
        user.passwordHash = passwordHash;
        user.roleCode = roleCode;
        user.employmentStatus = employmentStatus;
        user.positionName = positionName;
        user.titleName = titleName;
        user.joinDate = joinDate;
        user.phone = phone;
        user.profileImageUrl = profileImageUrl;
        return user;
    }

    /**
     * 로컬 시드 재실행 시 사용자 조직/프로필 문맥을 최신 기준으로 맞춘다.
     * 이미 존재하는 시드 사용자의 titleName, 직급, 부서, 권한이 바뀌어도 멱등하게 보정할 수 있게 한다.
     */
    public void synchronizeSeedProfile(Long departmentId,
                                       String userName,
                                       UserRole roleCode,
                                       EmploymentStatus employmentStatus,
                                       String positionName,
                                       String titleName,
                                       LocalDate joinDate) {
        this.departmentId = departmentId;
        this.userName = userName;
        this.roleCode = roleCode;
        this.employmentStatus = employmentStatus;
        this.positionName = positionName;
        this.titleName = titleName;
        this.joinDate = joinDate;
    }

    /** null 이 아닌 요청 필드만 사용자 기본 정보에 반영한다. */
    public void updatePartial(String userName,
                              String email,
                              String profileImageUrl,
                              String positionName,
                              String titleName,
                              Long departmentId,
                              String phone,
                              EmploymentStatus employmentStatus,
                              LocalDate joinDate) {
        if (userName != null) {
            this.userName = userName;
        }
        if (email != null) {
            this.email = email;
        }
        if (profileImageUrl != null) {
            this.profileImageUrl = profileImageUrl;
        }
        if (positionName != null) {
            this.positionName = positionName;
        }
        if (titleName != null) {
            this.titleName = titleName;
        }
        if (departmentId != null) {
            this.departmentId = departmentId;
        }
        if (phone != null) {
            this.phone = phone;
        }
        if (employmentStatus != null) {
            this.employmentStatus = employmentStatus;
        }
        if (joinDate != null) {
            this.joinDate = joinDate;
        }
    }
}
