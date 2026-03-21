import { describe, it, expect, vi, beforeEach } from 'vitest';
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
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('navigates to MR detail page when no detailed analysis', () => {
    render(
      <MemoryRouter>
        <AnalysisHistory analyses={[makeAnalysis()]} onDelete={vi.fn()} />
      </MemoryRouter>
    );

    fireEvent.click(screen.getByText('Test PR'));
    expect(mockNavigate).toHaveBeenCalledWith('/mr/1/10');
  });

  it('navigates to analysis detail page when hasDetailedAnalysis is true', () => {
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

  it('shows repo filter dropdown when multiple repos exist', () => {
    const analyses = [
      makeAnalysis({ reportId: 1, projectSlug: 'owner/repo1' }),
      makeAnalysis({ reportId: 2, projectSlug: 'owner/repo2', results: [
        {
          id: 20,
          externalId: '99',
          title: 'Other PR',
          author: 'dev2',
          score: 0.5,
          verdict: 'MAYBE',
          reasons: [],
          matchedRules: [],
          llmComment: null,
          url: 'https://github.com/owner/repo2/pull/99',
          hasDetailedAnalysis: false,
        },
      ] }),
    ];

    render(
      <MemoryRouter>
        <AnalysisHistory analyses={analyses} onDelete={vi.fn()} />
      </MemoryRouter>
    );

    // Filter dropdown should appear with repo options
    expect(screen.getByText(/Wszystkie/)).toBeInTheDocument();
    // Repo names appear in both the dropdown and the table rows, so use getAllByText
    expect(screen.getAllByText(/owner\/repo1/).length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText(/owner\/repo2/).length).toBeGreaterThanOrEqual(1);
  });

  it('filters rows by repo when dropdown changed', () => {
    const analyses = [
      makeAnalysis({ reportId: 1, projectSlug: 'owner/repo1', results: [
        {
          id: 10,
          externalId: '42',
          title: 'PR from repo1',
          author: 'dev1',
          score: 0.8,
          verdict: 'AUTOMATABLE',
          reasons: [],
          matchedRules: [],
          llmComment: null,
          url: 'https://github.com/owner/repo1/pull/42',
          hasDetailedAnalysis: false,
        },
      ] }),
      makeAnalysis({ reportId: 2, projectSlug: 'owner/repo2', results: [
        {
          id: 20,
          externalId: '99',
          title: 'PR from repo2',
          author: 'dev2',
          score: 0.5,
          verdict: 'MAYBE',
          reasons: [],
          matchedRules: [],
          llmComment: null,
          url: 'https://github.com/owner/repo2/pull/99',
          hasDetailedAnalysis: false,
        },
      ] }),
    ];

    render(
      <MemoryRouter>
        <AnalysisHistory analyses={analyses} onDelete={vi.fn()} />
      </MemoryRouter>
    );

    // Both PRs visible initially
    expect(screen.getByText('PR from repo1')).toBeInTheDocument();
    expect(screen.getByText('PR from repo2')).toBeInTheDocument();

    // Filter to repo2 only
    const select = screen.getByDisplayValue(/Wszystkie/);
    fireEvent.change(select, { target: { value: 'owner/repo2' } });

    expect(screen.queryByText('PR from repo1')).not.toBeInTheDocument();
    expect(screen.getByText('PR from repo2')).toBeInTheDocument();
  });

  it('delete confirmation shows MR count for multi-result analyses', () => {
    const confirmSpy = vi.fn(() => true);
    vi.stubGlobal('confirm', confirmSpy);
    const mockOnDelete = vi.fn();

    const analysis = makeAnalysis({
      reportId: 1,
      results: [
        {
          id: 10,
          externalId: '42',
          title: 'PR One',
          author: 'dev1',
          score: 0.8,
          verdict: 'AUTOMATABLE',
          reasons: [],
          matchedRules: [],
          llmComment: null,
          url: 'https://github.com/owner/repo/pull/42',
          hasDetailedAnalysis: false,
        },
        {
          id: 11,
          externalId: '43',
          title: 'PR Two',
          author: 'dev2',
          score: 0.5,
          verdict: 'MAYBE',
          reasons: [],
          matchedRules: [],
          llmComment: null,
          url: 'https://github.com/owner/repo/pull/43',
          hasDetailedAnalysis: false,
        },
      ],
    });

    render(
      <MemoryRouter>
        <AnalysisHistory analyses={[analysis]} onDelete={mockOnDelete} />
      </MemoryRouter>
    );

    // Click the first delete button
    const deleteButtons = screen.getAllByText('Usun');
    fireEvent.click(deleteButtons[0]);

    expect(confirmSpy).toHaveBeenCalledWith(
      'Usunac analize? Zostanie usunietych 2 wynikow MR.'
    );
    expect(mockOnDelete).toHaveBeenCalledWith(1);
  });

  it('delete confirmation shows simple message for single-result analysis', () => {
    const confirmSpy = vi.fn(() => true);
    vi.stubGlobal('confirm', confirmSpy);
    const mockOnDelete = vi.fn();

    render(
      <MemoryRouter>
        <AnalysisHistory analyses={[makeAnalysis()]} onDelete={mockOnDelete} />
      </MemoryRouter>
    );

    fireEvent.click(screen.getByText('Usun'));
    expect(confirmSpy).toHaveBeenCalledWith('Usunac analize?');
    expect(mockOnDelete).toHaveBeenCalledWith(1);
  });

  it('does not call onDelete when confirm is cancelled', () => {
    vi.stubGlobal('confirm', vi.fn(() => false));
    const mockOnDelete = vi.fn();

    render(
      <MemoryRouter>
        <AnalysisHistory analyses={[makeAnalysis()]} onDelete={mockOnDelete} />
      </MemoryRouter>
    );

    fireEvent.click(screen.getByText('Usun'));
    expect(mockOnDelete).not.toHaveBeenCalled();
  });

  it('navigates on Enter key', () => {
    render(
      <MemoryRouter>
        <AnalysisHistory analyses={[makeAnalysis()]} onDelete={vi.fn()} />
      </MemoryRouter>
    );

    const row = screen.getByText('Test PR').closest('tr')!;
    fireEvent.keyDown(row, { key: 'Enter' });
    expect(mockNavigate).toHaveBeenCalledWith('/mr/1/10');
  });

  it('shows verdict labels and classes', () => {
    const analysis = makeAnalysis({
      results: [
        {
          id: 10,
          externalId: '42',
          title: 'Auto PR',
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
    });

    render(
      <MemoryRouter>
        <AnalysisHistory analyses={[analysis]} onDelete={vi.fn()} />
      </MemoryRouter>
    );

    expect(screen.getByText('Auto')).toBeInTheDocument();
    expect(screen.getByText('Auto')).toHaveClass('text-success');
  });

  it('does not show filter dropdown for single repo', () => {
    render(
      <MemoryRouter>
        <AnalysisHistory analyses={[makeAnalysis()]} onDelete={vi.fn()} />
      </MemoryRouter>
    );

    expect(screen.queryByDisplayValue(/Wszystkie/)).not.toBeInTheDocument();
  });

  it('shows group badge for multi-result reports', () => {
    const analysis = makeAnalysis({
      results: [
        {
          id: 10,
          externalId: '42',
          title: 'PR One',
          author: 'dev1',
          score: 0.8,
          verdict: 'AUTOMATABLE',
          reasons: [],
          matchedRules: [],
          llmComment: null,
          url: 'https://github.com/owner/repo/pull/42',
          hasDetailedAnalysis: false,
        },
        {
          id: 11,
          externalId: '43',
          title: 'PR Two',
          author: 'dev2',
          score: 0.5,
          verdict: 'MAYBE',
          reasons: [],
          matchedRules: [],
          llmComment: null,
          url: 'https://github.com/owner/repo/pull/43',
          hasDetailedAnalysis: false,
        },
      ],
    });

    render(
      <MemoryRouter>
        <AnalysisHistory analyses={[analysis]} onDelete={vi.fn()} />
      </MemoryRouter>
    );

    expect(screen.getByText('x2')).toBeInTheDocument();
  });
});
