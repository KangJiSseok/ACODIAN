package com.ibank.axwms.domain.organization.department.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.ibank.axwms.domain.organization.department.dto.CreateDepartmentApiDto;
import com.ibank.axwms.domain.organization.department.dto.GetDepartmentsApiDto;
import com.ibank.axwms.domain.organization.department.service.DepartmentService;
import com.ibank.axwms.global.response.EmptyResponse;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@ExtendWith(MockitoExtension.class)
class DepartmentControllerTest {

    @Mock
    private DepartmentService departmentService;

    @InjectMocks
    private DepartmentController departmentController;

    @Test
    @DisplayName("부서 컨트롤러는 /departments 기본 경로를 사용한다")
    void 부서_컨트롤러는_departments_기본_경로를_사용한다() {
        RequestMapping requestMapping = DepartmentController.class.getAnnotation(RequestMapping.class);

        assertThat(requestMapping).isNotNull();
        assertThat(requestMapping.value()).containsExactly("/departments");
    }

    @Test
    @DisplayName("활성 부서 목록 조회 메서드는 GET 매핑과 DIRECTOR 권한을 사용한다")
    void 활성_부서_목록_조회_메서드는_get_매핑과_director_권한을_사용한다() throws NoSuchMethodException {
        Method method = DepartmentController.class.getMethod("getDepartments");
        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(getMapping).isNotNull();
        assertThat(getMapping.value()).isEmpty();
        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("hasRole('DIRECTOR')");
    }

    @Test
    @DisplayName("활성 부서 목록 조회 메서드는 서비스 결과를 그대로 반환한다")
    void 활성_부서_목록_조회_메서드는_서비스_결과를_그대로_반환한다() {
        GetDepartmentsApiDto.Response responseFromService = GetDepartmentsApiDto.Response.of(
                2,
                5,
                18,
                List.of(new GetDepartmentsApiDto.Response.DepartmentSummary(
                        10L,
                        "물류본부",
                        "전사 물류 운영 총괄",
                        1001L,
                        "박본부",
                        LocalDateTime.of(2026, 4, 1, 9, 0),
                        LocalDateTime.of(2026, 4, 20, 9, 0)
                ))
        );
        given(departmentService.getDepartments()).willReturn(responseFromService);

        GetDepartmentsApiDto.Response response = departmentController.getDepartments();

        assertThat(response.activeDepartmentCount()).isEqualTo(2);
        assertThat(response.activeTeamCount()).isEqualTo(5);
        assertThat(response.activeUserCount()).isEqualTo(18);
        assertThat(response.departments()).containsExactlyElementsOf(responseFromService.departments());
    }

    @Test
    @DisplayName("부서 삭제 메서드는 DELETE 경로와 DIRECTOR 권한을 사용한다")
    void 부서_삭제_메서드는_delete_경로와_director_권한을_사용한다() throws NoSuchMethodException {
        Method method = DepartmentController.class.getMethod("deleteDepartment", Long.class);
        DeleteMapping deleteMapping = method.getAnnotation(DeleteMapping.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(deleteMapping).isNotNull();
        assertThat(deleteMapping.value()).containsExactly("/{id}");
        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("hasRole('DIRECTOR')");
    }

    @Test
    @DisplayName("부서 삭제 메서드는 서비스를 호출하고 EmptyResponse 를 반환한다")
    void 부서_삭제_메서드는_서비스를_호출하고_EmptyResponse를_반환한다() {
        EmptyResponse response = departmentController.deleteDepartment(10L);

        then(departmentService).should().deleteDepartment(10L);
        assertThat(response).isSameAs(EmptyResponse.INSTANCE);
    }

    @Test
    @DisplayName("부서 등록 메서드는 POST 매핑과 201 응답 상태와 DIRECTOR 권한을 사용한다")
    void 부서_등록_메서드는_post_매핑과_201_응답_상태와_director_권한을_사용한다() throws NoSuchMethodException {
        Method method = DepartmentController.class.getMethod("createDepartment", CreateDepartmentApiDto.Request.class);
        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        ResponseStatus responseStatus = method.getAnnotation(ResponseStatus.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(postMapping).isNotNull();
        assertThat(postMapping.value()).isEmpty();
        assertThat(responseStatus).isNotNull();
        assertThat(responseStatus.value()).isEqualTo(HttpStatus.CREATED);
        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("hasRole('DIRECTOR')");
    }

    @Test
    @DisplayName("부서 등록 메서드는 서비스를 호출하고 EmptyResponse 를 반환한다")
    void 부서_등록_메서드는_서비스를_호출하고_EmptyResponse를_반환한다() {
        CreateDepartmentApiDto.Request request = new CreateDepartmentApiDto.Request("플랫폼전략본부", "전사 전략", 1001L);

        EmptyResponse response = departmentController.createDepartment(request);

        then(departmentService).should().createDepartment(request);
        assertThat(response).isSameAs(EmptyResponse.INSTANCE);
    }
}
