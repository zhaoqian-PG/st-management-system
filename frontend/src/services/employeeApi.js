import axios from 'axios';

const BASE = '/api/employee';

export const employeeApi = {
  list(params) { return axios.get(BASE, { params }); },
  getById(id) { return axios.get(`${BASE}/${id}`); },
  create(data) { return axios.post(BASE, data); },
  update(id, data) { return axios.put(`${BASE}/${id}`, data); },
  delete(id) { return axios.delete(`${BASE}/${id}`); },
  upload(id, formData) { return axios.post(`${BASE}/${id}/upload`, formData); },
  batchImport(formData) { return axios.post(`${BASE}/batch-import`, formData); },
  download(id) { return axios.get(`${BASE}/${id}/download`, { responseType: 'blob' }); },
};
