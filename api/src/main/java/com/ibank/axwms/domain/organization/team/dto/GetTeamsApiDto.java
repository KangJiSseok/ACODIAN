package com.ibank.axwms.domain.organization.team.dto;

import com.ibank.axwms.domain.organization.team.TeamStatus;
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
            @Schema(description = "페이지 번호", example = "1")
            @Min(value = 1, message = "page 는 1 이상이어야 합니다.")
            Integer page,
            @Schema(description = "페이지 크기", example = "20")
            @Min(value = 1, message = "pageSize 는 1 이상이어야 합니다.")
            @Max(value = MAX_PAGE_SIZE, message = "pageSize 는 100 이하여야 합니다.")
            Integer pageSize,
            @Schema(description = "부서 ID", example = "10")
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

        /** repository projection 페이지를 API 응답 페이지로 변환한다. */
        public static PageResponse<Item> fromPage(Page<TeamListProjection> page) {
            return PageResponse.from(page.map(Item::from));
        }

        @Schema(description = "팀 목록 항목")
        public record Item(
                @Schema(description = "팀 ID", example = "21")
                Long teamId,
                @Schema(description = "팀명", example = "물류혁신TF")
                String teamName,
                @Schema(description = "운영 상태", example = "ACTIVE")
                TeamStatus statusCode,
                @Schema(description = "부서 ID", example = "10")
                Long departmentId,
                @Schema(description = "부서명", example = "물류본부")
                String departmentName,
                @Schema(description = "팀 설명", example = "창고 자동화 개선 전담")
                String description,
                @Schema(description = "부서장 사용자 ID", example = "1001")
                Long departmentHeadUserId,
                @Schema(description = "부서장 사용자명", example = "박본부")
                String departmentHeadUserName,
                @Schema(description = "팀장 사용자 ID", example = "101")
                Long teamLeaderId,
                @Schema(description = "팀장 사용자명", example = "홍길동")
                String teamLeaderName,
                @Schema(description = "활성 멤버 수", example = "8")
                Integer memberCount,
                @Schema(description = "호출자가 해당 팀 팀장인지 여부", example = "true")
                Boolean myTeamLeader,
                @Schema(description = "호출자의 팀 역할", example = "프론트/웹")
                String teamRole,
                @Schema(description = "호출자의 배치 성격", example = "주담당")
                String allocation,
                @Schema(description = "호출자의 주 소속 팀 여부", example = "true")
                Boolean isPrimary,
                @Schema(description = "팀 시작일", example = "2026-04-01")
                LocalDate startDate,
                @Schema(description = "예상 종료일", example = "2026-12-31")
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
                        projection.myTeamLeader(),
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
