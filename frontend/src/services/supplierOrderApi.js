import api from '../api';
const BASE = '/api/supplier-order';
export const supplierOrderApi = {
  list(params) { return api.get(BASE, { params }); },
  getById(id) { return api.get(`${BASE}/${id}`); },
  create(data) { return api.post(BASE, data); },
  update(id, data) { return api.put(`${BASE}/${id}`, data); },
  delete(id) { return api.delete(`${BASE}/${id}`); },
  async downloadPdf(id) {
    const response = await api.get(`${BASE}/export/${id}`, { responseType: 'blob' });
    const url = window.URL.createObjectURL(new Blob([response.data], { type: 'application/pdf' }));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `hacchusho_${id}.pdf`);
    document.body.appendChild(link);
    link.click();
    link.remove();
    // Wait for browser to start download before resolving
    await new Promise(r => setTimeout(r, 1000));
    window.URL.revokeObjectURL(url);
  },
};
