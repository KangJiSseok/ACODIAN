package com.ibank.axwms.global.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.Page;

/**
 * 페이지네이션 응답 공통 포맷.
 * 클라이언트 친화적으로 page 를 1-based 로 노출한다(Spring Page 는 0-based).
 * isFirst/isLast 는 boolean record 컴포넌트의 Jackson 기본 직렬화가 {@code first}/{@code last} 로 떨어지므로
 * @JsonProperty 로 응답 필드명을 강제한다.
 */
public record PageResponse<T>(
        List<T> items,
        int page,
        int pageSize,
        long totalCount,
        int totalPages,
        @JsonProperty("isFirst") boolean isFirst,
        @JsonProperty("isLast") boolean isLast,
        boolean hasNext,
        boolean hasPrevious
) {

    public PageResponse {
        items = immutableItems(items);
        validatePositive("페이지 번호", page);
        validatePositive("페이지 크기", pageSize);
        validateNonNegative("전체 건수", totalCount);
        validateNonNegative("전체 페이지 수", totalPages);
    }

    /** Spring Data Page 를 응답 포맷으로 변환한다. page 번호는 0-based → 1-based 로 보정된다. */
    public static <T> PageResponse<T> from(Page<T> page) {
        Objects.requireNonNull(page, "페이지 정보는 null일 수 없습니다.");
        return new PageResponse<>(
                page.getContent(),
                page.getNumber() + 1,
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                page.hasNext(),
                page.hasPrevious());
    }

    private static <T> List<T> immutableItems(List<T> items) {
        return List.copyOf(Objects.requireNonNull(items, "목록 데이터는 null일 수 없습니다."));
    }

    private static void validatePositive(String fieldName, int value) {
        if (value < 1) {
            throw new IllegalArgumentException(fieldName + "는 1 이상이어야 합니다.");
        }
    }

    private static void validateNonNegative(String fieldName, long value) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + "는 0 이상이어야 합니다.");
        }
    }
}
