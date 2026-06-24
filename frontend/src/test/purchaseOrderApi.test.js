import { describe, it, expect, vi, beforeEach } from 'vitest';
import axios from 'axios';

vi.mock('axios');

describe('purchaseOrderApi', () => {
  const BASE = '/api/purchase-order';

  beforeEach(() => { vi.clearAllMocks(); });

  it('list calls GET with params', async () => {
    axios.get.mockResolvedValue({ data: { data: { content: [], totalElements: 0 } } });
    await axios.get(BASE, { params: { page: 0, size: 10 } });
    expect(axios.get).toHaveBeenCalledWith(BASE, { params: { page: 0, size: 10 } });
  });

  it('getById calls GET', async () => {
    axios.get.mockResolvedValue({ data: {} });
    await axios.get(`${BASE}/1`);
    expect(axios.get).toHaveBeenCalledWith(`${BASE}/1`);
  });

  it('create calls POST', async () => {
    axios.post.mockResolvedValue({ data: {} });
    const dto = { customerId: 1, orderDate: '2026-01-01', amount: 1000000 };
    await axios.post(BASE, dto);
    expect(axios.post).toHaveBeenCalledWith(BASE, dto);
  });

  it('update calls PUT', async () => {
    axios.put.mockResolvedValue({ data: {} });
    const dto = { amount: 2000000 };
    await axios.put(`${BASE}/1`, dto);
    expect(axios.put).toHaveBeenCalledWith(`${BASE}/1`, dto);
  });

  it('delete calls DELETE', async () => {
    axios.delete.mockResolvedValue({ data: {} });
    await axios.delete(`${BASE}/1`);
    expect(axios.delete).toHaveBeenCalledWith(`${BASE}/1`);
  });

  it('upload calls POST with FormData', async () => {
    axios.post.mockResolvedValue({ data: {} });
    const fd = new FormData();
    fd.append('file', new Blob(['test']));
    await axios.post(`${BASE}/1/upload`, fd);
    expect(axios.post).toHaveBeenCalledWith(`${BASE}/1/upload`, expect.any(FormData));
  });
});
