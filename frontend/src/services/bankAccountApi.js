import api from '../api';

const API_BASE = '/api/bank-accounts';

export const bankAccountApi = {
  // 一覧取得（顧客IDでフィルター可能）
  list(params = {}) {
    return api.get(API_BASE, { params });
  },

  // 詳細取得
  getById(id) {
    return api.get(`${API_BASE}/${id}`);
  },

  // 顧客別一覧
  getByCustomerId(customerId) {
    return api.get(`${API_BASE}/customer/${customerId}`);
  },

  // 新規登録
  create(data) {
    return api.post(API_BASE, data);
  },

  // 更新
  update(id, data) {
    return api.put(`${API_BASE}/${id}`, data);
  },

  // 削除
  delete(id) {
    return api.delete(`${API_BASE}/${id}`);
  },

  // 顧客に紐付け
  bindToCustomer(id, customerId) {
    return api.put(`${API_BASE}/${id}/bind/${customerId}`);
  },

  // 紐付け解除
  unbindFromCustomer(id) {
    return api.put(`${API_BASE}/${id}/unbind`);
  },
  // 社員紐付け
  bindToEmployee(id, employeeId) {
    return api.put(`${API_BASE}/${id}/bind-employee/${employeeId}`);
  },
  unbindFromEmployee(id) {
    return api.put(`${API_BASE}/${id}/unbind-employee`);
  },
  getByEmployeeId(employeeId) {
    return api.get(`${API_BASE}/employee/${employeeId}`);
  },
  setDefaultForEmployee(id, employeeId) {
    return api.put(`${API_BASE}/${id}/set-default-employee/${employeeId}`);
  },
  setDefaultForCustomer(id, customerId) {
    return api.put(`${API_BASE}/${id}/set-default-customer/${customerId}`);
  },
};
