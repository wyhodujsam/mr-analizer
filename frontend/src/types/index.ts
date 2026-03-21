export type Verdict = 'AUTOMATABLE' | 'MAYBE' | 'NOT_SUITABLE';

export interface AnalysisCategory {
  name: string;
  score: number;
  reasoning: string;
}

export interface HumanOversightItem {
  area: string;
  reasoning: string;
}

export interface SummaryAspect {
  aspect: string;
  score: number | null;
  note: string;
}

export interface AnalysisResultItem {
  id: number;
  externalId: string;
  title: string;
  author: string;
  score: number | null;
  verdict: Verdict | null;
  reasons: string[];
  matchedRules: string[];
  llmComment: string | null;
  url: string;
  hasDetailedAnalysis: boolean;
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
  sourceBranch: string | null;
  targetBranch: string | null;
  state: string | null;
  createdAt: string | null;
  mergedAt: string | null;
  labels: string[] | null;
  additions: number;
  deletions: number;
  changedFilesCount: number;
  hasTests: boolean;
  score: number | null;
  verdict: Verdict | null;
  scoreBreakdown: ScoreBreakdownEntry[] | null;
  llmComment: string | null;
  url: string;
  overallAutomatability: number | null;
  categories: AnalysisCategory[] | null;
  humanOversightRequired: HumanOversightItem[] | null;
  whyLlmFriendly: string[] | null;
  summaryTable: SummaryAspect[] | null;
  hasDetailedAnalysis: boolean;
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
  selectedMrIds?: string[];
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
