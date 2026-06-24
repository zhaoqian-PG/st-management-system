import api from '../api';
const BASE = '/api/invoice';
export const invoiceApi = {
  list(params) { return api.get(BASE, { params }); },
  getById(id) { return api.get(`${BASE}/${id}`); },
  create(data) { return api.post(BASE, data); },
  update(id, data) { return api.put(`${BASE}/${id}`, data); },
  delete(id) { return api.delete(`${BASE}/${id}`); },
  uploadDocument(id, formData) { return api.post(`${BASE}/${id}/documents`, formData); },
  deleteDocument(docId) { return api.delete(`${BASE}/documents/${docId}`); },
};
