package com.ibank.axwms.domain.organization.department.controller;

import com.ibank.axwms.domain.organization.department.dto.GetDepartmentsApiDto;
import com.ibank.axwms.domain.organization.department.service.DepartmentService;
import com.ibank.axwms.global.response.EmptyResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/departments")
@RequiredArgsConstructor
public class DepartmentController implements DepartmentControllerDocs {

    private final DepartmentService departmentService;

    @Override
    @GetMapping
    @PreAuthorize("hasRole('DIRECTOR')")
    public GetDepartmentsApiDto.Response getDepartments() {
        return departmentService.getDepartments();
    }

    @Override
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('DIRECTOR')")
    public EmptyResponse deleteDepartment(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
        return EmptyResponse.INSTANCE;
    }
}
