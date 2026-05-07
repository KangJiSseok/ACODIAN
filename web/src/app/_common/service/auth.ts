/* 1. import문 */
import type { ApiResponse, EmptyResponse } from "@/app/_common/types/api.types";
// 백엔드 공통 응답 타입을 가져옵니다.

import { useAuthStore, type AuthUser } from "@/app/_common/store/auth.store";
// 인증 상태 저장소와 /users/me 사용자 타입을 가져옵니다.

import { api } from "./axios";
// header를 직접 읽어야 하는 login/refresh/logout 요청에 사용합니다.

import { apiClient } from "./api-client";
// data unwrap이 필요한 /users/me, /auth/signup 요청에 사용합니다.

import { extractAccessToken } from "./auth-token";
// Authorization header에서 access token 문자열만 꺼낼 때 사용합니다.

/* 2. 요청 타입 */
export interface LoginCredentials {
  email: string;
  password: string;
}
// 로그인 요청 바디

export interface SignupPayload {
  departmentId: number;
  userName: string;
  email: string;
  password: string;
  positionName: string;
  titleName: string;
  phone: string;
  joinDate: string;
  profileImage: File | null;
}
// 회원가입 요청 데이터

export interface UpdateMyProfilePayload {
  userName: string;
  email: string;
  phone: string;
  profileImage: File | null;
}
// 현재 로그인한 사용자 프로필 수정 데이터

/* 3. 로그인 */
export async function login(credentials: LoginCredentials) {
  useAuthStore.getState().setStatus("loading");
  // 로그인 요청 시작 상태 Change

  try {
    const response = await api.post<ApiResponse<EmptyResponse>>(
      "/auth/login",
      credentials,
      { skipAuthRefresh: true },
    );
    // login은 access token을 response header에서 읽어야 해서 api를 직접 씀

    const accessToken = extractAccessToken(response.headers.authorization);
    // Authorization Header에서 Bearer prefix를 제거한 token만 꺼냅니다.

    if (!accessToken) {
      throw new Error("로그인 응답에 access token이 없습니다");
    }

    useAuthStore.getState().setAccessToken(accessToken);
    // /users/me 요청에 Authorization header가 붙도록 먼저 token을 저장합니다.

    const user = await fetchCurrentUser();
    // access token으로 현재 사용자 정보를 조회합니다.

    useAuthStore.getState().setAuth({ accessToken, user });
    // token과 user를 함께 저장하고 authenticated 상태로 전환합니다.

    return user;
  } catch (error) {
    useAuthStore.getState().resetAuth();
    // 실패하면 인증 상태를 초기화합니다.

    throw error;
    // 로그인 화면에서 에러 메시지를 보여줄 수 있도록 다시 던집니다.
  }
}

/* 4. 세션 복구 */
export async function refreshSession() {
  useAuthStore.getState().setStatus("loading");

  try {
    const response = await api.post<ApiResponse<EmptyResponse>>(
      "/auth/refresh",
      undefined,
      { skipAuthRefresh: true },
    );

    const accessToken = extractAccessToken(response.headers.authorization);

    if (!accessToken) {
      useAuthStore.getState().resetAuth();
      return null;
    }

    useAuthStore.getState().setAccessToken(accessToken);

    const user = await fetchCurrentUser();

    useAuthStore.getState().setAuth({ accessToken, user });

    return user;
  } catch {
    useAuthStore.getState().resetAuth();
    return null;
  }
}

/* 5. 로그아웃 */
export async function logout() {
  try {
    await api.post<ApiResponse<EmptyResponse>>("/auth/logout", undefined, {
      skipAuthRefresh: true,
    });
    // 로그아웃 요청은 바디 없이 보냅니다.
    // 실패해도 프론트 상태는 비워야 하므로 finally에서 resetAuth를 호출합니다.
  } finally {
    useAuthStore.getState().resetAuth();
    // 백엔드 logout 성공/실패와 관계없이 프론트 인증 상태를 초기화
  }
}

/* 6. 회원가입 */
export async function signup(payload: SignupPayload) {
  const request = {
    department_id: payload.departmentId,
    user_name: payload.userName,
    email: payload.email,
    password: payload.password,
    position_name: payload.positionName || null,
    title_name: payload.titleName,
    join_date: payload.joinDate,
    phone: payload.phone || null,
    employment_status: "ACTIVE",
  };
  // 백엔드 /auth/signup은 multipart/form-data로 JSON request part와 선택 이미지 파일을 받습니다.

  const formData = new FormData();
  formData.append(
    "request",
    new Blob([JSON.stringify(request)], { type: "application/json" }),
  );

  if (payload.profileImage) {
    formData.append("profile_image", payload.profileImage);
  }

  return apiClient.post<EmptyResponse, FormData>("/auth/signup", formData, {
    skipAuthRefresh: true,
  });
  // 회원가입은 응답 헤더를 직접 읽을 필요가 없으므로 apiClient를 사용
  // apiClient는 ApiResponse<T>에서 data만 꺼내 반환합니다.
}

/* 7. 내 프로필 수정 */
export async function updateMyProfile(payload: UpdateMyProfilePayload) {
  const request = {
    user_name: payload.userName,
    email: payload.email,
    phone: payload.phone || null,
  };

  const formData = new FormData();
  formData.append(
    "request",
    new Blob([JSON.stringify(request)], { type: "application/json" }),
  );

  if (payload.profileImage) {
    formData.append("profile_image", payload.profileImage);
  }

  await apiClient.patch<EmptyResponse, FormData>("/users/me", formData);

  const user = await fetchCurrentUser();
  useAuthStore.getState().setUser(user);

  return user;
}

/* 7. 현재 사용자 조회 helper */
async function fetchCurrentUser() {
  return apiClient.get<AuthUser>("/users/me");
  // 현재 로그인한 사용자 정보를 조회합니다.
  // 액세스 토큰은 이미 store에 저장되어 있으므로 axios interceptor가 Authorizatoin header에 자동으로 붙입니다.
}
