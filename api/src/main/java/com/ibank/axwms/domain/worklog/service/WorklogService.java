package com.ibank.axwms.domain.worklog.service;

import com.ibank.axwms.domain.file.repository.FileRepository;
import com.ibank.axwms.domain.file.service.FileService;
import com.ibank.axwms.domain.organization.team.entity.Team;
import com.ibank.axwms.domain.organization.team.service.TeamService;
import com.ibank.axwms.domain.tag.service.TagService;
import com.ibank.axwms.domain.worklog.WorklogStatus;
import com.ibank.axwms.domain.worklog.dto.CreateWorklogApiDto;
import com.ibank.axwms.domain.worklog.dto.GetWorklogDetailApiDto;
import com.ibank.axwms.domain.worklog.dto.GetWorklogsApiDto;
import com.ibank.axwms.domain.worklog.dto.InternalWorklogPolishApiDto;
import com.ibank.axwms.domain.worklog.dto.InternalWorklogTitleRecommendationApiDto;
import com.ibank.axwms.domain.worklog.dto.PolishWorklogApiDto;
import com.ibank.axwms.domain.worklog.dto.RecommendWorklogTitleApiDto;
import com.ibank.axwms.domain.worklog.dto.SearchPredecessorApiDto;
import com.ibank.axwms.domain.worklog.dto.UpdateWorklogApiDto;
import com.ibank.axwms.domain.worklog.dto.UpdateWorklogStatusApiDto;
import com.ibank.axwms.domain.worklog.entity.Worklog;
import com.ibank.axwms.domain.worklog.entity.WorklogTag;
import com.ibank.axwms.domain.worklog.event.WorklogAiPostProcessRequestedEvent;
import com.ibank.axwms.domain.worklog.event.WorklogCompletedEvent;
import com.ibank.axwms.domain.worklog.external.WorklogPolishClient;
import com.ibank.axwms.domain.worklog.policy.WorklogStatusPolicy;
import com.ibank.axwms.domain.worklog.repository.WorklogDependencyRepository;
import com.ibank.axwms.domain.worklog.repository.WorklogRepository;
import com.ibank.axwms.domain.worklog.repository.WorklogStatusHistoryRepository;
import com.ibank.axwms.domain.worklog.repository.WorklogTagRepository;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.WorklogDependencyProjection;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.WorklogDetailProjection;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.WorklogFileProjection;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.WorklogListProjection;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.WorklogStatusHistoryProjection;
import com.ibank.axwms.domain.worklog.repository.jooq.query.PredecessorCandidateSearchQuery;
import com.ibank.axwms.domain.worklog.repository.jooq.query.WorklogPageQuery;
import com.ibank.axwms.global.enums.AiProcessingStatus;
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import com.ibank.axwms.global.response.PageResponse;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
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
    private final WorklogStatusPolicy worklogStatusPolicy;
    private final TagService tagService;
    private final ApplicationEventPublisher eventPublisher;
    private final WorklogPolishClient worklogPolishClient;

    /**
     * 인증된 작성 보조 요청의 초안을 AI 서버에 전달하고 저장 없이 다듬어진 본문만 반환한다.
     * Controller 의 role gate 이후에는 사용자 식별자가 필요 없고, 원격 호출만 수행하므로 DB 트랜잭션을 열지 않는다.
     *
     * @param request 작성 보조 요청 DTO
     * @return 다듬어진 본문
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public PolishWorklogApiDto.Response polishWorklog(PolishWorklogApiDto.Request request) {
        InternalWorklogPolishApiDto.Response response = worklogPolishClient.polishWorklog(
                InternalWorklogPolishApiDto.Request.from(request)
        );
        return PolishWorklogApiDto.Response.of(response.workContent());
    }

    /**
     * 인증된 제목 추천 요청의 초안을 AI 서버에 전달하고 저장 없이 후보 제목만 반환한다.
     * 기존 작성 보조와 같은 동기 호출 경계이므로 별도 DB 트랜잭션을 열지 않는다.
     *
     * @param request 제목 추천 요청 DTO
     * @return 최대 3개의 제목 후보
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public RecommendWorklogTitleApiDto.Response recommendWorklogTitles(
            RecommendWorklogTitleApiDto.Request request
    ) {
        InternalWorklogTitleRecommendationApiDto.Response response = worklogPolishClient.recommendWorklogTitles(
                InternalWorklogTitleRecommendationApiDto.Request.from(request)
        );
        return RecommendWorklogTitleApiDto.Response.of(response.titles());
    }

    /**
     * 로그인 사용자의 권한으로 업무를 등록한다.
     * 팀 존재 여부와 사용자의 해당 팀 소속 여부를 확인한 뒤 신규 Worklog 엔티티를 저장한다.
     * 등록 트랜잭션 커밋 후에는 파일 요약, 본문 요약/태그 생성 파이프라인, Light v3 index 를 통합 후처리로 요청한다.
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
        Team team = teamService.getTeamOrThrow(request.teamId());
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
                request.statusCode(),
                request.importanceCode(),
                request.actualHours(),
                request.instructionDate(),
                request.dueDate()
        ));

        List<FileService.UploadedFile> uploadedFiles = fileService.uploadWorklogFilesWithoutAiSummaryRequest(
                savedWorklog.getId(),
                principal.userId(),
                files
        );
        worklogStatusHistoryService.createStatusHistory(
                savedWorklog.getId(),
                request.statusCode(),
                principal.userId()
        );
        registerManualTags(savedWorklog.getId(), request.tagIds());
        worklogDependencyService.registerPredecessor(
                savedWorklog.getId(),
                savedWorklog.getTeamId(),
                request.predecessorWorklogIds()
        );
        publishWorklogAiPostProcessRequest(savedWorklog, team, uploadedFiles);

        return CreateWorklogApiDto.Response.of(savedWorklog.getId());
    }

    /**
     * 등록 트랜잭션이 성공한 업무만 세 AI 요청 통합 후처리에 넘기도록 AFTER_COMMIT 이벤트로 snapshot 을 분리한다.
     */
    private void publishWorklogAiPostProcessRequest(Worklog worklog, Team team, List<FileService.UploadedFile> uploadedFiles) {
        eventPublisher.publishEvent(new WorklogAiPostProcessRequestedEvent(
                worklog.getId(),
                worklog.getRequestContent(),
                worklog.getWorkContent(),
                worklog.getAuthorId(),
                worklog.getTeamId(),
                team.getDepartmentId(),
                toFileSummaryTargets(uploadedFiles)
        ));
    }

    /**
     * file 모듈의 업로드 결과를 worklog 통합 후처리 이벤트가 소유하는 불변 snapshot 으로 변환한다.
     */
    private List<WorklogAiPostProcessRequestedEvent.FileSummaryTarget> toFileSummaryTargets(List<FileService.UploadedFile> uploadedFiles) {
        if (uploadedFiles == null || uploadedFiles.isEmpty()) {
            return List.of();
        }
        return uploadedFiles.stream()
                .map(file -> new WorklogAiPostProcessRequestedEvent.FileSummaryTarget(
                        file.fileId(),
                        file.storageKey(),
                        file.originalName(),
                        file.fileExtension()
                ))
                .toList();
    }


    /**
     * 등록 화면에서 직접 선택한 태그만 수동 태그로 연결하고 사용 횟수 캐시를 증가시킨다.
     */
    private void registerManualTags(Long worklogId, List<Long> tagIds) {
        List<Long> normalizedTagIds = tagService.normalizeExistingTagIds(tagIds);
        if (normalizedTagIds.isEmpty()) {
            return;
        }

        List<WorklogTag> worklogTags = normalizedTagIds.stream()
                .map(tagId -> WorklogTag.create(worklogId, tagId))
                .toList();
        worklogTagRepository.saveAll(worklogTags);
        tagService.incrementUsageCountByIds(normalizedTagIds);
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
     * 같은 팀 내 미완료 worklog 를 선행 후보로 검색한다.
     * 요청자는 teamId 의 ACTIVE 멤버여야 하며, query 가 있으면 제목 LIKE, excludeWorklogId 가 있으면 결과에서 제외한다.
     *
     * @throws BusinessException WORKLOG_TEAM_FORBIDDEN 사용자가 요청 팀의 ACTIVE 멤버가 아닐 때
     */
    public PageResponse<SearchPredecessorApiDto.Response.Item> searchPredecessor(CustomUserPrincipal principal,
                                                                                 SearchPredecessorApiDto.Request request) {
        if (!teamService.isMember(principal.userId(), request.teamId())) {
            throw new BusinessException(ErrorCode.WORKLOG_TEAM_FORBIDDEN);
        }
        PredecessorCandidateSearchQuery query = PredecessorCandidateSearchQuery.from(request);
        Page<WorklogListProjection> page = worklogRepository.searchPredecessorCandidatePage(query);
        return SearchPredecessorApiDto.Response.fromPage(page);
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

        return GetWorklogDetailApiDto.Response.of(
                detail,
                files,
                tags,
                dependencies,
                statusHistories,
                fileService::toPublicUrl
        );
    }

    /**
     * 작성자 본인이 자기 업무일지를 한 번의 multipart 요청으로 부분 수정한다.
     * 본문 부분 수정 + 선행 업무 replace + 첨부 파일 추가/삭제를 단일 트랜잭션으로 처리한다.
     * null 필드는 변경되지 않으며, predecessorWorklogIds 는 null=변경없음 / []=모두 제거 / [...]=전체 replace 시멘틱.
     * teamId 는 수정 불가. 일자 범위는 변경 후 합산값 기준으로 검증.
     * aiSummary 가 들어오면 aiSummaryEdited 가 true 로 자동 표시.
     * statusCode 가 실제로 변경되면 reason 을 상태 이력 사유로 함께 기록한다.
     * 파일 추가/삭제 중 어느 단계든 실패하면 본문 수정까지 함께 롤백된다.
     *
     * @param principal 현재 로그인 사용자
     * @param worklogId 수정 대상 worklog ID
     * @param request   부분 수정 요청 (multipart 의 JSON part)
     * @param newFiles  새로 추가할 첨부 파일들 (multipart 의 file parts). null/빈 리스트 허용.
     * @throws BusinessException WORKLOG_NOT_FOUND        worklog 가 없거나 소프트 삭제됨
     * @throws BusinessException WORKLOG_EDIT_FORBIDDEN   작성자 본인이 아님
     * @throws BusinessException WORKLOG_INVALID_DATE_RANGE  마감 일자가 지시 일자보다 앞섬
     * @throws BusinessException WORKLOG_PREDECESSOR_*    선행 업무 검증 실패 (자기참조 / 접근 불가 / 순환)
     * @throws BusinessException WORKLOG_FILE_NOT_FOUND   삭제 대상 fileId 가 해당 worklog 에 속하지 않거나 이미 삭제됨
     */
    @Transactional
    public void updateWorklog(CustomUserPrincipal principal,
                              Long worklogId,
                              UpdateWorklogApiDto.Request request,
                              List<MultipartFile> newFiles) {
        Worklog worklog = getEditableWorklogOrThrow(principal, worklogId);

        LocalDate effectiveInstructionDate = request.instructionDate() != null ? request.instructionDate() : worklog.getInstructionDate();
        LocalDate effectiveDueDate = request.dueDate() != null ? request.dueDate() : worklog.getDueDate();
        validateDateRange(effectiveInstructionDate, effectiveDueDate);

        WorklogStatus previousStatusCode = worklog.getStatusCode();
        boolean statusChanged = request.statusCode() != null && request.statusCode() != previousStatusCode;
        validateStatusTransitionIfChanged(previousStatusCode, request.statusCode(), statusChanged);

        worklog.updatePartial(
                request.title(),
                request.requestContent(),
                request.workContent(),
                request.statusCode(),
                request.importanceCode(),
                request.actualHours(),
                request.instructionDate(),
                request.dueDate(),
                request.aiSummary()
        );
        createStatusHistoryIfChanged(
                worklogId,
                previousStatusCode,
                request.statusCode(),
                principal.userId(),
                request.reason(),
                statusChanged
        );
        publishWorklogCompletedEventIfNeeded(worklogId, request.statusCode(), statusChanged);

        worklogDependencyService.replacePredecessors(
                worklogId,
                worklog.getTeamId(),
                request.predecessorWorklogIds()
        );

        removeManualTags(worklogId, request.removeTagIds());
        registerAdditionalManualTags(worklogId, request.tagIds());

        if (request.removeFileIds() != null && !request.removeFileIds().isEmpty()) {
            fileService.softDeleteWorklogFiles(worklogId, request.removeFileIds());
        }

        fileService.uploadWorklogFiles(worklogId, principal.userId(), newFiles);
    }

    /**
     * 작성자 본인의 상태 변경 요청만 허용하고 상태 값/완료일/상태 이력을 함께 갱신한다.
     *
     * @param principal 현재 로그인 사용자
     * @param worklogId 상태 변경 대상 worklog ID
     * @param request   변경할 상태와 변경 사유
     * @throws BusinessException WORKLOG_NOT_FOUND        worklog 가 없거나 소프트 삭제됨
     * @throws BusinessException WORKLOG_EDIT_FORBIDDEN   작성자 본인이 아님
     * @throws BusinessException WORKLOG_STATUS_TRANSITION_INVALID 현재 상태에서 요청 상태로 전이할 수 없음
     */
    @Transactional
    public void updateWorklogStatus(CustomUserPrincipal principal,
                                    Long worklogId,
                                    UpdateWorklogStatusApiDto.Request request) {
        Worklog worklog = getEditableWorklogOrThrow(principal, worklogId);
        WorklogStatus previousStatusCode = worklog.getStatusCode();
        boolean statusChanged = request.statusCode() != previousStatusCode;

        validateStatusTransitionIfChanged(previousStatusCode, request.statusCode(), statusChanged);
        if (!statusChanged) {
            return;
        }

        worklog.changeStatus(request.statusCode());
        createStatusHistoryIfChanged(
                worklogId,
                previousStatusCode,
                request.statusCode(),
                principal.userId(),
                request.reason(),
                true
        );
        publishWorklogCompletedEventIfNeeded(worklogId, request.statusCode(), true);
    }

    /**
     * 실패한 AI 요약 처리를 작성자 본인이 다시 요청한다.
     * 기존 등록 후처리 이벤트를 재사용하되 첨부 파일 요약은 재요청하지 않는다.
     */
    @Transactional
    public void retryAiSummary(CustomUserPrincipal principal, Long worklogId) {
        Worklog worklog = getEditableWorklogOrThrow(principal, worklogId);
        if (worklog.getAiProcessingStatus() != AiProcessingStatus.FAILED) {
            throw new BusinessException(ErrorCode.WORKLOG_AI_RETRY_STATUS_INVALID);
        }

        Team team = teamService.getTeamOrThrow(worklog.getTeamId());
        worklog.startAiProcessing();
        publishWorklogAiPostProcessRequest(worklog, team, List.of());
    }

    /**
     * 수정 계열 API 의 공통 작성자 경계로, 없는 업무와 작성자 불일치를 각각 표준 예외로 변환한다.
     */
    private Worklog getEditableWorklogOrThrow(CustomUserPrincipal principal, Long worklogId) {
        Worklog worklog = worklogRepository.findById(worklogId)
                .filter(w -> !Boolean.TRUE.equals(w.getIsDeleted()))
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKLOG_NOT_FOUND));

        if (!worklog.getAuthorId().equals(principal.userId())) {
            throw new BusinessException(ErrorCode.WORKLOG_EDIT_FORBIDDEN);
        }

        return worklog;
    }

    /**
     * 클라이언트 우회 요청도 프론트 수정 화면과 같은 상태 전이 규칙을 통과한 경우에만 저장한다.
     */
    private void validateStatusTransitionIfChanged(WorklogStatus previousStatusCode,
                                                   WorklogStatus nextStatusCode,
                                                   boolean statusChanged) {
        if (!statusChanged) {
            return;
        }
        if (!worklogStatusPolicy.canTransition(previousStatusCode, nextStatusCode)) {
            throw new BusinessException(ErrorCode.WORKLOG_STATUS_TRANSITION_INVALID);
        }
    }

    /**
     * 상태 값이 실제 변경된 요청만 상태 이력으로 남겨 수정 저장과 이력 표시를 동기화한다.
     */
    private void createStatusHistoryIfChanged(Long worklogId,
                                              WorklogStatus previousStatusCode,
                                              WorklogStatus newStatusCode,
                                              Long changedBy,
                                              String reason,
                                              boolean statusChanged) {
        if (!statusChanged) {
            return;
        }

        worklogStatusHistoryService.createStatusHistory(
                worklogId,
                previousStatusCode,
                newStatusCode,
                changedBy,
                normalizeStatusChangeReason(reason)
        );
    }

    /**
     * 빈 상태 변경 사유는 이력 조회에서 무의미한 공백으로 보이지 않도록 null 로 정규화한다.
     */
    private String normalizeStatusChangeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return null;
        }

        return reason.trim();
    }

    /**
     * 완료 전환 후속 알림 판정은 커밋 이후 이벤트 흐름에서만 수행해 본문 수정 트랜잭션의 책임을 상태 저장으로 제한한다.
     */
    private void publishWorklogCompletedEventIfNeeded(Long worklogId,
                                                      WorklogStatus nextStatusCode,
                                                      boolean statusChanged) {
        if (!(statusChanged && nextStatusCode == WorklogStatus.COMPLETED)) {
            return;
        }
        eventPublisher.publishEvent(new WorklogCompletedEvent(worklogId));
    }


    /**
     * 수정 요청에서 삭제를 명시한 태그만 연결 해제한다.
     */
    private void removeManualTags(Long worklogId, List<Long> removeTagIds) {
        List<Long> normalizedRemoveTagIds = tagService.normalizeExistingTagIds(removeTagIds);
        if (normalizedRemoveTagIds.isEmpty()) {
            return;
        }

        List<Long> linkedRemoveTagIds = worklogTagRepository
                .findByWorklogIdAndTagIdIn(worklogId, normalizedRemoveTagIds)
                .stream()
                .map(WorklogTag::getTagId)
                .toList();
        if (linkedRemoveTagIds.isEmpty()) {
            return;
        }

        worklogTagRepository.deleteByWorklogIdAndTagIdIn(worklogId, linkedRemoveTagIds);
        tagService.decrementUsageCountByIds(linkedRemoveTagIds);
    }

    /**
     * 최종 선택 태그 목록에서 이미 연결된 태그를 제외하고 새 수동 태그만 추가한다.
     */
    private void registerAdditionalManualTags(Long worklogId, List<Long> tagIds) {
        List<Long> normalizedTagIds = tagService.normalizeExistingTagIds(tagIds);
        if (normalizedTagIds.isEmpty()) {
            return;
        }

        Set<Long> linkedTagIds = worklogTagRepository.findByWorklogIdAndTagIdIn(worklogId, normalizedTagIds)
                .stream()
                .map(WorklogTag::getTagId)
                .collect(Collectors.toSet());
        List<Long> newTagIds = normalizedTagIds.stream()
                .filter(tagId -> !linkedTagIds.contains(tagId))
                .toList();
        if (newTagIds.isEmpty()) {
            return;
        }

        List<WorklogTag> worklogTags = newTagIds.stream()
                .map(tagId -> WorklogTag.create(worklogId, tagId))
                .toList();
        worklogTagRepository.saveAll(worklogTags);
        tagService.incrementUsageCountByIds(newTagIds);
    }

    private void validateDateRange(LocalDate instructionDate, LocalDate dueDate) {
        if (instructionDate != null && dueDate != null && dueDate.isBefore(instructionDate)) {
            throw new BusinessException(ErrorCode.WORKLOG_INVALID_DATE_RANGE);
        }
    }
}
