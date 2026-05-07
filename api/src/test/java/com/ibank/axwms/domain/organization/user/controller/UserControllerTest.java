package com.ibank.axwms.domain.organization.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.ibank.axwms.domain.organization.user.EmploymentStatus;
import com.ibank.axwms.domain.organization.user.dto.GetAdminCandidatesApiDto;
import com.ibank.axwms.domain.organization.user.dto.GetDepartmentCandidatesApiDto;
import com.ibank.axwms.domain.organization.user.dto.GetMyProfileApiDto;
import com.ibank.axwms.domain.organization.user.dto.GetUserApiDto;
import com.ibank.axwms.domain.organization.user.dto.GetUsersApiDto;
import com.ibank.axwms.domain.organization.user.dto.UpdateMyProfileApiDto;
import com.ibank.axwms.domain.organization.user.dto.UpdateUserApiDto;
import com.ibank.axwms.domain.organization.user.service.UserService;
import com.ibank.axwms.global.response.EmptyResponse;
import com.ibank.axwms.global.response.GlobalResponseAdvice;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private ObjectMapper objectMapper;
    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalResponseAdvice())
                .build();
    }

    @Test
    @DisplayName("현재 사용자 조회 컨트롤러는 /users 기본 경로를 사용한다")
    void 현재_사용자_조회_컨트롤러는_users_기본_경로를_사용한다() {
        RequestMapping requestMapping = UserController.class.getAnnotation(RequestMapping.class);

        assertThat(requestMapping).isNotNull();
        assertThat(requestMapping.value()).containsExactly("/users");
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
    @DisplayName("사용자 상세 조회 메서드는 /{id} GET 매핑을 사용한다")
    void 사용자_상세_조회_메서드는_id_GET_매핑을_사용한다() throws NoSuchMethodException {
        Method method = UserController.class.getMethod("getUser", CustomUserPrincipal.class, Long.class);
        GetMapping getMapping = method.getAnnotation(GetMapping.class);

        assertThat(getMapping).isNotNull();
        assertThat(getMapping.value()).containsExactly("/{id}");
    }

    @Test
    @DisplayName("사용자 상세 조회 id 는 PathVariable 로 바인딩한다")
    void 사용자_상세_조회_id는_PathVariable로_바인딩한다() throws NoSuchMethodException {
        Method method = UserController.class.getMethod("getUser", CustomUserPrincipal.class, Long.class);

        assertThat(method.getParameters()[1].getAnnotation(PathVariable.class)).isNotNull();
    }

    @Test
    @DisplayName("현재 사용자 부분 수정 메서드는 /me PATCH 매핑을 사용한다")
    void 현재_사용자_부분_수정_메서드는_me_PATCH_매핑을_사용한다() throws NoSuchMethodException {
        Method method = UserController.class.getMethod("updateMyProfile", CustomUserPrincipal.class, UpdateMyProfileApiDto.Request.class, MultipartFile.class);
        PatchMapping patchMapping = method.getAnnotation(PatchMapping.class);

        assertThat(patchMapping).isNotNull();
        assertThat(patchMapping.value()).containsExactly("/me");
        assertThat(patchMapping.consumes()).containsExactly(MediaType.MULTIPART_FORM_DATA_VALUE);
    }

    @Test
    @DisplayName("현재 사용자 부분 수정 메서드는 별도 role gate 를 선언하지 않는다")
    void 현재_사용자_부분_수정_메서드는_별도_role_gate를_선언하지_않는다() throws NoSuchMethodException {
        Method method = UserController.class.getMethod("updateMyProfile", CustomUserPrincipal.class, UpdateMyProfileApiDto.Request.class, MultipartFile.class);

        assertThat(method.getAnnotation(PreAuthorize.class)).isNull();
    }

    @Test
    @DisplayName("현재 사용자 부분 수정 요청은 RequestPart 로 바인딩한다")
    void 현재_사용자_부분_수정_요청은_RequestPart로_바인딩한다() throws NoSuchMethodException {
        Method method = UserController.class.getMethod("updateMyProfile", CustomUserPrincipal.class, UpdateMyProfileApiDto.Request.class, MultipartFile.class);

        assertThat(method.getParameters()[1].getAnnotation(RequestPart.class).value()).isEqualTo("request");
        RequestPart profileImagePart = method.getParameters()[2].getAnnotation(RequestPart.class);
        assertThat(profileImagePart.value()).isEqualTo("profile_image");
        assertThat(profileImagePart.required()).isFalse();
    }

    @Test
    @DisplayName("관리자 후보 조회 메서드는 /admin-candidates GET 매핑을 사용한다")
    void 관리자_후보_조회_메서드는_admin_candidates_get_매핑을_사용한다() throws NoSuchMethodException {
        Method method = UserController.class.getMethod("getAdminCandidates", CustomUserPrincipal.class);
        GetMapping getMapping = method.getAnnotation(GetMapping.class);

        assertThat(getMapping).isNotNull();
        assertThat(getMapping.value()).containsExactly("/admin-candidates");
    }

    @Test
    @DisplayName("관리자 후보 조회 메서드는 DIRECTOR 와 DEPT_HEAD 만 허용한다")
    void 관리자_후보_조회_메서드는_DIRECTOR와_DEPT_HEAD만_허용한다() throws NoSuchMethodException {
        Method method = UserController.class.getMethod("getAdminCandidates", CustomUserPrincipal.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("hasAnyRole('DIRECTOR','DEPT_HEAD')");
    }

    @Test
    @DisplayName("관리자 후보 조회 메서드는 인증 주체 외 요청 파라미터를 받지 않는다")
    void 관리자_후보_조회_메서드는_인증_주체_외_요청_파라미터를_받지_않는다() throws NoSuchMethodException {
        Method method = UserController.class.getMethod("getAdminCandidates", CustomUserPrincipal.class);

        assertThat(method.getParameterCount()).isEqualTo(1);
        assertThat(method.getParameterTypes()).containsExactly(CustomUserPrincipal.class);
    }

    @Test
    @DisplayName("부서 후보 조회 메서드는 /department-candidates GET 매핑을 사용한다")
    void 부서_후보_조회_메서드는_department_candidates_GET_매핑을_사용한다() throws NoSuchMethodException {
        Method method = UserController.class.getMethod("getDepartmentCandidates", CustomUserPrincipal.class);
        GetMapping getMapping = method.getAnnotation(GetMapping.class);

        assertThat(getMapping).isNotNull();
        assertThat(getMapping.value()).containsExactly("/department-candidates");
    }

    @Test
    @DisplayName("부서 후보 조회 메서드는 DIRECTOR role 만 허용한다")
    void 부서_후보_조회_메서드는_DIRECTOR_role만_허용한다() throws NoSuchMethodException {
        Method method = UserController.class.getMethod("getDepartmentCandidates", CustomUserPrincipal.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("hasRole('DIRECTOR')");
    }

    @Test
    @DisplayName("사용자 목록 조회 메서드는 users 루트 GET 매핑을 사용한다")
    void 사용자_목록_조회_메서드는_users_루트_GET_매핑을_사용한다() throws NoSuchMethodException {
        Method method = UserController.class.getMethod("getUsers", CustomUserPrincipal.class, GetUsersApiDto.Request.class);
        GetMapping getMapping = method.getAnnotation(GetMapping.class);

        assertThat(getMapping).isNotNull();
        assertThat(getMapping.value()).isEmpty();
    }

    @Test
    @DisplayName("사용자 목록 조회 메서드는 DIRECTOR 와 DEPT_HEAD role 을 허용한다")
    void 사용자_목록_조회_메서드는_DIRECTOR와_DEPT_HEAD_role을_허용한다() throws NoSuchMethodException {
        Method method = UserController.class.getMethod("getUsers", CustomUserPrincipal.class, GetUsersApiDto.Request.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("hasAnyRole('DIRECTOR','DEPT_HEAD')");
    }

    @Test
    @DisplayName("사용자 목록 조회 요청은 ModelAttribute 로 바인딩한다")
    void 사용자_목록_조회_요청은_ModelAttribute로_바인딩한다() throws NoSuchMethodException {
        Method method = UserController.class.getMethod("getUsers", CustomUserPrincipal.class, GetUsersApiDto.Request.class);

        assertThat(method.getParameters()[1].getAnnotation(ModelAttribute.class)).isNotNull();
    }

    @Test
    @DisplayName("사용자 부분 수정 메서드는 /{id} PATCH 매핑을 사용한다")
    void 사용자_부분_수정_메서드는_id_PATCH_매핑을_사용한다() throws NoSuchMethodException {
        Method method = UserController.class.getMethod("updateUser", CustomUserPrincipal.class, Long.class, UpdateUserApiDto.Request.class, MultipartFile.class);
        PatchMapping patchMapping = method.getAnnotation(PatchMapping.class);

        assertThat(patchMapping).isNotNull();
        assertThat(patchMapping.value()).containsExactly("/{id}");
        assertThat(patchMapping.consumes()).containsExactly(MediaType.MULTIPART_FORM_DATA_VALUE);
    }

    @Test
    @DisplayName("사용자 부분 수정 메서드는 DIRECTOR 와 DEPT_HEAD role 을 허용한다")
    void 사용자_부분_수정_메서드는_DIRECTOR와_DEPT_HEAD_role을_허용한다() throws NoSuchMethodException {
        Method method = UserController.class.getMethod("updateUser", CustomUserPrincipal.class, Long.class, UpdateUserApiDto.Request.class, MultipartFile.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("hasAnyRole('DIRECTOR','DEPT_HEAD')");
    }

    @Test
    @DisplayName("사용자 부분 수정 요청은 PathVariable 과 RequestPart 로 바인딩한다")
    void 사용자_부분_수정_요청은_PathVariable과_RequestPart로_바인딩한다() throws NoSuchMethodException {
        Method method = UserController.class.getMethod("updateUser", CustomUserPrincipal.class, Long.class, UpdateUserApiDto.Request.class, MultipartFile.class);

        assertThat(method.getParameters()[1].getAnnotation(PathVariable.class)).isNotNull();
        assertThat(method.getParameters()[2].getAnnotation(RequestPart.class).value()).isEqualTo("request");
        RequestPart profileImagePart = method.getParameters()[3].getAnnotation(RequestPart.class);
        assertThat(profileImagePart.value()).isEqualTo("profile_image");
        assertThat(profileImagePart.required()).isFalse();
    }

    @Test
    @DisplayName("현재 사용자 조회 메서드는 서비스 결과를 응답 DTO 로 변환한다")
    void 현재_사용자_조회_메서드는_서비스_결과를_응답_DTO로_변환한다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(101L, "user@ibank.com", "MEMBER");
        GetMyProfileApiDto.Response responseFromService = new GetMyProfileApiDto.Response(
                101L,
                "홍길동",
                "user@ibank.com",
                "010-1234-5678",
                10L,
                "물류본부",
                "과장",
                "팀장",
                LocalDate.of(2025, 1, 1),
                EmploymentStatus.ACTIVE,
                "https://cdn.axwms.com/profile/101.png",
                java.util.List.of(new GetMyProfileApiDto.Response.TeamSummary(
                        true,
                        21L,
                        "물류혁신TF",
                        true,
                        "플랫폼 총괄",
                        "주담당"
                ))
        );
        given(userService.getMyProfile(principal)).willReturn(responseFromService);

        GetMyProfileApiDto.Response response = userController.getMyProfile(principal);

        assertThat(response.teams()).containsExactly(new GetMyProfileApiDto.Response.TeamSummary(
                true,
                21L,
                "물류혁신TF",
                true,
                "플랫폼 총괄",
                "주담당"
        ));
    }

    @Test
    @DisplayName("사용자 상세 조회 메서드는 서비스 결과를 그대로 반환한다")
    void 사용자_상세_조회_메서드는_서비스_결과를_그대로_반환한다() throws Exception {
        CustomUserPrincipal principal = new CustomUserPrincipal(1L, "viewer@ibank.com", "MEMBER");
        GetUserApiDto.Response responseFromService = new GetUserApiDto.Response(
                101L,
                "홍길동",
                "hong@axwms.com",
                10L,
                "물류본부",
                "과장",
                "팀장",
                LocalDate.of(2024, 3, 1),
                "https://cdn.axwms.com/profile/101.png",
                "010-1234-5678",
                EmploymentStatus.ACTIVE,
                List.of(new GetUserApiDto.Response.TeamSummary(
                        true,
                        21L,
                        "웹서비스 개발",
                        true,
                        "플랫폼 총괄"
                ))
        );
        given(userService.getUser(101L)).willReturn(responseFromService);

        GetUserApiDto.Response response = userController.getUser(principal, 101L);
        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(response));

        assertThat(response).isEqualTo(responseFromService);
        assertThat(json.has("teamId")).isFalse();
        assertThat(json.has("teamName")).isFalse();
        assertThat(json.get("teams").get(0).has("teamId")).isTrue();
        assertThat(json.get("teams").get(0).has("teamName")).isTrue();
    }

    @Test
    @DisplayName("관리자 후보 조회 메서드는 서비스 결과 배열을 그대로 반환한다")
    void 관리자_후보_조회_메서드는_서비스_결과_배열을_그대로_반환한다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(1L, "director@ibank.com", "DIRECTOR");
        List<GetAdminCandidatesApiDto.Response> responseFromService = List.of(
                new GetAdminCandidatesApiDto.Response(201L, "김부서", "부서장", "부장"),
                new GetAdminCandidatesApiDto.Response(202L, "이본부", "부서장", null)
        );
        given(userService.getAdminCandidates(principal)).willReturn(responseFromService);

        List<GetAdminCandidatesApiDto.Response> response = userController.getAdminCandidates(principal);

        assertThat(response).containsExactlyElementsOf(responseFromService);
    }

    @Test
    @DisplayName("부서 후보 조회 메서드는 서비스 결과 배열을 그대로 반환한다")
    void 부서_후보_조회_메서드는_서비스_결과_배열을_그대로_반환한다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(1L, "director@ibank.com", "DIRECTOR");
        List<GetDepartmentCandidatesApiDto.Response> responseFromService = List.of(
                new GetDepartmentCandidatesApiDto.Response(301L, "무소속부서장"),
                new GetDepartmentCandidatesApiDto.Response(302L, "예비부서장")
        );
        given(userService.getDepartmentCandidates()).willReturn(responseFromService);

        List<GetDepartmentCandidatesApiDto.Response> response = userController.getDepartmentCandidates(principal);

        assertThat(response).containsExactlyElementsOf(responseFromService);
    }

    @Test
    @DisplayName("사용자 목록 조회 메서드는 서비스 결과 배열을 그대로 반환한다")
    void 사용자_목록_조회_메서드는_서비스_결과_배열을_그대로_반환한다() throws Exception {
        CustomUserPrincipal principal = new CustomUserPrincipal(101L, "user@ibank.com", "MEMBER");
        GetUsersApiDto.Request request = new GetUsersApiDto.Request("홍길동", 10L, "과장", EmploymentStatus.ACTIVE);
        List<GetUsersApiDto.Response> responseFromService = List.of(
                new GetUsersApiDto.Response(
                        101L,
                        "홍길동",
                        "hong@axwms.com",
                        "010-1234-1234",
                        10L,
                        "물류본부",
                        "https://cdn.axwms.com/profile/101.png",
                        21L,
                        "물류혁신TF",
                        "과장",
                        "팀장",
                        EmploymentStatus.ACTIVE
                )
        );
        given(userService.getUsers(request)).willReturn(responseFromService);

        List<GetUsersApiDto.Response> response = userController.getUsers(principal, request);
        String json = objectMapper.writeValueAsString(response);

        assertThat(response).containsExactlyElementsOf(responseFromService);
        assertThat(json).contains("\"phone\":\"010-1234-1234\"");
        assertThat(json).doesNotContain("pageSize", "totalCount", "items");
    }

    @Test
    @DisplayName("사용자 부분 수정 메서드는 서비스에 위임하고 빈 응답을 반환한다")
    void 사용자_부분_수정_메서드는_서비스에_위임하고_빈_응답을_반환한다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(201L, "dept-head@ibank.com", "DEPT_HEAD");
        MockMultipartFile profileImage = new MockMultipartFile(
                "profile_image",
                "profile.png",
                MediaType.IMAGE_PNG_VALUE,
                "image".getBytes(StandardCharsets.UTF_8)
        );
        UpdateUserApiDto.Request request = new UpdateUserApiDto.Request(
                "홍길동",
                "hong@axwms.com",
                "차장",
                "팀장",
                10L,
                "010-1234-5678",
                EmploymentStatus.ACTIVE,
                LocalDate.of(2024, 3, 1),
                21L
        );

        EmptyResponse response = userController.updateUser(principal, 101L, request, profileImage);

        assertThat(response).isEqualTo(EmptyResponse.INSTANCE);
        then(userService).should().updateUser(principal, 101L, request, profileImage);
    }

    @Test
    @DisplayName("현재 사용자 부분 수정 메서드는 서비스에 위임하고 빈 응답을 반환한다")
    void 현재_사용자_부분_수정_메서드는_서비스에_위임하고_빈_응답을_반환한다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(101L, "member@ibank.com", "MEMBER");
        MockMultipartFile profileImage = new MockMultipartFile(
                "profile_image",
                "profile.png",
                MediaType.IMAGE_PNG_VALUE,
                "image".getBytes(StandardCharsets.UTF_8)
        );
        UpdateMyProfileApiDto.Request request = new UpdateMyProfileApiDto.Request(
                10L,
                "홍길동",
                "hong@axwms.com",
                "차장",
                "팀장",
                LocalDate.of(2024, 3, 1),
                "010-1234-5678",
                EmploymentStatus.ACTIVE
        );

        EmptyResponse response = userController.updateMyProfile(principal, request, profileImage);

        assertThat(response).isEqualTo(EmptyResponse.INSTANCE);
        then(userService).should().updateMyProfile(principal, request, profileImage);
    }

    @Test
    @DisplayName("사용자 부분 수정 HTTP 응답은 빈 data 객체로 래핑된다")
    void 사용자_부분_수정_HTTP_응답은_빈_data_객체로_래핑된다() throws Exception {
        MockMultipartFile requestPart = new MockMultipartFile(
                "request",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                """
                                {
                                  "userName": "홍길동",
                                  "primaryTeamId": 21
                                }
                                """.getBytes(StandardCharsets.UTF_8)
        );

        MvcResult result = mockMvc.perform(multipart("/users/101")
                        .file(requestPart)
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());

        assertThat(json.get("success").asBoolean()).isTrue();
        assertThat(json.get("data").isObject()).isTrue();
        assertThat(json.get("data").size()).isZero();
        assertThat(json.get("timestamp").asText()).isNotBlank();
        then(userService).should().updateUser(new CustomUserPrincipal(null, null, null), 101L, new UpdateUserApiDto.Request(
                "홍길동",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                21L
        ), null);
    }

    @Test
    @DisplayName("현재 사용자 부분 수정 HTTP 응답은 빈 data 객체로 래핑된다")
    void 현재_사용자_부분_수정_HTTP_응답은_빈_data_객체로_래핑된다() throws Exception {
        MockMultipartFile requestPart = new MockMultipartFile(
                "request",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                """
                                {
                                  "department_id": 10,
                                  "user_name": "홍길동",
                                  "title_name": "본부장",
                                  "employment_status": "LEAVE"
                                }
                                """.getBytes(StandardCharsets.UTF_8)
        );

        MvcResult result = mockMvc.perform(multipart("/users/me")
                        .file(requestPart)
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());

        assertThat(json.get("success").asBoolean()).isTrue();
        assertThat(json.get("data").isObject()).isTrue();
        assertThat(json.get("data").size()).isZero();
        assertThat(json.get("timestamp").asText()).isNotBlank();
        then(userService).should().updateMyProfile(new CustomUserPrincipal(null, null, null), new UpdateMyProfileApiDto.Request(
                10L,
                "홍길동",
                null,
                null,
                "본부장",
                null,
                null,
                EmploymentStatus.LEAVE
        ), null);
    }

    @Test
    @DisplayName("현재 사용자 조회 응답의 joinDate 는 ISO-8601 문자열로 직렬화된다")
    void 현재_사용자_조회_응답의_joinDate_는_ISO_8601_문자열로_직렬화된다() throws Exception {
        GetMyProfileApiDto.Response response = new GetMyProfileApiDto.Response(
                101L,
                "홍길동",
                "user@ibank.com",
                "010-1234-5678",
                10L,
                "물류본부",
                "과장",
                "팀장",
                LocalDate.of(2025, 1, 1),
                EmploymentStatus.ACTIVE,
                "https://cdn.axwms.com/profile/101.png",
                java.util.List.of()
        );

        String json = objectMapper.writeValueAsString(response);

        assertThat(json).contains("\"joinDate\":\"2025-01-01\"");
        assertThat(json).contains("\"employmentStatus\":\"ACTIVE\"");
    }

    @Test
    @DisplayName("팀 요약 응답은 isLeader 를 포함한 고정 순서로 직렬화한다")
    void 팀_요약_응답은_isLeader_를_포함한_고정_순서로_직렬화한다() throws Exception {
        GetMyProfileApiDto.Response.TeamSummary teamSummary = new GetMyProfileApiDto.Response.TeamSummary(
                true,
                21L,
                "물류혁신TF",
                true,
                "플랫폼 총괄",
                "주담당"
        );

        String json = objectMapper.writeValueAsString(teamSummary);

        assertThat(json).startsWith("{\"isPrimary\":true,\"teamId\":21,\"teamName\":\"물류혁신TF\",\"isLeader\":true,\"teamRole\":\"플랫폼 총괄\",\"allocation\":\"주담당\"");
    }

    @Test
    @DisplayName("사용자 상세 팀 요약 응답은 allocation 없이 고정 순서로 직렬화한다")
    void 사용자_상세_팀_요약_응답은_allocation_없이_고정_순서로_직렬화한다() throws Exception {
        GetUserApiDto.Response.TeamSummary teamSummary = new GetUserApiDto.Response.TeamSummary(
                true,
                21L,
                "웹서비스 개발",
                true,
                "플랫폼 총괄"
        );

        String json = objectMapper.writeValueAsString(teamSummary);

        assertThat(json).startsWith("{\"isPrimary\":true,\"teamId\":21,\"teamName\":\"웹서비스 개발\",\"isLeader\":true,\"teamRole\":\"플랫폼 총괄\"");
        assertThat(json).doesNotContain("allocation");
    }
}
