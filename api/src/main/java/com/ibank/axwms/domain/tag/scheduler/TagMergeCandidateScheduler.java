package com.ibank.axwms.domain.tag.scheduler;

import com.ibank.axwms.domain.tag.dto.TagMergeCandidateApiDto;
import com.ibank.axwms.domain.tag.service.TagMergeCandidateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 운영자가 매일 최신 태그 풀 기준의 병합 후보를 검토할 수 있도록 AI 후보 생성을 예약 실행한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TagMergeCandidateScheduler {

    private final TagMergeCandidateService tagMergeCandidateService;

    /**
     * 매일 자정에 AI 병합 후보를 생성하고 저장한다.
     */
    @Scheduled(cron = "${tag.merge-candidate.generation-cron:0 0 0 * * *}")
    public void generateDailyMergeCandidates() {
        try {
            TagMergeCandidateApiDto.Response response =
                    tagMergeCandidateService.generateCandidates(
                            new TagMergeCandidateApiDto.GenerateRequest(null, null, null, null)
                    );
            log.info("태그 병합 후보 생성 완료 count={}", response.items().size());
        } catch (RuntimeException e) {
            log.error("태그 병합 후보 생성 실패", e);
            throw e;
        }
    }
}
