package com.ibank.axwms.domain.organization.evaluation.dto;

import com.ibank.axwms.domain.organization.evaluation.repository.jooq.projection.UserEvaluationSummaryProjection;
import com.ibank.axwms.global.response.PageResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GetUserEvaluationsApiDto {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    @Schema(description = "사용자 평가 이력 조회 요청 DTO")
    public record Request(
            @Schema(description = "페이지 번호", example = "1")
            @Min(value = 1, message = "page 는 1 이상이어야 합니다.")
            Integer page,
            @Schema(description = "페이지 크기", example = "20")
            @Min(value = 1, message = "pageSize 는 1 이상이어야 합니다.")
            @Max(value = MAX_PAGE_SIZE, message = "pageSize 는 100 이하여야 합니다.")
            Integer pageSize,
            @Schema(description = "정렬 방향", example = "DESC", allowableValues = {"ASC", "DESC"})
            String sortDirection
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
        public static PageResponse<Item> fromPage(Page<UserEvaluationSummaryProjection> page) {
            return PageResponse.from(page.map(Item::from));
        }

        @Schema(description = "사용자 평가 이력 항목")
        public record Item(
                @Schema(description = "평가 ID", example = "501")
                Long evaluationId,
                @Schema(description = "피평가자 사용자 ID", example = "101")
                Long evaluateeUserId,
                @Schema(description = "피평가자 이름", example = "홍길동")
                String evaluateeUserName,
                @Schema(description = "평가자 사용자 ID", example = "301")
                Long evaluatorUserId,
                @Schema(description = "평가자 이름", example = "김리더")
                String evaluatorUserName,
                @Schema(description = "평가 내용", example = "프로젝트 리딩이 안정적입니다.")
                String content,
                @Schema(description = "작성 시각", example = "2026-04-20T09:00:00")
                LocalDateTime createdAt
        ) {

            public static Item from(UserEvaluationSummaryProjection projection) {
                return new Item(
                        projection.evaluationId(),
                        projection.evaluateeUserId(),
                        projection.evaluateeUserName(),
                        projection.evaluatorUserId(),
                        projection.evaluatorUserName(),
                        projection.content(),
                        projection.createdAt()
                );
            }
        }
    }
}
