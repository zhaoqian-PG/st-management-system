import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import axios from 'axios';

vi.mock('axios');
vi.mock('antd', async () => {
  const actual = await vi.importActual('antd');
  return { ...actual, message: { success: vi.fn(), error: vi.fn(), warning: vi.fn(), info: vi.fn() } };
});
beforeEach(() => {
  vi.clearAllMocks();
  axios.get.mockResolvedValue({ data: { data: { content: [], totalElements: 0 } } });
});
const { default: Invoice } = await import('../pages/Invoice');

describe('Invoice Component', () => {
  it('renders page title', () => {
    render(<Invoice />);
    expect(screen.getByText('請求書管理')).toBeInTheDocument();
  });
});
