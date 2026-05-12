const notificationTypeLabelMap: Record<string, string> = {
  WORKLOG_DUE_SOON: "마감 임박",
};

export function getNotificationTypeLabel(type: string) {
  return notificationTypeLabelMap[type] ?? "알림";
}
