import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import Layout from '../components/Layout';

describe('Layout', () => {
  it('renders navbar with brand link', () => {
    render(
      <MemoryRouter>
        <Layout />
      </MemoryRouter>
    );

    expect(screen.getByText('MR Analizer')).toBeInTheDocument();
  });

  it('brand links to home', () => {
    render(
      <MemoryRouter>
        <Layout />
      </MemoryRouter>
    );

    const link = screen.getByText('MR Analizer');
    expect(link.closest('a')).toHaveAttribute('href', '/');
  });
});
