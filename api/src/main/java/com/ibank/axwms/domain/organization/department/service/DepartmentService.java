package com.ibank.axwms.domain.organization.department.service;

import com.ibank.axwms.domain.organization.department.DepartmentStatus;
import com.ibank.axwms.domain.organization.department.dto.CreateDepartmentApiDto;
import com.ibank.axwms.domain.organization.department.dto.GetDepartmentCandidatesApiDto;
import com.ibank.axwms.domain.organization.department.dto.GetDepartmentDetailApiDto;
import com.ibank.axwms.domain.organization.department.dto.GetDepartmentsApiDto;
import com.ibank.axwms.domain.organization.department.dto.UpdateDepartmentApiDto;
import com.ibank.axwms.domain.organization.department.entity.Department;
import com.ibank.axwms.domain.organization.department.repository.DepartmentRepository;
import com.ibank.axwms.domain.organization.department.repository.jooq.projection.DepartmentDetailHeaderProjection;
import com.ibank.axwms.domain.organization.department.repository.jooq.projection.DepartmentListItemProjection;
import com.ibank.axwms.domain.organization.department.repository.jooq.projection.DepartmentOverviewProjection;
import com.ibank.axwms.domain.organization.user.UserRole;
import com.ibank.axwms.domain.organization.user.entity.User;
import com.ibank.axwms.domain.organization.user.repository.UserRepository;
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;

    /**
     * 활성 부서 목록 화면이 필요한 상단 집계와 부서 목록을 함께 조립해 반환한다.
     * 집계와 목록을 같은 서비스에서 묶어 프론트엔드가 부서 대시보드 진입 시 한 번의 호출로 필요한 데이터를 확보하게 한다.
     */
    public GetDepartmentsApiDto.Response getDepartments() {
        DepartmentOverviewProjection overview = departmentRepository.getActiveDepartmentOverview();
        List<GetDepartmentsApiDto.Response.DepartmentSummary> departments = departmentRepository.findActiveDepartments().stream()
                .map(this::toDepartmentSummary)
                .toList();

        return GetDepartmentsApiDto.Response.of(
                overview.activeDepartmentCount(),
                overview.activeTeamCount(),
                overview.activeUserCount(),
                departments
        );
    }

    /**
     * 권한별 부서 선택 후보를 최소 필드로 반환한다.
     * DIRECTOR 는 전체 ACTIVE 부서를, DEPT_HEAD 는 자기 주 소속 ACTIVE 부서만 후보로 볼 수 있다.
     */
    public GetDepartmentCandidatesApiDto.Response getDepartmentCandidates(CustomUserPrincipal principal) {
        if (isDirector(principal)) {
            return GetDepartmentCandidatesApiDto.Response.of(
                    departmentRepository.findAllByStatusCodeOrderByIdAsc(DepartmentStatus.ACTIVE).stream()
                            .map(GetDepartmentCandidatesApiDto.Response.DepartmentCandidate::from)
                            .toList()
            );
        }

        User currentUser = getUserOrThrow(principal.userId());
        if (currentUser.getDepartmentId() == null) {
            return GetDepartmentCandidatesApiDto.Response.of(List.of());
        }

        return GetDepartmentCandidatesApiDto.Response.of(
                departmentRepository.findAllByIdAndStatusCodeOrderByIdAsc(
                                currentUser.getDepartmentId(),
                                DepartmentStatus.ACTIVE
                        )
                        .stream()
                        .map(GetDepartmentCandidatesApiDto.Response.DepartmentCandidate::from)
                        .toList()
        );
    }

    /** ACTIVE 부서 header 와 nullable ownership 팀 목록을 조립해 상세 화면 응답을 반환한다. */
    public GetDepartmentDetailApiDto.Response getDepartmentDetail(Long departmentId) {
        DepartmentDetailHeaderProjection header = departmentRepository.findActiveDepartmentDetailHeader(departmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEPARTMENT_NOT_FOUND));
        List<GetDepartmentDetailApiDto.Response.TeamSummary> teams = departmentRepository.findActiveDepartmentDetailTeams(departmentId)
                .stream()
                .map(GetDepartmentDetailApiDto.Response.TeamSummary::from)
                .toList();

        return GetDepartmentDetailApiDto.Response.of(
                header.departmentId(),
                header.departmentName(),
                header.departmentHeadUserId(),
                header.departmentHeadUserName(),
                teams
        );
    }

    /** 활성 부서의 기본 정보와 부서장을 수정한다. null head 또는 필드 생략은 부서장 해제로 처리한다. */
    @Transactional
    public void updateDepartment(Long departmentId, UpdateDepartmentApiDto.Request request) {
        Department department = getActiveDepartment(departmentId);
        ensureDepartmentNameAvailable(request.departmentName(), departmentId);
        Long departmentHeadUserId = resolveDepartmentHeadUserId(request.departmentHeadUserId(), departmentId);
        ensureDepartmentHeadUserAvailable(departmentHeadUserId, departmentId);

        department.updateBasicInfo(request.departmentName(), request.description());
        department.assignHeadUserId(departmentHeadUserId);
    }

    /** 요청한 부서를 soft-delete 한다. 이미 INACTIVE 면 no-op 성공으로 처리한다. */
    @Transactional
    public void deleteDepartment(Long departmentId) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEPARTMENT_NOT_FOUND));

        if (department.getStatusCode() == DepartmentStatus.INACTIVE) {
            return;
        }

        ensureNoActiveOwnedTeam(departmentId);
        department.changeStatus(DepartmentStatus.INACTIVE);
    }

    /** 새 부서를 등록한다. 부서명/부서장 UNIQUE 와 부서장 사용자 존재 여부를 사전에 검증한다. */
    @Transactional
    public void createDepartment(CreateDepartmentApiDto.Request request) {
        ensureDepartmentNameAvailable(request.departmentName());
        Long validatedDepartmentHeadUserId = validateDepartmentHeadAssignmentCandidate(request.departmentHeadUserId());
        ensureDepartmentHeadUserAvailable(validatedDepartmentHeadUserId);

        Department department = Department.create(request.departmentName(), request.description());
        department.assignHeadUserId(validatedDepartmentHeadUserId);
        departmentRepository.save(department);
    }

    /**  외부 도메인이 활성 부서 가입 가능 여부를 재사용할 수 있도록 검증 경계를 노출한다. */
    public void validateActiveDepartment(Long departmentId) {
        getActiveDepartment(departmentId);
    }

    /**
     * 외부 도메인(예: dashboard)이 부서 entity 를 직접 import 하지 않고도 활성 부서명만 조회할 수 있도록 노출한다.
     * "ACTIVE 부서만" 룰은 service 가 보유 — 호출자는 부서 존재성/활성 여부 결정 없이 결과 Optional 만 다룬다.
     */
    public Optional<String> findActiveDepartmentName(Long departmentId) {
        return departmentRepository.findByIdAndStatusCode(departmentId, DepartmentStatus.ACTIVE)
                .map(Department::getDepartmentName);
    }

    /** JOOQ projection 을 API 응답용 부서 요약 record 로 변환한다. */
    private GetDepartmentsApiDto.Response.DepartmentSummary toDepartmentSummary(DepartmentListItemProjection department) {
        return new GetDepartmentsApiDto.Response.DepartmentSummary(
                department.departmentId(),
                department.departmentName(),
                department.description(),
                department.departmentHeadUserId(),
                department.departmentHeadUserName(),
                department.createdAt(),
                department.updatedAt()
        );
    }

    /** Controller role gate 를 통과한 principal 중 전사 후보 범위를 가진 DIRECTOR 인지 확인한다. */
    private boolean isDirector(CustomUserPrincipal principal) {
        return UserRole.DIRECTOR.name().equals(principal.roleCode());
    }

    /** principal 의 사용자 문맥이 DB 에 남아 있는지 확인하고 DEPT_HEAD 부서 범위 산정에 사용한다. */
    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    /** 수정 대상은 ACTIVE 부서만 허용한다. */
    private Department getActiveDepartment(Long departmentId) {
        return departmentRepository.findByIdAndStatusCode(departmentId, DepartmentStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEPARTMENT_NOT_FOUND));
    }

    /** 부서가 직접 소유한 ACTIVE/non-deleted 팀이 남아 있으면 삭제를 차단한다. */
    private void ensureNoActiveOwnedTeam(Long departmentId) {
        if (departmentRepository.existsActiveOwnedTeam(departmentId)) {
            throw new BusinessException(ErrorCode.DEPARTMENT_HAS_ACTIVE_TEAMS);
        }
    }

    /** 신규 등록 전에 department_name UNIQUE 충돌을 확인한다. */
    private void ensureDepartmentNameAvailable(String departmentName) {
        if (departmentRepository.existsByDepartmentName(departmentName)) {
            throw new BusinessException(ErrorCode.DEPARTMENT_DUPLICATE_NAME);
        }
    }

    /** 자기 자신을 제외한 department_name UNIQUE 충돌을 확인한다. */
    private void ensureDepartmentNameAvailable(String departmentName, Long departmentId) {
        if (departmentRepository.existsByDepartmentNameAndIdNot(departmentName, departmentId)) {
            throw new BusinessException(ErrorCode.DEPARTMENT_DUPLICATE_NAME);
        }
    }

    /** 부서장 후보 사용자 ID 를 검증하고, 허용 가능한 head 후보면 같은 ID 를 반환한다. */
    private Long validateDepartmentHeadAssignmentCandidate(Long departmentHeadUserId) {
        if (departmentHeadUserId == null) {
            return null;
        }

        User departmentHeadUser = userRepository.findById(departmentHeadUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (!isAssignableDepartmentHeadRole(departmentHeadUser.getRoleCode())) {
            throw new BusinessException(ErrorCode.DEPARTMENT_INVALID_HEAD_USER_ROLE);
        }
        return departmentHeadUserId;
    }

    /** head 사용자 입력이 있을 때만 존재, 허용 역할, 수정 대상 부서 소속 여부를 검증하고 없으면 null 을 그대로 반환한다. */
    private Long resolveDepartmentHeadUserId(Long departmentHeadUserId, Long departmentId) {
        if (departmentHeadUserId == null) {
            return null;
        }

        User user = userRepository.findById(departmentHeadUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (!isAssignableDepartmentHeadRole(user.getRoleCode())) {
            throw new BusinessException(ErrorCode.DEPARTMENT_HEAD_ROLE_NOT_ALLOWED);
        }
        if (!departmentId.equals(user.getDepartmentId())) {
            throw new BusinessException(ErrorCode.DEPARTMENT_HEAD_USER_DEPARTMENT_MISMATCH);
        }
        return departmentHeadUserId;
    }

    /** 부서장으로 지정 가능한 역할인지 확인한다. */
    private boolean isAssignableDepartmentHeadRole(UserRole roleCode) {
        return roleCode == UserRole.DEPT_HEAD || roleCode == UserRole.DIRECTOR;
    }

    /** 같은 사용자가 다른 부서 head 로 이미 지정되어 있으면 등록을 차단한다. */
    private void ensureDepartmentHeadUserAvailable(Long departmentHeadUserId) {
        if (departmentHeadUserId != null && departmentRepository.existsByDepartmentHeadUserId(departmentHeadUserId)) {
            throw new BusinessException(ErrorCode.DEPARTMENT_DUPLICATE_HEAD_USER);
        }
    }

    /** 허용 역할 사용자에 한해 같은 사용자가 다른 부서 head 로 이미 지정되어 있으면 수정을 차단한다. */
    private void ensureDepartmentHeadUserAvailable(Long departmentHeadUserId, Long departmentId) {
        if (departmentHeadUserId != null
                && departmentRepository.existsByDepartmentHeadUserIdAndIdNot(departmentHeadUserId, departmentId)) {
            throw new BusinessException(ErrorCode.DEPARTMENT_DUPLICATE_HEAD_USER);
        }
    }
}
