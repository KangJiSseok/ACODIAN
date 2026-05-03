package com.ibank.axwms.domain.organization.user.dto;

import com.ibank.axwms.domain.organization.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GetManagerCandidatesApiDto {

    @Schema(description = "관리자 선택 후보 사용자 요약")
    public record Response(
            Long userId,
            String userName,
            String titleName,
            String positionName
    ) {
        public static Response from(User user) {
            return new Response(
                    user.getId(),
                    user.getUserName(),
                    user.getTitleName(),
                    user.getPositionName()
            );
        }
    }
}
