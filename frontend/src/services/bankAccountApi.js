import axios from 'axios';

const API_BASE = '/api/bank-accounts';

export const bankAccountApi = {
  // 一覧取得（顧客IDでフィルター可能）
  list(params = {}) {
    return axios.get(API_BASE, { params });
  },

  // 詳細取得
  getById(id) {
    return axios.get(`${API_BASE}/${id}`);
  },

  // 顧客別一覧
  getByCustomerId(customerId) {
    return axios.get(`${API_BASE}/customer/${customerId}`);
  },

  // 新規登録
  create(data) {
    return axios.post(API_BASE, data);
  },

  // 更新
  update(id, data) {
    return axios.put(`${API_BASE}/${id}`, data);
  },

  // 削除
  delete(id) {
    return axios.delete(`${API_BASE}/${id}`);
  },

  // 顧客に紐付け
  bindToCustomer(id, customerId) {
    return axios.put(`${API_BASE}/${id}/bind/${customerId}`);
  },

  // 紐付け解除
  unbindFromCustomer(id) {
    return axios.put(`${API_BASE}/${id}/unbind`);
  },
};
