package com.ibank.axwms.domain.worklog.dto;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class InternalWorklogPolishApiDto {

    /** API가 검증한 작성 보조 입력을 AI 서버 camelCase 계약으로 전달한다. */
    public record Request(
            String requestContent,
            String workContent
    ) {

        /** public 요청 DTO의 필드명을 내부 AI 호출 계약에 그대로 보존한다. */
        public static Request from(PolishWorklogApiDto.Request request) {
            return new Request(request.requestContent(), request.workContent());
        }
    }

    /** AI 서버의 작성 보조 결과를 저장 파이프라인과 분리된 임시 본문 응답으로 받는다. */
    public record Response(
            String workContent
    ) {

        /** 테스트와 내부 조립 지점에서 AI 서버 응답 계약을 명시적으로 만든다. */
        public static Response of(String workContent) {
            return new Response(workContent);
        }
    }
}
