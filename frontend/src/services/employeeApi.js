import api from '../api';

const BASE = '/api/employee';

export const employeeApi = {
  list(params) { return api.get(BASE, { params }); },
  getById(id) { return api.get(`${BASE}/${id}`); },
  create(data) { return api.post(BASE, data); },
  update(id, data) { return api.put(`${BASE}/${id}`, data); },
  delete(id) { return api.delete(`${BASE}/${id}`); },
  upload(id, formData) { return api.post(`${BASE}/${id}/upload`, formData); },
  batchImport(formData) { return api.post(`${BASE}/batch-import`, formData); },
  deleteAttachment(attId) { return api.delete(`${BASE}/attachments/${attId}`); },
};
