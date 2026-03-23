import { test, expect } from '@playwright/test';
import { mockRepos } from './fixtures';

test.beforeEach(async ({ page }) => {
  await page.route('**/api/repos', route => route.fulfill({ json: mockRepos }));
  await page.route('**/api/analysis', route => route.fulfill({ json: [] }));
});

test('navbar shows Analiza PR and Aktywność links', async ({ page }) => {
  await page.goto('/');
  await expect(page.getByRole('link', { name: 'Analiza PR' })).toBeVisible();
  await expect(page.getByRole('link', { name: 'Aktywność' })).toBeVisible();
});

test('clicking Aktywność navigates to /activity', async ({ page }) => {
  await page.goto('/');
  await page.getByRole('link', { name: 'Aktywność' }).click();
  await expect(page).toHaveURL(/\/activity/);
  await expect(page.getByText('Aktywność kontrybutora')).toBeVisible();
});

test('clicking Analiza PR navigates to /', async ({ page }) => {
  await page.goto('/activity');
  await page.getByRole('link', { name: 'Analiza PR' }).click();
  await expect(page).toHaveURL(/\/$/);
});

test('direct URL /activity works', async ({ page }) => {
  await page.route('**/api/activity/**', route => route.fulfill({ json: [] }));
  await page.goto('/activity');
  await expect(page.getByText('Aktywność kontrybutora')).toBeVisible();
});

test('unknown route shows 404', async ({ page }) => {
  await page.goto('/nonexistent');
  await expect(page.getByText('nie znaleziona')).toBeVisible();
});
