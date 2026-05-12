import type { AiProcessingStatus } from "@/app/(protected)/worklog/_types/worklog.types";

export interface FileWorklogSummary {
  worklogId: number;
  teamId: number | null;
  teamName: string | null;
  authorId: number;
  authorName: string | null;
  title: string;
  requestContent: string | null;
  workContent: string | null;
  aiSummary: string | null;
  aiProcessingStatus: string | null;
  dueDate: string | null;
  actualHours: number | string | null;
  dependencyCount: number | null;
}

export interface FileItem {
  id: number;
  worklogId: number;
  originalName: string;
  storedPath: string;
  fileExtension: string;
  fileSizeBytes: number;
  aiSummary: string | null;
  aiProcessingStatus: string;
  createdAt: string;
  worklog: FileWorklogSummary | null;
}

export interface GetFilesParams {
  page?: number;
  pageSize?: number;
}

export interface FileFiltersValue {
  type: string;
  period: "ALL" | "7D" | "30D" | "90D";
  aiStatus: AiProcessingStatus | "ALL";
}
