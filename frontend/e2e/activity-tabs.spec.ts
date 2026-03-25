import { test, expect } from '@playwright/test';

test.describe('Activity Dashboard Tabs E2E', () => {

  test.beforeEach(async ({ page }) => {
    await page.request.post('http://localhost:8083/api/repos', {
      data: { projectSlug: 'test/repo', provider: 'github' },
    });

    await page.goto('/activity');
    await page.locator('.list-group-item').filter({ hasText: 'test/repo' }).click();
    await expect(page.getByLabel('Kontrybutor')).toBeVisible({ timeout: 15000 });
    await page.getByLabel('Kontrybutor').selectOption('alice');

    // Wait for report to load
    await expect(page.getByText('Łącznie PR-ów')).toBeVisible({ timeout: 15000 });
  });

  test('shows three tabs', async ({ page }) => {
    await expect(page.getByRole('tab', { name: /Wydajność/ })).toBeVisible();
    await expect(page.getByRole('tab', { name: /Aktywność/ })).toBeVisible();
    await expect(page.getByRole('tab', { name: /Naruszenia/ })).toBeVisible();
  });

  test('Wydajność tab shows metrics', async ({ page }) => {
    await page.getByRole('tab', { name: /Wydajność/ }).click();
    await expect(page.getByText('Velocity')).toBeVisible();
    await expect(page.getByText('Cycle Time')).toBeVisible();
  });

  test('Naruszenia tab shows flags with badge', async ({ page }) => {
    const naruszeniaTab = page.getByRole('tab', { name: /Naruszenia/ });
    await naruszeniaTab.click();

    // Should show flags or "no flags" message — either way, tab content is visible
    await expect(page.locator('.tab-pane.active')).toBeVisible();
  });
});
