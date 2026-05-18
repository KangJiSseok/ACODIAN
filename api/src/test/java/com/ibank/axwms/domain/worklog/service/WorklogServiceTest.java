package com.ibank.axwms.domain.worklog.service;

import com.ibank.axwms.domain.file.repository.FileRepository;
import com.ibank.axwms.domain.file.service.FileService;
import com.ibank.axwms.domain.organization.team.TeamStatus;
import com.ibank.axwms.domain.organization.team.entity.Team;
import com.ibank.axwms.domain.organization.team.service.TeamService;
import com.ibank.axwms.domain.tag.service.TagService;
import com.ibank.axwms.domain.worklog.WorklogImportance;
import com.ibank.axwms.domain.worklog.WorklogStatus;
import com.ibank.axwms.domain.worklog.dto.CreateWorklogApiDto;
import com.ibank.axwms.domain.worklog.dto.GetWorklogDetailApiDto;
import com.ibank.axwms.domain.worklog.dto.GetWorklogsApiDto;
import com.ibank.axwms.domain.worklog.dto.InternalWorklogPolishApiDto;
import com.ibank.axwms.domain.worklog.dto.InternalWorklogTitleRecommendationApiDto;
import com.ibank.axwms.domain.worklog.dto.PolishWorklogApiDto;
import com.ibank.axwms.domain.worklog.dto.RecommendWorklogTitleApiDto;
import com.ibank.axwms.domain.worklog.dto.UpdateWorklogApiDto;
import com.ibank.axwms.domain.worklog.dto.UpdateWorklogStatusApiDto;
import com.ibank.axwms.domain.worklog.entity.Worklog;
import com.ibank.axwms.domain.worklog.entity.WorklogTag;
import com.ibank.axwms.domain.worklog.event.WorklogAiPipelineRequestedEvent;
import com.ibank.axwms.domain.worklog.event.WorklogAiPostProcessRequestedEvent;
import com.ibank.axwms.domain.worklog.event.WorklogCompletedEvent;
import com.ibank.axwms.domain.worklog.event.WorklogLightIndexRequestedEvent;
import com.ibank.axwms.domain.worklog.external.WorklogPolishClient;
import com.ibank.axwms.domain.worklog.policy.WorklogStatusPolicy;
import com.ibank.axwms.domain.worklog.repository.WorklogDependencyRepository;
import com.ibank.axwms.domain.worklog.repository.WorklogRepository;
import com.ibank.axwms.domain.worklog.repository.WorklogStatusHistoryRepository;
import com.ibank.axwms.domain.worklog.repository.WorklogTagRepository;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.WorklogDetailProjection;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.WorklogFileProjection;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.WorklogListProjection;
import com.ibank.axwms.domain.worklog.repository.jooq.query.WorklogPageQuery;
import com.ibank.axwms.global.enums.AiProcessingStatus;
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import com.ibank.axwms.global.response.PageResponse;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class WorklogServiceTest {
    @Mock private WorklogRepository worklogRepository;
    @Mock private WorklogTagRepository worklogTagRepository;
    @Mock private WorklogDependencyRepository worklogDependencyRepository;
    @Mock private WorklogStatusHistoryRepository worklogStatusHistoryRepository;
    @Mock private FileRepository fileRepository;
    @Mock private TeamService teamService;
    @Mock private FileService fileService;
    @Mock private WorklogStatusHistoryService worklogStatusHistoryService;
    @Mock private WorklogDependencyService worklogDependencyService;
    @Mock private WorklogStatusPolicy worklogStatusPolicy;
    @Mock private TagService tagService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private WorklogPolishClient worklogPolishClient;

    @InjectMocks private WorklogService worklogService;

    private static final Long USER_ID = 101L;
    private static final Long TEAM_ID = 21L;
    private static final Long DEPARTMENT_ID = 9L;
    private static final Long WORKLOG_ID = 501L;
    private static final List<Long> TAG_IDS = List.of(1L, 2L);
    private static final LocalDate INSTRUCTION_DATE = LocalDate.of(2026, 4, 22);
    private static final LocalDate DUE_DATE = LocalDate.of(2026, 4, 25);

    @Test
    @DisplayName("작성 보조는 요청 본문을 AI 클라이언트에 전달하고 응답으로 매핑한다")
    void 작성_보조는_요청_본문을_ai_클라이언트에_전달하고_응답으로_매핑한다() {
        PolishWorklogApiDto.Request request = new PolishWorklogApiDto.Request(
                "재고 동기화 지연 원인을 정리해 주세요.",
                "배치 로그를 비교하고 병목 구간을 확인했습니다."
        );
        InternalWorklogPolishApiDto.Request internalRequest = InternalWorklogPolishApiDto.Request.from(request);
        given(worklogPolishClient.polishWorklog(internalRequest)).willReturn(
                InternalWorklogPolishApiDto.Response.of(
                        "배치 로그를 비교하고 병목 구간을 확인했습니다."
                )
        );

        PolishWorklogApiDto.Response response = worklogService.polishWorklog(request);

        assertThat(response).isEqualTo(PolishWorklogApiDto.Response.of(
                "배치 로그를 비교하고 병목 구간을 확인했습니다."
        ));
        verify(worklogPolishClient).polishWorklog(internalRequest);
        verifyNoInteractions(worklogRepository);
    }

    @Test
    @DisplayName("AI 클라이언트 실패는 작성 보조 실패 코드로 드러난다")
    void ai_클라이언트_실패는_작성_보조_실패_코드로_드러난다() {
        PolishWorklogApiDto.Request request = new PolishWorklogApiDto.Request("요청", "수행 내용");
        BusinessException failure = new BusinessException(ErrorCode.WORKLOG_AI_POLISH_FAILED);
        given(worklogPolishClient.polishWorklog(InternalWorklogPolishApiDto.Request.from(request)))
                .willThrow(failure);

        assertThatThrownBy(() -> worklogService.polishWorklog(request))
                .isSameAs(failure)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.WORKLOG_AI_POLISH_FAILED);
    }

    @Test
    @DisplayName("제목 추천은 요청 본문을 AI 클라이언트에 전달하고 후보 제목으로 매핑한다")
    void 제목_추천은_요청_본문을_ai_클라이언트에_전달하고_후보_제목으로_매핑한다() {
        RecommendWorklogTitleApiDto.Request request = new RecommendWorklogTitleApiDto.Request(
                "재고 동기화 지연 원인을 정리해 주세요.",
                "배치 로그를 비교하고 병목 구간을 확인했습니다."
        );
        InternalWorklogTitleRecommendationApiDto.Request internalRequest =
                InternalWorklogTitleRecommendationApiDto.Request.from(request);
        given(worklogPolishClient.recommendWorklogTitles(internalRequest)).willReturn(
                InternalWorklogTitleRecommendationApiDto.Response.of(
                        List.of("배치 로그 병목 구간 확인", "", "재고 동기화 지연 분석", "병목 구간 조치", "초과 후보")
                )
        );

        RecommendWorklogTitleApiDto.Response response = worklogService.recommendWorklogTitles(request);

        assertThat(response).isEqualTo(RecommendWorklogTitleApiDto.Response.of(
                List.of("배치 로그 병목 구간 확인", "재고 동기화 지연 분석", "병목 구간 조치")
        ));
        verify(worklogPolishClient).recommendWorklogTitles(internalRequest);
        verifyNoInteractions(worklogRepository);
    }

    @Test
    @DisplayName("AI 클라이언트 실패는 제목 추천에서도 작성 보조 실패 코드로 드러난다")
    void ai_클라이언트_실패는_제목_추천에서도_작성_보조_실패_코드로_드러난다() {
        RecommendWorklogTitleApiDto.Request request = new RecommendWorklogTitleApiDto.Request("요청", "수행 내용");
        BusinessException failure = new BusinessException(ErrorCode.WORKLOG_AI_POLISH_FAILED);
        given(worklogPolishClient.recommendWorklogTitles(InternalWorklogTitleRecommendationApiDto.Request.from(request)))
                .willThrow(failure);

        assertThatThrownBy(() -> worklogService.recommendWorklogTitles(request))
                .isSameAs(failure)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.WORKLOG_AI_POLISH_FAILED);
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("정상 입력이면 업무를 저장하고 첨부 파일 업로드를 위임하며 생성된 ID를 반환한다.")
    void 정상_입력이면_업무를_저장하고_첨부_파일_업로드를_위임하며_생성된_ID를_반환한다() {
        // given
        CustomUserPrincipal principal = principal();
        CreateWorklogApiDto.Request request = request(INSTRUCTION_DATE, DUE_DATE);
        List<MultipartFile> files = sampleFiles();

        given(teamService.getTeamOrThrow(TEAM_ID)).willReturn(sampleTeam());
        given(teamService.isMember(USER_ID, TEAM_ID)).willReturn(true);
        given(worklogRepository.save(any(Worklog.class))).willAnswer(invocation -> {
            Worklog toSave = invocation.getArgument(0);
            ReflectionTestUtils.setField(toSave, "id", WORKLOG_ID);
            return toSave;
        });
        given(tagService.normalizeExistingTagIds(TAG_IDS)).willReturn(TAG_IDS);
        given(fileService.uploadWorklogFilesWithoutAiSummaryRequest(WORKLOG_ID, USER_ID, files))
                .willReturn(List.of(new FileService.UploadedFile(9001L, "worklog/501/report.txt", "report.txt", "txt", 7L)));

        // when
        CreateWorklogApiDto.Response response = worklogService.createWorklog(principal, request, files);

        // then
        assertThat(response.worklogId()).isEqualTo(WORKLOG_ID);
        ArgumentCaptor<Worklog> worklogCaptor = ArgumentCaptor.forClass(Worklog.class);
        verify(worklogRepository).save(worklogCaptor.capture());
        assertThat(worklogCaptor.getValue().getStatusCode()).isEqualTo(WorklogStatus.IN_PROGRESS);
        assertThat(worklogCaptor.getValue().getActualHours()).isEqualByComparingTo("3.10");
        verify(fileService).uploadWorklogFilesWithoutAiSummaryRequest(eq(WORKLOG_ID), eq(USER_ID), eq(files));
        verify(fileService, never()).uploadWorklogFiles(eq(WORKLOG_ID), eq(USER_ID), eq(files));
        verify(worklogStatusHistoryService).createStatusHistory(
                eq(WORKLOG_ID),
                eq(WorklogStatus.IN_PROGRESS),
                eq(USER_ID)
        );
        ArgumentCaptor<List<WorklogTag>> tagCaptor = ArgumentCaptor.forClass(List.class);
        verify(worklogTagRepository).saveAll(tagCaptor.capture());
        assertThat(tagCaptor.getValue())
                .extracting(WorklogTag::getTagId)
                .containsExactly(1L, 2L);
        verify(tagService).incrementUsageCountByIds(TAG_IDS);
        verify(worklogDependencyService).registerPredecessor(eq(WORKLOG_ID), eq(TEAM_ID), eq(request.predecessorWorklogIds()));
        ArgumentCaptor<WorklogAiPostProcessRequestedEvent> eventCaptor = ArgumentCaptor.forClass(WorklogAiPostProcessRequestedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().worklogId()).isEqualTo(WORKLOG_ID);
        assertThat(eventCaptor.getValue().requestContent()).isEqualTo(request.requestContent());
        assertThat(eventCaptor.getValue().workContent()).isEqualTo(request.workContent());
        assertThat(eventCaptor.getValue().authorId()).isEqualTo(USER_ID);
        assertThat(eventCaptor.getValue().teamId()).isEqualTo(TEAM_ID);
        assertThat(eventCaptor.getValue().departmentId()).isEqualTo(DEPARTMENT_ID);
        assertThat(eventCaptor.getValue().files())
                .singleElement()
                .extracting(
                        WorklogAiPostProcessRequestedEvent.FileSummaryTarget::fileId,
                        WorklogAiPostProcessRequestedEvent.FileSummaryTarget::storageKey,
                        WorklogAiPostProcessRequestedEvent.FileSummaryTarget::originalName,
                        WorklogAiPostProcessRequestedEvent.FileSummaryTarget::fileExtension
                )
                .containsExactly(9001L, "worklog/501/report.txt", "report.txt", "txt");
        verify(eventPublisher, never()).publishEvent(any(WorklogAiPipelineRequestedEvent.class));
        verify(eventPublisher, never()).publishEvent(any(WorklogLightIndexRequestedEvent.class));
    }

    @Test
    @DisplayName("최초 상태가 COMPLETED 인 업무 생성은 완료 전환 이벤트를 발행하지 않는다.")
    void 최초_상태가_COMPLETED인_업무_생성은_완료_전환_이벤트를_발행하지_않는다() {
        // given
        CustomUserPrincipal principal = principal();
        CreateWorklogApiDto.Request request = request(INSTRUCTION_DATE, DUE_DATE, WorklogStatus.COMPLETED);
        given(teamService.getTeamOrThrow(TEAM_ID)).willReturn(sampleTeam());
        given(teamService.isMember(USER_ID, TEAM_ID)).willReturn(true);
        given(worklogRepository.save(any(Worklog.class))).willAnswer(invocation -> {
            Worklog toSave = invocation.getArgument(0);
            ReflectionTestUtils.setField(toSave, "id", WORKLOG_ID);
            return toSave;
        });
        given(tagService.normalizeExistingTagIds(TAG_IDS)).willReturn(TAG_IDS);

        // when
        worklogService.createWorklog(principal, request, List.of());

        // then
        verify(worklogStatusHistoryService).createStatusHistory(
                eq(WORKLOG_ID),
                eq(WorklogStatus.COMPLETED),
                eq(USER_ID)
        );
        verify(eventPublisher, never()).publishEvent(any(WorklogCompletedEvent.class));
        ArgumentCaptor<WorklogAiPostProcessRequestedEvent> eventCaptor = ArgumentCaptor.forClass(WorklogAiPostProcessRequestedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().worklogId()).isEqualTo(WORKLOG_ID);
        assertThat(eventCaptor.getValue().requestContent()).isEqualTo(request.requestContent());
        assertThat(eventCaptor.getValue().workContent()).isEqualTo(request.workContent());
        assertThat(eventCaptor.getValue().authorId()).isEqualTo(USER_ID);
        assertThat(eventCaptor.getValue().teamId()).isEqualTo(TEAM_ID);
        assertThat(eventCaptor.getValue().departmentId()).isEqualTo(DEPARTMENT_ID);
        assertThat(eventCaptor.getValue().files()).isEmpty();
    }

    @Test
    @DisplayName("팀이 존재하지 않으면 TEAM_NOT_FOUND 를 던지고 이후 단계는 수행하지 않는다.")
    void 팀이_존재하지_않으면_TEAM_NOT_FOUND_를_던진다() {
        // given
        CustomUserPrincipal principal = principal();
        CreateWorklogApiDto.Request request = request(INSTRUCTION_DATE, DUE_DATE);
        List<MultipartFile> files = sampleFiles();
        given(teamService.getTeamOrThrow(TEAM_ID))
                .willThrow(new BusinessException(ErrorCode.TEAM_NOT_FOUND));

        // when & then
        assertThatThrownBy(() -> worklogService.createWorklog(principal, request, files))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.TEAM_NOT_FOUND);

        verify(teamService, never()).isMember(any(), any());
        verify(worklogRepository, never()).save(any(Worklog.class));
        verify(fileService, never()).uploadWorklogFiles(any(), any(), any());
        verify(worklogStatusHistoryService, never()).createStatusHistory(any(), any(), any());
    }

    @Test
    @DisplayName("사용자가 팀 소속이 아니면 WORKLOG_TEAM_FORBIDDEN 을 던지고 이후 단계는 수행하지 않는다.")
    void 사용자가_팀_소속이_아니면_WORKLOG_TEAM_FORBIDDEN_을_던진다() {
        // given
        CustomUserPrincipal principal = principal();
        CreateWorklogApiDto.Request request = request(INSTRUCTION_DATE, DUE_DATE);
        List<MultipartFile> files = sampleFiles();
        given(teamService.isMember(USER_ID, TEAM_ID)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> worklogService.createWorklog(principal, request, files))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.WORKLOG_TEAM_FORBIDDEN);

        verify(worklogRepository, never()).save(any(Worklog.class));
        verify(fileService, never()).uploadWorklogFiles(any(), any(), any());
        verify(worklogStatusHistoryService, never()).createStatusHistory(any(), any(), any());
    }

    @Test
    @DisplayName("마감 일자가 지시 일자보다 앞서면 WORKLOG_INVALID_DATE_RANGE 를 던진다.")
    void 마감_일자가_지시_일자보다_앞서면_WORKLOG_INVALID_DATE_RANGE_를_던진다() {
        // given
        // 지시(2026-04-25) 보다 마감(2026-04-22) 이 앞서는 비정상 범위를 검증한다.
        CustomUserPrincipal principal = principal();
        CreateWorklogApiDto.Request request = request(DUE_DATE, INSTRUCTION_DATE);
        List<MultipartFile> files = sampleFiles();
        given(teamService.isMember(USER_ID, TEAM_ID)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> worklogService.createWorklog(principal, request, files))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.WORKLOG_INVALID_DATE_RANGE);

        verify(worklogRepository, never()).save(any(Worklog.class));
        verify(fileService, never()).uploadWorklogFiles(any(), any(), any());
        verify(worklogStatusHistoryService, never()).createStatusHistory(any(), any(), any());
    }

    @Test
    @DisplayName("지시 일자 또는 마감 일자가 null 이면 날짜 범위 검증을 생략하고 정상 저장한다.")
    void 지시_또는_마감_일자가_null_이면_날짜_범위_검증을_생략한다() {
        // given
        // instructionDate/dueDate 둘 중 하나라도 null 이면 비교를 건너뛴다.
        CustomUserPrincipal principal = principal();
        CreateWorklogApiDto.Request request = request(null, null);
        List<MultipartFile> files = sampleFiles();

        given(teamService.getTeamOrThrow(TEAM_ID)).willReturn(sampleTeam());
        given(teamService.isMember(USER_ID, TEAM_ID)).willReturn(true);
        given(worklogRepository.save(any(Worklog.class))).willAnswer(invocation -> {
            Worklog toSave = invocation.getArgument(0);
            ReflectionTestUtils.setField(toSave, "id", WORKLOG_ID);
            return toSave;
        });
        given(tagService.normalizeExistingTagIds(TAG_IDS)).willReturn(TAG_IDS);

        // when
        CreateWorklogApiDto.Response response = worklogService.createWorklog(principal, request, files);

        // then
        assertThat(response.worklogId()).isEqualTo(WORKLOG_ID);
        verify(fileService).uploadWorklogFilesWithoutAiSummaryRequest(eq(WORKLOG_ID), eq(USER_ID), eq(files));
        verify(worklogStatusHistoryService).createStatusHistory(
                eq(WORKLOG_ID),
                eq(WorklogStatus.IN_PROGRESS),
                eq(USER_ID)
        );
        verify(worklogTagRepository).saveAll(any());
        verify(tagService).incrementUsageCountByIds(TAG_IDS);
    }

    @Test
    @DisplayName("getWorklogs 는 사용자 ID 와 페이지 쿼리를 Repository 에 위임하고 응답으로 변환한다")
    void getWorklogs_는_사용자_ID_와_페이지_쿼리를_Repository_에_위임한다() {
        // given
        CustomUserPrincipal principal = principal();
        GetWorklogsApiDto.Request request = new GetWorklogsApiDto.Request(1, 20);
        Page<WorklogListProjection> projectionPage = new PageImpl<>(
                List.of(sampleProjection()),
                PageRequest.of(0, 20),
                1
        );

        given(worklogRepository.findWorklogPage(eq(USER_ID), any(WorklogPageQuery.class)))
                .willReturn(projectionPage);
        given(worklogDependencyRepository.countByWorklogIds(List.of(WORKLOG_ID)))
                .willReturn(Map.of(WORKLOG_ID, 2L));

        // when
        PageResponse<GetWorklogsApiDto.Response.Item> response = worklogService.getWorklogs(principal, request);

        // then
        assertThat(response.items()).hasSize(1);
        GetWorklogsApiDto.Response.Item item = response.items().get(0);
        assertThat(item.worklogId()).isEqualTo(WORKLOG_ID);
        assertThat(item.teamName()).isEqualTo("물류혁신TF");
        assertThat(item.predecessorCount()).isEqualTo(2L);
        assertThat(response.totalCount()).isEqualTo(1);
        assertThat(response.page()).isEqualTo(1);

        ArgumentCaptor<WorklogPageQuery> queryCaptor = ArgumentCaptor.forClass(WorklogPageQuery.class);
        verify(worklogRepository).findWorklogPage(eq(USER_ID), queryCaptor.capture());
        assertThat(queryCaptor.getValue().page()).isEqualTo(1);
        assertThat(queryCaptor.getValue().pageSize()).isEqualTo(20);
        assertThat(queryCaptor.getValue().pageIndex()).isEqualTo(0);
    }

    @Test
    @DisplayName("수정으로 상태가 COMPLETED 로 바뀌면 완료일을 오늘로 기록한다")
    void updateWorklog_records_completion_date_when_status_changes_to_completed() {
        // given
        CustomUserPrincipal principal = principal();
        Worklog worklog = savedWorklog(WorklogStatus.IN_PROGRESS);
        UpdateWorklogApiDto.Request request = updateRequest(WorklogStatus.COMPLETED);

        given(worklogRepository.findById(WORKLOG_ID)).willReturn(Optional.of(worklog));
        given(worklogStatusPolicy.canTransition(WorklogStatus.IN_PROGRESS, WorklogStatus.COMPLETED)).willReturn(true);

        // when
        worklogService.updateWorklog(principal, WORKLOG_ID, request, List.of());

        // then
        assertThat(worklog.getStatusCode()).isEqualTo(WorklogStatus.COMPLETED);
        assertThat(worklog.getCompletionDate()).isEqualTo(LocalDate.now());
        verify(worklogStatusHistoryService).createStatusHistory(
                eq(WORKLOG_ID),
                eq(WorklogStatus.IN_PROGRESS),
                eq(WorklogStatus.COMPLETED),
                eq(USER_ID),
                eq("완료 처리")
        );
        verify(eventPublisher).publishEvent(new WorklogCompletedEvent(WORKLOG_ID));
    }

    @Test
    @DisplayName("작성자 본인이 상태를 COMPLETED 로 변경하면 완료일과 상태 이력을 기록한다")
    void updateWorklogStatus_records_completion_date_and_history() {
        // given
        CustomUserPrincipal principal = principal();
        Worklog worklog = savedWorklog(WorklogStatus.IN_PROGRESS);
        UpdateWorklogStatusApiDto.Request request = new UpdateWorklogStatusApiDto.Request(
                WorklogStatus.COMPLETED,
                "  완료 처리  "
        );

        given(worklogRepository.findById(WORKLOG_ID)).willReturn(Optional.of(worklog));
        given(worklogStatusPolicy.canTransition(WorklogStatus.IN_PROGRESS, WorklogStatus.COMPLETED)).willReturn(true);

        // when
        worklogService.updateWorklogStatus(principal, WORKLOG_ID, request);

        // then
        assertThat(worklog.getStatusCode()).isEqualTo(WorklogStatus.COMPLETED);
        assertThat(worklog.getCompletionDate()).isEqualTo(LocalDate.now());
        verify(worklogStatusHistoryService).createStatusHistory(
                eq(WORKLOG_ID),
                eq(WorklogStatus.IN_PROGRESS),
                eq(WorklogStatus.COMPLETED),
                eq(USER_ID),
                eq("완료 처리")
        );
        verify(eventPublisher).publishEvent(new WorklogCompletedEvent(WORKLOG_ID));
    }

    @Test
    @DisplayName("작성자가 아니면 상태 변경 전용 API 는 WORKLOG_EDIT_FORBIDDEN 을 던진다")
    void updateWorklogStatus_rejects_non_author() {
        // given
        CustomUserPrincipal principal = new CustomUserPrincipal(999L, "other@test.com", "MEMBER");
        Worklog worklog = savedWorklog(WorklogStatus.IN_PROGRESS);
        UpdateWorklogStatusApiDto.Request request = new UpdateWorklogStatusApiDto.Request(
                WorklogStatus.COMPLETED,
                "완료 처리"
        );

        given(worklogRepository.findById(WORKLOG_ID)).willReturn(Optional.of(worklog));

        // when & then
        assertThatThrownBy(() -> worklogService.updateWorklogStatus(principal, WORKLOG_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.WORKLOG_EDIT_FORBIDDEN);

        assertThat(worklog.getStatusCode()).isEqualTo(WorklogStatus.IN_PROGRESS);
        verify(worklogStatusPolicy, never()).canTransition(any(), any());
        verify(worklogStatusHistoryService, never()).createStatusHistory(any(), any(), any(), any(), any());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("상태가 실제로 바뀌지 않으면 상태 변경 전용 API 는 이력을 남기지 않는다")
    void updateWorklogStatus_skips_history_when_status_is_unchanged() {
        // given
        CustomUserPrincipal principal = principal();
        Worklog worklog = savedWorklog(WorklogStatus.IN_PROGRESS);
        UpdateWorklogStatusApiDto.Request request = new UpdateWorklogStatusApiDto.Request(
                WorklogStatus.IN_PROGRESS,
                "   "
        );

        given(worklogRepository.findById(WORKLOG_ID)).willReturn(Optional.of(worklog));

        // when
        worklogService.updateWorklogStatus(principal, WORKLOG_ID, request);

        // then
        assertThat(worklog.getStatusCode()).isEqualTo(WorklogStatus.IN_PROGRESS);
        assertThat(worklog.getCompletionDate()).isNull();
        verify(worklogStatusPolicy, never()).canTransition(any(), any());
        verify(worklogStatusHistoryService, never()).createStatusHistory(any(), any(), any(), any(), any());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("허용되지 않은 상태 전이면 WORKLOG_STATUS_TRANSITION_INVALID 를 던진다")
    void updateWorklog_rejects_invalid_status_transition() {
        // given
        CustomUserPrincipal principal = principal();
        Worklog worklog = savedWorklog(WorklogStatus.PENDING);
        UpdateWorklogApiDto.Request request = updateRequest(WorklogStatus.COMPLETED);

        given(worklogRepository.findById(WORKLOG_ID)).willReturn(Optional.of(worklog));
        given(worklogStatusPolicy.canTransition(WorklogStatus.PENDING, WorklogStatus.COMPLETED)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> worklogService.updateWorklog(principal, WORKLOG_ID, request, List.of()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.WORKLOG_STATUS_TRANSITION_INVALID);

        assertThat(worklog.getStatusCode()).isEqualTo(WorklogStatus.PENDING);
        assertThat(worklog.getCompletionDate()).isNull();
        verify(fileService, never()).uploadWorklogFiles(any(), any(), any());
        verify(worklogStatusHistoryService, never()).createStatusHistory(any(), any(), any(), any(), any());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }


    @Test
    @DisplayName("업무 수정으로 신규 파일을 추가하면 기존 파일 AI 요약 요청 업로드 경로를 유지한다")
    void updateWorklog_keeps_existing_file_summary_request_for_new_files() {
        // given
        CustomUserPrincipal principal = principal();
        Worklog worklog = savedWorklog(WorklogStatus.IN_PROGRESS);
        UpdateWorklogApiDto.Request request = updateRequest(WorklogStatus.IN_PROGRESS);
        List<MultipartFile> newFiles = sampleFiles();

        given(worklogRepository.findById(WORKLOG_ID)).willReturn(Optional.of(worklog));

        // when
        worklogService.updateWorklog(principal, WORKLOG_ID, request, newFiles);

        // then
        verify(fileService).uploadWorklogFiles(eq(WORKLOG_ID), eq(USER_ID), eq(newFiles));
        verify(fileService, never()).uploadWorklogFilesWithoutAiSummaryRequest(any(), any(), any());
    }

    @Test
    @DisplayName("getWorklogDetail 은 첨부 파일의 내부 저장 key 를 공개 URL 로 변환해 반환한다")
    void getWorklogDetail_은_첨부_파일의_내부_저장_key를_공개_URL로_변환해_반환한다() {
        // given
        CustomUserPrincipal principal = principal();
        String storageKey = "worklog/2026/04/report.pdf";
        String publicUrl = "https://cdn.example.com/worklog/2026/04/report.pdf";
        WorklogFileProjection fileProjection = new WorklogFileProjection(
                9001L,
                "report.pdf",
                storageKey,
                "pdf",
                204800L,
                null,
                AiProcessingStatus.PENDING
        );

        given(worklogRepository.findWorklogDetail(USER_ID, WORKLOG_ID))
                .willReturn(Optional.of(sampleDetailProjection()));
        given(fileRepository.findByWorklogId(WORKLOG_ID))
                .willReturn(List.of(fileProjection));
        given(worklogTagRepository.findTagNames(WORKLOG_ID)).willReturn(List.of("결산"));
        given(worklogDependencyRepository.findDirectDependencies(WORKLOG_ID)).willReturn(List.of());
        given(worklogStatusHistoryRepository.findStatusHistories(WORKLOG_ID)).willReturn(List.of());
        given(fileService.toPublicUrl(storageKey)).willReturn(publicUrl);

        // when
        GetWorklogDetailApiDto.Response response = worklogService.getWorklogDetail(principal, WORKLOG_ID);

        // then
        assertThat(response.files()).hasSize(1);
        assertThat(response.files().get(0).storedPath()).isEqualTo(publicUrl);
        verify(fileService).toPublicUrl(storageKey);
    }

    private static WorklogListProjection sampleProjection() {
        return new WorklogListProjection(
                WORKLOG_ID,
                "결산 보고서 작성",
                "IN_PROGRESS",
                "수행 내용",
                new BigDecimal("3.10"),
                "HIGH",
                "AI 요약",
                "COMPLETED",
                Boolean.FALSE,
                TEAM_ID,
                "물류혁신TF",
                USER_ID,
                "홍길동",
                INSTRUCTION_DATE,
                DUE_DATE
        );
    }

    /**
     * 상세조회 응답 조립 경로만 검증하도록 본문 projection 은 성공 케이스의 필수 필드로 고정한다.
     */
    private static WorklogDetailProjection sampleDetailProjection() {
        return new WorklogDetailProjection(
                WORKLOG_ID,
                TEAM_ID,
                "물류혁신TF",
                USER_ID,
                "홍길동",
                "결산 보고서 작성",
                "요청 내용",
                "수행 내용",
                "AI 요약",
                Boolean.FALSE,
                "COMPLETED",
                "IN_PROGRESS",
                "HIGH",
                new BigDecimal("3.10"),
                INSTRUCTION_DATE,
                DUE_DATE,
                null,
                LocalDateTime.of(2026, 4, 22, 9, 0),
                LocalDateTime.of(2026, 4, 22, 10, 0)
        );
    }

    private static CustomUserPrincipal principal() {
        return new CustomUserPrincipal(USER_ID, "test@test.com", "MEMBER");
    }

    private static CreateWorklogApiDto.Request request(LocalDate instructionDate, LocalDate dueDate) {
        return request(instructionDate, dueDate, WorklogStatus.IN_PROGRESS);
    }

    /** 생성 시 최초 상태별 이벤트 scope 를 분리해 검증할 수 있도록 상태만 바꾸는 fixture 를 제공한다. */
    private static CreateWorklogApiDto.Request request(LocalDate instructionDate, LocalDate dueDate, WorklogStatus statusCode) {
        return new CreateWorklogApiDto.Request(
                TEAM_ID,
                "test",
                "test",
                "test",
                statusCode,
                WorklogImportance.HIGH,
                new BigDecimal("3.10"),
                instructionDate,
                dueDate,
                TAG_IDS,
                null
        );
    }

    /** 상태 수정 테스트가 검증 대상 필드만 바꿀 수 있도록 공통 수정 요청 fixture 를 만든다. */
    private static UpdateWorklogApiDto.Request updateRequest(WorklogStatus statusCode) {
        return new UpdateWorklogApiDto.Request(
                "updated title",
                "updated request",
                "updated work",
                statusCode,
                "완료 처리",
                WorklogImportance.HIGH,
                new BigDecimal("4.00"),
                INSTRUCTION_DATE,
                DUE_DATE,
                List.of(),
                List.of(),
                List.of(),
                "수정된 AI 요약",
                List.of()
        );
    }

    /** 수정 대상 업무의 현재 상태만 테스트별로 바꾸고 나머지 필드는 유효한 기본값으로 고정한다. */
    private static Worklog savedWorklog(WorklogStatus statusCode) {
        Worklog worklog = Worklog.create(
                USER_ID,
                TEAM_ID,
                "test",
                "test",
                "test",
                statusCode,
                WorklogImportance.NORMAL,
                new BigDecimal("1.00"),
                INSTRUCTION_DATE,
                DUE_DATE
        );
        ReflectionTestUtils.setField(worklog, "id", WORKLOG_ID);
        return worklog;
    }

    private static List<MultipartFile> sampleFiles() {
        return List.of(
                new MockMultipartFile(
                        "files",
                        "report.txt",
                        "text/plain",
                        "content".getBytes()
                )
        );
    }

    private Team sampleTeam() {
        Team team = Team.create(
                DEPARTMENT_ID,
                "물류혁신TF",
                TeamStatus.ACTIVE,
                "테스트 팀",
                LocalDate.of(2025, 1, 1),
                null
        );
        ReflectionTestUtils.setField(team, "id", TEAM_ID);
        return team;
    }
}
