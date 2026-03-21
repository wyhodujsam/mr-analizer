import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import AnalysisHistory from '../components/AnalysisHistory';
import type { AnalysisResponse } from '../types';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...actual, useNavigate: () => mockNavigate };
});

function makeAnalysis(overrides: Partial<AnalysisResponse> = {}): AnalysisResponse {
  return {
    reportId: 1,
    projectSlug: 'owner/repo',
    provider: 'github',
    analyzedAt: '2026-03-20T10:00:00',
    totalMrs: 1,
    automatableCount: 1,
    maybeCount: 0,
    notSuitableCount: 0,
    results: [
      {
        id: 10,
        externalId: '42',
        title: 'Test PR',
        author: 'dev1',
        score: 0.8,
        verdict: 'AUTOMATABLE',
        reasons: [],
        matchedRules: [],
        llmComment: null,
        url: 'https://github.com/owner/repo/pull/42',
        hasDetailedAnalysis: false,
      },
    ],
    ...overrides,
  };
}

describe('AnalysisHistory', () => {
  it('navigates to MR detail page when no detailed analysis', () => {
    mockNavigate.mockClear();
    render(
      <MemoryRouter>
        <AnalysisHistory analyses={[makeAnalysis()]} onDelete={vi.fn()} />
      </MemoryRouter>
    );

    fireEvent.click(screen.getByText('Test PR'));
    expect(mockNavigate).toHaveBeenCalledWith('/mr/1/10');
  });

  it('navigates to analysis detail page when hasDetailedAnalysis is true', () => {
    mockNavigate.mockClear();
    const analysis = makeAnalysis({
      results: [
        {
          id: 10,
          externalId: '42',
          title: 'Detailed PR',
          author: 'dev1',
          score: 0.9,
          verdict: 'AUTOMATABLE',
          reasons: [],
          matchedRules: [],
          llmComment: 'good',
          url: 'https://github.com/owner/repo/pull/42',
          hasDetailedAnalysis: true,
        },
      ],
    });

    render(
      <MemoryRouter>
        <AnalysisHistory analyses={[analysis]} onDelete={vi.fn()} />
      </MemoryRouter>
    );

    fireEvent.click(screen.getByText('Detailed PR'));
    expect(mockNavigate).toHaveBeenCalledWith('/analysis/1/10');
  });

  it('renders nothing when analyses list is empty', () => {
    const { container } = render(
      <MemoryRouter>
        <AnalysisHistory analyses={[]} onDelete={vi.fn()} />
      </MemoryRouter>
    );
    expect(container.innerHTML).toBe('');
  });
});
