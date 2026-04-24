package com.ibank.axwms.domain.worklog.service;

import com.ibank.axwms.domain.file.service.FileService;
import com.ibank.axwms.domain.organization.team.service.TeamService;
import com.ibank.axwms.domain.worklog.dto.CreateWorklogApiDto;
import com.ibank.axwms.domain.worklog.entity.Worklog;
import com.ibank.axwms.domain.worklog.repository.WorklogRepository;
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorklogService {

    private final WorklogRepository worklogRepository;
    private final TeamService teamService;
    private final FileService fileService;

    /**
     * 로그인 사용자의 권한으로 업무를 등록한다.
     * 팀 존재 여부와 사용자의 해당 팀 소속 여부를 확인한 뒤 신규 Worklog 엔티티를 저장한다.
     *
     * @param principal 현재 로그인 사용자
     * @param request 업무 등록 요청 DTO
     * @return 생성된 업무 ID 를 담은 응답 DTO
     * @throws BusinessException TEAM_NOT_FOUND 대상 팀이 없을 때
     * @throws BusinessException WORKLOG_TEAM_FORBIDDEN 사용자가 대상 팀 소속이 아닐 때
     * @throws BusinessException WORKLOG_INVALID_DATE_RANGE 마감 일자가 지시 일자보다 앞설 때
     */
    @Transactional
    public CreateWorklogApiDto.Response createWorklog(
            CustomUserPrincipal principal,
            CreateWorklogApiDto.Request request,
            List<MultipartFile> files
    ) {
        teamService.getTeamOrThrow(request.teamId());
        if (!teamService.isMember(principal.userId(), request.teamId())) {
            throw new BusinessException(ErrorCode.WORKLOG_TEAM_FORBIDDEN);
        }

        validateDateRange(request.instructionDate(), request.dueDate());

        Worklog savedWorklog = worklogRepository.save(Worklog.create(
                principal.userId(),
                request.teamId(),
                request.title(),
                request.requestContent(),
                request.workContent(),
                request.importanceCode(),
                request.instructionDate(),
                request.dueDate()
        ));

        fileService.uploadWorklogFiles(savedWorklog.getId(), principal.userId(), files);

        return CreateWorklogApiDto.Response.of(savedWorklog.getId());
    }

    private void validateDateRange(LocalDate instructionDate, LocalDate dueDate) {
        if (instructionDate != null && dueDate != null && dueDate.isBefore(instructionDate)) {
            throw new BusinessException(ErrorCode.WORKLOG_INVALID_DATE_RANGE);
        }
    }
}
