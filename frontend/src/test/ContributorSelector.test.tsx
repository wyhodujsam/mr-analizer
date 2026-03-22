import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import ContributorSelector from '../components/activity/ContributorSelector';
import type { ContributorInfo } from '../types/activity';

describe('ContributorSelector', () => {
  const contributors: ContributorInfo[] = [
    { login: 'jan.kowalski', prCount: 15 },
    { login: 'anna.nowak', prCount: 8 },
  ];

  it('renders contributor options with PR counts', () => {
    render(<ContributorSelector contributors={contributors} selected="" onChange={vi.fn()} />);
    expect(screen.getByText('jan.kowalski (15 PR-ów)')).toBeInTheDocument();
    expect(screen.getByText('anna.nowak (8 PR-ów)')).toBeInTheDocument();
  });

  it('shows placeholder when nothing selected', () => {
    render(<ContributorSelector contributors={contributors} selected="" onChange={vi.fn()} />);
    expect(screen.getByText('— Wybierz kontrybutora —')).toBeInTheDocument();
  });

  it('calls onChange when selection changes', () => {
    const onChange = vi.fn();
    render(<ContributorSelector contributors={contributors} selected="" onChange={onChange} />);
    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'jan.kowalski' } });
    expect(onChange).toHaveBeenCalledWith('jan.kowalski');
  });

  it('disables when loading', () => {
    render(<ContributorSelector contributors={contributors} selected="" onChange={vi.fn()} loading />);
    expect(screen.getByRole('combobox')).toBeDisabled();
  });

  it('disables when no contributors', () => {
    render(<ContributorSelector contributors={[]} selected="" onChange={vi.fn()} />);
    expect(screen.getByRole('combobox')).toBeDisabled();
  });
});
