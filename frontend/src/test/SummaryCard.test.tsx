import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import SummaryCard from '../components/SummaryCard';

describe('SummaryCard', () => {
  it('renders counts and percentages', () => {
    render(
      <SummaryCard
        totalMrs={10}
        automatable={{ count: 5, percentage: 50 }}
        maybe={{ count: 3, percentage: 30 }}
        notSuitable={{ count: 2, percentage: 20 }}
      />
    );

    expect(screen.getByText('10')).toBeInTheDocument();
    expect(screen.getByText('5')).toBeInTheDocument();
    expect(screen.getByText('3')).toBeInTheDocument();
    expect(screen.getByText('2')).toBeInTheDocument();
    expect(screen.getByText('50.0%')).toBeInTheDocument();
    expect(screen.getByText('30.0%')).toBeInTheDocument();
    expect(screen.getByText('20.0%')).toBeInTheDocument();
  });

  it('shows correct labels', () => {
    render(
      <SummaryCard
        totalMrs={0}
        automatable={{ count: 0, percentage: 0 }}
        maybe={{ count: 0, percentage: 0 }}
        notSuitable={{ count: 0, percentage: 0 }}
      />
    );

    expect(screen.getByText('Do automatyzacji')).toBeInTheDocument();
    expect(screen.getByText('Moze')).toBeInTheDocument();
    expect(screen.getByText('Nie nadaje sie')).toBeInTheDocument();
    expect(screen.getByText(/Przeanalizowanych PR-ow/)).toBeInTheDocument();
  });

  it('renders with decimal percentages', () => {
    render(
      <SummaryCard
        totalMrs={3}
        automatable={{ count: 1, percentage: 33.3333 }}
        maybe={{ count: 1, percentage: 33.3333 }}
        notSuitable={{ count: 1, percentage: 33.3333 }}
      />
    );

    // toFixed(1) rounds
    expect(screen.getAllByText('33.3%')).toHaveLength(3);
  });
});
