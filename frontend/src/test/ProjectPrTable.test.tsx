import { describe, it, expect } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import ProjectPrTable from '../components/project/ProjectPrTable';
import type { PrAnalysisRow } from '../types/project';

const rows: PrAnalysisRow[] = [
  {
    prId: '1', title: 'Add login', author: 'alice', state: 'merged',
    url: 'https://github.com/test/1', createdAt: '2026-03-10T10:00:00', mergedAt: '2026-03-10T12:00:00',
    additions: 100, deletions: 20, aiScore: 0.85, aiVerdict: 'AUTOMATABLE',
    ruleResults: [{ ruleName: 'boost:hasTests', matched: true, weight: 0.15, reason: 'Has tests' }],
    llmComment: null, hasBdd: true, hasSdd: true,
    bddFiles: ['src/test/features/login.feature'], sddFiles: ['specs/001/spec.md'],
  },
  {
    prId: '2', title: 'Big refactor', author: 'bob', state: 'merged',
    url: null, createdAt: '2026-03-11T10:00:00', mergedAt: '2026-03-11T15:00:00',
    additions: 800, deletions: 200, aiScore: 0.25, aiVerdict: 'NOT_SUITABLE',
    ruleResults: [{ ruleName: 'penalize:largeDiff', matched: true, weight: -0.2, reason: 'Large diff' }],
    llmComment: null, hasBdd: false, hasSdd: false,
    bddFiles: [], sddFiles: [],
  },
];

describe('ProjectPrTable', () => {
  it('renders all rows', () => {
    render(<ProjectPrTable rows={rows} />);
    expect(screen.getByText('Add login')).toBeInTheDocument();
    expect(screen.getByText('Big refactor')).toBeInTheDocument();
  });

  it('shows BDD checkmark and cross', () => {
    render(<ProjectPrTable rows={rows} />);
    const checks = screen.getAllByText('✓');
    const crosses = screen.getAllByText('✗');
    expect(checks.length).toBeGreaterThanOrEqual(1);
    expect(crosses.length).toBeGreaterThanOrEqual(1);
  });

  it('filters by verdict', () => {
    render(<ProjectPrTable rows={rows} />);
    const select = screen.getAllByRole('combobox')[0];
    fireEvent.change(select, { target: { value: 'AUTOMATABLE' } });
    expect(screen.getByText('Add login')).toBeInTheDocument();
    expect(screen.queryByText('Big refactor')).not.toBeInTheDocument();
  });

  it('filters by BDD', () => {
    render(<ProjectPrTable rows={rows} />);
    const selects = screen.getAllByRole('combobox');
    fireEvent.change(selects[1], { target: { value: 'tak' } });
    expect(screen.getByText('Add login')).toBeInTheDocument();
    expect(screen.queryByText('Big refactor')).not.toBeInTheDocument();
  });

  it('expands drill-down on click', () => {
    render(<ProjectPrTable rows={rows} />);
    fireEvent.click(screen.getByText('Add login'));
    expect(screen.getAllByText('Score Breakdown').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('boost:hasTests')).toBeInTheDocument();
    expect(screen.getByText('src/test/features/login.feature')).toBeInTheDocument();
    expect(screen.getByText('specs/001/spec.md')).toBeInTheDocument();
  });

  it('shows row count with filters', () => {
    render(<ProjectPrTable rows={rows} />);
    expect(screen.getByText('2 / 2 PR-ów')).toBeInTheDocument();
  });
});
