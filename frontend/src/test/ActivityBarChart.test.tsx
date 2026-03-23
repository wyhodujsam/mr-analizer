import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import ActivityBarChart from '../components/activity/ActivityBarChart';
import type { DailyActivity } from '../types/activity';

// Mock chart.js to avoid canvas issues in jsdom
vi.mock('react-chartjs-2', () => ({
  Bar: (props: any) => (
    <div data-testid="bar-chart" data-labels={JSON.stringify(props.data.labels)}>
      Bar Chart
    </div>
  ),
}));

describe('ActivityBarChart', () => {
  it('renders chart with sorted dates', () => {
    const daily: Record<string, DailyActivity> = {
      '2026-03-15': { count: 1, pullRequests: [{ id: '1', title: 'PR 1', size: 100 }] },
      '2026-03-10': { count: 2, pullRequests: [{ id: '2', title: 'PR 2', size: 50 }, { id: '3', title: 'PR 3', size: 30 }] },
    };
    render(<ActivityBarChart dailyActivity={daily} />);
    const chart = screen.getByTestId('bar-chart');
    const labels = JSON.parse(chart.getAttribute('data-labels')!);
    expect(labels).toEqual(['2026-03-10', '2026-03-15']);
  });

  it('returns null for empty data', () => {
    const { container } = render(<ActivityBarChart dailyActivity={{}} />);
    expect(container.innerHTML).toBe('');
  });
});
