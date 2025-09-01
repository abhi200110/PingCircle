import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';

const setJwtToken = (token) => {
  localStorage.setItem('jwt-token', token);
};

const getJwtToken = () => {
  return localStorage.getItem('jwt-token');
};

const removeJwtToken = () => {
  localStorage.removeItem('jwt-token');
};

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use(
  (config) => {
    const token = getJwtToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

api.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    if (error.response?.status === 401 || error.response?.status === 403) {
      console.log('Authentication error:', error.response?.status, '- Redirecting to login');
      removeJwtToken();
      localStorage.removeItem('chat-username');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export { setJwtToken, getJwtToken, removeJwtToken };
export default api; 