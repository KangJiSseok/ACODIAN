package com.ibank.axwms.domain.worklog.dto;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class InternalWorklogTitleRecommendationApiDto {

    private static final int TITLE_MAX_COUNT = 3;

    /** API가 검증한 제목 추천 입력을 AI 서버 camelCase 계약으로 전달한다. */
    public record Request(
            String requestContent,
            String workContent
    ) {

        /** public 제목 추천 요청 DTO의 필드명을 내부 AI 호출 계약에 그대로 보존한다. */
        public static Request from(RecommendWorklogTitleApiDto.Request request) {
            return new Request(request.requestContent(), request.workContent());
        }
    }

    /** AI 서버의 제목 추천 결과를 저장 없이 작성 화면 응답으로 변환할 내부 후보 목록으로 받는다. */
    public record Response(
            List<String> titles
    ) {

        public Response {
            titles = normalizeTitles(titles);
        }

        /** 테스트와 내부 조립 지점에서 AI 서버 제목 추천 응답 계약을 명시적으로 만든다. */
        public static Response of(List<String> titles) {
            return new Response(titles);
        }

        /** AI 서버가 초과 후보나 공백 값을 보내도 public 응답 계약이 흔들리지 않도록 정규화한다. */
        private static List<String> normalizeTitles(List<String> titles) {
            if (titles == null) {
                return List.of();
            }
            return titles.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(title -> !title.isBlank())
                    .limit(TITLE_MAX_COUNT)
                    .toList();
        }
    }
}
