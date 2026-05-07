"use client";

/* import문 */
import { useCallback } from "react";
import {
  selectAccessToken,
  selectAuthUser,
  selectIsAuthenticated,
  useAuthStore,
} from "@/app/_common/store/auth.store";
import {
  login as loginRequest,
  logout as logoutRequest,
  refreshSession as refreshSessionRequest,
  signup as signupRequest,
  updateMyProfile as updateMyProfileRequest,
  type LoginCredentials,
  type SignupPayload,
  type UpdateMyProfilePayload,
} from "@/app/_common/service/auth";

/* 인증 hook */
export function useAuth() {
  // 현재 인증 상태
  const status = useAuthStore((state) => state.status);

  // 현재 로그인한 사용자 정보
  const user = useAuthStore(selectAuthUser);

  // 현재 메모리에 저장된 액세스 토큰
  const accessToken = useAuthStore(selectAccessToken);

  // 로그인 여부
  const isAuthenticated = useAuthStore(selectIsAuthenticated);

  // 인증 상태 직접 변경 함수들
  const setStatus = useAuthStore((state) => state.setStatus);
  const setAccessToken = useAuthStore((state) => state.setAccessToken);
  const setUser = useAuthStore((state) => state.setUser);
  const setAuth = useAuthStore((state) => state.setAuth);
  const resetAuth = useAuthStore((state) => state.resetAuth);

  /* 로그인 */
  const login = useCallback((credentials: LoginCredentials) => {
    return loginRequest(credentials);
  }, []);

  /* 로그아웃 */
  const logout = useCallback(() => {
    return logoutRequest();
  }, []);

  /* 세션 복구 */
  const refreshSession = useCallback(() => {
    return refreshSessionRequest();
  }, []);

  /* 회원가입 */
  const signup = useCallback((payload: SignupPayload) => {
    return signupRequest(payload);
  }, []);

  /* 내 프로필 수정 */
  const updateMyProfile = useCallback((payload: UpdateMyProfilePayload) => {
    return updateMyProfileRequest(payload);
  }, []);

  /* 7. 화면에서 사용할 값 반환 */
  return {
    status,
    user,
    accessToken,
    isAuthenticated,
    login,
    logout,
    refreshSession,
    signup,
    updateMyProfile,
    setStatus,
    setAccessToken,
    setUser,
    setAuth,
    resetAuth,
  };
}
