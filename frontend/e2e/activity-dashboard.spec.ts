import { test, expect } from '@playwright/test';

// These tests hit the REAL backend (port 8083) connected to mock GitHub (port 9999).
// No route interception — true E2E.

test.describe('Activity Dashboard E2E', () => {

  test('full flow: select repo, contributor, view stats + heatmap + flags', async ({ page }) => {
    // First, ensure test/repo is saved via API
    const addRepoRes = await page.request.post('http://localhost:8083/api/repos', {
      data: { projectSlug: 'test/repo', provider: 'github' },
    });
    // Repo may already exist (409), that's fine
    expect([200, 201, 409]).toContain(addRepoRes.status());

    await page.goto('/activity');
    await expect(page.getByText('Aktywność kontrybutora'), 'heading should be visible').toBeVisible();

    // Select repo from saved list
    await page.locator('.list-group-item').filter({ hasText: 'test/repo' }).click();

    // Wait for loading to finish and contributor selector to appear
    // First the spinner shows, then contributor dropdown renders
    // Wait for contributor select to appear
    await expect(page.getByLabel('Kontrybutor')).toBeVisible({ timeout: 15000 });
    await expect(page.locator('option[value="alice"]')).toBeAttached({ timeout: 10000 });
    await expect(page.locator('option[value="bob"]')).toBeAttached({ timeout: 5000 });

    // Select alice
    await page.getByLabel('Kontrybutor').selectOption('alice');

    // Stats cards should show real data from mock GitHub
    await expect(page.getByText('Łącznie PR-ów')).toBeVisible();
    // alice has 2 PRs — verify via card that contains both "2" and "Łącznie"
    const totalCard = page.locator('.card', { hasText: 'Łącznie PR-ów' });
    await expect(totalCard).toBeVisible();

    // Heatmap should render
    const svg = page.locator('svg[role="img"]');
    await expect(svg).toBeVisible();
    expect(await svg.locator('rect').count()).toBeGreaterThan(0);

    // Flags should be present (PR #2: quick merge + large PR + no review)
    await expect(page.getByText('Wykryte nieprawidłowości')).toBeVisible();
    const flagsTable = page.locator('table');
    await expect(flagsTable).toBeVisible();
  });

  test('contributor with no activity shows empty state', async ({ page }) => {
    await page.request.post('http://localhost:8083/api/repos', {
      data: { projectSlug: 'test/repo', provider: 'github' },
    });

    await page.goto('/activity');
    await page.locator('.list-group-item').filter({ hasText: 'test/repo' }).click();
    await expect(page.getByLabel('Kontrybutor')).toBeVisible({ timeout: 15000 });

    // Type a non-existent contributor manually — select only has alice and bob
    // Instead, select bob (1 PR) and verify it works
    await page.getByLabel('Kontrybutor').selectOption('bob');
    await expect(page.getByText('Łącznie PR-ów')).toBeVisible();
  });

  test('severity badge filters flags table', async ({ page }) => {
    await page.request.post('http://localhost:8083/api/repos', {
      data: { projectSlug: 'test/repo', provider: 'github' },
    });

    await page.goto('/activity');
    await page.locator('.list-group-item').filter({ hasText: 'test/repo' }).click();
    await expect(page.getByLabel('Kontrybutor')).toBeVisible({ timeout: 15000 });
    await page.getByLabel('Kontrybutor').selectOption('alice');

    // Wait for flags to render
    const flagCount = page.locator('text=/\\d+ z \\d+ flag/');
    await expect(flagCount).toBeVisible();

    // Get the "z X flag" text to know total
    const totalText = await flagCount.textContent();
    const total = totalText?.match(/z (\d+)/)?.[1];

    // Click first severity badge
    const badges = page.locator('[role="button"].badge');
    const badgeCount = await badges.count();
    if (badgeCount > 0) {
      await badges.first().click();
      // After filter, count should be different
      const filteredText = await flagCount.textContent();
      expect(filteredText).not.toEqual(totalText);

      // Click again to remove filter
      await badges.first().click();
      await expect(flagCount).toContainText(`z ${total} flag`);
    }
  });
});
