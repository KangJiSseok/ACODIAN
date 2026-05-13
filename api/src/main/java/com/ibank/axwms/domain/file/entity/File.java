package com.ibank.axwms.domain.file.entity;

import com.ibank.axwms.global.enums.AiProcessingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_file")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class File {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_id")
    private Long id;

    @Column(name = "worklog_id", nullable = false)
    private Long worklogId;

    @Column(name = "uploaded_by", nullable = false)
    private Long uploadedBy;

    @Column(name = "original_name", nullable = false, length = 500)
    private String originalName;

    @Column(name = "stored_path", nullable = false, length = 1000)
    private String storedPath;

    @Column(name = "file_extension", nullable = false, length = 20)
    private String fileExtension;

    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes;

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_processing_status", nullable = false, length = 20)
    private AiProcessingStatus aiProcessingStatus;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private static final String DEFAULT_EXTENSION = "";

    /**
     * 업로드가 완료된 파일의 메타데이터로 초기 엔티티를 만든다.
     * AI 처리 상태는 PENDING 으로 시작하며, 호출측 서비스가 같은 트랜잭션 안에서
     * AI 트리거 이벤트 발행 직전에 {@link #startAiSummaryProcessing()} 로 전이시킨다.
     * is_deleted 는 false.
     *
     * @param worklogId   소속 업무 ID
     * @param uploaderId  업로드 수행자 사용자 ID
     * @param storedPath  스토리지 내부 식별자(key)
     * @param file        원본 MultipartFile (원본명/크기 참조용)
     * @return 저장 전 File 엔티티
     */
    public static File create(Long worklogId, Long uploaderId, String storedPath, MultipartFile file) {
        File entity = new File();
        entity.worklogId = worklogId;
        entity.uploadedBy = uploaderId;
        entity.originalName = file.getOriginalFilename();
        entity.storedPath = storedPath;
        entity.fileExtension = extensionOf(file.getOriginalFilename());
        entity.fileSizeBytes = file.getSize();
        entity.aiProcessingStatus = AiProcessingStatus.PENDING;
        entity.isDeleted = Boolean.FALSE;
        return entity;
    }

    /**
     * 첨부 파일을 소프트 삭제 상태로 표시한다.
     * S3 객체 자체의 삭제는 별도 cleanup 으로 처리하고, 본 엔티티는 isDeleted 플래그만 토글한다.
     */
    public void markDeleted() {
        this.isDeleted = Boolean.TRUE;
    }

    /**
     * AI 요약 트리거를 곧 발사하기 직전에 호출되어 처리 상태를 PROCESSING 으로 전이한다.
     * 같은 트랜잭션에서 발행되는 {@code WorklogFileAiSummaryRequestedEvent} 와 짝을 이루므로,
     * 트랜잭션이 롤백되면 상태 전이와 트리거 발화가 함께 무효화된다.
     */
    public void startAiSummaryProcessing() {
        this.aiProcessingStatus = AiProcessingStatus.PROCESSING;
    }

    /**
     * AI 가 생성한 요약을 파일에 반영한다. 콜백 재시도 시 가장 최근 값으로 덮어쓴다.
     */
    public void changeAiSummary(String summary) {
        this.aiSummary = summary;
    }

    /**
     * AI 요약 파이프라인이 정상 종료되었음을 표시한다.
     */
    public void completeAiSummaryProcessing() {
        this.aiProcessingStatus = AiProcessingStatus.COMPLETED;
    }

    /**
     * AI 요약 파이프라인이 실패로 종료되었음을 표시한다. aiSummary 는 콜백 페이로드 그대로 유지한다.
     */
    public void failAiSummaryProcessing() {
        this.aiProcessingStatus = AiProcessingStatus.FAILED;
    }

    private static String extensionOf(String originalName) {
        if (originalName == null) {
            return DEFAULT_EXTENSION;
        }
        int dot = originalName.lastIndexOf('.');
        if (dot <= 0 || dot == originalName.length() - 1) {
            return DEFAULT_EXTENSION;
        }
        return originalName.substring(dot + 1).toLowerCase();
    }
}
