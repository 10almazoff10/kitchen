<script setup lang="ts">
import { ref } from 'vue';
import { useAuthStore } from '../stores/auth';
import { useToastStore } from '../stores/toast';
import { ShieldCheck, ShieldAlert, Loader2, LogOut, KeyRound } from 'lucide-vue-next';

const props = defineProps<{
  userFullName?: string;
  legacyUsername?: string;
}>();

const emit = defineEmits<{
  (e: 'success'): void;
}>();

const authStore = useAuthStore();
const toastStore = useToastStore();

const adUsername = ref('');
const adPassword = ref('');
const error = ref<string | null>(null);

async function onLinkSubmit() {
  if (!adUsername.value.trim() || !adPassword.value) {
    error.value = 'Пожалуйста, введите корпоративный логин и пароль Active Directory';
    return;
  }

  error.value = null;
  try {
    await authStore.linkAd(adUsername.value.trim(), adPassword.value);
    toastStore.show('Учетная запись Active Directory успешно привязана! История сохранена.', 'success');
    emit('success');
  } catch (err: any) {
    if (err.response?.data?.message) {
      error.value = err.response.data.message;
    } else {
      error.value = 'Не удалось привязать учетную запись AD. Проверьте правильность доменного логина и пароля.';
    }
  }
}

function onLogout() {
  authStore.logout();
  window.location.reload();
}
</script>

<template>
  <div class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/70 backdrop-blur-md">
    <div
      class="max-w-md w-full bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-3xl shadow-2xl p-6 sm:p-8 space-y-6 animate-in fade-in zoom-in-95 duration-200"
    >
      <!-- Заголовок и иконка -->
      <div class="text-center space-y-2">
        <div class="w-14 h-14 bg-blue-50 dark:bg-blue-950/60 border border-blue-200 dark:border-blue-900/60 text-[#274eac] dark:text-blue-400 rounded-2xl mx-auto flex items-center justify-center shadow-xs">
          <ShieldCheck class="w-7 h-7" />
        </div>
        <h2 class="text-xl font-bold text-slate-900 dark:text-slate-100 tracking-tight">
          Привязка Active Directory
        </h2>
        <p class="text-xs sm:text-sm text-slate-600 dark:text-slate-400 leading-relaxed">
          Вы вошли под старой учетной записью
          <strong v-if="props.userFullName" class="text-slate-800 dark:text-slate-200 font-semibold">{{ props.userFullName }}</strong>
          <span v-if="props.legacyUsername"> ({{ props.legacyUsername }})</span>.
          Для переноса истории ваших заказов, долгов и отзывов в новую версию сервиса привяжите корпоративную учетную запись.
        </p>
      </div>

      <!-- Сообщение об ошибке -->
      <div
        v-if="error"
        class="p-3.5 rounded-xl bg-rose-50 dark:bg-rose-950/40 border border-rose-200 dark:border-rose-900/60 text-rose-700 dark:text-rose-300 text-xs sm:text-sm flex items-start gap-2.5"
      >
        <ShieldAlert class="w-4 h-4 shrink-0 text-rose-500 mt-0.5" />
        <span>{{ error }}</span>
      </div>

      <!-- Форма ввода данных AD -->
      <form @submit.prevent="onLinkSubmit" class="space-y-4">
        <div>
          <label class="block text-xs font-semibold uppercase tracking-wider text-slate-600 dark:text-slate-400 mb-1.5">
            Доменный логин AD
          </label>
          <div class="relative">
            <input
              v-model="adUsername"
              type="text"
              required
              autocomplete="username"
              placeholder="например ivanov.i, или ivanov.i@example.com"
              class="w-full px-4 py-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 placeholder-slate-400 dark:placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-[#274eac]/20 focus:border-[#274eac] transition text-sm"
            />
          </div>
          <p class="text-[11px] text-slate-400 mt-1">
            Ваш логин в корпоративном домене Active Directory
          </p>
        </div>

        <div>
          <label class="block text-xs font-semibold uppercase tracking-wider text-slate-600 dark:text-slate-400 mb-1.5">
            Пароль Active Directory
          </label>
          <div class="relative">
            <input
              v-model="adPassword"
              type="password"
              required
              autocomplete="current-password"
              placeholder="••••••••••••"
              class="w-full px-4 py-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 placeholder-slate-400 dark:placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-[#274eac]/20 focus:border-[#274eac] transition text-sm"
            />
          </div>
        </div>

        <!-- Кнопки действий -->
        <div class="pt-2 flex flex-col sm:flex-row items-center gap-2.5">
          <button
            type="submit"
            :disabled="authStore.loading"
            class="w-full sm:flex-1 py-2.5 px-4 rounded-xl bg-[#274eac] hover:bg-[#1f3f8d] text-white font-medium text-sm transition shadow-sm flex items-center justify-center gap-2 disabled:opacity-60 active:scale-[0.98] cursor-pointer"
          >
            <Loader2 v-if="authStore.loading" class="w-4 h-4 animate-spin" />
            <KeyRound v-else class="w-4 h-4" />
            <span>{{ authStore.loading ? 'Проверка в AD...' : 'Привязать и войти' }}</span>
          </button>

          <button
            type="button"
            @click="onLogout"
            class="w-full sm:w-auto py-2.5 px-3.5 rounded-xl border border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 font-medium text-sm transition flex items-center justify-center gap-1.5 cursor-pointer"
            title="Выйти из аккаунта"
          >
            <LogOut class="w-4 h-4 text-slate-400" />
            <span class="sm:hidden">Выйти</span>
          </button>
        </div>
      </form>
    </div>
  </div>
</template>
