import { useQuery } from "@tanstack/react-query";
import { fileService } from "../_service/file.service";
import type { GetFilesParams } from "../_types/file.types";

export const fileKeys = {
  all: ["files"] as const,
  list: (params: GetFilesParams = {}) =>
    [...fileKeys.all, "list", params] as const,
  types: () => [...fileKeys.all, "types"] as const,
};

export function useFileList(params: GetFilesParams = {}) {
  return useQuery({
    queryKey: fileKeys.list(params),
    queryFn: () => fileService.getFiles(params),
  });
}

export function useFileTypes() {
  return useQuery({
    queryKey: fileKeys.types(),
    queryFn: () => fileService.getFileTypes(),
  });
}
