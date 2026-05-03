package com.ibank.axwms.domain.organization.user.repository.jooq.projection;

import com.ibank.axwms.domain.organization.user.EmploymentStatus;
import org.jooq.Record;

/** 사용자 목록 한 행에 필요한 필드만 담는 읽기 전용 projection 이다. */
public record UserSummaryProjection(
        Long userId,
        String userName,
        String email,
        String phone,
        Long departmentId,
        String departmentName,
        String profileImageUrl,
        Long teamId,
        String teamName,
        String positionName,
        String titleName,
        EmploymentStatus employmentStatus
) {

    /** JOOQ 조회 결과 record 를 사용자 목록 projection 으로 조립한다. */
    public static UserSummaryProjection from(Record record) {
        String employmentStatus = record.get("employment_status", String.class);
        return new UserSummaryProjection(
                record.get("user_id", Long.class),
                record.get("user_name", String.class),
                record.get("email", String.class),
                record.get("phone", String.class),
                record.get("department_id", Long.class),
                record.get("department_name", String.class),
                record.get("profile_image_url", String.class),
                record.get("team_id", Long.class),
                record.get("team_name", String.class),
                record.get("position_name", String.class),
                record.get("title_name", String.class),
                employmentStatus == null ? null : EmploymentStatus.valueOf(employmentStatus)
        );
    }
}
