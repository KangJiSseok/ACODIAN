import type { PageResponse } from "@/app/_common/types/api.types";

export type NotificationTypeCode = "WORKLOG_DUE_SOON" | string;
export type NotificationReferenceTypeCode = "WORKLOG" | string;
export type NotificationView = "TOTAL" | "UNREAD" | "READ";

export interface NotificationApiItem {
  notificationId: number;
  notificationType: NotificationTypeCode;
  title: string;
  content: string | null;
  referenceType: NotificationReferenceTypeCode | null;
  referenceId: number | null;
  departmentId: number | null;
  teamId: number | null;
  isRead: boolean;
  readAt: string | null;
  createdAt: string;
}

export interface NotificationItem extends NotificationApiItem {
  id: number;
  type: NotificationTypeCode;
}

export interface GetNotificationsParams {
  isRead?: boolean;
  departmentId?: number;
  teamId?: number;
  page?: number;
  pageSize?: number;
}

export type NotificationPageResponse = PageResponse<NotificationItem>;

export interface MarkAllNotificationsReadResponse {
  updatedCount: number;
}
