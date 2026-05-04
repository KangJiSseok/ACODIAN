package com.ibank.axwms.domain.organization.skill.entity;

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
@Table(name = "tb_user_skill", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "skill_name"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_skill_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "skill_name", nullable = false, length = 100)
    private String skillName;

    @Column(name = "skill_level", nullable = false)
    private Short skillLevel;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** 특정 사용자에게 새 스킬을 등록한다. userId 와 skillName 조합은 UNIQUE 제약을 따른다. */
    public static UserSkill create(Long userId, String skillName, Short skillLevel) {
        UserSkill userSkill = new UserSkill();
        userSkill.userId = userId;
        userSkill.skillName = skillName;
        userSkill.skillLevel = skillLevel;
        return userSkill;
    }
}
