import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import StatsCards from '../components/activity/StatsCards';
import type { ContributorStats } from '../types/activity';

describe('StatsCards', () => {
  it('renders all stat values', () => {
    const stats: ContributorStats = {
      totalPrs: 15,
      avgSize: 320.5,
      avgReviewTimeMinutes: 45,
      weekendPercentage: 13.3,
      flagCounts: { CRITICAL: 1, WARNING: 3, INFO: 2 },
    };
    render(<StatsCards stats={stats} activeSeverity={null} onSeverityClick={vi.fn()} />);
    expect(screen.getByText('15')).toBeInTheDocument();
    expect(screen.getByText('321 linii')).toBeInTheDocument();
    expect(screen.getByText('45 min')).toBeInTheDocument();
    expect(screen.getByText('13.3%')).toBeInTheDocument();
  });

  it('renders clickable severity badges', () => {
    const stats: ContributorStats = {
      totalPrs: 10,
      avgSize: 200,
      avgReviewTimeMinutes: 30,
      weekendPercentage: 0,
      flagCounts: { WARNING: 2 },
    };
    render(<StatsCards stats={stats} activeSeverity={null} onSeverityClick={vi.fn()} />);
    expect(screen.getByText('2 ostrzeżeń')).toBeInTheDocument();
  });

  it('formats hours correctly', () => {
    const stats: ContributorStats = {
      totalPrs: 5,
      avgSize: 100,
      avgReviewTimeMinutes: 90,
      weekendPercentage: 0,
      flagCounts: {},
    };
    render(<StatsCards stats={stats} activeSeverity={null} onSeverityClick={vi.fn()} />);
    expect(screen.getByText('1h 30m')).toBeInTheDocument();
  });
});
