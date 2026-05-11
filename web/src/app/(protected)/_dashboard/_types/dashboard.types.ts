export interface DashboardProgress {
  completed: number;
  total: number;
  rate: number;
}

export interface DepartmentCompletionRate {
  departmentId: number;
  departmentName: string;
  completed: number;
  total: number;
  rate: number;
}

export interface DepartmentLoad {
  departmentId: number;
  departmentName: string;
  activeWorklogCount: number;
}

export interface TeamCompletionRate {
  teamId: number;
  teamName: string;
  completed: number;
  total: number;
  rate: number;
}

export interface TeamLoad {
  teamId: number;
  teamName: string;
  activeWorklogCount: number;
}

export interface DashboardWorklogBrief {
  worklogId: number;
  title: string;
  statusCode: string;
  dueDate: string | null;
  daysOverdue: number | null;
  teamName: string | null;
  departmentName: string | null;
  authorName: string | null;
}

export interface DashboardCompletedInPeriod {
  from: string;
  to: string;
  count: number;
}

export interface DashboardPredecessorBrief {
  worklogId: number;
  title: string;
  statusCode: string;
}

export interface DashboardBlockedWorklog {
  worklogId: number;
  title: string;
  predecessors: DashboardPredecessorBrief[];
}

export interface DirectorDashboard {
  totalProgress: DashboardProgress;
  departmentLoadBalanceIndex: number;
  weeklyCompleted: number;
  aiPipelineSuccessRate: number;
  departmentCompletionRates: DepartmentCompletionRate[];
  departmentWorkload: DepartmentLoad[];
  imminentAndOverdue: DashboardWorklogBrief[];
}

export interface DepartmentDashboard {
  departmentId: number;
  departmentName: string;
  totalProgress: DashboardProgress;
  teamLoadBalanceIndex: number;
  weeklyCompleted: number;
  aiPipelineSuccessRate: number;
  teamCompletionRates: TeamCompletionRate[];
  teamWorkload: TeamLoad[];
  imminentAndOverdue: DashboardWorklogBrief[];
}

export interface MyDashboard {
  inProgressCount: number;
  completedInPeriod: DashboardCompletedInPeriod;
  aiFailedCount: number;
  thisWeekDue: DashboardWorklogBrief[];
  todayItems: DashboardWorklogBrief[];
  imminentAndOverdue: DashboardWorklogBrief[];
  blockedByPredecessors: DashboardBlockedWorklog[];
}

export type DashboardScopeSelection =
  | {
      view: "ADMIN";
      scope: "DEPARTMENT_COMPARISON";
    }
  | {
      view: "ADMIN";
      scope: "DEPARTMENT_DETAIL";
      departmentId: number;
    }
  | {
      view: "ME";
      scope: "ME";
      teamId: number;
    };
