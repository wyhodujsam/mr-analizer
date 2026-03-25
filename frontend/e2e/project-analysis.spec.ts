import { test, expect } from '@playwright/test';

test.describe('Project Analysis E2E', () => {

  test('navigation to /project shows page', async ({ page }) => {
    await page.goto('/project');
    await expect(page.getByText('Analiza projektu')).toBeVisible();
  });

  test('full flow: select repo → analyze → view results', async ({ page }) => {
    // Ensure repo is saved
    await page.request.post('http://localhost:8083/api/repos', {
      data: { projectSlug: 'test/repo', provider: 'github' },
    });

    await page.goto('/project');

    // Select repo
    await page.locator('.list-group-item').filter({ hasText: 'test/repo' }).click();

    // Click analyze
    await page.getByRole('button', { name: /Analizuj projekt/ }).click();

    // Wait for progress or result (up to 30s for slow API)
    await expect(page.getByText(/PR-ów/).first()).toBeVisible({ timeout: 30000 });

    // Summary cards should show AI Potential
    await expect(page.getByText('AI Potential')).toBeVisible();

    // BDD/SDD cards
    await expect(page.getByText(/BDD/)).toBeVisible();
    await expect(page.getByText(/SDD/)).toBeVisible();

    // Table with PR rows
    await expect(page.locator('table')).toBeVisible();
  });

  test('drill-down: click PR row shows score breakdown', async ({ page }) => {
    await page.request.post('http://localhost:8083/api/repos', {
      data: { projectSlug: 'test/repo', provider: 'github' },
    });

    await page.goto('/project');
    await page.locator('.list-group-item').filter({ hasText: 'test/repo' }).click();
    await page.getByRole('button', { name: /Analizuj projekt/ }).click();
    await expect(page.locator('table')).toBeVisible({ timeout: 30000 });

    // Click first PR row
    await page.locator('table tbody tr').first().click();

    // Score Breakdown should expand
    await expect(page.getByText('Score Breakdown')).toBeVisible();
  });

  test('saved analyses list shows after analysis', async ({ page }) => {
    await page.goto('/project');

    // Navigate to project page with test/repo
    await page.request.post('http://localhost:8083/api/repos', {
      data: { projectSlug: 'test/repo', provider: 'github' },
    });
    await page.reload();
    await page.locator('.list-group-item').filter({ hasText: 'test/repo' }).click();
    await page.getByRole('button', { name: /Analizuj/ }).click();
    await expect(page.locator('table')).toBeVisible({ timeout: 30000 });

    // Show saved analyses
    if (await page.getByText('Pokaż zapisane analizy').isVisible()) {
      await page.getByText('Pokaż zapisane analizy').click();
      await expect(page.getByText('Zapisane analizy')).toBeVisible();
    }
  });
});
