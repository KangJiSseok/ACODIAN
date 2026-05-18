package com.ibank.axwms.domain.worklog.dto;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TriggerWorklogLightIndexDto {

    /**
     * Light v3 index endpoint 가 여러 업무 ID 일괄 색인 계약만 제공하므로 단건 생성도 배열로 전달한다.
     */
    public record Request(
            List<Long> worklogIds
    ) {

        /** 단건 업무 생성 흐름에서 나온 ID 를 AI index 요청의 일괄 계약으로 감싼다. */
        public static Request of(Long worklogId) {
            return new Request(List.of(worklogId));
        }
    }
}
