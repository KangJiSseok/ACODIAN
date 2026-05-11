package com.ibank.axwms.domain.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.ibank.axwms.domain.file.dto.UpdateFileAiApiDto;
import com.ibank.axwms.domain.file.entity.File;
import com.ibank.axwms.domain.file.repository.FileRepository;
import com.ibank.axwms.global.enums.AiProcessingStatus;
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class InternalFileAiCallbackServiceTest {

    private static final Long FILE_ID = 101L;
    private static final Long WORKLOG_ID = 501L;
    private static final String AI_SUMMARY = "AI가 첨부 파일을 요약한 결과";

    @Mock
    private FileRepository fileRepository;

    @InjectMocks
    private InternalFileAiCallbackService internalFileAiCallbackService;

    @Test
    @DisplayName("AI 상태가 COMPLETED 이면 파일에 요약문과 완료 상태를 반영한다")
    void AI_상태가_completed_이면_파일에_요약문과_완료_상태를_반영한다() {
        File file = createFile();
        UpdateFileAiApiDto.Request request = new UpdateFileAiApiDto.Request(
                AI_SUMMARY,
                AiProcessingStatus.COMPLETED
        );
        given(fileRepository.findById(FILE_ID)).willReturn(Optional.of(file));

        internalFileAiCallbackService.updateAiResult(FILE_ID, request);

        assertThat(file.getAiSummary()).isEqualTo(AI_SUMMARY);
        assertThat(file.getAiProcessingStatus()).isEqualTo(AiProcessingStatus.COMPLETED);
    }

    @Test
    @DisplayName("AI 상태가 FAILED 이면 파일에 실패 상태만 반영한다")
    void AI_상태가_failed_이면_파일에_실패_상태만_반영한다() {
        File file = createFile();
        UpdateFileAiApiDto.Request request = new UpdateFileAiApiDto.Request(
                "",
                AiProcessingStatus.FAILED
        );
        given(fileRepository.findById(FILE_ID)).willReturn(Optional.of(file));

        internalFileAiCallbackService.updateAiResult(FILE_ID, request);

        assertThat(file.getAiSummary()).isNull();
        assertThat(file.getAiProcessingStatus()).isEqualTo(AiProcessingStatus.FAILED);
    }

    @Test
    @DisplayName("파일이 없으면 FILE_NOT_FOUND 를 던진다")
    void 파일이_없으면_file_not_found_를_던진다() {
        UpdateFileAiApiDto.Request request = new UpdateFileAiApiDto.Request(
                AI_SUMMARY,
                AiProcessingStatus.COMPLETED
        );
        given(fileRepository.findById(FILE_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> internalFileAiCallbackService.updateAiResult(FILE_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.FILE_NOT_FOUND);

        verify(fileRepository).findById(FILE_ID);
        verifyNoMoreInteractions(fileRepository);
    }

    /**
     * COMPLETED 콜백이 두 번 들어와도 가장 최근 요약으로 덮어쓰는지 검증한다.
     * (트리거 fire-and-forget + AI 재시도 시 동일 fileId 로 콜백이 중복 도달할 수 있음)
     */
    @Test
    @DisplayName("COMPLETED 콜백이 두 번 들어오면 마지막 요약으로 덮어쓴다")
    void COMPLETED_콜백이_두_번_들어오면_마지막_요약으로_덮어쓴다() {
        File file = createFile();
        given(fileRepository.findById(FILE_ID)).willReturn(Optional.of(file));

        internalFileAiCallbackService.updateAiResult(FILE_ID, new UpdateFileAiApiDto.Request(
                "첫번째 요약",
                AiProcessingStatus.COMPLETED
        ));
        internalFileAiCallbackService.updateAiResult(FILE_ID, new UpdateFileAiApiDto.Request(
                "두번째 요약",
                AiProcessingStatus.COMPLETED
        ));

        assertThat(file.getAiSummary()).isEqualTo("두번째 요약");
        assertThat(file.getAiProcessingStatus()).isEqualTo(AiProcessingStatus.COMPLETED);
    }

    private File createFile() {
        MockMultipartFile multipart = new MockMultipartFile(
                "file",
                "report.pdf",
                "application/pdf",
                "dummy".getBytes()
        );
        File file = File.create(WORKLOG_ID, 11L, "uploads/2026/05/abc.pdf", multipart);
        ReflectionTestUtils.setField(file, "id", FILE_ID);
        return file;
    }
}
