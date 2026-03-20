export type Verdict = 'AUTOMATABLE' | 'MAYBE' | 'NOT_SUITABLE';

export interface AnalysisResultItem {
  id: number;
  externalId: string;
  title: string;
  author: string;
  score: number;
  verdict: Verdict;
  reasons: string[];
  matchedRules: string[];
  llmComment: string | null;
  url: string;
}

export interface AnalysisResponse {
  reportId: number;
  projectSlug: string;
  provider: string;
  analyzedAt: string;
  totalMrs: number;
  automatableCount: number;
  maybeCount: number;
  notSuitableCount: number;
  results: AnalysisResultItem[];
}

export interface ScoreBreakdownEntry {
  rule: string;
  type: string;
  weight: number;
  reason: string;
}

export interface MrDetailResponse {
  id: number;
  externalId: string;
  title: string;
  author: string;
  description: string | null;
  sourceBranch: string;
  targetBranch: string;
  state: string;
  createdAt: string;
  mergedAt: string | null;
  labels: string[];
  diffStats: { additions: number; deletions: number; changedFilesCount: number };
  hasTests: boolean;
  score: number;
  verdict: Verdict;
  scoreBreakdown: ScoreBreakdownEntry[];
  llmComment: string | null;
  url: string;
}

export interface AnalysisSummary {
  reportId: number;
  projectSlug: string;
  totalMrs: number;
  automatable: { count: number; percentage: number };
  maybe: { count: number; percentage: number };
  notSuitable: { count: number; percentage: number };
}

export interface AnalysisRequest {
  projectSlug: string;
  provider: string;
  targetBranch?: string;
  state?: string;
  after?: string;
  before?: string;
  limit: number;
  useLlm: boolean;
}

export interface ErrorResponse {
  error: string;
  message: string;
}

export interface SavedRepository {
  id: number;
  projectSlug: string;
  provider: string;
  addedAt: string;
  lastAnalyzedAt: string | null;
}

export interface MrBrowseItem {
  externalId: string;
  title: string;
  author: string;
  createdAt: string;
  mergedAt: string | null;
  state: string;
  changedFilesCount: number;
  labels: string[];
  url: string;
}
