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
    if (url.startsWith('/api/purchase-order')) {
      return Promise.resolve({ data: { data: { content: [
        { id: 1, orderNumber: 'PO-2026-0001', customerName: 'テスト株式会社', subject: 'テスト', orderDate: '2026-05-01', deliveryDate: '2026-06-15', totalWithTax: 1100000, status: '下書き' }
      ], totalElements: 1, totalPages: 1 } } });
    }
    if (url.startsWith('/api/supplier-order')) {
      return Promise.resolve({ data: { data: { content: [
        { id: 1, orderNumber: 'PO-SUP-2026-0001', supplierName: 'テスト', subject: 'テスト', orderDate: '2026-05-10', deliveryDate: '2026-07-01', totalWithTax: 3850000, status: '発注済' }
      ], totalElements: 1, totalPages: 1 } } });
    }
    if (url.startsWith('/api/customer')) return Promise.resolve({ data: { data: { content: [{ id: 1, companyName: 'テスト', customerCode: 'CUS0001' }], totalElements: 1 } } });
    if (url.startsWith('/api/employee')) return Promise.resolve({ data: { data: { content: [{ id: 1, name: '山田 太郎', department: '営業部', phone: '090-1111-2222' }], totalElements: 1 } } });
    return Promise.resolve({ data: { data: { content: [], totalElements: 0 } } });
  });
});

const { default: PurchaseOrder } = await import('../pages/PurchaseOrder');

describe('PurchaseOrder Component', () => {
  it('renders page title', () => {
    render(<PurchaseOrder />);
    expect(screen.getByText('受注書・発注管理')).toBeInTheDocument();
  });

  it('shows tab labels', () => {
    render(<PurchaseOrder />);
    expect(screen.getByText('受注書')).toBeInTheDocument();
    expect(screen.getByText('発注書')).toBeInTheDocument();
  });

  it('renders create buttons', async () => {
    render(<PurchaseOrder />);
    expect(screen.getAllByText('新規登録').length).toBeGreaterThan(0);
  });
});
