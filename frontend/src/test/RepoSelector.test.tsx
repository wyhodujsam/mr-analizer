import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import RepoSelector from '../components/RepoSelector';
import type { SavedRepository } from '../types';

function makeRepo(overrides: Partial<SavedRepository> = {}): SavedRepository {
  return {
    id: 1,
    projectSlug: 'owner/repo',
    provider: 'github',
    addedAt: '2026-03-20T10:00:00',
    lastAnalyzedAt: '2026-03-20T12:00:00',
    ...overrides,
  };
}

describe('RepoSelector', () => {
  let mockOnSelect: ReturnType<typeof vi.fn>;
  let mockOnDelete: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    vi.clearAllMocks();
    mockOnSelect = vi.fn();
    mockOnDelete = vi.fn();
  });

  it('renders saved repos in list', () => {
    const repos = [
      makeRepo({ id: 1, projectSlug: 'owner/repo1', provider: 'github' }),
      makeRepo({ id: 2, projectSlug: 'owner/repo2', provider: 'gitlab' }),
    ];
    render(
      <RepoSelector
        savedRepos={repos}
        onSelect={mockOnSelect}
        onDelete={mockOnDelete}
        selectedSlug=""
        selectedProvider="github"
      />
    );

    expect(screen.getByText('owner/repo1')).toBeInTheDocument();
    expect(screen.getByText('owner/repo2')).toBeInTheDocument();
    expect(screen.getByText('Zapisane repozytoria')).toBeInTheDocument();
  });

  it('calls onSelect when repo clicked', () => {
    const repos = [makeRepo({ id: 1, projectSlug: 'owner/repo1', provider: 'github' })];
    render(
      <RepoSelector
        savedRepos={repos}
        onSelect={mockOnSelect}
        onDelete={mockOnDelete}
        selectedSlug=""
        selectedProvider="github"
      />
    );

    fireEvent.click(screen.getByText('owner/repo1'));
    expect(mockOnSelect).toHaveBeenCalledWith('owner/repo1', 'github');
  });

  it('calls onDelete when delete button clicked with confirm', () => {
    vi.stubGlobal('confirm', vi.fn(() => true));
    const repos = [makeRepo({ id: 5, projectSlug: 'owner/repo1' })];
    render(
      <RepoSelector
        savedRepos={repos}
        onSelect={mockOnSelect}
        onDelete={mockOnDelete}
        selectedSlug=""
        selectedProvider="github"
      />
    );

    fireEvent.click(screen.getByTitle('Usun'));
    expect(window.confirm).toHaveBeenCalled();
    expect(mockOnDelete).toHaveBeenCalledWith(5);
  });

  it('does not call onDelete when confirm is cancelled', () => {
    vi.stubGlobal('confirm', vi.fn(() => false));
    const repos = [makeRepo({ id: 5 })];
    render(
      <RepoSelector
        savedRepos={repos}
        onSelect={mockOnSelect}
        onDelete={mockOnDelete}
        selectedSlug=""
        selectedProvider="github"
      />
    );

    fireEvent.click(screen.getByTitle('Usun'));
    expect(mockOnDelete).not.toHaveBeenCalled();
  });

  it('manual slug input submits on Enter', () => {
    render(
      <RepoSelector
        savedRepos={[]}
        onSelect={mockOnSelect}
        onDelete={mockOnDelete}
        selectedSlug=""
        selectedProvider="github"
      />
    );

    const input = screen.getByPlaceholderText('owner/repo');
    fireEvent.change(input, { target: { value: 'my-org/my-repo' } });
    fireEvent.keyDown(input, { key: 'Enter' });
    expect(mockOnSelect).toHaveBeenCalledWith('my-org/my-repo', 'github');
  });

  it('manual slug input submits on blur', () => {
    render(
      <RepoSelector
        savedRepos={[]}
        onSelect={mockOnSelect}
        onDelete={mockOnDelete}
        selectedSlug=""
        selectedProvider="github"
      />
    );

    const input = screen.getByPlaceholderText('owner/repo');
    fireEvent.change(input, { target: { value: 'my-org/my-repo' } });
    fireEvent.blur(input);
    expect(mockOnSelect).toHaveBeenCalledWith('my-org/my-repo', 'github');
  });

  it('does not submit empty slug', () => {
    render(
      <RepoSelector
        savedRepos={[]}
        onSelect={mockOnSelect}
        onDelete={mockOnDelete}
        selectedSlug=""
        selectedProvider="github"
      />
    );

    const input = screen.getByPlaceholderText('owner/repo');
    fireEvent.keyDown(input, { key: 'Enter' });
    expect(mockOnSelect).not.toHaveBeenCalled();
  });

  it('shows provider selector with GitHub and GitLab options', () => {
    render(
      <RepoSelector
        savedRepos={[]}
        onSelect={mockOnSelect}
        onDelete={mockOnDelete}
        selectedSlug=""
        selectedProvider="github"
      />
    );

    expect(screen.getByText('GitHub')).toBeInTheDocument();
    expect(screen.getByText('GitLab')).toBeInTheDocument();
  });

  it('uses selected provider when submitting manually', () => {
    render(
      <RepoSelector
        savedRepos={[]}
        onSelect={mockOnSelect}
        onDelete={mockOnDelete}
        selectedSlug=""
        selectedProvider="github"
      />
    );

    const providerSelect = screen.getByDisplayValue('GitHub');
    fireEvent.change(providerSelect, { target: { value: 'gitlab' } });

    const input = screen.getByPlaceholderText('owner/repo');
    fireEvent.change(input, { target: { value: 'my-org/my-repo' } });
    fireEvent.keyDown(input, { key: 'Enter' });
    expect(mockOnSelect).toHaveBeenCalledWith('my-org/my-repo', 'gitlab');
  });

  it('submits via Wybierz button', () => {
    render(
      <RepoSelector
        savedRepos={[]}
        onSelect={mockOnSelect}
        onDelete={mockOnDelete}
        selectedSlug=""
        selectedProvider="github"
      />
    );

    const input = screen.getByPlaceholderText('owner/repo');
    fireEvent.change(input, { target: { value: 'my-org/my-repo' } });
    fireEvent.click(screen.getByText('Wybierz'));
    expect(mockOnSelect).toHaveBeenCalledWith('my-org/my-repo', 'github');
  });

  it('highlights active repo', () => {
    const repos = [makeRepo({ id: 1, projectSlug: 'owner/repo1', provider: 'github' })];
    render(
      <RepoSelector
        savedRepos={repos}
        onSelect={mockOnSelect}
        onDelete={mockOnDelete}
        selectedSlug="owner/repo1"
        selectedProvider="github"
      />
    );

    const item = screen.getByText('owner/repo1').closest('[class*="list-group-item"]');
    expect(item).toHaveClass('active');
  });

  it('shows last analyzed date when available', () => {
    const repos = [makeRepo({ lastAnalyzedAt: '2026-03-20T12:00:00' })];
    render(
      <RepoSelector
        savedRepos={repos}
        onSelect={mockOnSelect}
        onDelete={mockOnDelete}
        selectedSlug=""
        selectedProvider="github"
      />
    );

    expect(screen.getByText(/ostatnia analiza/)).toBeInTheDocument();
  });

  it('does not show saved repos section when list is empty', () => {
    render(
      <RepoSelector
        savedRepos={[]}
        onSelect={mockOnSelect}
        onDelete={mockOnDelete}
        selectedSlug=""
        selectedProvider="github"
      />
    );

    expect(screen.queryByText('Zapisane repozytoria')).not.toBeInTheDocument();
  });
});
