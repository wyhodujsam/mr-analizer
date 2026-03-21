import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';

// Mock chart.js components to avoid canvas issues in jsdom
vi.mock('react-chartjs-2', () => ({
  Doughnut: (props: { data: { labels: string[]; datasets: { data: number[] }[] } }) => (
    <div data-testid="doughnut-chart">
      {props.data.labels.map((l: string) => (
        <span key={l}>{l}</span>
      ))}
    </div>
  ),
}));

vi.mock('chart.js', () => ({
  Chart: { register: vi.fn() },
  ArcElement: {},
  Tooltip: {},
  Legend: {},
}));

import VerdictPieChart from '../components/VerdictPieChart';

describe('VerdictPieChart', () => {
  it('renders without crashing with data', () => {
    render(<VerdictPieChart automatable={5} maybe={3} notSuitable={2} />);
    expect(screen.getByTestId('doughnut-chart')).toBeInTheDocument();
  });

  it('receives correct data labels', () => {
    render(<VerdictPieChart automatable={5} maybe={3} notSuitable={2} />);
    expect(screen.getByText('Automatable (5)')).toBeInTheDocument();
    expect(screen.getByText('Maybe (3)')).toBeInTheDocument();
    expect(screen.getByText('Not Suitable (2)')).toBeInTheDocument();
  });

  it('returns null when all values are zero', () => {
    const { container } = render(
      <VerdictPieChart automatable={0} maybe={0} notSuitable={0} />
    );
    expect(container.innerHTML).toBe('');
  });

  it('renders when only one category has values', () => {
    render(<VerdictPieChart automatable={10} maybe={0} notSuitable={0} />);
    expect(screen.getByTestId('doughnut-chart')).toBeInTheDocument();
    expect(screen.getByText('Automatable (10)')).toBeInTheDocument();
  });
});
