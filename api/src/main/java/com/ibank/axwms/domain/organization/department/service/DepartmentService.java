package com.ibank.axwms.domain.organization.department.service;

import com.ibank.axwms.domain.organization.department.dto.GetDepartmentsApiDto;
import com.ibank.axwms.domain.organization.department.repository.DepartmentRepository;
import com.ibank.axwms.domain.organization.department.repository.jooq.projection.DepartmentListItemProjection;
import com.ibank.axwms.domain.organization.department.repository.jooq.projection.DepartmentOverviewProjection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

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
