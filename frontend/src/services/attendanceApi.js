import axios from 'axios';
const BASE = '/api/attendance';
export const attendanceApi = {
  list(params) { return axios.get(BASE, { params }); },
  getById(id) { return axios.get(`${BASE}/${id}`); },
  create(data) { return axios.post(BASE, data); },
  update(id, data) { return axios.put(`${BASE}/${id}`, data); },
  delete(id) { return axios.delete(`${BASE}/${id}`); },
};
