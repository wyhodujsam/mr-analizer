import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import ActivityHeatmap from '../components/activity/ActivityHeatmap';
import type { DailyActivity } from '../types/activity';

const mockDaily: Record<string, DailyActivity> = {
  '2026-03-15': {
    count: 2,
    pullRequests: [
      { id: '1', title: 'PR #1', size: 100 },
      { id: '2', title: 'PR #2', size: 200 },
    ],
  },
  '2026-03-16': {
    count: 1,
    pullRequests: [{ id: '3', title: 'PR #3', size: 50 }],
  },
};

describe('ActivityHeatmap', () => {
  it('renders SVG with cells', () => {
    const onDayClick = vi.fn();
    const { container } = render(
      <ActivityHeatmap dailyActivity={mockDaily} onDayClick={onDayClick} />
    );
    const rects = container.querySelectorAll('rect');
    expect(rects.length).toBeGreaterThan(0);
  });

  it('renders legend', () => {
    const onDayClick = vi.fn();
    render(<ActivityHeatmap dailyActivity={mockDaily} onDayClick={onDayClick} />);
    expect(screen.getByText('Mniej')).toBeInTheDocument();
    expect(screen.getByText('Więcej')).toBeInTheDocument();
  });

  it('calls onDayClick when cell clicked', () => {
    const onDayClick = vi.fn();
    const { container } = render(
      <ActivityHeatmap dailyActivity={mockDaily} onDayClick={onDayClick} />
    );
    const rects = container.querySelectorAll('rect');
    if (rects.length > 0) {
      fireEvent.click(rects[0]);
      expect(onDayClick).toHaveBeenCalled();
    }
  });
});
