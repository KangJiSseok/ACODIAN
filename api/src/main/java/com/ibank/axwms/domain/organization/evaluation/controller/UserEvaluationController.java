package com.ibank.axwms.domain.organization.evaluation.controller;

import com.ibank.axwms.domain.organization.evaluation.dto.CreateUserEvaluationApiDto;
import com.ibank.axwms.domain.organization.evaluation.dto.GetUserEvaluationsApiDto;
import com.ibank.axwms.domain.organization.evaluation.dto.UpdateUserEvaluationApiDto;
import com.ibank.axwms.domain.organization.evaluation.service.UserEvaluationService;
import com.ibank.axwms.global.response.EmptyResponse;
import com.ibank.axwms.global.response.PageResponse;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users/{userId}/evaluations")
@RequiredArgsConstructor
public class UserEvaluationController implements UserEvaluationControllerDocs {

    private final UserEvaluationService userEvaluationService;

    @Override
    @GetMapping
    @PreAuthorize("hasAnyRole('DIRECTOR','DEPT_HEAD')")
    public PageResponse<GetUserEvaluationsApiDto.EvaluationSummary> getUserEvaluations(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long userId,
            @Valid @ModelAttribute GetUserEvaluationsApiDto.Request request
    ) {
        return userEvaluationService.getUserEvaluations(principal, userId, request);
    }

    @Override
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('DIRECTOR','DEPT_HEAD')")
    public EmptyResponse createUserEvaluation(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long userId,
            @Valid @RequestBody CreateUserEvaluationApiDto.Request request
    ) {
        userEvaluationService.createUserEvaluation(principal, userId, request);
        return EmptyResponse.INSTANCE;
    }

    @Override
    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRECTOR','DEPT_HEAD')")
    public EmptyResponse updateUserEvaluation(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long userId,
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserEvaluationApiDto.Request request
    ) {
        userEvaluationService.updateUserEvaluation(principal, userId, id, request);
        return EmptyResponse.INSTANCE;
    }
}
