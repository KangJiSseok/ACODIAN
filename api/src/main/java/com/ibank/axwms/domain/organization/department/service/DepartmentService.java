package com.ibank.axwms.domain.organization.department.service;

import com.ibank.axwms.domain.organization.department.DepartmentStatus;
import com.ibank.axwms.domain.organization.department.dto.GetDepartmentsApiDto;
import com.ibank.axwms.domain.organization.department.entity.Department;
import com.ibank.axwms.domain.organization.department.repository.DepartmentRepository;
import com.ibank.axwms.domain.organization.department.repository.jooq.projection.DepartmentListItemProjection;
import com.ibank.axwms.domain.organization.department.repository.jooq.projection.DepartmentOverviewProjection;
import com.ibank.axwms.domain.organization.team.TeamStatus;
import com.ibank.axwms.domain.organization.team.repository.TeamRepository;
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
}
