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
  axios.get.mockImplementation((url) => {
    if (url.startsWith('/api/invoice')) {
      return Promise.resolve({ data: { data: { content: [
        { id:1, invoiceNumber:'INV-2026-0501', customerName:'テスト株式会社', subject:'テスト', amount:1500000,
          taxAmount:150000, totalWithTax:1650000, status:'下書き', invoiceDate:'2026-05-01', year:2026, month:5 }
      ], totalElements:1, totalPages:1 } } });
    }
    if (url.startsWith('/api/customer')) return Promise.resolve({ data: { data: { content: [{ id:1, companyName:'テスト', customerCode:'CUS0001' }], totalElements:1 } } });
    if (url.startsWith('/api/employee')) return Promise.resolve({ data: { data: { content: [], totalElements:0 } } });
    return Promise.resolve({ data: { data: { content: [], totalElements: 0 } } });
  });
});

const { default: Invoice } = await import('../pages/Invoice');

describe('Invoice Component', () => {
  it('renders page title', () => {
    render(<Invoice />);
    expect(screen.getByText('請求書管理')).toBeInTheDocument();
  });
  it('renders create button', () => {
    render(<Invoice />);
    expect(screen.getByText('新規登録')).toBeInTheDocument();
  });
  it('shows table headers', () => {
    render(<Invoice />);
    expect(screen.getAllByText('請求書番号').length).toBeGreaterThan(0);
    expect(screen.getAllByText('顧客名').length).toBeGreaterThan(0);
  });
});
