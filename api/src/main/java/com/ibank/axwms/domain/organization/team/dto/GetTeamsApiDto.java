package com.ibank.axwms.domain.organization.team.dto;

import com.ibank.axwms.domain.organization.team.repository.jooq.projection.TeamSummaryProjection;
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
            @Schema(description = "페이지 번호", example = "1")
            @Min(value = 1, message = "page 는 1 이상이어야 합니다.")
            Integer page,
            @Schema(description = "페이지 크기", example = "20")
            @Min(value = 1, message = "pageSize 는 1 이상이어야 합니다.")
            @Max(value = MAX_PAGE_SIZE, message = "pageSize 는 100 이하여야 합니다.")
            Integer pageSize
    ) {
        public int pageOrDefault() {
            return page == null ? DEFAULT_PAGE : page;
        }

        public int pageSizeOrDefault() {
            return pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;
        }
    }

    @Schema(description = "팀 목록 항목")
    public record Response(
            @Schema(description = "팀 ID", example = "21")
            Long teamId,
            @Schema(description = "팀명", example = "물류혁신TF")
            String teamName,
            @Schema(description = "팀 상태 코드", example = "ACTIVE")
            String statusCode,
            @Schema(description = "팀 설명", example = "창고 자동화 개선 전담")
            String description,
            @Schema(description = "팀 대표 사용자 ID", example = "101")
            Long teamLeaderId,
            @Schema(description = "팀 대표 사용자명", example = "홍길동")
            String teamLeaderName,
            @Schema(description = "ACTIVE membership 사용자 수", example = "6")
            Long memberCount,
            @Schema(description = "호출자의 해당 팀 리더 여부", example = "true")
            Boolean myIsLeader,
            @Schema(description = "호출자의 팀 내 업무 역할", example = "플랫폼 총괄")
            String teamRole,
            @Schema(description = "호출자의 배치 성격", example = "PRIMARY")
            String allocation,
            @Schema(description = "호출자의 대표 소속 여부", example = "true")
            Boolean isPrimary,
            @Schema(description = "팀 시작일", example = "2026-04-01")
            LocalDate startDate,
            @Schema(description = "팀 종료 예정일", example = "2026-12-31")
            LocalDate expectedEndDate
    ) {

        /** repository projection 페이지를 팀 목록 API 응답 페이지로 변환한다. */
        public static PageResponse<Response> fromPage(Page<TeamSummaryProjection> page) {
            return PageResponse.from(page.map(Response::from));
        }

        /** repository projection 한 행을 팀 목록 응답 항목으로 변환한다. */
        public static Response from(TeamSummaryProjection projection) {
            return new Response(
                    projection.teamId(),
                    projection.teamName(),
                    projection.statusCode(),
                    projection.description(),
                    projection.teamLeaderId(),
                    projection.teamLeaderName(),
                    projection.memberCount(),
                    projection.myIsLeader(),
                    projection.teamRole(),
                    projection.allocation(),
                    projection.isPrimary(),
                    projection.startDate(),
                    projection.expectedEndDate()
            );
        }
    }
}
