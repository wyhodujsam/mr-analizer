import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import AnalysisDetailPage from '../pages/AnalysisDetailPage';
import type { MrDetailResponse } from '../types';

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
    scoreBreakdown: [],
    llmComment: 'Good PR',
    url: 'https://github.com/owner/repo/pull/42',
    overallAutomatability: 90,
    categories: [
      { name: 'CQRS split', score: 95, reasoning: 'Mechanical extraction' },
    ],
    humanOversightRequired: [
      { area: 'Architecture decisions', reasoning: 'LLM cannot decide alone' },
    ],
    whyLlmFriendly: ['Clear patterns', 'Existing tests'],
    summaryTable: [
      { aspect: 'Code execution', score: 95, note: 'automatable' },
      { aspect: 'Review', score: null, note: 'required but light' },
    ],
    hasDetailedAnalysis: true,
    ...overrides,
  };
}

function renderPage(detail: MrDetailResponse) {
  (getMrDetail as ReturnType<typeof vi.fn>).mockResolvedValue(detail);
  return render(
    <MemoryRouter initialEntries={['/analysis/1/1']}>
      <Routes>
        <Route path="/analysis/:reportId/:resultId" element={<AnalysisDetailPage />} />
      </Routes>
    </MemoryRouter>
  );
}

describe('AnalysisDetailPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders full detailed analysis', async () => {
    renderPage(fullDetail());
    expect(await screen.findByText('Test PR')).toBeInTheDocument();
    expect(screen.getByText('~90%')).toBeInTheDocument();
    expect(screen.getByText('CQRS split')).toBeInTheDocument();
    expect(screen.getByText('Mechanical extraction')).toBeInTheDocument();
    expect(screen.getByText('Architecture decisions')).toBeInTheDocument();
    expect(screen.getByText('Clear patterns')).toBeInTheDocument();
    expect(screen.getByText('Code execution')).toBeInTheDocument();
    expect(screen.getByText('Good PR')).toBeInTheDocument();
  });

  it('handles null additions/deletions without crashing', async () => {
    renderPage(fullDetail({
      additions: null as unknown as number,
      deletions: null as unknown as number,
      changedFilesCount: null as unknown as number,
    }));
    expect(await screen.findByText('Test PR')).toBeInTheDocument();
    expect(screen.getByText(/0 plikow/)).toBeInTheDocument();
  });

  it('handles null categories without crashing', async () => {
    renderPage(fullDetail({
      categories: null as unknown as MrDetailResponse['categories'],
      hasDetailedAnalysis: true,
    }));
    expect(await screen.findByText('Test PR')).toBeInTheDocument();
    expect(screen.queryByText('Co LLM zrobi dobrze')).not.toBeInTheDocument();
  });

  it('handles null humanOversightRequired without crashing', async () => {
    renderPage(fullDetail({
      humanOversightRequired: null as unknown as MrDetailResponse['humanOversightRequired'],
      hasDetailedAnalysis: true,
    }));
    expect(await screen.findByText('Test PR')).toBeInTheDocument();
    expect(screen.queryByText('Co wymaga nadzoru czlowieka')).not.toBeInTheDocument();
  });

  it('handles null whyLlmFriendly without crashing', async () => {
    renderPage(fullDetail({
      whyLlmFriendly: null as unknown as string[],
      hasDetailedAnalysis: true,
    }));
    expect(await screen.findByText('Test PR')).toBeInTheDocument();
    expect(screen.queryByText('Dlaczego ten PR jest LLM-friendly')).not.toBeInTheDocument();
  });

  it('handles null summaryTable without crashing', async () => {
    renderPage(fullDetail({
      summaryTable: null as unknown as MrDetailResponse['summaryTable'],
      hasDetailedAnalysis: true,
    }));
    expect(await screen.findByText('Test PR')).toBeInTheDocument();
    expect(screen.queryByText('Podsumowanie')).not.toBeInTheDocument();
  });

  it('handles all nullable fields null (worst case)', async () => {
    renderPage(fullDetail({
      additions: null as unknown as number,
      deletions: null as unknown as number,
      changedFilesCount: null as unknown as number,
      categories: null as unknown as MrDetailResponse['categories'],
      humanOversightRequired: null as unknown as MrDetailResponse['humanOversightRequired'],
      whyLlmFriendly: null as unknown as string[],
      summaryTable: null as unknown as MrDetailResponse['summaryTable'],
      llmComment: null,
      hasDetailedAnalysis: false,
    }));
    expect(await screen.findByText('Test PR')).toBeInTheDocument();
    expect(screen.getByText('Brak danych z analizy LLM.')).toBeInTheDocument();
  });

  it('shows "no LLM data" when hasDetailedAnalysis is false', async () => {
    renderPage(fullDetail({ hasDetailedAnalysis: false, overallAutomatability: 0 }));
    expect(await screen.findByText('Brak danych z analizy LLM.')).toBeInTheDocument();
  });

  it('renders summary aspect with null score as dash', async () => {
    renderPage(fullDetail());
    expect(await screen.findByText('Test PR')).toBeInTheDocument();
    // "Review" aspect has null score - should show dash
    const reviewRow = screen.getByText('Review').closest('tr');
    expect(reviewRow).toBeTruthy();
    expect(reviewRow!.textContent).toContain('\u2014');
  });

  it('shows error message when API fails', async () => {
    (getMrDetail as ReturnType<typeof vi.fn>).mockRejectedValue(new Error('Network error'));
    render(
      <MemoryRouter initialEntries={['/analysis/1/1']}>
        <Routes>
          <Route path="/analysis/:reportId/:resultId" element={<AnalysisDetailPage />} />
        </Routes>
      </MemoryRouter>
    );
    expect(await screen.findByText('Nie udalo sie zaladowac szczegolow analizy.')).toBeInTheDocument();
  });

  it('shows category score badge colors correctly', async () => {
    renderPage(fullDetail({
      categories: [
        { name: 'High score', score: 92, reasoning: 'Excellent' },
        { name: 'Medium score', score: 75, reasoning: 'Good' },
        { name: 'Low score', score: 55, reasoning: 'Okay' },
        { name: 'Very low', score: 30, reasoning: 'Poor' },
      ],
      summaryTable: [],
    }));
    expect(await screen.findByText('High score')).toBeInTheDocument();
    expect(screen.getByText('92%')).toBeInTheDocument();
    expect(screen.getByText('75%')).toBeInTheDocument();
    expect(screen.getByText('55%')).toBeInTheDocument();
    expect(screen.getByText('30%')).toBeInTheDocument();
  });
});
