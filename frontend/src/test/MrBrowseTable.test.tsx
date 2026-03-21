import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import MrBrowseTable from '../components/MrBrowseTable';
import type { MrBrowseItem } from '../types';

function makeItem(overrides: Partial<MrBrowseItem> = {}): MrBrowseItem {
  return {
    externalId: '42',
    title: 'Fix login bug',
    author: 'dev1',
    createdAt: '2026-03-20T10:00:00',
    mergedAt: null,
    state: 'merged',
    changedFilesCount: 5,
    labels: [],
    url: 'https://github.com/owner/repo/pull/42',
    ...overrides,
  };
}

describe('MrBrowseTable', () => {
  let mockOnSelectionChange: ReturnType<typeof vi.fn>;
  let mockWindowOpen: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    vi.clearAllMocks();
    mockOnSelectionChange = vi.fn();
    mockWindowOpen = vi.fn();
    vi.stubGlobal('open', mockWindowOpen);
  });

  it('renders items with checkboxes', () => {
    const items = [
      makeItem({ externalId: '42', title: 'MR one' }),
      makeItem({ externalId: '43', title: 'MR two' }),
    ];
    render(
      <MrBrowseTable
        items={items}
        selectedIds={new Set(['42', '43'])}
        onSelectionChange={mockOnSelectionChange}
      />
    );

    expect(screen.getByText('MR one')).toBeInTheDocument();
    expect(screen.getByText('MR two')).toBeInTheDocument();
    expect(screen.getByLabelText('Zaznacz MR #42')).toBeChecked();
    expect(screen.getByLabelText('Zaznacz MR #43')).toBeChecked();
  });

  it('toggle all selects all when none selected', () => {
    const items = [
      makeItem({ externalId: '42' }),
      makeItem({ externalId: '43' }),
    ];
    render(
      <MrBrowseTable
        items={items}
        selectedIds={new Set()}
        onSelectionChange={mockOnSelectionChange}
      />
    );

    fireEvent.click(screen.getByLabelText('Zaznacz wszystkie'));
    expect(mockOnSelectionChange).toHaveBeenCalledWith(new Set(['42', '43']));
  });

  it('toggle all deselects all when all selected', () => {
    const items = [
      makeItem({ externalId: '42' }),
      makeItem({ externalId: '43' }),
    ];
    render(
      <MrBrowseTable
        items={items}
        selectedIds={new Set(['42', '43'])}
        onSelectionChange={mockOnSelectionChange}
      />
    );

    fireEvent.click(screen.getByLabelText('Zaznacz wszystkie'));
    expect(mockOnSelectionChange).toHaveBeenCalledWith(new Set());
  });

  it('toggle one item adds it to selection', () => {
    const items = [makeItem({ externalId: '42' })];
    render(
      <MrBrowseTable
        items={items}
        selectedIds={new Set()}
        onSelectionChange={mockOnSelectionChange}
      />
    );

    fireEvent.click(screen.getByLabelText('Zaznacz MR #42'));
    const calledWith = mockOnSelectionChange.mock.calls[0][0] as Set<string>;
    expect(calledWith.has('42')).toBe(true);
  });

  it('toggle one item removes it from selection', () => {
    const items = [makeItem({ externalId: '42' })];
    render(
      <MrBrowseTable
        items={items}
        selectedIds={new Set(['42'])}
        onSelectionChange={mockOnSelectionChange}
      />
    );

    fireEvent.click(screen.getByLabelText('Zaznacz MR #42'));
    const calledWith = mockOnSelectionChange.mock.calls[0][0] as Set<string>;
    expect(calledWith.has('42')).toBe(false);
  });

  it('row click opens URL in new tab', () => {
    const items = [makeItem({ url: 'https://github.com/owner/repo/pull/42' })];
    render(
      <MrBrowseTable
        items={items}
        selectedIds={new Set()}
        onSelectionChange={mockOnSelectionChange}
      />
    );

    fireEvent.click(screen.getByText('Fix login bug'));
    expect(mockWindowOpen).toHaveBeenCalledWith(
      'https://github.com/owner/repo/pull/42',
      '_blank',
      'noopener,noreferrer'
    );
  });

  it('Enter key opens URL in new tab', () => {
    const items = [makeItem({ url: 'https://github.com/owner/repo/pull/42' })];
    render(
      <MrBrowseTable
        items={items}
        selectedIds={new Set()}
        onSelectionChange={mockOnSelectionChange}
      />
    );

    const row = screen.getByText('Fix login bug').closest('tr')!;
    fireEvent.keyDown(row, { key: 'Enter' });
    expect(mockWindowOpen).toHaveBeenCalledWith(
      'https://github.com/owner/repo/pull/42',
      '_blank',
      'noopener,noreferrer'
    );
  });

  it('shows labels as badges', () => {
    const items = [makeItem({ labels: ['bugfix', 'urgent'] })];
    render(
      <MrBrowseTable
        items={items}
        selectedIds={new Set()}
        onSelectionChange={mockOnSelectionChange}
      />
    );

    expect(screen.getByText('bugfix')).toBeInTheDocument();
    expect(screen.getByText('urgent')).toBeInTheDocument();
  });

  it('shows dash when no labels', () => {
    const items = [makeItem({ labels: [] })];
    render(
      <MrBrowseTable
        items={items}
        selectedIds={new Set()}
        onSelectionChange={mockOnSelectionChange}
      />
    );

    expect(screen.getByText('-')).toBeInTheDocument();
  });

  it('shows state badge with correct variant for merged', () => {
    const items = [makeItem({ state: 'merged' })];
    render(
      <MrBrowseTable
        items={items}
        selectedIds={new Set()}
        onSelectionChange={mockOnSelectionChange}
      />
    );

    const badge = screen.getByText('merged');
    expect(badge).toBeInTheDocument();
    expect(badge).toHaveClass('bg-success');
  });

  it('shows state badge with correct variant for open', () => {
    const items = [makeItem({ state: 'open' })];
    render(
      <MrBrowseTable
        items={items}
        selectedIds={new Set()}
        onSelectionChange={mockOnSelectionChange}
      />
    );

    const badge = screen.getByText('open');
    expect(badge).toHaveClass('bg-primary');
  });

  it('shows state badge with correct variant for closed', () => {
    const items = [makeItem({ state: 'closed' })];
    render(
      <MrBrowseTable
        items={items}
        selectedIds={new Set()}
        onSelectionChange={mockOnSelectionChange}
      />
    );

    const badge = screen.getByText('closed');
    expect(badge).toHaveClass('bg-danger');
  });

  it('shows state badge with secondary variant for unknown state', () => {
    const items = [makeItem({ state: 'draft' })];
    render(
      <MrBrowseTable
        items={items}
        selectedIds={new Set()}
        onSelectionChange={mockOnSelectionChange}
      />
    );

    const badge = screen.getByText('draft');
    expect(badge).toHaveClass('bg-secondary');
  });
});
