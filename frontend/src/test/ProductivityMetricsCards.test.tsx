import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import ProductivityMetricsCards from '../components/activity/ProductivityMetricsCards';
import type { ProductivityMetrics } from '../types/activity';

const mockProductivity: ProductivityMetrics = {
  velocity: {
    prsPerWeek: 3.0,
    weeklyBreakdown: [
      { weekStart: '2026-03-02', count: 2 },
      { weekStart: '2026-03-09', count: 4 },
      { weekStart: '2026-03-16', count: 3 },
      { weekStart: '2026-03-23', count: 3 },
    ],
    trend: 'stable',
  },
  cycleTime: {
    avgHours: 15.6,
    medianHours: 3.0,
    p90Hours: 48.0,
  },
  impact: {
    totalAdditions: 2500,
    totalDeletions: 800,
    totalLines: 3300,
    avgLinesPerPr: 330,
    addDeleteRatio: 3.125,
  },
  codeChurn: {
    churnRatio: 0.32,
    label: 'Zbalansowany',
  },
  reviewEngagement: {
    reviewsGiven: 8,
    reviewsReceived: 12,
    ratio: 0.67,
    label: 'Zbalansowany',
  },
};

describe('ProductivityMetricsCards', () => {
  it('renders velocity metrics', () => {
    render(<ProductivityMetricsCards productivity={mockProductivity} />);
    expect(screen.getByText('Wydajność')).toBeInTheDocument();
    expect(screen.getByText('3.0')).toBeInTheDocument();
    expect(screen.getByText('PR/tyg')).toBeInTheDocument();
  });

  it('renders cycle time metrics', () => {
    render(<ProductivityMetricsCards productivity={mockProductivity} />);
    expect(screen.getByText('3.0h')).toBeInTheDocument();
  });

  it('renders impact metrics', () => {
    render(<ProductivityMetricsCards productivity={mockProductivity} />);
    expect(screen.getByText('3,300')).toBeInTheDocument();
    expect(screen.getByText('+2500')).toBeInTheDocument();
    expect(screen.getByText('-800')).toBeInTheDocument();
  });

  it('renders code churn', () => {
    render(<ProductivityMetricsCards productivity={mockProductivity} />);
    expect(screen.getByText('0.32')).toBeInTheDocument();
    expect(screen.getByText('Zbalansowany')).toBeInTheDocument();
  });

  it('renders review engagement', () => {
    render(<ProductivityMetricsCards productivity={mockProductivity} />);
    expect(screen.getByText('8')).toBeInTheDocument();
    expect(screen.getByText('12')).toBeInTheDocument();
  });

  it('renders nothing when productivity is null', () => {
    const { container } = render(<ProductivityMetricsCards productivity={null} />);
    expect(container.innerHTML).toBe('');
  });

  it('renders without review engagement when null', () => {
    const noEngagement = { ...mockProductivity, reviewEngagement: null };
    render(<ProductivityMetricsCards productivity={noEngagement} />);
    expect(screen.getByText('Wydajność')).toBeInTheDocument();
    expect(screen.queryByText('Review Engagement')).not.toBeInTheDocument();
  });
});
