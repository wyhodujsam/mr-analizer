import type { ContributorInfo, ActivityReport } from '../src/types/activity';

export const mockRepos = [
  { id: 1, projectSlug: 'test/repo', provider: 'github', lastAnalyzedAt: '2026-03-20T10:00:00' },
];

export const mockContributors: ContributorInfo[] = [
  { login: 'alice', prCount: 5 },
  { login: 'bob', prCount: 3 },
];

export const mockActivityReport: ActivityReport = {
  contributor: 'alice',
  projectSlug: 'test/repo',
  stats: {
    totalPrs: 5,
    avgSize: 320,
    avgReviewTimeMinutes: 45,
    weekendPercentage: 20,
    flagCounts: { CRITICAL: 1, WARNING: 2, INFO: 1 },
  },
  flags: [
    { type: 'SUSPICIOUS_QUICK_MERGE', displayName: 'Podejrzanie szybki merge', severity: 'CRITICAL', description: 'PR #2 w 2 min', prReference: '#2' },
    { type: 'LARGE_PR', displayName: 'Za duży PR', severity: 'WARNING', description: 'PR #3 ma 650 linii', prReference: '#3' },
    { type: 'NO_REVIEW', displayName: 'Brak review', severity: 'WARNING', description: 'PR #4 bez review', prReference: '#4' },
    { type: 'WEEKEND_WORK', displayName: 'Praca w weekend', severity: 'INFO', description: 'PR #5 w sobotę', prReference: '#5' },
  ],
  dailyActivity: {
    '2026-03-10': { count: 2, pullRequests: [{ id: '1', title: 'Fix auth', size: 200 }, { id: '2', title: 'Update deps', size: 50 }] },
    '2026-03-15': { count: 1, pullRequests: [{ id: '3', title: 'Refactor DB', size: 650 }] },
  },
  pullRequests: [
    { id: '1', title: 'Fix auth', size: 200, createdAt: '2026-03-10T10:00:00', mergedAt: '2026-03-10T11:00:00', state: 'merged', flags: [] },
    { id: '2', title: 'Update deps', size: 50, createdAt: '2026-03-10T14:00:00', mergedAt: '2026-03-10T14:02:00', state: 'merged', flags: [] },
  ],
};

export const mockBrowseResponse = [
  { externalId: '1', title: 'Fix auth', author: 'alice', createdAt: '2026-03-10T10:00:00', mergedAt: '2026-03-10T11:00:00', state: 'merged', changedFilesCount: 8, labels: [], url: 'https://github.com/test/repo/pull/1' },
  { externalId: '2', title: 'Update deps', author: 'bob', createdAt: '2026-03-12T09:00:00', mergedAt: '2026-03-12T09:30:00', state: 'merged', changedFilesCount: 3, labels: [], url: 'https://github.com/test/repo/pull/2' },
];

export const mockAnalysisResponse = {
  reportId: 1,
  projectSlug: 'test/repo',
  createdAt: '2026-03-20T10:00:00',
  results: [
    { resultId: 1, externalId: '1', title: 'Fix auth', author: 'alice', score: 0.75, verdict: 'AUTOMATABLE', reasons: ['Has tests'], matchedRules: ['hasTests'], llmComment: null, hasDetailedAnalysis: false },
  ],
  counts: { automatable: 1, maybe: 0, notSuitable: 0, total: 1 },
};
