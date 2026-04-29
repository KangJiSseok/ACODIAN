package com.ibank.axwms.domain.organization.team.dto;

import com.ibank.axwms.domain.organization.team.TeamStatus;
import com.ibank.axwms.domain.organization.team.UserTeamAuthority;
import com.ibank.axwms.domain.organization.team.repository.jooq.projection.TeamListProjection;
import com.ibank.axwms.global.response.PageResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GetTeamsApiDto {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    @Schema(description = "팀 목록 조회 요청 DTO")
    public record Request(
            @Min(value = 1, message = "page 는 1 이상이어야 합니다.") Integer page,
            @Min(value = 1, message = "pageSize 는 1 이상이어야 합니다.") @Max(value = MAX_PAGE_SIZE, message = "pageSize 는 100 이하여야 합니다.") Integer pageSize,
            Long departmentId
    ) {
        public int pageOrDefault() {
            return page == null ? DEFAULT_PAGE : page;
        }

        public int pageSizeOrDefault() {
            return pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;
        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Response {
        public static PageResponse<Item> fromPage(Page<TeamListProjection> page) {
            return PageResponse.from(page.map(Item::from));
        }

        @Schema(description = "팀 목록 항목")
        public record Item(
                Long teamId,
                String teamName,
                TeamStatus statusCode,
                Long departmentId,
                String departmentName,
                String description,
                Long departmentHeadUserId,
                String departmentHeadUserName,
                Long teamLeaderId,
                String teamLeaderName,
                Integer memberCount,
                UserTeamAuthority myTeamAuthority,
                String teamRole,
                String allocation,
                Boolean isPrimary,
                LocalDate startDate,
                LocalDate expectedEndDate
        ) {
            public static Item from(TeamListProjection projection) {
                return new Item(
                        projection.teamId(),
                        projection.teamName(),
                        projection.statusCode(),
                        projection.departmentId(),
                        projection.departmentName(),
                        projection.description(),
                        projection.departmentHeadUserId(),
                        projection.departmentHeadUserName(),
                        projection.teamLeaderId(),
                        projection.teamLeaderName(),
                        projection.memberCount(),
                        projection.myTeamAuthority(),
                        projection.teamRole(),
                        projection.allocation(),
                        projection.isPrimary(),
                        projection.startDate(),
                        projection.expectedEndDate()
                );
            }
        }
    }
}
