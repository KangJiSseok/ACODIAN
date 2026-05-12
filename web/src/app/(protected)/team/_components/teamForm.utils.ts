import type { TeamUserCandidate } from "../_types/team.types";

export interface TeamMemberSelection {
  userId: number;
  teamRole: string;
  isLeader: boolean;
}

interface CandidateFilter {
  query: string;
  departmentName: string;
  rankName: string;
}

export const ALL_MEMBER_FILTER_VALUE = "all";

export function getCandidateRankName(candidate: TeamUserCandidate) {
  return candidate.titleName ?? candidate.positionName ?? "";
}

export function filterTeamUserCandidates(
  candidates: TeamUserCandidate[],
  filters: CandidateFilter,
) {
  const normalizedQuery = filters.query.trim().toLowerCase();

  return candidates.filter((candidate) => {
    const departmentMatches =
      filters.departmentName === ALL_MEMBER_FILTER_VALUE ||
      candidate.departmentName === filters.departmentName;
    const rankMatches =
      filters.rankName === ALL_MEMBER_FILTER_VALUE ||
      candidate.titleName === filters.rankName ||
      candidate.positionName === filters.rankName;

    if (!departmentMatches || !rankMatches) {
      return false;
    }

    if (!normalizedQuery) {
      return true;
    }

    return [
      candidate.userName,
      candidate.email,
      candidate.departmentName,
      candidate.positionName,
      candidate.titleName,
    ]
      .filter(Boolean)
      .join(" ")
      .toLowerCase()
      .includes(normalizedQuery);
  });
}

export function addTeamMemberSelection(
  current: TeamMemberSelection[],
  userId: number,
) {
  if (current.some((member) => member.userId === userId)) {
    return current;
  }

  return [
    ...current,
    {
      userId,
      teamRole: "",
      isLeader: current.length === 0,
    },
  ];
}
