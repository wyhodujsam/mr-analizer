import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import MrDetailPage from '../pages/MrDetailPage';
import type { MrDetailResponse } from '../types';

// Mock the API module
vi.mock('../api/analysisApi', () => ({
  getMrDetail: vi.fn(),
}));

import { getMrDetail } from '../api/analysisApi';

function fullDetail(overrides: Partial<MrDetailResponse> = {}): MrDetailResponse {
  return {
    id: 1,
    externalId: '42',
    title: 'Test PR',
    author: 'dev1',
    description: 'Some description',
    sourceBranch: 'feature/test',
    targetBranch: 'main',
    state: 'merged',
    createdAt: '2026-03-20T10:00:00',
    mergedAt: '2026-03-20T12:00:00',
    labels: ['bugfix'],
    additions: 50,
    deletions: 10,
    changedFilesCount: 5,
    hasTests: true,
    score: 0.75,
    verdict: 'AUTOMATABLE',
    scoreBreakdown: [{ rule: 'boost-by-has-tests', type: 'boost', weight: 0.15, reason: 'has tests' }],
    llmComment: null,
    url: 'https://github.com/owner/repo/pull/42',
    overallAutomatability: 0,
    categories: [],
    humanOversightRequired: [],
    whyLlmFriendly: [],
    summaryTable: [],
    hasDetailedAnalysis: false,
    ...overrides,
  };
}

function renderPage(detail: MrDetailResponse) {
  (getMrDetail as ReturnType<typeof vi.fn>).mockResolvedValue(detail);
  return render(
    <MemoryRouter initialEntries={['/mr/1/1']}>
      <Routes>
        <Route path="/mr/:reportId/:resultId" element={<MrDetailPage />} />
      </Routes>
    </MemoryRouter>
  );
}

describe('MrDetailPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders with full data', async () => {
    renderPage(fullDetail());
    expect(await screen.findByText('Test PR')).toBeInTheDocument();
    expect(screen.getByText(/42.*autor/)).toBeTruthy();
  });

  it('handles null labels without crashing', async () => {
    renderPage(fullDetail({ labels: null as unknown as string[] }));
    expect(await screen.findByText('Test PR')).toBeInTheDocument();
    expect(screen.getByText('brak')).toBeInTheDocument();
  });

  it('handles null additions/deletions without crashing', async () => {
    renderPage(fullDetail({
      additions: null as unknown as number,
      deletions: null as unknown as number,
      changedFilesCount: null as unknown as number,
    }));
    expect(await screen.findByText('Test PR')).toBeInTheDocument();
    expect(screen.getByText('+0')).toBeInTheDocument();
  });

  it('handles null scoreBreakdown without crashing', async () => {
    renderPage(fullDetail({ scoreBreakdown: null as unknown as MrDetailResponse['scoreBreakdown'] }));
    expect(await screen.findByText('Test PR')).toBeInTheDocument();
    // Score Breakdown section is hidden when data is empty/useless
    expect(screen.queryByText('Rozbicie punktacji')).not.toBeInTheDocument();
  });

  it('handles null sourceBranch/targetBranch/state without crashing', async () => {
    renderPage(fullDetail({
      sourceBranch: null as unknown as string,
      targetBranch: null as unknown as string,
      state: null as unknown as string,
    }));
    expect(await screen.findByText('Test PR')).toBeInTheDocument();
  });

  it('handles all nullable fields being null (worst case API response)', async () => {
    renderPage(fullDetail({
      description: null,
      sourceBranch: null as unknown as string,
      targetBranch: null as unknown as string,
      state: null as unknown as string,
      createdAt: null as unknown as string,
      mergedAt: null,
      labels: null as unknown as string[],
      additions: null as unknown as number,
      deletions: null as unknown as number,
      changedFilesCount: null as unknown as number,
      scoreBreakdown: null as unknown as MrDetailResponse['scoreBreakdown'],
      llmComment: null,
    }));
    expect(await screen.findByText('Test PR')).toBeInTheDocument();
  });

  it('shows link to analysis details when hasDetailedAnalysis is true', async () => {
    renderPage(fullDetail({ hasDetailedAnalysis: true }));
    expect(await screen.findByText('Szczegoly analizy LLM')).toBeTruthy();
  });

  it('hides link to analysis details when hasDetailedAnalysis is false', async () => {
    renderPage(fullDetail({ hasDetailedAnalysis: false }));
    await screen.findByText('Test PR');
    expect(screen.queryByText('Szczegoly analizy LLM')).not.toBeInTheDocument();
  });
});
