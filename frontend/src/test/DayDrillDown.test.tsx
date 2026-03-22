import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import DayDrillDown from '../components/activity/DayDrillDown';
import type { DailyActivity } from '../types/activity';

describe('DayDrillDown', () => {
  const activity: DailyActivity = {
    count: 2,
    pullRequests: [
      { id: '1', title: 'Fix login bug', size: 120 },
      { id: '2', title: 'Add tests', size: 45 },
    ],
  };

  it('renders PR list with titles and sizes', () => {
    render(<DayDrillDown date="2026-03-15" activity={activity} />);
    expect(screen.getByText('Fix login bug')).toBeInTheDocument();
    expect(screen.getByText('Add tests')).toBeInTheDocument();
    expect(screen.getByText('120 linii')).toBeInTheDocument();
    expect(screen.getByText('45 linii')).toBeInTheDocument();
  });

  it('shows date and count', () => {
    render(<DayDrillDown date="2026-03-15" activity={activity} />);
    expect(screen.getByText(/2026-03-15/)).toBeInTheDocument();
    expect(screen.getByText('2')).toBeInTheDocument();
  });

  it('shows PR numbers', () => {
    render(<DayDrillDown date="2026-03-15" activity={activity} />);
    expect(screen.getByText('#1')).toBeInTheDocument();
    expect(screen.getByText('#2')).toBeInTheDocument();
  });
});
