import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import ProjectAnalysisPage from '../pages/ProjectAnalysisPage';

vi.mock('../api/analysisApi', () => ({
  getRepos: vi.fn().mockResolvedValue([{ id: 1, projectSlug: 'owner/repo', provider: 'github' }]),
  addRepo: vi.fn().mockResolvedValue({}),
  deleteRepo: vi.fn().mockResolvedValue({}),
}));

vi.mock('../api/projectApi', () => ({
  analyzeProjectWithProgress: vi.fn().mockResolvedValue({
    id: 1,
    projectSlug: 'owner/repo',
    analyzedAt: '2026-03-25T10:00:00',
    summary: {
      totalPrs: 3, automatableCount: 2, maybeCount: 1, notSuitableCount: 0,
      automatablePercent: 66.7, maybePercent: 33.3, notSuitablePercent: 0,
      avgScore: 0.65, topRules: [], histogram: [],
      bddCount: 1, bddPercent: 33.3, sddCount: 1, sddPercent: 33.3,
    },
    rows: [
      { prId: '1', title: 'PR 1', author: 'a', state: 'merged', url: null,
        createdAt: '2026-03-10', mergedAt: '2026-03-10', additions: 100, deletions: 20,
        aiScore: 0.8, aiVerdict: 'AUTOMATABLE', ruleResults: [], llmComment: null,
        hasBdd: true, hasSdd: false, bddFiles: ['test.feature'], sddFiles: [] },
    ],
  }),
  getSavedAnalyses: vi.fn().mockResolvedValue([
    {
      id: 1, projectSlug: 'owner/repo', analyzedAt: '2026-03-25T10:00:00',
      summary: { totalPrs: 3, automatablePercent: 66.7, bddPercent: 33, sddPercent: 33,
        automatableCount: 2, maybeCount: 1, notSuitableCount: 0, maybePercent: 33,
        notSuitablePercent: 0, avgScore: 0.65, topRules: [], histogram: [],
        bddCount: 1, sddCount: 1 },
      rows: [],
    },
  ]),
  deleteProjectAnalysis: vi.fn().mockResolvedValue(undefined),
}));

function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/project/owner/repo']}>
      <Routes>
        <Route path="project/:owner/:repo" element={<ProjectAnalysisPage />} />
      </Routes>
    </MemoryRouter>
  );
}

describe('ProjectAnalysisPage', () => {
  it('renders page with analyze button', async () => {
    renderPage();
    await expect(screen.findByText('Analiza projektu')).resolves.toBeInTheDocument();
    expect(await screen.findByRole('button', { name: /Analizuj/ })).toBeInTheDocument();
  });

  it('shows saved analyses list', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Zapisane analizy')).toBeInTheDocument();
    });
  });

  it('loads saved analysis on click', async () => {
    renderPage();
    // Click the saved analysis item (has role="button" in ListGroup)
    const savedItem = await screen.findByRole('button', { name: /Usuń/ });
    const listItem = savedItem.closest('.list-group-item');
    const clickable = listItem?.querySelector('[role="button"]');
    if (clickable) fireEvent.click(clickable);
    await waitFor(() => {
      expect(screen.getByText('AI Potential')).toBeInTheDocument();
    });
  });

  it('triggers analysis on button click', async () => {
    renderPage();
    const btn = await screen.findByRole('button', { name: /Analizuj/ });
    fireEvent.click(btn);
    // After async analysis completes, should show results
    await waitFor(() => {
      expect(screen.getByText('AI Potential')).toBeInTheDocument();
    }, { timeout: 5000 });
  });
});
