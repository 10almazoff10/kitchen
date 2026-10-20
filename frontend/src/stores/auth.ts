import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import api from '../api/client';

export interface User {
  id: number;
  username: string;
  fullName: string;
  email?: string;
  avatarUrl?: string;
  adUsername?: string;
  legacyUsername?: string;
  isAdLinked?: boolean;
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem('kitchen_token'));
  const user = ref<User | null>(
    localStorage.getItem('kitchen_user')
      ? JSON.parse(localStorage.getItem('kitchen_user') as string)
      : null
  );
  const loading = ref(false);

  const isAuthenticated = computed(() => !!token.value);

  async function login(username: string, password: string) {
    loading.value = true;
    try {
      const response = await api.post('/auth/login', { username, password });
      token.value = response.data.token;
      user.value = response.data.user;

      localStorage.setItem('kitchen_token', response.data.token);
      localStorage.setItem('kitchen_user', JSON.stringify(response.data.user));
      return response.data;
    } finally {
      loading.value = false;
    }
  }

  async function linkAd(adUsername: string, adPassword: string) {
    loading.value = true;
    try {
      const response = await api.post('/auth/link-ad', { adUsername, adPassword });
      token.value = response.data.token;
      user.value = response.data.user;

      localStorage.setItem('kitchen_token', response.data.token);
      localStorage.setItem('kitchen_user', JSON.stringify(response.data.user));
      return response.data;
    } finally {
      loading.value = false;
    }
  }

  async function fetchMe() {
    if (!token.value) return;
    try {
      const response = await api.get('/auth/me');
      user.value = response.data;
      localStorage.setItem('kitchen_user', JSON.stringify(response.data));
    } catch {
      logout();
    }
  }

  async function updateProfile(data: { email?: string; fullName?: string }) {
    loading.value = true;
    try {
      const response = await api.put('/auth/profile', data);
      user.value = response.data;
      localStorage.setItem('kitchen_user', JSON.stringify(response.data));
      return response.data;
    } finally {
      loading.value = false;
    }
  }

  function logout() {
    token.value = null;
    user.value = null;
    localStorage.removeItem('kitchen_token');
    localStorage.removeItem('kitchen_user');
  }

  return {
    token,
    user,
    loading,
    isAuthenticated,
    login,
    linkAd,
    updateProfile,
    fetchMe,
    logout,
  };
});
