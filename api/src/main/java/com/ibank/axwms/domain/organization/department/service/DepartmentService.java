package com.ibank.axwms.domain.organization.department.service;

import com.ibank.axwms.domain.organization.department.DepartmentStatus;
import com.ibank.axwms.domain.organization.department.dto.CreateDepartmentApiDto;
import com.ibank.axwms.domain.organization.department.dto.GetDepartmentsApiDto;
import com.ibank.axwms.domain.organization.department.dto.UpdateDepartmentApiDto;
import com.ibank.axwms.domain.organization.department.entity.Department;
import com.ibank.axwms.domain.organization.department.repository.DepartmentRepository;
import com.ibank.axwms.domain.organization.department.repository.jooq.projection.DepartmentListItemProjection;
import com.ibank.axwms.domain.organization.department.repository.jooq.projection.DepartmentOverviewProjection;
import com.ibank.axwms.domain.organization.team.TeamStatus;
import com.ibank.axwms.domain.organization.team.repository.TeamRepository;
import com.ibank.axwms.domain.organization.user.UserRole;
import com.ibank.axwms.domain.organization.user.entity.User;
import com.ibank.axwms.domain.organization.user.repository.UserRepository;
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final TeamRepository teamRepository;
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

    /** 활성 부서의 기본 정보와 부서장을 수정한다. null head 또는 필드 생략은 부서장 해제로 처리한다. */
    @Transactional
    public void updateDepartment(Long departmentId, UpdateDepartmentApiDto.Request request) {
        Department department = getActiveDepartment(departmentId);
        ensureDepartmentNameAvailable(request.departmentName(), departmentId);
        Long departmentHeadUserId = resolveDepartmentHeadUserId(request.departmentHeadUserId());
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

        if (teamRepository.existsByDepartmentIdAndStatusCode(departmentId, TeamStatus.ACTIVE)) {
            throw new BusinessException(ErrorCode.DEPARTMENT_HAS_ACTIVE_TEAMS);
        }

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

    /** 수정 대상은 ACTIVE 부서만 허용한다. */
    private Department getActiveDepartment(Long departmentId) {
        return departmentRepository.findByIdAndStatusCode(departmentId, DepartmentStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEPARTMENT_NOT_FOUND));
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

    /** head 사용자 입력이 있을 때만 존재와 허용 역할을 검증하고, 없으면 null 을 그대로 반환한다. */
    private Long resolveDepartmentHeadUserId(Long departmentHeadUserId) {
        if (departmentHeadUserId == null) {
            return null;
        }

        User user = userRepository.findById(departmentHeadUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (!isAssignableDepartmentHeadRole(user.getRoleCode())) {
            throw new BusinessException(ErrorCode.DEPARTMENT_HEAD_ROLE_NOT_ALLOWED);
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
