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
  const refreshToken = ref<string | null>(localStorage.getItem('kitchen_refresh_token'));
  const user = ref<User | null>(
    localStorage.getItem('kitchen_user')
      ? JSON.parse(localStorage.getItem('kitchen_user') as string)
      : null
  );
  const isLdapEnabled = ref<boolean>(localStorage.getItem('kitchen_ldap_enabled') === 'true');
  const loading = ref(false);

  const isAuthenticated = computed(() => !!token.value || !!refreshToken.value);

  function setAuthData(data: { token: string; refreshToken?: string; user?: User; ldapEnabled?: boolean }) {
    token.value = data.token;
    localStorage.setItem('kitchen_token', data.token);

    if (data.refreshToken) {
      refreshToken.value = data.refreshToken;
      localStorage.setItem('kitchen_refresh_token', data.refreshToken);
    }
    if (data.user) {
      user.value = data.user;
      localStorage.setItem('kitchen_user', JSON.stringify(data.user));
    }
    if (data.ldapEnabled !== undefined) {
      isLdapEnabled.value = data.ldapEnabled;
      localStorage.setItem('kitchen_ldap_enabled', String(data.ldapEnabled));
    }
  }

  async function fetchAuthConfig() {
    try {
      const resp = await api.get('/auth/config');
      if (resp.data && resp.data.ldapEnabled !== undefined) {
        isLdapEnabled.value = Boolean(resp.data.ldapEnabled);
        localStorage.setItem('kitchen_ldap_enabled', String(resp.data.ldapEnabled));
      }
    } catch {
      // ignore
    }
  }

  async function login(username: string, password: string) {
    loading.value = true;
    try {
      const response = await api.post('/auth/login', { username, password });
      setAuthData(response.data);
      return response.data;
    } finally {
      loading.value = false;
    }
  }

  async function linkAd(adUsername: string, adPassword: string) {
    loading.value = true;
    try {
      const response = await api.post('/auth/link-ad', { adUsername, adPassword });
      setAuthData(response.data);
      return response.data;
    } finally {
      loading.value = false;
    }
  }

  async function refreshSession() {
    if (!refreshToken.value) {
      logout();
      throw new Error('Refresh token отсутствует');
    }
    try {
      const response = await api.post('/auth/refresh', { refreshToken: refreshToken.value });
      setAuthData(response.data);
      return response.data;
    } catch (e) {
      logout();
      throw e;
    }
  }

  async function fetchMe() {
    if (!token.value && !refreshToken.value) return;
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

  async function logout() {
    const currentRefreshToken = refreshToken.value;
    token.value = null;
    refreshToken.value = null;
    user.value = null;
    localStorage.removeItem('kitchen_token');
    localStorage.removeItem('kitchen_refresh_token');
    localStorage.removeItem('kitchen_user');

    if (currentRefreshToken) {
      try {
        await api.post('/auth/logout', { refreshToken: currentRefreshToken });
      } catch {
        // Игнорируем сетевые ошибки при выходе
      }
    }
  }

  return {
    token,
    refreshToken,
    user,
    loading,
    isAuthenticated,
    isLdapEnabled,
    fetchAuthConfig,
    setAuthData,
    login,
    linkAd,
    refreshSession,
    updateProfile,
    fetchMe,
    logout,
  };
});
