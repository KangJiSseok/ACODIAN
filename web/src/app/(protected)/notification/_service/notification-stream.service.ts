import type {
  NotificationReferenceTypeCode,
  NotificationTypeCode,
} from "../_types/notification.types";

const DEFAULT_API_BASE_URL = "/api";
const NOTIFICATION_STREAM_PATH = "/notifications/stream";
const DEFAULT_RETRY_DELAY_MS = 3000;

export interface NotificationStreamPayload {
  notificationId: number;
  type: NotificationTypeCode;
  title: string;
  content: string | null;
  referenceType: NotificationReferenceTypeCode | null;
  referenceId: number | null;
  createdAt: string;
}

export interface NotificationStreamSubscription {
  close: () => void;
}

interface NotificationStreamOptions {
  accessToken: string;
  retryDelayMs?: number;
  onNotification: (notification: NotificationStreamPayload) => void;
  onError?: (error: unknown) => void;
}

interface ParsedSseEvent {
  eventName: string;
  data: string;
}

export function subscribeNotificationStream({
  accessToken,
  retryDelayMs = DEFAULT_RETRY_DELAY_MS,
  onNotification,
  onError,
}: NotificationStreamOptions): NotificationStreamSubscription {
  let isClosed = false;
  let activeController: AbortController | null = null;
  let retryTimer: ReturnType<typeof setTimeout> | null = null;

  const scheduleReconnect = () => {
    if (isClosed) {
      return;
    }

    retryTimer = setTimeout(connect, retryDelayMs);
  };

  const connect = () => {
    activeController = new AbortController();

    void readNotificationStream({
      accessToken,
      signal: activeController.signal,
      onNotification,
    })
      .then(scheduleReconnect)
      .catch((error) => {
        if (isClosed || isAbortError(error)) {
          return;
        }

        onError?.(error);
        scheduleReconnect();
      });
  };

  connect();

  return {
    close: () => {
      isClosed = true;
      activeController?.abort();
      if (retryTimer) {
        clearTimeout(retryTimer);
      }
    },
  };
}

function resolveNotificationStreamUrl() {
  const baseUrl =
    process.env.NEXT_PUBLIC_API_BASE_URL?.trim() || DEFAULT_API_BASE_URL;
  return `${baseUrl.replace(/\/$/, "")}${NOTIFICATION_STREAM_PATH}`;
}

async function readNotificationStream({
  accessToken,
  signal,
  onNotification,
}: {
  accessToken: string;
  signal: AbortSignal;
  onNotification: (notification: NotificationStreamPayload) => void;
}) {
  const response = await fetch(resolveNotificationStreamUrl(), {
    method: "GET",
    headers: {
      Accept: "text/event-stream",
      Authorization: `Bearer ${accessToken}`,
    },
    credentials: "include",
    signal,
  });

  if (!response.ok) {
    throw new Error(`알림 SSE 연결에 실패했습니다. status=${response.status}`);
  }

  if (!response.body) {
    throw new Error("알림 SSE 응답 스트림을 읽을 수 없습니다.");
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";

  while (true) {
    const { done, value } = await reader.read();
    if (done) {
      break;
    }

    buffer += decoder.decode(value, { stream: true });
    buffer = consumeSseBuffer(buffer, onNotification);
  }

  buffer += decoder.decode();
  consumeSseBuffer(`${buffer}\n\n`, onNotification);
}

function consumeSseBuffer(
  buffer: string,
  onNotification: (notification: NotificationStreamPayload) => void,
) {
  const blocks = buffer.split(/\r?\n\r?\n/);
  const nextBuffer = blocks.pop() ?? "";

  for (const block of blocks) {
    const event = parseSseEvent(block);
    if (event.eventName !== "notification" || !event.data) {
      continue;
    }

    onNotification(JSON.parse(event.data) as NotificationStreamPayload);
  }

  return nextBuffer;
}

function parseSseEvent(block: string): ParsedSseEvent {
  let eventName = "message";
  const dataLines: string[] = [];

  for (const line of block.split(/\r?\n/)) {
    if (!line || line.startsWith(":")) {
      continue;
    }

    const separatorIndex = line.indexOf(":");
    const field =
      separatorIndex === -1 ? line : line.slice(0, separatorIndex);
    let value = separatorIndex === -1 ? "" : line.slice(separatorIndex + 1);

    if (value.startsWith(" ")) {
      value = value.slice(1);
    }

    if (field === "event") {
      eventName = value;
    }

    if (field === "data") {
      dataLines.push(value);
    }
  }

  return {
    eventName,
    data: dataLines.join("\n"),
  };
}

function isAbortError(error: unknown) {
  return error instanceof DOMException && error.name === "AbortError";
}
