import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import axios from 'axios';

vi.mock('axios');

// Mock antd message
vi.mock('antd', async () => {
  const actual = await vi.importActual('antd');
  return {
    ...actual,
    message: { success: vi.fn(), error: vi.fn(), warning: vi.fn(), info: vi.fn() },
  };
});

// Setup before importing the component
beforeEach(() => {
  vi.clearAllMocks();
  axios.get.mockImplementation((url) => {
    if (url.startsWith('/api/supplier-order')) {
      return Promise.resolve({
        data: {
          data: {
            content: [
              {
                id: 1, orderNumber: 'PO-SUP-2026-0001',
                supplierName: '株式会社テスト', subject: 'テスト発注',
                orderDate: '2026-05-10', deliveryDate: '2026-07-01',
                totalWithTax: 3850000, amount: 3500000,
                status: '発注済', remark: 'テスト'
              },
              {
                id: 2, orderNumber: 'PO-SUP-2026-0002',
                supplierName: '株式会社サンプル', subject: 'サンプル発注',
                orderDate: '2026-06-01', deliveryDate: '2026-08-01',
                totalWithTax: 2200000, amount: 2000000,
                status: '下書き', remark: null
              }
            ],
            totalElements: 2,
            totalPages: 1,
          },
        },
      });
    }
    if (url.startsWith('/api/employee')) {
      return Promise.resolve({
        data: {
          data: {
            content: [
              { id: 1, name: '山田 太郎', department: '営業部', phone: '090-1111-2222', japanAddress: '東京都新宿区' },
              { id: 2, name: '鈴木 花子', department: '技術部', phone: '090-3333-4444', japanAddress: '東京都港区' },
            ],
          },
        },
      });
    }
    return Promise.resolve({ data: { data: { content: [], totalElements: 0 } } });
  });
});

// Dynamic import after mocks
const { default: SupplierOrder } = await import('../pages/SupplierOrder');

describe('SupplierOrder Component', () => {
  it('renders page title', () => {
    render(<SupplierOrder />);
    expect(screen.getByText('発注管理（我社→他社）')).toBeInTheDocument();
  });

  it('renders create button', () => {
    render(<SupplierOrder />);
    expect(screen.getByText('新規登録')).toBeInTheDocument();
  });

  it('renders table columns', async () => {
    render(<SupplierOrder />);
    expect(screen.getAllByText('発注番号').length).toBeGreaterThan(0);
    expect(screen.getAllByText('件名').length).toBeGreaterThan(0);
    expect(screen.getAllByText('受注方').length).toBeGreaterThan(0);
    expect(screen.getAllByText('発注日').length).toBeGreaterThan(0);
    expect(screen.getAllByText('状態').length).toBeGreaterThan(0);
  });

  it('displays supplier order data in table', async () => {
    render(<SupplierOrder />);

    // Wait for data to load
    const row1 = await screen.findByText('PO-SUP-2026-0001');
    const row2 = await screen.findByText('PO-SUP-2026-0002');

    expect(row1).toBeInTheDocument();
    expect(row2).toBeInTheDocument();
  });
});
