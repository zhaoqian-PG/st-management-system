import axios from 'axios';

// In production (vite build / vite preview), point to the Render cloud backend.
// In development (vite dev), use an empty base so requests go through the Vite proxy.
const API_BASE_URL = import.meta.env.PROD
  ? 'https://st-management-api.onrender.com'
  : '';

const api = axios.create({
  baseURL: API_BASE_URL,
});

export default api;
