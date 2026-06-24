import { describe, it, expect, vi, beforeEach } from 'vitest';
import axios from 'axios';

vi.mock('axios');

const { supplierOrderApi } = await vi.importActual('../services/supplierOrderApi');

describe('supplierOrderApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('list calls GET with params', async () => {
    const mockData = { data: { data: { content: [], totalElements: 0 } } };
    axios.get.mockResolvedValue(mockData);

    const result = await supplierOrderApi.list({ page: 0, size: 10 });

    expect(axios.get).toHaveBeenCalledWith('/api/supplier-order', { params: { page: 0, size: 10 } });
    expect(result).toEqual(mockData);
  });

  it('getById calls GET with id', async () => {
    const mockData = { data: { data: { id: 1, orderNumber: 'PO-SUP-2026-0001' } } };
    axios.get.mockResolvedValue(mockData);

    const result = await supplierOrderApi.getById(1);

    expect(axios.get).toHaveBeenCalledWith('/api/supplier-order/1');
    expect(result).toEqual(mockData);
  });

  it('create calls POST', async () => {
    const dto = { supplierName: 'Test', orderDate: '2026-01-01' };
    axios.post.mockResolvedValue({ data: {} });

    await supplierOrderApi.create(dto);

    expect(axios.post).toHaveBeenCalledWith('/api/supplier-order', dto);
  });

  it('update calls PUT', async () => {
    const dto = { supplierName: 'Updated' };
    axios.put.mockResolvedValue({ data: {} });

    await supplierOrderApi.update(1, dto);

    expect(axios.put).toHaveBeenCalledWith('/api/supplier-order/1', dto);
  });

  it('delete calls DELETE', async () => {
    axios.delete.mockResolvedValue({ data: {} });

    await supplierOrderApi.delete(1);

    expect(axios.delete).toHaveBeenCalledWith('/api/supplier-order/1');
  });

  it('downloadPdf creates blob and triggers download', async () => {
    const mockBlob = new Blob(['fake-pdf'], { type: 'application/pdf' });
    axios.get.mockResolvedValue({ data: mockBlob });

    // Mock URL and link creation
    const mockUrl = 'blob:test';
    global.URL.createObjectURL = vi.fn(() => mockUrl);
    global.URL.revokeObjectURL = vi.fn();
    const mockClick = vi.fn();
    const mockRemove = vi.fn();
    document.body.appendChild = vi.fn();
    document.createElement = vi.fn(() => ({
      href: '',
      click: mockClick,
      remove: mockRemove,
      setAttribute: vi.fn(),
    }));

    await supplierOrderApi.downloadPdf(1);

    expect(axios.get).toHaveBeenCalledWith('/api/supplier-order/export/1', { responseType: 'blob' });
  });
});
