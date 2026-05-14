import { apiClient } from "@/app/_common/service/api-client";
import type { PageResponse } from "@/app/_common/types/api.types";
import type { FileItem, FileTypeOption, GetFilesParams } from "../_types/file.types";

export const fileService = {
  getFiles: (params: GetFilesParams = {}) =>
    apiClient.get<PageResponse<FileItem>>("/files", { params }),
  getFileTypes: () => apiClient.get<FileTypeOption[]>("/files/types"),
};
