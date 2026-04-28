package com.ibank.axwms.domain.file.service;

import com.ibank.axwms.domain.file.entity.File;
import com.ibank.axwms.domain.file.event.WorklogFileUploadedEvent;
import com.ibank.axwms.domain.file.external.FilePathGenerator;
import com.ibank.axwms.domain.file.external.ObjectStoragePort;
import com.ibank.axwms.domain.file.repository.FileRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
public class FileService {

    private final ObjectStoragePort objectStoragePort;
    private final FileRepository fileRepository;
    private final FilePathGenerator filePathGenerator;
    private final ApplicationEventPublisher eventPublisher;

    public FileService(
            @Qualifier("s3ObjectStorageAdapter") ObjectStoragePort objectStoragePort,
            FileRepository fileRepository,
            FilePathGenerator filePathGenerator,
            ApplicationEventPublisher eventPublisher
    ) {
        this.objectStoragePort = objectStoragePort;
        this.fileRepository = fileRepository;
        this.filePathGenerator = filePathGenerator;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 서비스 간 내부 계약용 결과 타입. Controller 경계로 드러나지 않으며
     * 호출측이 응답 DTO 를 조립할 때 식별자·메타데이터 재료로 사용한다.
     */
    public record UploadedFile(
            Long fileId,
            String storageKey,
            String originalName,
            long sizeBytes
    ) {}

    /**
     * 업무에 첨부될 파일들을 스토리지에 업로드하고 메타데이터를 DB 에 저장한다.
     * 파일 목록이 비어 있으면 빈 리스트를 반환해 호출측에서 null/분기 처리를 강요하지 않는다.
     * 업로드 직후 WorklogFileUploadedEvent 를 발행해, 활성 트랜잭션이 롤백되면
     * AFTER_ROLLBACK listener 가 객체 스토리지에서 해당 key 를 보상 삭제한다.
     *
     * @param worklogId  첨부 대상 업무 ID
     * @param uploaderId 업로드 수행자 사용자 ID
     * @param files      요청으로 수신한 MultipartFile 목록. null 또는 빈 리스트 허용.
     * @return 저장된 파일들의 내부 표현 목록
     */
    @Transactional
    public List<UploadedFile> uploadWorklogFiles(Long worklogId, Long uploaderId, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }
        List<MultipartFile> present = files.stream()
                .filter(file -> !file.isEmpty())
                .toList();
        if (present.isEmpty()) {
            return List.of();
        }

        List<UploadedFile> results = new ArrayList<>(present.size());
        for (MultipartFile file : present) {
            String key = filePathGenerator.generate(worklogId, file.getOriginalFilename());
            objectStoragePort.upload(file, key);
            eventPublisher.publishEvent(new WorklogFileUploadedEvent(key));

            File saved = fileRepository.save(File.create(worklogId, uploaderId, key, file));
            results.add(new UploadedFile(saved.getId(), key, file.getOriginalFilename(), file.getSize()));
        }
        return results;
    }
}
