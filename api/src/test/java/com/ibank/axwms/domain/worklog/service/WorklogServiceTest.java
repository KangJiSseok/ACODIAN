package com.ibank.axwms.domain.worklog.service;

import com.ibank.axwms.domain.file.service.FileService;
import com.ibank.axwms.domain.organization.team.TeamStatus;
import com.ibank.axwms.domain.organization.team.entity.Team;
import com.ibank.axwms.domain.organization.team.service.TeamService;
import com.ibank.axwms.domain.worklog.WorklogImportance;
import com.ibank.axwms.domain.worklog.dto.CreateWorklogApiDto;
import com.ibank.axwms.domain.worklog.entity.Worklog;
import com.ibank.axwms.domain.worklog.repository.WorklogRepository;
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WorklogServiceTest {
    @Mock private WorklogRepository worklogRepository;
    @Mock private TeamService teamService;
    @Mock private FileService fileService;
    @Mock private WorklogStatusHistoryService worklogStatusHistoryService;

    @InjectMocks private WorklogService worklogService;

    private static final Long USER_ID = 101L;
    private static final Long TEAM_ID = 21L;
    private static final Long WORKLOG_ID = 501L;
    private static final Long DEPARTMENT_ID = 31L;
    private static final LocalDate INSTRUCTION_DATE = LocalDate.of(2026, 4, 22);
    private static final LocalDate DUE_DATE = LocalDate.of(2026, 4, 25);

    @Test
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

        // when
        CreateWorklogApiDto.Response response = worklogService.createWorklog(principal, request, files);

        // then
        assertThat(response.worklogId()).isEqualTo(WORKLOG_ID);
        verify(fileService).uploadWorklogFiles(eq(WORKLOG_ID), eq(USER_ID), eq(files));
        verify(worklogStatusHistoryService).createStatusHistory(eq(WORKLOG_ID), eq(USER_ID));
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
        verify(worklogStatusHistoryService, never()).createStatusHistory(any(), any());
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
        verify(worklogStatusHistoryService, never()).createStatusHistory(any(), any());
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
        verify(worklogStatusHistoryService, never()).createStatusHistory(any(), any());
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

        // when
        CreateWorklogApiDto.Response response = worklogService.createWorklog(principal, request, files);

        // then
        assertThat(response.worklogId()).isEqualTo(WORKLOG_ID);
        verify(fileService).uploadWorklogFiles(eq(WORKLOG_ID), eq(USER_ID), eq(files));
        verify(worklogStatusHistoryService).createStatusHistory(eq(WORKLOG_ID), eq(USER_ID));
    }

    private static CustomUserPrincipal principal() {
        return new CustomUserPrincipal(USER_ID, "test@test.com", "MEMBER");
    }

    private static CreateWorklogApiDto.Request request(LocalDate instructionDate, LocalDate dueDate) {
        return new CreateWorklogApiDto.Request(
                TEAM_ID,
                "test",
                "test",
                "test",
                WorklogImportance.HIGH,
                new BigDecimal("3.10"),
                instructionDate,
                dueDate
        );
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
