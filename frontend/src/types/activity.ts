export type Severity = 'CRITICAL' | 'WARNING' | 'INFO';

export interface ContributorInfo {
  login: string;
  prCount: number;
}

export interface ActivityFlag {
  type: string;
  displayName: string;
  severity: Severity;
  description: string;
  prReference: string | null;
}

export interface ContributorStats {
  totalPrs: number;
  avgSize: number;
  avgReviewTimeMinutes: number;
  weekendPercentage: number;
  flagCounts: Record<string, number>;
}

export interface DailyPr {
  id: string;
  title: string;
  size: number;
}

export interface DailyActivity {
  count: number;
  pullRequests: DailyPr[];
}

export interface PullRequestItem {
  id: string;
  title: string;
  size: number;
  createdAt: string;
  mergedAt: string | null;
  state: string;
  flags: ActivityFlag[];
}

export interface ActivityReport {
  contributor: string;
  projectSlug: string;
  stats: ContributorStats;
  flags: ActivityFlag[];
  dailyActivity: Record<string, DailyActivity>;
  pullRequests: PullRequestItem[];
}
