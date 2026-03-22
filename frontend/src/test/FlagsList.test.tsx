import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import FlagsList from '../components/activity/FlagsList';
import type { ActivityFlag } from '../types/activity';

describe('FlagsList', () => {
  it('shows empty message when no flags', () => {
    render(<FlagsList flags={[]} severityFilter={null} typeFilter="all" onTypeFilterChange={vi.fn()} />);
    expect(screen.getByText('Brak wykrytych nieprawidłowości')).toBeInTheDocument();
  });

  it('renders all flags when no filter', () => {
    const flags: ActivityFlag[] = [
      { type: 'LARGE_PR', displayName: 'Za duży PR', severity: 'WARNING', description: 'PR #1 ma 600 linii', prReference: '#1' },
      { type: 'SUSPICIOUS_QUICK_MERGE', displayName: 'Podejrzanie szybki merge', severity: 'CRITICAL', description: 'PR #2 w 3 min', prReference: '#2' },
      { type: 'WEEKEND_WORK', displayName: 'Praca w weekend', severity: 'INFO', description: 'PR #3 w sobotę', prReference: '#3' },
    ];
    render(<FlagsList flags={flags} severityFilter={null} typeFilter="all" onTypeFilterChange={vi.fn()} />);
    // Each displayName appears in table + dropdown, so use getAllByText
    expect(screen.getAllByText('Za duży PR').length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText('Podejrzanie szybki merge').length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText('Praca w weekend').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('3 z 3 flag')).toBeInTheDocument();
  });

  it('filters by severity', () => {
    const flags: ActivityFlag[] = [
      { type: 'LARGE_PR', displayName: 'Za duży PR', severity: 'WARNING', description: 'desc', prReference: '#1' },
      { type: 'WEEKEND_WORK', displayName: 'Praca w weekend', severity: 'INFO', description: 'desc', prReference: '#2' },
    ];
    render(<FlagsList flags={flags} severityFilter="WARNING" typeFilter="all" onTypeFilterChange={vi.fn()} />);
    expect(screen.getByText('1 z 2 flag')).toBeInTheDocument();
  });
});
