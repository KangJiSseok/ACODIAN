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

export interface DirectorDashboard {
  totalProgress: DashboardProgress;
  departmentLoadBalanceIndex: number;
  weeklyCompleted: number;
  aiPipelineSuccessRate: number;
  departmentCompletionRates: DepartmentCompletionRate[];
  departmentWorkload: DepartmentLoad[];
  imminentAndOverdue: DashboardWorklogBrief[];
}
