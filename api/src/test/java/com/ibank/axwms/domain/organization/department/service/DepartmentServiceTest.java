package com.ibank.axwms.domain.organization.department.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.ibank.axwms.domain.organization.department.dto.GetDepartmentsApiDto;
import com.ibank.axwms.domain.organization.department.repository.DepartmentRepository;
import com.ibank.axwms.domain.organization.department.repository.jooq.projection.DepartmentListItemProjection;
import com.ibank.axwms.domain.organization.department.repository.jooq.projection.DepartmentOverviewProjection;
import java.time.LocalDateTime;
import java.util.List;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private DepartmentService departmentService;

    @Test
    @DisplayName("활성 부서 집계와 목록이 있으면 API 응답 DTO로 조립한다")
    void 활성_부서_집계와_목록이_있으면_api_응답_dto로_조립한다() {
        given(departmentRepository.getActiveDepartmentOverview())
                .willReturn(new DepartmentOverviewProjection(2, 2, 2));
        given(departmentRepository.findActiveDepartments())
                .willReturn(List.of(
                        new DepartmentListItemProjection(
                                10L,
                                "물류본부",
                                "전사 물류 운영 총괄",
                                1001L,
                                "박본부",
                                LocalDateTime.of(2026, 4, 1, 9, 0),
                                LocalDateTime.of(2026, 4, 20, 9, 0)
                        ),
                        new DepartmentListItemProjection(
                                11L,
                                "무부장본부",
                                "부서장 없는 활성 부서",
                                null,
                                null,
                                LocalDateTime.of(2026, 4, 2, 9, 0),
                                LocalDateTime.of(2026, 4, 21, 9, 0)
                        )
                ));

        GetDepartmentsApiDto.Response result = departmentService.getDepartments();

        assertThat(result.activeDepartmentCount()).isEqualTo(2);
        assertThat(result.activeTeamCount()).isEqualTo(2);
        assertThat(result.activeUserCount()).isEqualTo(2);
        assertThat(result.departments())
                .extracting(
                        GetDepartmentsApiDto.Response.DepartmentSummary::departmentId,
                        GetDepartmentsApiDto.Response.DepartmentSummary::departmentName,
                        GetDepartmentsApiDto.Response.DepartmentSummary::departmentHeadUserId,
                        GetDepartmentsApiDto.Response.DepartmentSummary::departmentHeadUserName
                )
                .containsExactly(
                        Tuple.tuple(10L, "물류본부", 1001L, "박본부"),
                        Tuple.tuple(11L, "무부장본부", null, null)
                );
    }
}
