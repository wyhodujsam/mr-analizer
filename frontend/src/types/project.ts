export interface RuleResultItem {
  ruleName: string;
  matched: boolean;
  weight: number;
  reason: string;
}

export interface PrAnalysisRow {
  prId: string;
  title: string;
  author: string;
  state: string;
  url: string | null;
  createdAt: string;
  mergedAt: string | null;
  additions: number;
  deletions: number;
  aiScore: number;
  aiVerdict: 'AUTOMATABLE' | 'MAYBE' | 'NOT_SUITABLE';
  ruleResults: RuleResultItem[];
  llmComment: string | null;
  hasBdd: boolean;
  hasSdd: boolean;
  bddFiles: string[];
  sddFiles: string[];
}

export interface RuleFrequency {
  ruleName: string;
  matchCount: number;
  avgWeight: number;
}

export interface ScoreHistogramBucket {
  rangeStart: number;
  rangeEnd: number;
  count: number;
}

export interface ProjectSummary {
  totalPrs: number;
  automatableCount: number;
  maybeCount: number;
  notSuitableCount: number;
  automatablePercent: number;
  maybePercent: number;
  notSuitablePercent: number;
  avgScore: number;
  topRules: RuleFrequency[];
  histogram: ScoreHistogramBucket[];
  bddCount: number;
  bddPercent: number;
  sddCount: number;
  sddPercent: number;
}

export interface ProjectAnalysisResult {
  id: number | null;
  projectSlug: string;
  analyzedAt: string;
  summary: ProjectSummary;
  rows: PrAnalysisRow[];
}
