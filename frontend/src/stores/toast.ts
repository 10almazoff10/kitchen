import { defineStore } from 'pinia';
import { ref } from 'vue';

export interface Toast {
  id: number;
  message: string;
  type: 'success' | 'error' | 'info';
}

export const useToastStore = defineStore('toast', () => {
  const toasts = ref<Toast[]>([]);
  let nextId = 1;

  function show(message: string, type: 'success' | 'error' | 'info' = 'info', duration = 3000) {
    const id = nextId++;
    toasts.value.push({ id, message, type });
    setTimeout(() => {
      remove(id);
    }, duration);
  }

  function success(message: string) {
    show(message, 'success');
  }

  function error(message: string) {
    show(message, 'error', 4000);
  }

  function remove(id: number) {
    toasts.value = toasts.value.filter((t) => t.id !== id);
  }

  return { toasts, show, success, error, remove };
});
