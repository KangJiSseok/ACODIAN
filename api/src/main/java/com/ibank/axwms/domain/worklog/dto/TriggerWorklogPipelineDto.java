package com.ibank.axwms.domain.worklog.dto;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TriggerWorklogPipelineDto {

    /**
     * AI 서버가 콜백으로 다시 식별할 수 있도록 업무 식별자와 본문 생성에 필요한 최소 문맥만 전달한다.
     */
    public record Request(
            Long worklogId,
            String requestContent,
            String workContent,
            Long authorId,
            Long teamId,
            Long departmentId
    ) {
    }
}
