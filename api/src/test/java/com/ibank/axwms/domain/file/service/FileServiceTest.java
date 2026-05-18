package com.ibank.axwms.domain.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ibank.axwms.domain.file.FileType;
import com.ibank.axwms.domain.file.dto.GetFilesApiDto;
import com.ibank.axwms.domain.file.entity.File;
import com.ibank.axwms.domain.file.event.WorklogFileAiSummaryRequestedEvent;
import com.ibank.axwms.domain.file.external.AiFileSummaryProperties;
import com.ibank.axwms.domain.file.external.FilePathGenerator;
import com.ibank.axwms.domain.file.external.ObjectStoragePort;
import com.ibank.axwms.domain.file.repository.FileRepository;
import com.ibank.axwms.domain.file.repository.jooq.projection.FileSummaryProjection;
import com.ibank.axwms.domain.file.repository.jooq.query.FilePageQuery;
import com.ibank.axwms.domain.worklog.config.WorklogFileProperties;
import com.ibank.axwms.global.enums.AiProcessingStatus;
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    private static final Long USER_ID = 101L;

    @Mock
    private ObjectStoragePort objectStoragePort;

    @Mock
    private FileRepository fileRepository;

    @Mock
    private FilePathGenerator filePathGenerator;

    @Mock
    private AiFileSummaryProperties aiFileSummaryProperties;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private WorklogFileProperties worklogFileProperties;

    @InjectMocks
    private FileService fileService;

    @Test
    @DisplayName("업무일지 첨부 파일 수가 정책 상한을 넘으면 검증 예외를 던진다")
    void 업무일지_첨부_파일_수가_정책_상한을_넘으면_검증_예외를_던진다() {
        given(worklogFileProperties.maxCount()).willReturn(2);

        assertThatThrownBy(() -> fileService.uploadWorklogFiles(1L, USER_ID, sampleFiles(3, 1024)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.COMMON_VALIDATION_ERROR);
    }

    @Test
    @DisplayName("업무일지 첨부 파일 하나가 개별 용량 상한을 넘으면 업로드를 차단한다")
    void 업무일지_첨부_파일_하나가_개별_용량_상한을_넘으면_업로드를_차단한다() {
        given(worklogFileProperties.maxCount()).willReturn(10);
        given(worklogFileProperties.maxFileSizeBytes()).willReturn(1024L);

        assertThatThrownBy(() -> fileService.uploadWorklogFiles(1L, USER_ID, sampleFiles(1, 2048)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.COMMON_FILE_TOO_LARGE);
    }

    @Test
    @DisplayName("업무일지 첨부 파일 총합이 정책 상한을 넘으면 업로드를 차단한다")
    void 업무일지_첨부_파일_총합이_정책_상한을_넘으면_업로드를_차단한다() {
        given(worklogFileProperties.maxCount()).willReturn(10);
        given(worklogFileProperties.maxFileSizeBytes()).willReturn(10L * 1024 * 1024);
        given(worklogFileProperties.maxTotalSizeBytes()).willReturn(1500L);

        assertThatThrownBy(() -> fileService.uploadWorklogFiles(1L, USER_ID, sampleFiles(2, 800)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.COMMON_FILE_TOO_LARGE);
    }

    @Test
    @DisplayName("업무일지 첨부 파일이 정책 범위 안이면 업로드와 메타데이터 저장을 진행한다")
    void 업무일지_첨부_파일이_정책_범위_안이면_업로드와_메타데이터_저장을_진행한다() {
        List<MultipartFile> files = sampleFiles(2, 1024);
        given(worklogFileProperties.maxCount()).willReturn(10);
        given(worklogFileProperties.maxFileSizeBytes()).willReturn(10L * 1024 * 1024);
        given(worklogFileProperties.maxTotalSizeBytes()).willReturn(100L * 1024 * 1024);
        given(filePathGenerator.generate(eq(501L), any())).willReturn("worklog/2026/05/18/file-1.txt", "worklog/2026/05/18/file-2.txt");
        given(fileRepository.save(any(File.class))).willAnswer(invocation -> {
            File file = invocation.getArgument(0);
            ReflectionTestUtils.setField(file, "id", 1L);
            return file;
        });

        var result = fileService.uploadWorklogFiles(501L, USER_ID, files);

        assertThat(result).hasSize(2);
        verify(objectStoragePort).upload(eq(files.get(0)), eq("worklog/2026/05/18/file-1.txt"));
        verify(objectStoragePort).upload(eq(files.get(1)), eq("worklog/2026/05/18/file-2.txt"));
        verify(fileRepository, org.mockito.Mockito.times(2)).save(any(File.class));
    }

    @Test
    @DisplayName("기간 필터가 있으면 해당 일수만큼 등록 시각 하한을 변환한다")
    void 기간_필터가_있으면_해당_일수만큼_등록_시각_하한을_변환한다() {
        given(fileRepository.findFilePage(eq(USER_ID), any(FilePageQuery.class)))
                .willReturn(new PageImpl<FileSummaryProjection>(List.of(), PageRequest.of(0, 20), 0));
        given(fileRepository.findFileWorklogsByIds(eq(USER_ID), any())).willReturn(List.of());
        LocalDateTime before = LocalDateTime.now().minusDays(15);

        fileService.getFiles(principal(), new GetFilesApiDto.Request(null, null, null, 15));

        LocalDateTime after = LocalDateTime.now().minusDays(15);
        ArgumentCaptor<FilePageQuery> queryCaptor = ArgumentCaptor.forClass(FilePageQuery.class);
        verify(fileRepository).findFilePage(eq(USER_ID), queryCaptor.capture());
        assertThat(queryCaptor.getValue().createdFrom()).isBetween(before, after);
    }

    @Test
    @DisplayName("파일 목록 응답의 storedPath 는 공개 접근 URL 로 변환한다")
    void 파일_목록_응답의_storedPath_는_공개_접근_URL_로_변환한다() {
        String storageKey = "worklog/2026/05/12/report.pdf";
        String publicUrl = "https://cdn.example.com/worklog/2026/05/12/report.pdf";
        FileSummaryProjection projection = new FileSummaryProjection(
                1L,
                501L,
                "report.pdf",
                storageKey,
                "pdf",
                245678L,
                "요약",
                com.ibank.axwms.global.enums.AiProcessingStatus.COMPLETED,
                LocalDateTime.now()
        );
        given(fileRepository.findFilePage(eq(USER_ID), any(FilePageQuery.class)))
                .willReturn(new PageImpl<>(List.of(projection), PageRequest.of(0, 20), 1));
        given(fileRepository.findFileWorklogsByIds(eq(USER_ID), any())).willReturn(List.of());
        given(objectStoragePort.toPublicUrl(storageKey)).willReturn(publicUrl);

        var result = fileService.getFiles(principal(), new GetFilesApiDto.Request(null, null, null, null));

        assertThat(result.items())
                .singleElement()
                .extracting(GetFilesApiDto.Response.Item::storedPath)
                .isEqualTo(publicUrl);
        verify(objectStoragePort).toPublicUrl(storageKey);
    }

    @Test
    @DisplayName("파일 형식 목록을 enum 선언 순서대로 반환한다")
    void 파일_형식_목록을_enum_선언_순서대로_반환한다() {
        var result = fileService.getFileTypes();

        assertThat(result)
                .extracting(item -> item.fileType())
                .containsExactly(FileType.values());
        assertThat(result)
                .extracting(item -> item.extension())
                .containsExactly("docx", "hwp", "md", "pdf", "png", "pptx", "xlsx");
    }


    @Test
    @DisplayName("기존 업무 파일 업로드는 파일 요약 AI 이벤트를 발행한다")
    void uploadWorklogFiles_publishes_file_summary_event() {
        // given
        MultipartFile file = new MockMultipartFile("files", "report.txt", "text/plain", "content".getBytes());
        givenWorklogFilePolicyAllowsUploads();
        given(filePathGenerator.generate(501L, "report.txt")).willReturn("worklog/501/report.txt");
        given(aiFileSummaryProperties.enabled()).willReturn(true);
        given(fileRepository.save(any(File.class))).willAnswer(invocation -> {
            File saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 9001L);
            return saved;
        });

        // when
        List<FileService.UploadedFile> uploadedFiles = fileService.uploadWorklogFiles(501L, USER_ID, List.of(file));

        // then
        assertThat(uploadedFiles)
                .singleElement()
                .extracting(
                        FileService.UploadedFile::fileId,
                        FileService.UploadedFile::storageKey,
                        FileService.UploadedFile::originalName,
                        FileService.UploadedFile::fileExtension,
                        FileService.UploadedFile::sizeBytes
                )
                .containsExactly(9001L, "worklog/501/report.txt", "report.txt", "txt", 7L);
        verify(eventPublisher).publishEvent(any(WorklogFileAiSummaryRequestedEvent.class));
        ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
        verify(fileRepository).save(fileCaptor.capture());
        assertThat(fileCaptor.getValue().getAiProcessingStatus()).isEqualTo(AiProcessingStatus.PROCESSING);
    }


    @Test
    @DisplayName("파일 요약이 비활성화되어 있으면 기존 업로드 경로도 요약 이벤트 없이 PENDING 상태를 유지한다")
    void uploadWorklogFiles_keeps_pending_when_file_summary_disabled() {
        // given
        MultipartFile file = new MockMultipartFile("files", "report.txt", "text/plain", "content".getBytes());
        givenWorklogFilePolicyAllowsUploads();
        given(filePathGenerator.generate(501L, "report.txt")).willReturn("worklog/501/report.txt");
        given(aiFileSummaryProperties.enabled()).willReturn(false);
        given(fileRepository.save(any(File.class))).willAnswer(invocation -> {
            File saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 9001L);
            return saved;
        });

        // when
        fileService.uploadWorklogFiles(501L, USER_ID, List.of(file));

        // then
        verify(eventPublisher, never()).publishEvent(any(WorklogFileAiSummaryRequestedEvent.class));
        ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
        verify(fileRepository).save(fileCaptor.capture());
        assertThat(fileCaptor.getValue().getAiProcessingStatus()).isEqualTo(AiProcessingStatus.PENDING);
    }

    @Test
    @DisplayName("업무 생성 전용 파일 업로드는 파일 요약 AI 이벤트를 발행하지 않는다")
    void uploadWorklogFilesWithoutAiSummaryRequest_does_not_publish_file_summary_event() {
        // given
        MultipartFile file = new MockMultipartFile("files", "report.txt", "text/plain", "content".getBytes());
        givenWorklogFilePolicyAllowsUploads();
        given(filePathGenerator.generate(501L, "report.txt")).willReturn("worklog/501/report.txt");
        given(fileRepository.save(any(File.class))).willAnswer(invocation -> {
            File saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 9001L);
            return saved;
        });

        // when
        List<FileService.UploadedFile> uploadedFiles = fileService.uploadWorklogFilesWithoutAiSummaryRequest(501L, USER_ID, List.of(file));

        // then
        assertThat(uploadedFiles).singleElement()
                .extracting(FileService.UploadedFile::fileId)
                .isEqualTo(9001L);
        verify(eventPublisher, never()).publishEvent(any(WorklogFileAiSummaryRequestedEvent.class));
        ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
        verify(fileRepository).save(fileCaptor.capture());
        assertThat(fileCaptor.getValue().getAiProcessingStatus()).isEqualTo(AiProcessingStatus.PENDING);
    }

    /** 파일 요약 경로 테스트가 첨부 정책 차단이 아니라 AI 이벤트 분기만 검증하도록 허용 정책을 고정한다. */
    private void givenWorklogFilePolicyAllowsUploads() {
        given(worklogFileProperties.maxCount()).willReturn(10);
        given(worklogFileProperties.maxFileSizeBytes()).willReturn(10L * 1024 * 1024);
        given(worklogFileProperties.maxTotalSizeBytes()).willReturn(100L * 1024 * 1024);
    }

    /** protected Controller 경유 호출과 같은 최소 인증 문맥만 service 에 전달한다. */
    private CustomUserPrincipal principal() {
        return new CustomUserPrincipal(USER_ID, "file-service@test.com", "MEMBER");
    }

    /**
     * 첨부 정책 검증 테스트에서 요청 파일 수와 파일당 바이트 수만 조절하면 되도록 고정 패턴 fixture 를 만든다.
     */
    private List<MultipartFile> sampleFiles(int count, int bytesPerFile) {
        byte[] payload = new byte[bytesPerFile];
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(index -> new MockMultipartFile(
                        "files",
                        "report-%d.txt".formatted(index),
                        "text/plain",
                        payload
                ))
                .map(MultipartFile.class::cast)
                .toList();
    }
}
