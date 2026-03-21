import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import MrTable from '../components/MrTable';
import type { AnalysisResultItem } from '../types';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...actual, useNavigate: () => mockNavigate };
});

function makeItem(overrides: Partial<AnalysisResultItem> = {}): AnalysisResultItem {
  return {
    id: 1,
    externalId: '42',
    title: 'Fix login bug',
    author: 'dev1',
    score: 0.85,
    verdict: 'AUTOMATABLE',
    reasons: ['small diff'],
    matchedRules: ['boost-by-has-tests'],
    llmComment: null,
    url: 'https://github.com/owner/repo/pull/42',
    hasDetailedAnalysis: false,
    ...overrides,
  };
}

describe('MrTable', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders table with results', () => {
    const items = [
      makeItem({ id: 1, externalId: '42', title: 'Fix login bug', author: 'dev1' }),
      makeItem({ id: 2, externalId: '43', title: 'Add feature', author: 'dev2' }),
    ];
    render(
      <MemoryRouter>
        <MrTable results={items} reportId={10} />
      </MemoryRouter>
    );

    expect(screen.getByText('Fix login bug')).toBeInTheDocument();
    expect(screen.getByText('Add feature')).toBeInTheDocument();
    expect(screen.getByText('dev1')).toBeInTheDocument();
    expect(screen.getByText('dev2')).toBeInTheDocument();
  });

  it('navigates on row click', () => {
    render(
      <MemoryRouter>
        <MrTable results={[makeItem()]} reportId={10} />
      </MemoryRouter>
    );

    fireEvent.click(screen.getByText('Fix login bug'));
    expect(mockNavigate).toHaveBeenCalledWith('/mr/10/1');
  });

  it('navigates on Enter key', () => {
    render(
      <MemoryRouter>
        <MrTable results={[makeItem()]} reportId={10} />
      </MemoryRouter>
    );

    const row = screen.getByText('Fix login bug').closest('tr')!;
    fireEvent.keyDown(row, { key: 'Enter' });
    expect(mockNavigate).toHaveBeenCalledWith('/mr/10/1');
  });

  it('shows LLM badge when hasDetailedAnalysis', () => {
    render(
      <MemoryRouter>
        <MrTable results={[makeItem({ hasDetailedAnalysis: true })]} reportId={10} />
      </MemoryRouter>
    );

    expect(screen.getByText('LLM')).toBeInTheDocument();
  });

  it('does not show LLM badge when hasDetailedAnalysis is false', () => {
    render(
      <MemoryRouter>
        <MrTable results={[makeItem({ hasDetailedAnalysis: false })]} reportId={10} />
      </MemoryRouter>
    );

    expect(screen.queryByText('LLM')).not.toBeInTheDocument();
  });

  it('shows ScoreBadge with correct score', () => {
    render(
      <MemoryRouter>
        <MrTable results={[makeItem({ score: 0.85, verdict: 'AUTOMATABLE' })]} reportId={10} />
      </MemoryRouter>
    );

    expect(screen.getByText('0.85')).toBeInTheDocument();
  });

  it('shows "Szczegoly analizy" button for detailed analysis', () => {
    render(
      <MemoryRouter>
        <MrTable results={[makeItem({ hasDetailedAnalysis: true })]} reportId={10} />
      </MemoryRouter>
    );

    const btn = screen.getByText('Szczegoly analizy');
    expect(btn).toBeInTheDocument();
    fireEvent.click(btn);
    expect(mockNavigate).toHaveBeenCalledWith('/analysis/10/1');
  });

  it('does not show "Szczegoly analizy" button when no detailed analysis', () => {
    render(
      <MemoryRouter>
        <MrTable results={[makeItem({ hasDetailedAnalysis: false })]} reportId={10} />
      </MemoryRouter>
    );

    expect(screen.queryByText('Szczegoly analizy')).not.toBeInTheDocument();
  });

  it('applies verdict CSS classes correctly', () => {
    const items = [
      makeItem({ id: 1, verdict: 'AUTOMATABLE', title: 'Auto MR' }),
      makeItem({ id: 2, verdict: 'MAYBE', title: 'Maybe MR' }),
      makeItem({ id: 3, verdict: 'NOT_SUITABLE', title: 'Not MR' }),
    ];
    render(
      <MemoryRouter>
        <MrTable results={items} reportId={10} />
      </MemoryRouter>
    );

    expect(screen.getByText('Auto MR').closest('tr')).toHaveClass('verdict-automatable');
    expect(screen.getByText('Maybe MR').closest('tr')).toHaveClass('verdict-maybe');
    expect(screen.getByText('Not MR').closest('tr')).toHaveClass('verdict-not-suitable');
  });
});
