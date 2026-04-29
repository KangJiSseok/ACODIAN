package com.ibank.axwms.domain.organization.evaluation.controller;

import com.ibank.axwms.domain.organization.evaluation.dto.GetUserEvaluationsApiDto;
import com.ibank.axwms.domain.organization.evaluation.service.UserEvaluationService;
import com.ibank.axwms.global.response.PageResponse;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserEvaluationController implements UserEvaluationControllerDocs {

    private final UserEvaluationService userEvaluationService;

    @Override
    @GetMapping("/{id}/evaluations")
    @PreAuthorize("hasAnyRole('DIRECTOR','DEPT_HEAD')")
    public PageResponse<GetUserEvaluationsApiDto.Response.Item> getUserEvaluations(@AuthenticationPrincipal CustomUserPrincipal principal,
                                                                                   @PathVariable Long id,
                                                                                   @Valid @ModelAttribute GetUserEvaluationsApiDto.Request request) {
        return userEvaluationService.getUserEvaluations(principal, id, request);
    }
}
