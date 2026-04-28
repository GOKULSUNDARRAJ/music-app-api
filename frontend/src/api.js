import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:3000/api'
});

export const setAuthToken = (token) => {
  if (token) {
    api.defaults.headers.common.Authorization = `Bearer ${token}`;
    localStorage.setItem('adminToken', token);
  } else {
    delete api.defaults.headers.common.Authorization;
    localStorage.removeItem('adminToken');
  }
};

const persistedToken = localStorage.getItem('adminToken');
if (persistedToken) {
  setAuthToken(persistedToken);
}

export default api;
