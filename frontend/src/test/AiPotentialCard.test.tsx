import { describe, it, expect } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import AiPotentialCard from '../components/project/AiPotentialCard';
import type { ProjectSummary, PrAnalysisRow } from '../types/project';

const summary: ProjectSummary = {
  totalPrs: 3,
  automatableCount: 2, maybeCount: 0, notSuitableCount: 1,
  automatablePercent: 66.7, maybePercent: 0, notSuitablePercent: 33.3,
  avgScore: 0.62,
  topRules: [
    { ruleName: 'boost:hasTests', matchCount: 2, avgWeight: 0.15 },
    { ruleName: 'penalize:largeDiff', matchCount: 1, avgWeight: -0.2 },
  ],
  histogram: [
    { rangeStart: 0, rangeEnd: 0.2, count: 0 },
    { rangeStart: 0.2, rangeEnd: 0.4, count: 1 },
    { rangeStart: 0.4, rangeEnd: 0.6, count: 0 },
    { rangeStart: 0.6, rangeEnd: 0.8, count: 2 },
    { rangeStart: 0.8, rangeEnd: 1, count: 0 },
  ],
  bddCount: 1, bddPercent: 33,
  sddCount: 1, sddPercent: 33,
};

const rows: PrAnalysisRow[] = [
  {
    prId: '1', title: 'PR 1', author: 'a', state: 'merged', url: null,
    createdAt: '2026-03-10', mergedAt: '2026-03-10', additions: 200, deletions: 50,
    aiScore: 0.75, aiVerdict: 'AUTOMATABLE', ruleResults: [], llmComment: null,
    hasBdd: true, hasSdd: false, bddFiles: [], sddFiles: [],
  },
  {
    prId: '2', title: 'PR 2', author: 'b', state: 'merged', url: null,
    createdAt: '2026-03-11', mergedAt: '2026-03-11', additions: 100, deletions: 30,
    aiScore: 0.7, aiVerdict: 'AUTOMATABLE', ruleResults: [], llmComment: null,
    hasBdd: false, hasSdd: true, bddFiles: [], sddFiles: [],
  },
  {
    prId: '3', title: 'PR 3', author: 'c', state: 'merged', url: null,
    createdAt: '2026-03-12', mergedAt: '2026-03-12', additions: 600, deletions: 200,
    aiScore: 0.3, aiVerdict: 'NOT_SUITABLE', ruleResults: [], llmComment: null,
    hasBdd: false, hasSdd: false, bddFiles: [], sddFiles: [],
  },
];

describe('AiPotentialCard', () => {
  it('renders verdict counts in percent mode (default)', () => {
    render(<AiPotentialCard summary={summary} rows={rows} />);
    expect(screen.getByText('AI Potential')).toBeInTheDocument();
    expect(screen.getByText('Avg score: 0.62')).toBeInTheDocument();
  });

  it('renders top rules', () => {
    render(<AiPotentialCard summary={summary} rows={rows} />);
    expect(screen.getByText('boost:hasTests')).toBeInTheDocument();
    expect(screen.getByText('penalize:largeDiff')).toBeInTheDocument();
  });

  it('renders histogram', () => {
    render(<AiPotentialCard summary={summary} rows={rows} />);
    expect(screen.getByText('Ile PR-ów w każdym przedziale score')).toBeInTheDocument();
  });

  it('switches to lines mode and shows totals', () => {
    render(<AiPotentialCard summary={summary} rows={rows} />);
    fireEvent.click(screen.getByText('Linie kodu'));

    // Automatable: PR1(250) + PR2(130) = 380, NOT_SUITABLE: PR3(800) = 800
    // Total = 1180 (locale formatting may vary in test env)
    expect(screen.getAllByText((_, el) =>
      el?.textContent?.replace(/\s/g, '').includes('1180') ?? false
    ).length).toBeGreaterThanOrEqual(1);
  });

  it('switches lines sub-mode to added only', () => {
    render(<AiPotentialCard summary={summary} rows={rows} />);
    fireEvent.click(screen.getByText('Linie kodu'));
    fireEvent.click(screen.getByText('Dodane'));

    // Automatable additions: 200+100=300, NOT_SUITABLE: 600, total=900
    expect(screen.getByText(/Dodane linie: 900/)).toBeInTheDocument();
  });

  it('switches lines sub-mode to deleted only', () => {
    render(<AiPotentialCard summary={summary} rows={rows} />);
    fireEvent.click(screen.getByText('Linie kodu'));
    fireEvent.click(screen.getByText('Usunięte'));

    // Automatable deletions: 50+30=80, NOT_SUITABLE: 200, total=280
    expect(screen.getByText(/Usunięte linie: 280/)).toBeInTheDocument();
  });

  it('switches back to percent mode', () => {
    render(<AiPotentialCard summary={summary} rows={rows} />);
    fireEvent.click(screen.getByText('Linie kodu'));
    fireEvent.click(screen.getByText('%'));
    expect(screen.getByText('Avg score: 0.62')).toBeInTheDocument();
  });
});
