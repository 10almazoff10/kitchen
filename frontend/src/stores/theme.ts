import { defineStore } from 'pinia';
import { ref } from 'vue';

const STORAGE_KEY = 'kitchen-theme';

export const useThemeStore = defineStore('theme', () => {
  const isDark = ref(false);

  function applyTheme(dark: boolean) {
    isDark.value = dark;
    if (dark) {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  }

  function initTheme() {
    try {
      const stored = localStorage.getItem(STORAGE_KEY);
      if (stored === 'dark') {
        applyTheme(true);
      } else if (stored === 'light') {
        applyTheme(false);
      } else {
        const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
        applyTheme(prefersDark);
      }

      // Listen for system theme changes if no explicit user preference is set
      window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', (e) => {
        if (!localStorage.getItem(STORAGE_KEY)) {
          applyTheme(e.matches);
        }
      });
    } catch {
      // Fallback
      applyTheme(false);
    }
  }

  function toggleTheme() {
    const next = !isDark.value;
    applyTheme(next);
    try {
      localStorage.setItem(STORAGE_KEY, next ? 'dark' : 'light');
    } catch {
      // ignore storage error
    }
  }

  return {
    isDark,
    initTheme,
    toggleTheme,
  };
});
