package com.ibank.axwms.domain.organization.department.controller;

import com.ibank.axwms.domain.organization.department.dto.GetDepartmentsApiDto;
import com.ibank.axwms.domain.organization.department.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
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
}
