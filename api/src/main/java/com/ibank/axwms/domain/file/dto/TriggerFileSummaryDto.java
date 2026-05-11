package com.ibank.axwms.domain.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 서버 {@code POST /summarize/files} 트리거 페이로드.
 * 콜백 식별자 회수(fileId)와 객체 다운로드 키(storageKey), 확장자/원본명을 함께 전달한다.
 */
public final class TriggerFileSummaryDto {

    @Schema(description = "AI 파일 요약 트리거 요청")
    public record Request(

            @Schema(description = "요약 대상 파일 ID")
            Long fileId,

            @Schema(description = "파일이 속한 업무일지 ID")
            Long worklogId,

            @Schema(description = "객체 스토리지 키 (다운로드용)")
            String storageKey,

            @Schema(description = "원본 파일명")
            String originalName,

            @Schema(description = "파일 확장자 (소문자, dot 제외)")
            String fileExtension
    ) {
    }
}
