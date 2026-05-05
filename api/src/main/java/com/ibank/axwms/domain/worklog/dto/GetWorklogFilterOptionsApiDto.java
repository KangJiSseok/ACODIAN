package com.ibank.axwms.domain.worklog.dto;

import com.ibank.axwms.domain.organization.team.repository.jooq.projection.TeamMemberFilterProjection;
import com.ibank.axwms.domain.tag.repository.jooq.projection.TagSummaryProjection;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GetWorklogFilterOptionsApiDto {

    @Schema(description = "업무일지 검색 화면 필터 옵션 응답")
    public record Response(
            @Schema(description = "사용자가 볼 수 있는 팀 목록 (각 팀의 ACTIVE 멤버 포함)")
            List<TeamItem> teams,
            @Schema(description = "전체 태그 목록")
            List<TagItem> tags
    ) {

        public static Response of(List<TeamMemberFilterProjection> teamMemberRows,
                                  List<TagSummaryProjection> tagRows) {
            return new Response(toTeamItems(teamMemberRows), toTagItems(tagRows));
        }

        /**
         * (teamId, teamName, userId, userName) 행들을 teamId 순서를 보존하며 그룹핑해 TeamItem 리스트로 만든다.
         * userId 가 null 인 행은 멤버가 없는 팀을 의미하므로 members 에 추가하지 않는다.
         */
        private static List<TeamItem> toTeamItems(List<TeamMemberFilterProjection> rows) {
            Map<Long, TeamItem> indexed = new LinkedHashMap<>();
            for (TeamMemberFilterProjection row : rows) {
                TeamItem item = indexed.computeIfAbsent(row.teamId(),
                        id -> new TeamItem(id, row.teamName(), new java.util.ArrayList<>()));
                if (row.userId() != null) {
                    item.members().add(new MemberItem(row.userId(), row.userName()));
                }
            }
            return List.copyOf(indexed.values());
        }

        private static List<TagItem> toTagItems(List<TagSummaryProjection> rows) {
            return rows.stream().map(t -> new TagItem(t.tagId(), t.tagName())).toList();
        }

        @Schema(description = "필터용 팀 항목 (멤버 포함)")
        public record TeamItem(
                @Schema(description = "팀 ID", example = "21")
                Long teamId,
                @Schema(description = "팀명", example = "물류혁신TF")
                String teamName,
                @Schema(description = "팀 ACTIVE 멤버 목록")
                List<MemberItem> members
        ) {
        }

        @Schema(description = "필터용 멤버 항목")
        public record MemberItem(
                @Schema(description = "사용자 ID", example = "101")
                Long userId,
                @Schema(description = "사용자명", example = "홍길동")
                String userName
        ) {
        }

        @Schema(description = "필터용 태그 항목")
        public record TagItem(
                @Schema(description = "태그 ID", example = "1")
                Long tagId,
                @Schema(description = "태그 이름", example = "결산")
                String tagName
        ) {
        }
    }
}
