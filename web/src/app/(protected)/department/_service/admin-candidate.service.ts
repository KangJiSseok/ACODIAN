import { apiClient } from "@/app/_common/service/api-client";
import type { AdminCandidate } from "../_types/admin-candidate.types";

export const adminCandidateService = {
  // 부서장(=admin)으로 지정 가능한 사용자 후보 목록을 조회합니다.
  getAdminCandidates: () =>
    apiClient.get<AdminCandidate[]>("/users/admin-candidates"),
};
