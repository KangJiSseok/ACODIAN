// 백엔드 공통 응답 envelope의 error 영역입니다.
// 화면에서는 message를 주로 사용하고, code/details는 디버깅이나 세부 분기에 활용할 수 있습니다.
export interface ApiErrorPayload {
  code?: string;
  message?: string;
  details?: unknown;
}

// 백엔드는 실제 payload를 data 안에 담아 내려줍니다.
// api-client.ts는 이 형태를 받아 성공 시 data만 꺼내서 호출부에 반환합니다.
export interface ApiResponse<T> {
  success: boolean;
  data: T;
  error?: ApiErrorPayload;
  timestamp: string;
}

// body가 비어 있는 성공 응답을 표현할 때 사용하는 타입입니다.
export type EmptyResponse = Record<string, never>;

// 백엔드 공통 페이지네이션 응답입니다.
export interface PageResponse<T> {
  items: T[];
  page: number;
  pageSize: number;
  totalCount: number;
  totalPages: number;
  isFirst: boolean;
  isLast: boolean;
  hasNext: boolean;
  hasPrevious: boolean;
}
