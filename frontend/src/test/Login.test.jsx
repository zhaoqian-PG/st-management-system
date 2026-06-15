import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';

vi.mock('axios');
vi.mock('antd', async () => {
  const actual = await vi.importActual('antd');
  return { ...actual, message: { success: vi.fn(), error: vi.fn() } };
});

const { default: Login } = await import('../pages/Login');

describe('Login Component', () => {
  it('renders login form', () => {
    render(<Login onLogin={vi.fn()} />);
    expect(screen.getByText('ST Management System')).toBeInTheDocument();
  });
  it('has username and password fields', () => {
    render(<Login onLogin={vi.fn()} />);
    expect(screen.getByPlaceholderText('ユーザー名')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('パスワード')).toBeInTheDocument();
  });
  it('has login button', () => {
    render(<Login onLogin={vi.fn()} />);
    expect(screen.getByRole('button', { name: /ログイン/i })).toBeInTheDocument();
  });
});
