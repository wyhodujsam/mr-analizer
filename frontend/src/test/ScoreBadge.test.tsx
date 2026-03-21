import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import ScoreBadge from '../components/ScoreBadge';

describe('ScoreBadge', () => {
  it('shows dash when score is null', () => {
    render(<ScoreBadge score={null} verdict={null} />);
    expect(screen.getByText('\u2014')).toBeInTheDocument();
  });

  it('shows dash when verdict is null but score exists', () => {
    render(<ScoreBadge score={0.5} verdict={null} />);
    expect(screen.getByText('\u2014')).toBeInTheDocument();
  });

  it('shows score with success bg for AUTOMATABLE', () => {
    render(<ScoreBadge score={0.85} verdict="AUTOMATABLE" />);
    const badge = screen.getByText('0.85');
    expect(badge).toBeInTheDocument();
    expect(badge).toHaveClass('bg-success');
  });

  it('shows score with warning bg for MAYBE', () => {
    render(<ScoreBadge score={0.55} verdict="MAYBE" />);
    const badge = screen.getByText('0.55');
    expect(badge).toHaveClass('bg-warning');
  });

  it('shows score with danger bg for NOT_SUITABLE', () => {
    render(<ScoreBadge score={0.2} verdict="NOT_SUITABLE" />);
    const badge = screen.getByText('0.20');
    expect(badge).toHaveClass('bg-danger');
  });
});
