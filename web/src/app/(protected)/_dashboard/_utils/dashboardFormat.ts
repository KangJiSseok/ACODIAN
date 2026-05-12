const PERCENT_FORMATTER = new Intl.NumberFormat("ko-KR", {
  maximumFractionDigits: 1,
});

const NUMBER_FORMATTER = new Intl.NumberFormat("ko-KR");

const INDEX_FORMATTER = new Intl.NumberFormat("ko-KR", {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

const DATE_FORMATTER = new Intl.DateTimeFormat("ko-KR", {
  month: "short",
  day: "numeric",
});

export function clampRate(value: number) {
  if (!Number.isFinite(value)) {
    return 0;
  }

  return Math.min(Math.max(value, 0), 1);
}

export function formatPercent(value: number) {
  return `${PERCENT_FORMATTER.format(clampRate(value) * 100)}%`;
}

export function formatCount(value: number) {
  return NUMBER_FORMATTER.format(value);
}

export function formatIndex(value: number) {
  return INDEX_FORMATTER.format(clampRate(value));
}

export function formatDueDate(value: string | null) {
  if (!value) {
    return "마감일 없음";
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return DATE_FORMATTER.format(date);
}

export function formatOverdueDays(daysOverdue: number | null) {
  if (daysOverdue === null) {
    return "임박";
  }

  if (daysOverdue <= 0) {
    return "오늘 마감";
  }

  return `${formatCount(daysOverdue)}일 지연`;
}
