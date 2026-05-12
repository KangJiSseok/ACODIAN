package com.ibank.axwms.domain.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.ibank.axwms.domain.file.FileType;
import com.ibank.axwms.domain.file.dto.GetFilesApiDto;
import com.ibank.axwms.domain.file.external.FilePathGenerator;
import com.ibank.axwms.domain.file.external.ObjectStoragePort;
import com.ibank.axwms.domain.file.repository.FileRepository;
import com.ibank.axwms.domain.file.repository.jooq.projection.FileSummaryProjection;
import com.ibank.axwms.domain.file.repository.jooq.query.FilePageQuery;
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
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private FileService fileService;

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

    /** protected Controller 경유 호출과 같은 최소 인증 문맥만 service 에 전달한다. */
    private CustomUserPrincipal principal() {
        return new CustomUserPrincipal(USER_ID, "file-service@test.com", "MEMBER");
    }
}
