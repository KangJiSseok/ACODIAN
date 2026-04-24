type SseParamValue = string | number | boolean | null | undefined;

export type SseMessageHandler<TData> = (
  data: TData,
  event: MessageEvent<string>,
) => void;

export interface SseClientOptions<TData = unknown> {
  path: string;
  baseUrl?: string;
  params?: Record<string, SseParamValue>;
  accessToken?: string;
  accessTokenParamName?: string;
  withCredentials?: boolean;
  onOpen?: (event: Event) => void;
  onMessage?: SseMessageHandler<TData>;
  onError?: (event: Event) => void;
}

export interface SseClient<TData = unknown> {
  source: EventSource;
  close: () => void;
  addListener: <TEventData = TData>(
    eventName: string,
    handler: SseMessageHandler<TEventData>,
  ) => () => void;
}

function serializeQueryParams(params?: Record<string, SseParamValue>) {
  const searchParams = new URLSearchParams();

  if (!params) {
    return searchParams;
  }

  for (const [key, value] of Object.entries(params)) {
    if (value === undefined || value === null) {
      continue;
    }

    searchParams.set(key, String(value));
  }

  return searchParams;
}

function resolveSseUrl({
  path,
  baseUrl,
  params,
  accessToken,
  accessTokenParamName = "accessToken",
}: Pick<
  SseClientOptions,
  "path" | "baseUrl" | "params" | "accessToken" | "accessTokenParamName"
>) {
  const resolvedBaseUrl =
    baseUrl ?? process.env.NEXT_PUBLIC_API_BASE_URL ?? window.location.origin;
  const url = new URL(path, resolvedBaseUrl);
  // URLSearchParams로 정리해두면 SSE 연결 주소를 한 방식으로 조합할 수 있습니다.
  const searchParams = serializeQueryParams(params);

  if (accessToken) {
    // EventSource는 커스텀 Authorization 헤더를 직접 붙일 수 없어 query param 방식을 함께 지원합니다.
    searchParams.set(accessTokenParamName, accessToken);
  }

  searchParams.forEach((value, key) => {
    url.searchParams.set(key, value);
  });

  return url.toString();
}

function parseSseData<TData>(rawData: string) {
  // JSON 문자열이면 객체로 바꾸고, 아니면 원문 문자열 그대로 넘깁니다.
  if (!rawData) {
    return undefined as TData;
  }

  try {
    return JSON.parse(rawData) as TData;
  } catch {
    return rawData as TData;
  }
}

export function createSseClient<TData = unknown>(
  options: SseClientOptions<TData>,
): SseClient<TData> {
  // EventSource는 브라우저 API라서 서버 컴포넌트나 SSR 단계에서는 만들 수 없습니다.
  if (typeof window === "undefined" || typeof EventSource === "undefined") {
    throw new Error("SSE client는 브라우저 환경에서만 생성할 수 있습니다.");
  }

  const source = new EventSource(resolveSseUrl(options), {
    withCredentials: options.withCredentials ?? true,
  });

  source.onopen = (event) => {
    options.onOpen?.(event);
  };

  source.onmessage = (event) => {
    options.onMessage?.(parseSseData<TData>(event.data), event);
  };

  source.onerror = (event) => {
    options.onError?.(event);
  };

  return {
    source,
    close: () => source.close(),
    addListener: (eventName, handler) => {
      // 기본 message 외에 서버가 보내는 커스텀 이벤트도 같은 방식으로 구독합니다.
      const wrappedHandler = ((event: MessageEvent<string>) => {
        handler(parseSseData(event.data), event);
      }) as EventListener;

      source.addEventListener(eventName, wrappedHandler);

      return () => {
        source.removeEventListener(eventName, wrappedHandler);
      };
    },
  };
}
