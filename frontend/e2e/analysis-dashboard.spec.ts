import { test, expect } from '@playwright/test';
import { mockRepos, mockBrowseResponse, mockAnalysisResponse } from './fixtures';

test.beforeEach(async ({ page }) => {
  await page.route('**/api/repos', route => route.fulfill({ json: mockRepos }));
  await page.route('**/api/analysis', async route => {
    if (route.request().method() === 'GET') {
      return route.fulfill({ json: [] });
    }
    return route.fulfill({ json: mockAnalysisResponse });
  });
  await page.route('**/api/browse**', route =>
    route.fulfill({ json: mockBrowseResponse }));
});

test('selecting repo shows browse button', async ({ page }) => {
  await page.goto('/');
  await page.getByText('test/repo').click();
  await expect(page.getByText('Pobierz MR')).toBeVisible();
});

test('browsing shows PR list with checkboxes', async ({ page }) => {
  await page.goto('/');
  await page.getByText('test/repo').click();
  await page.getByText('Pobierz MR').click();

  await expect(page.getByText('Fix auth')).toBeVisible();
  await expect(page.getByText('Update deps')).toBeVisible();
});

test('analysis shows results with score and verdict', async ({ page }) => {
  await page.goto('/');
  await page.getByText('test/repo').click();
  await page.getByText('Pobierz MR').click();

  // Select all and analyze
  const checkboxes = page.locator('input[type="checkbox"]');
  const count = await checkboxes.count();
  for (let i = 0; i < count; i++) {
    await checkboxes.nth(i).check();
  }

  await page.getByText(/Analizuj/).click();

  // Should show results
  await expect(page.getByText('0.75')).toBeVisible();
  await expect(page.getByText('AUTOMATABLE')).toBeVisible();
});
