import axios, {
  AxiosError,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from "axios";
import { useAuthStore } from "@/app/_common/store/auth.store";
import { extractAccessToken } from "./auth-token";

// TypeScript의 모듈 보강 문법입니다.
// 같은 속성이어도 요청을 보낼 때 타입과 axios 내부 interceptor 타입이 분리되어 있어 둘 다 확장합니다.
declare module "axios" {
  // 개발자가 api.post(url, data, config)처럼 요청 설정을 넘길 때 사용하는 타입입니다.
  interface AxiosRequestConfig {
    // refresh 요청 자체처럼 401 재시도 흐름에서 제외해야 하는 요청에 사용합니다.
    skipAuthRefresh?: boolean;
    // 같은 요청이 refresh 이후에도 실패할 때 무한 재시도하지 않도록 표시합니다.
    _retry?: boolean;
  }

  // axios interceptor 내부에서 실제로 다루는 요청 설정 타입입니다.
  interface InternalAxiosRequestConfig {
    skipAuthRefresh?: boolean;
    _retry?: boolean;
  }
}

const DEFAULT_API_BASE_URL = "http://localhost:8100/api";

// .env.local에 빈 문자열이 들어와도 로컬 API 주소로 안전하게 fallback합니다.
const apiBaseUrl =
  process.env.NEXT_PUBLIC_API_BASE_URL?.trim() || DEFAULT_API_BASE_URL;

// 여러 요청이 동시에 401을 받아도 refresh 요청은 하나만 보내도록 공유합니다.
let refreshPromise: Promise<string | null> | null = null;

// 프로젝트 공통 API 설정이 들어간 axios instance입니다.
export const api = axios.create({
  baseURL: apiBaseUrl,
  // refresh token은 HttpOnly cookie로 오가므로 API 요청에 cookie를 포함해야 합니다.
  withCredentials: true,
});

// API 요청이 서버로 나가기 직전에 access token을 Authorization 헤더에 붙입니다.
api.interceptors.request.use((config) => {
  const accessToken = useAuthStore.getState().accessToken;

  // access token은 메모리에만 보관하고, 보호 API 요청 직전에 Authorization 헤더로 붙입니다.
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }

  return config;
});

// 응답이 401일 때, refresh token으로 새 access token을 받아 원래 요청을
// 한 번만 재시도하는 "자동 토큰 갱신" 로직입니다.
api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const response = error.response;
    const originalRequest = error.config as
      | InternalAxiosRequestConfig
      | undefined;

    // 인증 만료로 볼 수 있는 401만 refresh 대상입니다.
    // refresh 요청 자체나 이미 재시도한 요청은 그대로 실패시켜 루프를 막습니다.
    if (
      // 1. 응답 객체가 아예 없을 때
      !response ||
      // 2. 에러코드가 401이 아닐 때
      response.status !== 401 ||
      // 3. 원래 보낸 요청 정보가 없을 때
      !originalRequest ||
      // 4. 이미 재시도를 한 요청일 때
      originalRequest._retry ||
      // 5. refresh 재시도를 원치 않는 요청일 때
      originalRequest.skipAuthRefresh
    ) {
      // refresh 대상이 아니면 원래 에러를 그대로 호출부로 넘깁니다.
      return Promise.reject(error);
    }

    // 같은 요청이 다시 401을 받아도 무한 반복하지 않도록 표시합니다.
    originalRequest._retry = true;

    // refresh token cookie로 새 access token을 받고, 실패했던 원 요청을 한 번만 재실행합니다.
    const nextAccessToken = await refreshAccessToken();
    if (!nextAccessToken) {
      useAuthStore.getState().resetAuth();
      return Promise.reject(error);
    }

    originalRequest.headers.Authorization = `Bearer ${nextAccessToken}`;
    return api(originalRequest);
  },
);

async function refreshAccessToken() {
  // 이미 refresh 중이면 새 요청을 만들지 않고 기존 Promise를 함께 기다립니다.
  refreshPromise ??= requestAccessTokenRefresh().finally(() => {
    refreshPromise = null;
  });

  return refreshPromise;
}

async function requestAccessTokenRefresh() {
  try {
    // refresh endpoint는 body 없이 HttpOnly cookie만으로 access token을 재발급합니다.
    const response: AxiosResponse = await api.post("/auth/refresh", undefined, {
      skipAuthRefresh: true,
    });
    const accessToken = extractAccessToken(response.headers.authorization);
    if (!accessToken) {
      return null;
    }

    useAuthStore.getState().setAccessToken(accessToken);
    return accessToken;
  } catch {
    useAuthStore.getState().resetAuth();
    return null;
  }
}

export default api;
