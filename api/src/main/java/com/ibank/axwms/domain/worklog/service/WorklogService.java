package com.ibank.axwms.domain.worklog.service;

import com.ibank.axwms.domain.file.repository.FileRepository;
import com.ibank.axwms.domain.file.service.FileService;
import com.ibank.axwms.domain.organization.team.service.TeamService;
import com.ibank.axwms.domain.tag.repository.jooq.projection.MetaTagDetailProjection;
import com.ibank.axwms.domain.tag.service.TagService;
import com.ibank.axwms.domain.worklog.dto.CreateWorklogApiDto;
import com.ibank.axwms.domain.worklog.dto.GetWorklogOptionsApiDto;
import com.ibank.axwms.domain.worklog.dto.GetWorklogDetailApiDto;
import com.ibank.axwms.domain.worklog.dto.GetWorklogsApiDto;
import com.ibank.axwms.domain.worklog.entity.Worklog;
import com.ibank.axwms.domain.worklog.repository.WorklogDependencyRepository;
import com.ibank.axwms.domain.worklog.repository.WorklogRepository;
import com.ibank.axwms.domain.worklog.repository.WorklogStatusHistoryRepository;
import com.ibank.axwms.domain.worklog.repository.WorklogTagRepository;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.WorklogDependencyProjection;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.WorklogDetailProjection;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.WorklogFileProjection;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.WorklogListProjection;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.WorklogStatusHistoryProjection;
import com.ibank.axwms.domain.worklog.repository.jooq.query.WorklogPageQuery;
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import com.ibank.axwms.global.response.PageResponse;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorklogService {

    private final WorklogRepository worklogRepository;
    private final WorklogTagRepository worklogTagRepository;
    private final WorklogDependencyRepository worklogDependencyRepository;
    private final WorklogStatusHistoryRepository worklogStatusHistoryRepository;
    private final FileRepository fileRepository;
    private final TeamService teamService;
    private final FileService fileService;
    private final WorklogStatusHistoryService worklogStatusHistoryService;
    private final WorklogDependencyService worklogDependencyService;
    private final TagService tagService;

    /**
     * 로그인 사용자의 권한으로 업무를 등록한다.
     * 팀 존재 여부와 사용자의 해당 팀 소속 여부를 확인한 뒤 신규 Worklog 엔티티를 저장한다.
     *
     * @param principal 현재 로그인 사용자
     * @param request   업무 등록 요청 DTO
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
        worklogStatusHistoryService.createStatusHistory(savedWorklog.getId(), principal.userId());
        worklogDependencyService.registerPredecessor(
                principal.userId(),
                savedWorklog.getId(),
                request.predecessorWorklogIds()
        );

        return CreateWorklogApiDto.Response.of(savedWorklog.getId());
    }

    /**
     * 로그인 사용자가 접근할 수 있는 업무 목록을 페이지로 조회한다.
     * 사용자가 admin 이거나 ACTIVE 멤버인 팀의 업무만 노출하며, 소프트 삭제된 업무는 제외한다.
     * 행마다 직접 연결된 선행 업무 개수를 함께 집계한다.
     *
     * @param principal 현재 로그인 사용자
     * @param request   페이지/사이즈 요청 DTO. null 이면 기본값을 사용한다.
     * @return 페이지네이션 응답
     */
    public PageResponse<GetWorklogsApiDto.Response.Item> getWorklogs(CustomUserPrincipal principal,
                                                                     GetWorklogsApiDto.Request request) {
        WorklogPageQuery query = WorklogPageQuery.from(request);
        Page<WorklogListProjection> page = worklogRepository.findWorklogPage(principal.userId(), query);

        List<Long> worklogIds = page.getContent().stream().map(WorklogListProjection::worklogId).toList();
        Map<Long, Long> predecessorCountByWorklogId = worklogDependencyRepository.countByWorklogIds(worklogIds);

        return GetWorklogsApiDto.Response.fromPage(page, predecessorCountByWorklogId);
    }

    /**
     * 로그인 사용자가 접근 가능한 업무 한 건의 상세 정보를 조회한다.
     * 본문 1개 + 첨부 파일/태그/선행 업무/상태 이력 컬렉션 4개, 총 5개의 쿼리로 구성된다.
     * 가시성/소프트삭제 검증은 본문 쿼리에서 수행하므로, 권한 밖이거나 없는 업무는 WORKLOG_NOT_FOUND 로 응답한다.
     *
     * @param principal 현재 로그인 사용자
     * @param worklogId 조회 대상 업무 ID
     * @return 업무 상세 응답 DTO
     * @throws BusinessException WORKLOG_NOT_FOUND 업무가 없거나 가시 범위 밖일 때
     */
    public GetWorklogDetailApiDto.Response getWorklogDetail(CustomUserPrincipal principal, Long worklogId) {
        WorklogDetailProjection detail = worklogRepository
                .findWorklogDetail(principal.userId(), worklogId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKLOG_NOT_FOUND));

        List<WorklogFileProjection> files = fileRepository.findByWorklogId(worklogId);
        List<String> tags = worklogTagRepository.findTagNames(worklogId);
        List<WorklogDependencyProjection> dependencies = worklogDependencyRepository.findDirectDependencies(worklogId);
        List<WorklogStatusHistoryProjection> statusHistories = worklogStatusHistoryRepository.findStatusHistories(worklogId);

        return GetWorklogDetailApiDto.Response.of(detail, files, tags, dependencies, statusHistories);
    }

    /**
     * 업무 등록 화면 진입 시 사용할 폼 옵션을 한 번에 반환한다.
     * 선행 업무 후보는 사용자가 접근 가능한 (admin 또는 ACTIVE 멤버) 팀의 미삭제, 미완료 worklog 만 최신순으로 포함한다.
     * 태그는 메타 태그 전체를 이름순으로 포함한다.
     */
    public GetWorklogOptionsApiDto.Response getWorklogOptions(CustomUserPrincipal principal) {
        List<WorklogListProjection> predecessorCandidates =
                worklogRepository.findActivePredecessorCandidates(principal.userId());
        List<MetaTagDetailProjection> tags = tagService.findAllTagDetails();
        return GetWorklogOptionsApiDto.Response.of(predecessorCandidates, tags);
    }

    private void validateDateRange(LocalDate instructionDate, LocalDate dueDate) {
        if (instructionDate != null && dueDate != null && dueDate.isBefore(instructionDate)) {
            throw new BusinessException(ErrorCode.WORKLOG_INVALID_DATE_RANGE);
        }
    }
}
