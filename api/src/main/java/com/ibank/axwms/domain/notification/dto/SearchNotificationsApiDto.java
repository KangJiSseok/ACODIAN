package com.ibank.axwms.domain.notification.dto;

import com.ibank.axwms.domain.notification.repository.jooq.projection.NotificationSearchProjection;
import com.ibank.axwms.global.response.PageResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SearchNotificationsApiDto {
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    @Schema(description = "알림 조회 요청 DTO")
    public record Request(
            @Schema(description = "읽음 상태 필터. null 이면 전체, true 이면 읽은 알림, false 이면 안읽은 알림", example = "false")
            Boolean isRead,
            @Schema(description = "부서 ID 필터 (단일)")
            Long departmentId,
            @Schema(description = "팀 ID 필터 (단일)")
            Long teamId,
            @Schema(description = "페이지 번호", example = "1")
            @Min(value = 1, message = "page 는 1 이상이어야 합니다.")
            Integer page,
            @Schema(description = "페이지 크기", example = "20")
            @Min(value = 1, message = "pageSize 는 1 이상이어야 합니다.")
            @Max(value = MAX_PAGE_SIZE, message = "pageSize 는 100 이하여야 합니다.")
            Integer pageSize

    ) {
        /** 페이지 번호가 없으면 목록 API 표준 기본값인 1페이지를 사용한다. */
        public int pageOrDefault() {
            return page == null ? DEFAULT_PAGE : page;
        }

        /** 페이지 크기가 없으면 목록 API 표준 기본값인 20건을 사용한다. */
        public int pageSizeOrDefault() {
            return pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;
        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Response {

        /** repository projection 페이지를 내 알림 목록 API 응답 페이지로 변환한다. */
        public static PageResponse<Item> fromPage(Page<NotificationSearchProjection> page) {
            return PageResponse.from(page.map(Item::from));
        }

        @Schema(description = "조회된 알림 항목")
        public record Item(
                @Schema(description = "알림 ID", example = "1001")
                Long notificationId,
                @Schema(description = "알림 유형", example = "WORKLOG_CREATED")
                String notificationType,    // TODO enum 추가되면 반영될 예정
                @Schema(description = "알림 제목", example = "새 업무가 등록되었습니다.")
                String title,
                @Schema(description = "알림 본문")
                String content,
                @Schema(description = "참조 대상 타입", example = "WORKLOG")
                String referenceType,       // TODO enum 추가되면 반영될 예정
                @Schema(description = "참조 대상 ID", example = "501")
                Long referenceId,
                @Schema(description = "알림이 속한 부서 ID", example = "3")
                Long departmentId,
                @Schema(description = "알림이 속한 팀 ID", example = "21")
                Long teamId,
                @Schema(description = "읽음 여부", example = "false")
                Boolean isRead,
                @Schema(description = "읽은 시각")
                LocalDateTime readAt,
                @Schema(description = "알림 생성 시각")
                LocalDateTime createdAt
        ) {

            /** repository projection 한 행을 알림 목록 응답 항목으로 변환한다. */
            public static Item from(NotificationSearchProjection projection) {
                return new Item(
                        projection.notificationId(),
                        projection.notificationType(),
                        projection.title(),
                        projection.content(),
                        projection.referenceType(),
                        projection.referenceId(),
                        projection.departmentId(),
                        projection.teamId(),
                        projection.isRead(),
                        projection.readAt(),
                        projection.createdAt()
                );
            }
        }
    }
}
