package com.ibank.axwms.domain.file.service;

import com.ibank.axwms.domain.file.dto.UpdateFileAiApiDto;
import com.ibank.axwms.domain.file.entity.File;
import com.ibank.axwms.domain.file.repository.FileRepository;
import com.ibank.axwms.global.enums.AiProcessingStatus;
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InternalFileAiCallbackService {

    private final FileRepository fileRepository;

    /**
     * AI 요약 처리 결과를 파일 엔티티에 반영하고 성공/실패 상태를 갱신한다.
     * 재시도 콜백은 가장 최근 값으로 덮어쓰는 정책을 따른다 ({@link File#changeAiSummary} 참조).
     */
    @Transactional
    public void updateAiResult(Long fileId, UpdateFileAiApiDto.Request request) {

        File file = getFileOrThrow(fileId);

        if (request.aiProcessingStatus() == AiProcessingStatus.COMPLETED) {
            file.changeAiSummary(request.aiSummary());
            file.completeAiSummaryProcessing();
        } else if (request.aiProcessingStatus() == AiProcessingStatus.FAILED) {
            file.failAiSummaryProcessing();
        }

    }

    /**
     * 잘못된 fileId 로 AI 결과가 오염되지 않도록 대상 존재를 먼저 확정한다.
     */
    private File getFileOrThrow(Long fileId) {
        return fileRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));
    }
}
