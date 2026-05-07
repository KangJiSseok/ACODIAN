package com.ibank.axwms.domain.organization.user.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.ibank.axwms.domain.organization.department.entity.Department;
import com.ibank.axwms.domain.organization.department.repository.DepartmentRepository;
import com.ibank.axwms.domain.organization.user.EmploymentStatus;
import com.ibank.axwms.domain.organization.user.UserRole;
import com.ibank.axwms.domain.organization.user.dto.UpdateMyProfileApiDto;
import com.ibank.axwms.domain.organization.user.entity.User;
import com.ibank.axwms.domain.organization.user.repository.UserRepository;
import com.ibank.axwms.domain.organization.user.service.ProfileImageStorageService.FinalUploadResult;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import com.ibank.axwms.testsupport.IntegrationTestSupport;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

class UserServiceTransactionTest extends IntegrationTestSupport {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @MockitoBean
    private ProfileImageStorageService profileImageStorageService;

    @BeforeEach
    void setUp() {
        userRepository.deleteAllInBatch();
        departmentRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("현재 사용자 이미지 수정 커밋이 성공하면 기존 이미지만 afterCommit 에서 삭제한다")
    void 현재_사용자_이미지_수정_커밋이_성공하면_기존_이미지만_afterCommit에서_삭제한다() {
        User user = saveUser("self-commit@ibank.com", "https://cdn.axwms.com/profile/old.png");
        MockMultipartFile profileImage = profileImage();
        given(profileImageStorageService.uploadFinal(profileImage))
                .willReturn(new FinalUploadResult("profile/new.png", "https://cdn.axwms.com/profile/new.png"));
        given(profileImageStorageService.resolveDeletableProfileImageKey("https://cdn.axwms.com/profile/old.png"))
                .willReturn("profile/old.png");

        transactionTemplate.executeWithoutResult(status -> userService.updateMyProfile(
                principal(user),
                new UpdateMyProfileApiDto.Request(null, null, null, null, null, null, null, null),
                profileImage
        ));

        then(profileImageStorageService).should().deleteBestEffort("profile/old.png");
        then(profileImageStorageService).should(never()).deleteBestEffort("profile/new.png");
    }

    @Test
    @DisplayName("현재 사용자 이미지 수정 트랜잭션이 rollback 되면 새 final 이미지만 삭제한다")
    void 현재_사용자_이미지_수정_트랜잭션이_rollback되면_새_final_이미지만_삭제한다() {
        User user = saveUser("self-rollback@ibank.com", "https://cdn.axwms.com/profile/old.png");
        MockMultipartFile profileImage = profileImage();
        given(profileImageStorageService.uploadFinal(profileImage))
                .willReturn(new FinalUploadResult("profile/new.png", "https://cdn.axwms.com/profile/new.png"));
        given(profileImageStorageService.resolveDeletableProfileImageKey("https://cdn.axwms.com/profile/old.png"))
                .willReturn("profile/old.png");

        transactionTemplate.executeWithoutResult(status -> {
            userService.updateMyProfile(
                    principal(user),
                    new UpdateMyProfileApiDto.Request(null, "롤백사용자", null, null, null, null, null, null),
                    profileImage
            );
            status.setRollbackOnly();
        });

        then(profileImageStorageService).should().deleteBestEffort("profile/new.png");
        then(profileImageStorageService).should(never()).deleteBestEffort("profile/old.png");
    }

    @Test
    @DisplayName("flush 제약 실패로 rollback 되면 새 final 이미지 보상 삭제가 실행된다")
    void flush_제약_실패로_rollback되면_새_final_이미지_보상_삭제가_실행된다() {
        User user = saveUser("self-constraint@ibank.com", "https://cdn.axwms.com/profile/old.png");
        saveUser("duplicate@ibank.com", null);
        MockMultipartFile profileImage = profileImage();
        given(profileImageStorageService.uploadFinal(profileImage))
                .willReturn(new FinalUploadResult("profile/new.png", "https://cdn.axwms.com/profile/new.png"));
        given(profileImageStorageService.resolveDeletableProfileImageKey("https://cdn.axwms.com/profile/old.png"))
                .willReturn("profile/old.png");

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> userService.updateMyProfile(
                principal(user),
                new UpdateMyProfileApiDto.Request(null, null, "duplicate@ibank.com", null, null, null, null, null),
                profileImage
        ))).isInstanceOf(RuntimeException.class);

        then(profileImageStorageService).should().deleteBestEffort("profile/new.png");
        then(profileImageStorageService).should(never()).deleteBestEffort("profile/old.png");
    }

    private User saveUser(String email, String profileImageUrl) {
        Department department = departmentRepository.save(Department.create(
                "트랜잭션부서-" + email,
                "이미지 트랜잭션 테스트 부서"
        ));
        return userRepository.saveAndFlush(User.create(
                department.getId(),
                "트랜잭션사용자",
                email,
                "$2a$10$fake-hashed",
                UserRole.MEMBER,
                EmploymentStatus.ACTIVE,
                "과장",
                "팀원",
                LocalDate.of(2025, 1, 1),
                "010-0000-0000",
                profileImageUrl
        ));
    }

    private CustomUserPrincipal principal(User user) {
        return new CustomUserPrincipal(user.getId(), user.getEmail(), user.getRoleCode().name());
    }

    private MockMultipartFile profileImage() {
        return new MockMultipartFile(
                "profile_image",
                "profile.png",
                MediaType.IMAGE_PNG_VALUE,
                "image".getBytes(StandardCharsets.UTF_8)
        );
    }
}
