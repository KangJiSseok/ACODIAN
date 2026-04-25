package com.ibank.axwms.domain.organization.department.controller;

import com.ibank.axwms.domain.organization.department.dto.CreateDepartmentApiDto;
import com.ibank.axwms.domain.organization.department.dto.GetDepartmentsApiDto;
import com.ibank.axwms.domain.organization.department.dto.UpdateDepartmentApiDto;
import com.ibank.axwms.domain.organization.department.service.DepartmentService;
import com.ibank.axwms.global.response.EmptyResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
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
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('DIRECTOR')")
    public EmptyResponse updateDepartment(@PathVariable Long id, @Valid @RequestBody UpdateDepartmentApiDto.Request request) {
        departmentService.updateDepartment(id, request);
        return EmptyResponse.INSTANCE;
    }

    @Override
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('DIRECTOR')")
    public EmptyResponse deleteDepartment(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
        return EmptyResponse.INSTANCE;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('DIRECTOR')")
    public EmptyResponse createDepartment(@Valid @RequestBody CreateDepartmentApiDto.Request request) {
        departmentService.createDepartment(request);
        return EmptyResponse.INSTANCE;
    }
}
