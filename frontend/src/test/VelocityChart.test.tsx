import { describe, it, expect } from 'vitest';
import { render } from '@testing-library/react';
import VelocityChart from '../components/activity/VelocityChart';

describe('VelocityChart', () => {
  it('renders 4 bars for weekly breakdown', () => {
    const breakdown = [
      { weekStart: '2026-03-02', count: 2 },
      { weekStart: '2026-03-09', count: 4 },
      { weekStart: '2026-03-16', count: 1 },
      { weekStart: '2026-03-23', count: 3 },
    ];
    const { container } = render(<VelocityChart breakdown={breakdown} />);
    const rects = container.querySelectorAll('rect');
    expect(rects).toHaveLength(4);
  });

  it('renders nothing for empty data', () => {
    const { container } = render(<VelocityChart breakdown={[]} />);
    expect(container.querySelector('svg')).toBeNull();
  });

  it('highlights last bar', () => {
    const breakdown = [
      { weekStart: '2026-03-02', count: 1 },
      { weekStart: '2026-03-09', count: 2 },
    ];
    const { container } = render(<VelocityChart breakdown={breakdown} />);
    const rects = container.querySelectorAll('rect');
    expect(rects[1].getAttribute('fill')).toBe('#0d6efd');
    expect(rects[0].getAttribute('fill')).toBe('#adb5bd');
  });
});
