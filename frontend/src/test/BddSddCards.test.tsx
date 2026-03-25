import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import BddSddCards from '../components/project/BddSddCards';
import type { ProjectSummary } from '../types/project';

const summary: ProjectSummary = {
  totalPrs: 50,
  automatableCount: 30, maybeCount: 12, notSuitableCount: 8,
  automatablePercent: 60, maybePercent: 24, notSuitablePercent: 16,
  avgScore: 0.62,
  topRules: [], histogram: [],
  bddCount: 18, bddPercent: 36,
  sddCount: 10, sddPercent: 20,
};

describe('BddSddCards', () => {
  it('renders BDD percentage and count', () => {
    render(<BddSddCards summary={summary} />);
    expect(screen.getByText('36%')).toBeInTheDocument();
    expect(screen.getByText(/18 z 50/)).toBeInTheDocument();
  });

  it('renders SDD percentage and count', () => {
    render(<BddSddCards summary={summary} />);
    expect(screen.getByText('20%')).toBeInTheDocument();
    expect(screen.getByText(/10 z 50/)).toBeInTheDocument();
  });

  it('shows correct badge for BDD partial coverage', () => {
    render(<BddSddCards summary={summary} />);
    // 36% → "Częściowe" (yellow badge)
    expect(screen.getAllByText('Częściowe').length).toBeGreaterThanOrEqual(1);
  });

  it('shows low coverage badge when below 20%', () => {
    const low = { ...summary, bddPercent: 10, sddPercent: 5 };
    render(<BddSddCards summary={low} />);
    expect(screen.getAllByText('Niskie pokrycie').length).toBeGreaterThanOrEqual(1);
  });
});
