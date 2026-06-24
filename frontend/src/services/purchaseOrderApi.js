import api from '../api';
const BASE = '/api/purchase-order';
export const purchaseOrderApi = {
  list(params) { return api.get(BASE, { params }); },
  getById(id) { return api.get(`${BASE}/${id}`); },
  create(data) { return api.post(BASE, data); },
  update(id, data) { return api.put(`${BASE}/${id}`, data); },
  delete(id) { return api.delete(`${BASE}/${id}`); },
};
