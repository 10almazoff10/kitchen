import axios, { type InternalAxiosRequestConfig } from 'axios';

const api = axios.create({
  baseURL: '/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('kitchen_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

let isRefreshing = false;
let failedQueue: Array<{
  resolve: (token: string) => void;
  reject: (error: any) => void;
}> = [];

function processQueue(error: any, token: string | null = null) {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else if (token) {
      prom.resolve(token);
    }
  });
  failedQueue = [];
}

function handleAuthFailure(error: any) {
  localStorage.removeItem('kitchen_token');
  localStorage.removeItem('kitchen_refresh_token');
  localStorage.removeItem('kitchen_user');
  if (window.location.pathname !== '/login') {
    window.location.href = '/login';
  }
  return Promise.reject(error);
}

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config as (InternalAxiosRequestConfig & { _retry?: boolean }) | undefined;

    if (!error.response || error.response.status !== 401 || !originalRequest) {
      return Promise.reject(error);
    }

    const requestUrl = originalRequest.url || '';
    const isAuthEndpoint =
      requestUrl.includes('/auth/login') ||
      requestUrl.includes('/auth/refresh') ||
      requestUrl.includes('/auth/logout');

    if (originalRequest._retry || isAuthEndpoint) {
      return handleAuthFailure(error);
    }

    const refreshToken = localStorage.getItem('kitchen_refresh_token');
    if (!refreshToken) {
      return handleAuthFailure(error);
    }

    if (isRefreshing) {
      return new Promise<string>((resolve, reject) => {
        failedQueue.push({ resolve, reject });
      })
        .then((newToken) => {
          originalRequest.headers.Authorization = `Bearer ${newToken}`;
          return api(originalRequest);
        })
        .catch((err) => Promise.reject(err));
    }

    originalRequest._retry = true;
    isRefreshing = true;

    try {
      // Use raw axios call to bypass api interceptor on refresh request
      const { data } = await axios.post('/api/v1/auth/refresh', { refreshToken });

      localStorage.setItem('kitchen_token', data.token);
      if (data.refreshToken) {
        localStorage.setItem('kitchen_refresh_token', data.refreshToken);
      }
      if (data.user) {
        localStorage.setItem('kitchen_user', JSON.stringify(data.user));
      }

      api.defaults.headers.common.Authorization = `Bearer ${data.token}`;
      originalRequest.headers.Authorization = `Bearer ${data.token}`;

      processQueue(null, data.token);
      return api(originalRequest);
    } catch (refreshErr) {
      processQueue(refreshErr, null);
      return handleAuthFailure(refreshErr);
    } finally {
      isRefreshing = false;
    }
  }
);

export default api;
