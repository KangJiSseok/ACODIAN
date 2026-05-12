package com.ibank.axwms.domain.organization.department.dto;

import com.ibank.axwms.domain.organization.department.repository.jooq.projection.DepartmentDetailTeamProjection;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GetDepartmentDetailApiDto {

    /** 부서 상세 화면이 부서 header 와 owned team 목록을 한 번에 렌더링할 수 있도록 고정한 응답 DTO. */
    @Schema(description = "부서 상세 응답")
    public record Response(
            @Schema(description = "부서 ID", example = "10")
            Long departmentId,
            @Schema(description = "부서명", example = "물류본부")
            String departmentName,
            @Schema(description = "부서장 사용자 ID", example = "1001", nullable = true)
            Long departmentHeadUserId,
            @Schema(description = "부서장 사용자명", example = "박본부", nullable = true)
            String departmentHeadUserName,
            @Schema(description = "부서가 직접 소유한 활성 팀 목록")
            List<TeamSummary> teams
    ) {

        /** header 조회 결과와 team projection 목록을 방어적 복사해 단일 상세 응답으로 조립한다. */
        public static Response of(Long departmentId,
                                  String departmentName,
                                  Long departmentHeadUserId,
                                  String departmentHeadUserName,
                                  List<TeamSummary> teams) {
            return new Response(
                    departmentId,
                    departmentName,
                    departmentHeadUserId,
                    departmentHeadUserName,
                    teams == null ? List.of() : List.copyOf(teams)
            );
        }

        /** 상세 화면의 팀 행에서 필요한 ownership 기반 팀 요약 정보다. */
        @Schema(description = "부서 상세 팀 요약")
        public record TeamSummary(
                @Schema(description = "팀 ID", example = "21")
                Long teamId,
                @Schema(description = "팀명", example = "플랫폼개발팀")
                String teamName,
                @Schema(description = "리더 사용자 ID", example = "1002", nullable = true)
                Long leaderId,
                @Schema(description = "리더 사용자명", example = "류팀장", nullable = true)
                String leaderName,
                @Schema(description = "팀 시작일")
                LocalDate startDate,
                @Schema(description = "팀 종료 예정일", nullable = true)
                LocalDate expectedEndDate,
                @Schema(description = "ACTIVE membership 수", example = "5")
                long memberCount
        ) {

            /** repository ownership projection 을 부서 상세 API 의 teams[] vocabulary 로 변환한다. */
            public static TeamSummary from(DepartmentDetailTeamProjection team) {
                return new TeamSummary(
                        team.teamId(),
                        team.teamName(),
                        team.leaderId(),
                        team.leaderName(),
                        team.startDate(),
                        team.expectedEndDate(),
                        team.memberCount()
                );
            }
        }
    }
}
