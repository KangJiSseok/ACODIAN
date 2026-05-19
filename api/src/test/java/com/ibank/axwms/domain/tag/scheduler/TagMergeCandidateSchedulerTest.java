package com.ibank.axwms.domain.tag.scheduler;

import com.ibank.axwms.domain.tag.dto.TagMergeCandidateApiDto;
import com.ibank.axwms.domain.tag.service.TagMergeCandidateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TagMergeCandidateSchedulerTest {

    @Test
    @DisplayName("자정 스케줄은 기본 설정으로 태그 병합 후보 생성을 호출한다")
    void 자정_스케줄은_기본_설정으로_태그_병합_후보_생성을_호출한다() {
        TagMergeCandidateService tagMergeCandidateService = mock(TagMergeCandidateService.class);
        TagMergeCandidateApiDto.GenerateRequest request =
                new TagMergeCandidateApiDto.GenerateRequest(null, null, null, null);
        given(tagMergeCandidateService.generateCandidates(request))
                .willReturn(new TagMergeCandidateApiDto.Response(List.of()));
        TagMergeCandidateScheduler scheduler = new TagMergeCandidateScheduler(tagMergeCandidateService);

        scheduler.generateDailyMergeCandidates();

        verify(tagMergeCandidateService).generateCandidates(eq(request));
    }
}
