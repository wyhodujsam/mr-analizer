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

export interface WeeklyCount {
  weekStart: string;
  count: number;
}

export interface VelocityMetrics {
  prsPerWeek: number;
  weeklyBreakdown: WeeklyCount[];
  trend: 'rising' | 'falling' | 'stable';
}

export interface CycleTimeMetrics {
  avgHours: number;
  medianHours: number;
  p90Hours: number;
}

export interface ImpactMetrics {
  totalAdditions: number;
  totalDeletions: number;
  totalLines: number;
  avgLinesPerPr: number;
  addDeleteRatio: number;
}

export interface CodeChurnMetrics {
  churnRatio: number;
  label: string;
}

export interface ReviewEngagementMetrics {
  reviewsGiven: number;
  reviewsReceived: number;
  ratio: number;
  label: string;
}

export interface ProductivityMetrics {
  velocity: VelocityMetrics;
  cycleTime: CycleTimeMetrics;
  impact: ImpactMetrics;
  codeChurn: CodeChurnMetrics;
  reviewEngagement: ReviewEngagementMetrics | null;
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
  productivity: ProductivityMetrics | null;
  flags: ActivityFlag[];
  dailyActivity: Record<string, DailyActivity>;
  pullRequests: PullRequestItem[];
}
