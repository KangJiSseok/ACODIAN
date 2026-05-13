package com.ibank.axwms.domain.file.service;

import com.ibank.axwms.domain.file.dto.FileWorklogItem;
import com.ibank.axwms.domain.file.dto.GetFileTypesApiDto;
import com.ibank.axwms.domain.file.dto.GetFilesApiDto;
import com.ibank.axwms.domain.file.entity.File;
import com.ibank.axwms.domain.file.event.WorklogFileAiSummaryRequestedEvent;
import com.ibank.axwms.domain.file.event.WorklogFileUploadedEvent;
import com.ibank.axwms.domain.file.external.FilePathGenerator;
import com.ibank.axwms.domain.file.external.ObjectStoragePort;
import com.ibank.axwms.domain.file.repository.FileRepository;
import com.ibank.axwms.domain.file.repository.jooq.projection.FileSummaryProjection;
import com.ibank.axwms.domain.file.repository.jooq.query.FilePageQuery;
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import com.ibank.axwms.global.response.PageResponse;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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
     * DB save 가 끝난 뒤에는 WorklogFileAiSummaryRequestedEvent 를 발행해, 트랜잭션이 커밋되면
     * AFTER_COMMIT listener 가 AI 서버에 파일 단위 요약 요청을 fire-and-forget 으로 보낸다.
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
            // 같은 트랜잭션 안에서 PROCESSING 으로 전이 → 커밋 시점에 이미 처리중 상태.
            // 트랜잭션 롤백 시 PROCESSING 전이도 함께 무효화돼 트리거 발화와 상태가 항상 일치한다.
            saved.startAiSummaryProcessing();
            eventPublisher.publishEvent(new WorklogFileAiSummaryRequestedEvent(
                    saved.getId(),
                    worklogId,
                    key,
                    saved.getOriginalName(),
                    saved.getFileExtension()
            ));
            results.add(new UploadedFile(saved.getId(), key, file.getOriginalFilename(), file.getSize()));
        }
        return results;
    }

    /**
     * 주어진 worklog 의 첨부 파일 중 fileIds 에 해당하는 파일들을 일괄 소프트 삭제한다.
     * 보안 가드: 요청된 fileId 가 worklog 소속이 아니거나 이미 삭제된 경우 size 불일치로 감지해 단일 ErrorCode 로 거부한다.
     * 실제 S3 객체 삭제는 별도 cleanup 책임이며, 본 메서드는 DB isDeleted 플래그만 토글한다.
     *
     * @param worklogId 삭제 대상 파일들이 속한 worklog ID
     * @param fileIds 삭제할 file ID 들. null/빈 컬렉션은 no-op.
     * @throws BusinessException WORKLOG_FILE_NOT_FOUND 일부 fileId 가 해당 worklog 에 속하지 않거나 이미 삭제됨
     */
    @Transactional
    public void softDeleteWorklogFiles(Long worklogId, Collection<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }
        Set<Long> uniqueIds = new LinkedHashSet<>(fileIds);
        List<File> files = fileRepository.findAllByIdInAndWorklogIdAndIsDeletedFalse(uniqueIds, worklogId);
        if (files.size() != uniqueIds.size()) {
            throw new BusinessException(ErrorCode.WORKLOG_FILE_NOT_FOUND);
        }
        files.forEach(File::markDeleted);
    }

    /**
     * 업무 상세처럼 file 모듈 밖에서 첨부 파일 key 를 응답 URL 로 노출해야 할 때 동일한 저장소 공개 URL 규칙을 재사용한다.
     */
    public String toPublicUrl(String storageKey) {
        return objectStoragePort.toPublicUrl(storageKey);
    }

    /**
     * 사용자가 접근 가능한 (admin grant ∪ ACTIVE membership 의 미삭제 team) worklog 들의 첨부 파일을
     * 최신 등록순으로 페이지 조회한다. 각 행에 해당 worklog 의 요약(제목/팀/작성자/마감일/업무시간/AI/선행업무수)도 박는다.
     * 파일 형식과 등록 기간 필터는 값이 있을 때만 AND 조건으로 적용한다.
     *
     * 성능:
     *  - file page 1 쿼리 + worklog batch 1 쿼리 (= 총 2 쿼리). 가시성 필터가 file page 에 이미 들어가 있어
     *    batch 결과는 정상 케이스에서 입력 ID 와 1:1 매핑된다.
     */
    public PageResponse<GetFilesApiDto.Response.Item> getFiles(CustomUserPrincipal principal, GetFilesApiDto.Request request) {
        FilePageQuery query = FilePageQuery.from(request);
        Page<FileSummaryProjection> page = fileRepository.findFilePage(principal.userId(), query);

        Set<Long> uniqueWorklogIds = page.getContent().stream()
                .map(FileSummaryProjection::worklogId)
                .collect(Collectors.toSet());

        Map<Long, FileWorklogItem> worklogByWorklogId = fileRepository
                .findFileWorklogsByIds(principal.userId(), uniqueWorklogIds)
                .stream()
                .map(FileWorklogItem::from)
                .collect(Collectors.toMap(FileWorklogItem::worklogId, Function.identity()));

        Page<GetFilesApiDto.Response.Item> responsePage = page.map(projection ->
                GetFilesApiDto.Response.Item.from(
                        projection,
                        worklogByWorklogId.get(projection.worklogId()),
                        objectStoragePort.toPublicUrl(projection.storedPath())
                ));
        return GetFilesApiDto.Response.fromPage(responsePage);
    }

    /**
     * 파일 목록 필터에서 사용할 수 있는 FileType 선택지를 enum 선언 순서대로 제공한다.
     */
    public List<GetFileTypesApiDto.Response> getFileTypes() {
        return GetFileTypesApiDto.Response.all();
    }
}
