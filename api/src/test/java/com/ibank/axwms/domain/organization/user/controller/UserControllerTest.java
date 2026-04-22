package com.ibank.axwms.domain.organization.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibank.axwms.domain.organization.user.dto.GetMyProfileApiDto;
import com.ibank.axwms.domain.organization.user.service.UserService;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import java.lang.reflect.Method;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private ObjectMapper objectMapper;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("현재 사용자 조회 컨트롤러는 /api/users 기본 경로를 사용한다")
    void 현재_사용자_조회_컨트롤러는_api_users_기본_경로를_사용한다() {
        RequestMapping requestMapping = UserController.class.getAnnotation(RequestMapping.class);

        assertThat(requestMapping).isNotNull();
        assertThat(requestMapping.value()).containsExactly("/api/users");
    }

    @Test
    @DisplayName("현재 사용자 조회 메서드는 /me GET 매핑을 사용한다")
    void 현재_사용자_조회_메서드는_me_get_매핑을_사용한다() throws NoSuchMethodException {
        Method method = UserController.class.getMethod("getMyProfile", CustomUserPrincipal.class);
        GetMapping getMapping = method.getAnnotation(GetMapping.class);

        assertThat(getMapping).isNotNull();
        assertThat(getMapping.value()).containsExactly("/me");
    }

    @Test
    @DisplayName("현재 사용자 조회 메서드는 서비스 결과를 응답 DTO 로 변환한다")
    void 현재_사용자_조회_메서드는_서비스_결과를_응답_DTO로_변환한다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(101L, "user@ibank.com", "MEMBER");
        GetMyProfileApiDto.Response responseFromService = new GetMyProfileApiDto.Response(
                101L,
                "홍길동",
                10L,
                "물류본부",
                "과장",
                "팀장",
                "https://cdn.axwms.com/profile/101.png",
                java.util.List.of(new GetMyProfileApiDto.TeamSummary(true, 21L, "물류혁신TF"))
        );
        given(userService.getMyProfile(principal)).willReturn(responseFromService);

        GetMyProfileApiDto.Response response = userController.getMyProfile(principal);

        assertThat(response.userId()).isEqualTo(101L);
        assertThat(response.userName()).isEqualTo("홍길동");
        assertThat(response.departmentId()).isEqualTo(10L);
        assertThat(response.departmentName()).isEqualTo("물류본부");
        assertThat(response.teams()).containsExactly(new GetMyProfileApiDto.TeamSummary(true, 21L, "물류혁신TF"));
    }

    @Test
    @DisplayName("팀 요약 응답은 isPrimary 필드를 먼저 직렬화한다")
    void 팀_요약_응답은_isPrimary_필드를_먼저_직렬화한다() throws Exception {
        GetMyProfileApiDto.TeamSummary teamSummary = new GetMyProfileApiDto.TeamSummary(true, 21L, "물류혁신TF");

        String json = objectMapper.writeValueAsString(teamSummary);

        assertThat(json).startsWith("{\"isPrimary\":true,\"teamId\":21,\"teamName\":\"물류혁신TF\"");
    }
}
