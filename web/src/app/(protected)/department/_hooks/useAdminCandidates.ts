import { useQuery } from "@tanstack/react-query";
import { adminCandidateService } from "../_service/admin-candidate.service";

export const adminCandidateKeys = {
  all: ["adminCandidates"] as const,
  list: () => [...adminCandidateKeys.all, "list"] as const,
};

export function useAdminCandidates() {
  return useQuery({
    queryKey: adminCandidateKeys.list(),
    queryFn: adminCandidateService.getAdminCandidates,
  });
}
