import axios, { type AxiosRequestConfig } from "axios";
import type {
  ApiErrorPayload,
  ApiResponse,
} from "@/app/_common/types/api.types";
import { api } from "./axios";

// 호출부에서 axios 에러 구조를 직접 알 필요 없도록,
// API 요청 실패 정보를 프로젝트 공통 에러 타입으로 감쌉니다.
export class ApiClientError extends Error {
  status?: number;
  code?: string;
  details?: unknown;

  constructor({
    message,
    status,
    code,
    details,
  }: {
    message: string;
    status?: number;
    code?: string;
    details?: unknown;
  }) {
    super(message);
    this.name = "ApiClientError";
    this.status = status;
    this.code = code;
    this.details = details;
  }
}

// 백엔드는 { success, data, error, timestamp } 형태의 공통 응답을 내려줍니다.
// 화면/서비스 코드에서는 실제 payload만 쓰기 쉽도록 data만 꺼내서 반환합니다.
function unwrapApiResponse<T>(response: ApiResponse<T>, status?: number): T {
  if (response.success) {
    return response.data;
  }

  throw new ApiClientError({
    message: response.error?.message ?? "API 요청을 처리하지 못했습니다.",
    status,
    code: response.error?.code,
    details: response.error?.details,
  });
}

// 어떤 형태로 발생한 에러든 호출부가 같은 방식으로 다룰 수 있게 변환합니다.
// 이미 ApiClientError면 그대로 쓰고, axios 에러면 HTTP status와 서버 error payload를 꺼냅니다.
function toApiClientError(error: unknown, fallbackMessage: string) {
  if (error instanceof ApiClientError) {
    return error;
  }

  if (axios.isAxiosError(error)) {
    const responseBody = error.response?.data as
      | Partial<ApiResponse<unknown>>
      | undefined;
    const apiError = responseBody?.error as ApiErrorPayload | undefined;

    return new ApiClientError({
      message: apiError?.message ?? error.message ?? fallbackMessage,
      status: error.response?.status,
      code: apiError?.code,
      details: apiError?.details,
    });
  }

  return new ApiClientError({ message: fallbackMessage });
}

// 실제 axios 요청은 이 함수 한 곳으로 모읍니다.
// 성공하면 ApiResponse<T>에서 data를 반환하고, 실패하면 ApiClientError로 통일합니다.
async function request<T>(config: AxiosRequestConfig) {
  try {
    const response = await api.request<ApiResponse<T>>(config);
    return unwrapApiResponse(response.data, response.status);
  } catch (error) {
    throw toApiClientError(error, "API 요청 중 문제가 발생했습니다.");
  }
}

// form이나 page에서 catch한 에러를 사용자에게 보여줄 문자열로 바꿀 때 사용합니다.
export function getApiErrorMessage(error: unknown, fallbackMessage: string) {
  return toApiClientError(error, fallbackMessage).message;
}

// 호출부에서는 apiClient.get<User>("/users/me")처럼 간단히 사용합니다.
// 각 메서드는 request()에 HTTP method만 채워 넣는 얇은 wrapper입니다.
export const apiClient = {
  get: <T>(url: string, config?: AxiosRequestConfig) =>
    request<T>({ ...config, method: "GET", url }),
  post: <T, TBody = unknown>(
    url: string,
    data?: TBody,
    config?: AxiosRequestConfig,
  ) => request<T>({ ...config, method: "POST", url, data }),
  put: <T, TBody = unknown>(
    url: string,
    data?: TBody,
    config?: AxiosRequestConfig,
  ) => request<T>({ ...config, method: "PUT", url, data }),
  patch: <T, TBody = unknown>(
    url: string,
    data?: TBody,
    config?: AxiosRequestConfig,
  ) => request<T>({ ...config, method: "PATCH", url, data }),
  delete: <T>(url: string, config?: AxiosRequestConfig) =>
    request<T>({ ...config, method: "DELETE", url }),
};
