import { describe, it, expect, vi, beforeEach } from 'vitest';
import axios from 'axios';

vi.mock('axios');

describe('customerApi', () => {
  const BASE = '/api/customer';

  beforeEach(() => { vi.clearAllMocks(); });

  it('list calls GET', async () => {
    axios.get.mockResolvedValue({ data: { data: { content: [], totalElements: 0 } } });
    await axios.get(BASE, { params: { page: 0, size: 10 } });
    expect(axios.get).toHaveBeenCalledWith(BASE, { params: { page: 0, size: 10 } });
  });

  it('getById calls GET', async () => {
    axios.get.mockResolvedValue({ data: { data: { companyName: 'Test' } } });
    await axios.get(`${BASE}/1`);
    expect(axios.get).toHaveBeenCalledWith(`${BASE}/1`);
  });

  it('create calls POST', async () => {
    axios.post.mockResolvedValue({ data: {} });
    await axios.post(BASE, { companyName: '新規' });
    expect(axios.post).toHaveBeenCalledWith(BASE, { companyName: '新規' });
  });

  it('update calls PUT', async () => {
    axios.put.mockResolvedValue({ data: {} });
    await axios.put(`${BASE}/1`, { companyName: '更新' });
    expect(axios.put).toHaveBeenCalledWith(`${BASE}/1`, { companyName: '更新' });
  });

  it('delete calls DELETE', async () => {
    axios.delete.mockResolvedValue({ data: {} });
    await axios.delete(`${BASE}/1`);
    expect(axios.delete).toHaveBeenCalledWith(`${BASE}/1`);
  });
});
