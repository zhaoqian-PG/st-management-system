import { describe, it, expect, vi, beforeEach } from 'vitest';
import axios from 'axios';

vi.mock('axios');

describe('invoiceApi', () => {
  const BASE = '/api/invoice';

  beforeEach(() => { vi.clearAllMocks(); });

  it('list calls GET with params', async () => {
    axios.get.mockResolvedValue({ data: { data: { content: [], totalElements: 0 } } });
    await axios.get(BASE, { params: { page: 0, size: 10, year: 2026 } });
    expect(axios.get).toHaveBeenCalledWith(BASE, { params: { page: 0, size: 10, year: 2026 } });
  });

  it('getById calls GET', async () => {
    axios.get.mockResolvedValue({ data: {} });
    await axios.get(`${BASE}/1`);
    expect(axios.get).toHaveBeenCalledWith(`${BASE}/1`);
  });

  it('create calls POST', async () => {
    axios.post.mockResolvedValue({ data: {} });
    const dto = { customerId: 1, year: 2026, month: 6, amount: 500000 };
    await axios.post(BASE, dto);
    expect(axios.post).toHaveBeenCalledWith(BASE, dto);
  });

  it('update calls PUT', async () => {
    axios.put.mockResolvedValue({ data: {} });
    const dto = { status: '送付済' };
    await axios.put(`${BASE}/1`, dto);
    expect(axios.put).toHaveBeenCalledWith(`${BASE}/1`, dto);
  });

  it('delete calls DELETE', async () => {
    axios.delete.mockResolvedValue({ data: {} });
    await axios.delete(`${BASE}/1`);
    expect(axios.delete).toHaveBeenCalledWith(`${BASE}/1`);
  });

  it('uploadDocument calls POST with FormData', async () => {
    axios.post.mockResolvedValue({ data: {} });
    const fd = new FormData(); fd.append('file', new Blob(['test']));
    await axios.post(`${BASE}/1/upload-document`, fd);
    expect(axios.post).toHaveBeenCalledWith(`${BASE}/1/upload-document`, expect.any(FormData));
  });
});
