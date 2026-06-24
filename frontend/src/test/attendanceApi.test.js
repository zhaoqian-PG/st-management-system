import { describe, it, expect, vi, beforeEach } from 'vitest';
import axios from 'axios';

vi.mock('axios');
const BASE = '/api/attendance';

describe('attendanceApi', () => {
  beforeEach(() => { vi.clearAllMocks(); });
  it('list calls GET', async () => {
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
    await axios.post(BASE, { employeeId: 1, workDate: '2026-05-01', workHours: 8 });
    expect(axios.post).toHaveBeenCalledWith(BASE, { employeeId: 1, workDate: '2026-05-01', workHours: 8 });
  });
  it('update calls PUT', async () => {
    axios.put.mockResolvedValue({ data: {} });
    await axios.put(`${BASE}/1`, { workHours: 7 });
    expect(axios.put).toHaveBeenCalledWith(`${BASE}/1`, { workHours: 7 });
  });
  it('delete calls DELETE', async () => {
    axios.delete.mockResolvedValue({ data: {} });
    await axios.delete(`${BASE}/1`);
    expect(axios.delete).toHaveBeenCalledWith(`${BASE}/1`);
  });
  it('monthlySummary calls GET', async () => {
    axios.get.mockResolvedValue({ data: { data: {} } });
    await axios.get(`${BASE}/monthly-summary`, { params: { year: 2026, month: 5 } });
    expect(axios.get).toHaveBeenCalledWith(`${BASE}/monthly-summary`, { params: { year: 2026, month: 5 } });
  });
  it('generate calls POST', async () => {
    axios.post.mockResolvedValue({ data: {} });
    await axios.post(`${BASE}/generate`, null, { params: { year: 2026, month: 5, employeeId: 1 } });
    expect(axios.post).toHaveBeenCalled();
  });
});
