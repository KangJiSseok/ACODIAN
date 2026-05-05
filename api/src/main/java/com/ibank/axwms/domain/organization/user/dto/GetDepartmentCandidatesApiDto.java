package com.ibank.axwms.domain.organization.user.dto;

import com.ibank.axwms.domain.organization.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GetDepartmentCandidatesApiDto {

    @Schema(description = "부서 배정 후보 사용자 요약")
    public record Response(
            @Schema(description = "사용자 ID", example = "201")
            Long userId,
            @Schema(description = "사용자명", example = "김부서")
            String userName
    ) {

        /** 부서에 아직 소속되지 않은 DEPT_HEAD 사용자를 후보 응답으로 변환한다. */
        public static Response from(User user) {
            return new Response(
                    user.getId(),
                    user.getUserName()
            );
        }
    }
}
