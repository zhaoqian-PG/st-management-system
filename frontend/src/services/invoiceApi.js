import axios from 'axios';
const BASE = '/api/invoice';
export const invoiceApi = {
  list(params) { return axios.get(BASE, { params }); },
  getById(id) { return axios.get(`${BASE}/${id}`); },
  create(data) { return axios.post(BASE, data); },
  update(id, data) { return axios.put(`${BASE}/${id}`, data); },
  delete(id) { return axios.delete(`${BASE}/${id}`); },
  uploadDocument(id, formData) { return axios.post(`${BASE}/${id}/documents`, formData); },
  deleteDocument(docId) { return axios.delete(`${BASE}/documents/${docId}`); },
};
