import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent, waitFor, within } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import ActivityDashboardPage from '../pages/ActivityDashboardPage';

vi.mock('../components/activity/ActivityBarChart', () => ({
  default: () => <div data-testid="bar-chart">Bar Chart Mock</div>,
}));

vi.mock('../api/analysisApi', () => ({
  getRepos: vi.fn().mockResolvedValue([]),
  addRepo: vi.fn().mockResolvedValue({}),
  deleteRepo: vi.fn().mockResolvedValue({}),
}));

vi.mock('../api/activityApi', () => ({
  getContributors: vi.fn().mockResolvedValue([{ login: 'alice', prCount: 10 }]),
  getActivityReport: vi.fn().mockResolvedValue({
    contributor: 'alice',
    projectSlug: 'owner/repo',
    stats: {
      totalPrs: 10,
      avgSize: 200,
      avgReviewTimeMinutes: 30,
      weekendPercentage: 10,
      flagCounts: { WARNING: 2, INFO: 1 },
    },
    productivity: {
      velocity: { prsPerWeek: 2.5, weeklyBreakdown: [{ weekStart: '2026-03-16', count: 3 }], trend: 'stable' },
      cycleTime: { avgHours: 5.0, medianHours: 3.0, p90Hours: 12.0 },
      impact: { totalAdditions: 1000, totalDeletions: 300, totalLines: 1300, avgLinesPerPr: 130, addDeleteRatio: 3.33 },
      codeChurn: { churnRatio: 0.3, label: 'Zbalansowany' },
      reviewEngagement: { reviewsGiven: 5, reviewsReceived: 8, ratio: 0.63, label: 'Zbalansowany' },
    },
    flags: [
      { type: 'LARGE_PR', displayName: 'Za duży PR', severity: 'WARNING', description: 'PR ma 600 linii', prReference: '#1' },
      { type: 'LARGE_PR', displayName: 'Za duży PR', severity: 'WARNING', description: 'PR ma 550 linii', prReference: '#2' },
      { type: 'WEEKEND_WORK', displayName: 'Praca w weekend', severity: 'INFO', description: 'PR w sobotę', prReference: '#3' },
    ],
    dailyActivity: {
      '2026-03-10': { count: 2, pullRequests: [{ id: '1', title: 'PR 1', size: 100 }, { id: '2', title: 'PR 2', size: 200 }] },
    },
    pullRequests: [],
  }),
  refreshActivityCache: vi.fn().mockResolvedValue(undefined),
}));

function renderDashboard() {
  return render(
    <MemoryRouter initialEntries={['/activity/owner/repo']}>
      <Routes>
        <Route path="activity/:owner/:repo" element={<ActivityDashboardPage />} />
      </Routes>
    </MemoryRouter>
  );
}

async function loadReportAndWait() {
  renderDashboard();
  const selector = await screen.findByLabelText('Kontrybutor');
  fireEvent.change(selector, { target: { value: 'alice' } });
  // Wait for tablist to appear (report loaded)
  await screen.findByRole('tablist');
}

function getTabByName(name: RegExp) {
  const tablist = screen.getByRole('tablist');
  return within(tablist).getByText(name);
}

describe('ActivityDashboardPage tabs', () => {
  it('shows three tabs after report loads', async () => {
    await loadReportAndWait();
    const tablist = screen.getByRole('tablist');
    const tabs = within(tablist).getAllByRole('tab');
    expect(tabs).toHaveLength(3);
    expect(tabs[0]).toHaveTextContent('Wydajność');
    expect(tabs[1]).toHaveTextContent('Aktywność');
    expect(tabs[2]).toHaveTextContent(/Naruszenia/);
  });

  it('shows flag count badge on Naruszenia tab', async () => {
    await loadReportAndWait();
    const naruszeniaTab = getTabByName(/Naruszenia/);
    expect(naruszeniaTab.querySelector('.badge')).toHaveTextContent('3');
  });

  it('defaults to Wydajność tab showing productivity metrics', async () => {
    await loadReportAndWait();
    expect(screen.getByText('2.5')).toBeInTheDocument();
    expect(screen.getByText('PR/tyg')).toBeInTheDocument();
  });

  it('switches to Aktywność tab showing heatmap and chart', async () => {
    await loadReportAndWait();
    fireEvent.click(getTabByName(/Aktywność/));
    await waitFor(() => {
      expect(screen.getByText('Heatmapa aktywności')).toBeInTheDocument();
    });
    expect(screen.getByText('Aktywność w czasie')).toBeInTheDocument();
  });

  it('switches to Naruszenia tab showing flags', async () => {
    await loadReportAndWait();
    fireEvent.click(getTabByName(/Naruszenia/));
    await waitFor(() => {
      expect(screen.getAllByText('Za duży PR').length).toBeGreaterThanOrEqual(2);
    });
  });

  it('keeps StatsCards visible across tab switches', async () => {
    await loadReportAndWait();
    // avgSize rendered as "200 linii" in StatsCards
    expect(screen.getByText('200 linii')).toBeInTheDocument();

    fireEvent.click(getTabByName(/Naruszenia/));
    expect(screen.getByText('200 linii')).toBeInTheDocument();

    fireEvent.click(getTabByName(/Aktywność/));
    expect(screen.getByText('200 linii')).toBeInTheDocument();
  });
});
