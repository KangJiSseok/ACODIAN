"use client";

import {
  selectAccessToken,
  selectAuthUser,
  selectIsAuthenticated,
  useAuthStore,
} from "@/app/_common/store/auth.store";

// 화면 컴포넌트가 auth store 내부 구조에 직접 의존하지 않도록 한 곳에서 묶어 노출합니다.
export function useAuth() {
  const status = useAuthStore((state) => state.status);
  const user = useAuthStore(selectAuthUser);
  const accessToken = useAuthStore(selectAccessToken);
  const isAuthenticated = useAuthStore(selectIsAuthenticated);
  const setStatus = useAuthStore((state) => state.setStatus);
  const setAccessToken = useAuthStore((state) => state.setAccessToken);
  const setUser = useAuthStore((state) => state.setUser);
  const setAuth = useAuthStore((state) => state.setAuth);
  const resetAuth = useAuthStore((state) => state.resetAuth);

  return {
    status,
    user,
    accessToken,
    isAuthenticated,
    setStatus,
    setAccessToken,
    setUser,
    setAuth,
    resetAuth,
  };
}
