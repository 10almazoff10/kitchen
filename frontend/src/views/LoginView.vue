<script setup lang="ts">
import { ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';
import { ShieldAlert, Loader2, Info } from 'lucide-vue-next';

const router = useRouter();
const authStore = useAuthStore();

const username = ref('');
const password = ref('');
const error = ref<string | null>(null);

watch(() => authStore.user?.isAdLinked, (linked) => {
  if (linked) {
    router.push('/');
  }
});

async function onSubmit() {
  if (!username.value.trim() || !password.value) {
    error.value = 'Пожалуйста, введите логин и пароль';
    return;
  }

  error.value = null;
  try {
    const res = await authStore.login(username.value.trim(), password.value);
    if (!res.requiresAdLink) {
      router.push('/');
    }
  } catch (err: any) {
    if (err.response?.data?.message) {
      error.value = err.response.data.message;
    } else {
      error.value = 'Не удалось войти. Проверьте правильность логина и пароля.';
    }
  }
}
</script>

<template>
  <div class="min-h-[85vh] flex items-center justify-center p-4">
    <div class="max-w-md w-full bg-white/95 dark:bg-slate-900/95 backdrop-blur-xl border border-slate-200/80 dark:border-slate-800 rounded-3xl shadow-xl p-8 transition">
      <div class="text-center mb-6">
        <div class="w-16 h-16 bg-[#274eac] rounded-2xl mx-auto flex items-center justify-center shadow-md shadow-blue-900/20 mb-4 p-2.5">
          <img src="/img/logo_kitchen.png" alt="Kitchen" class="w-full h-full object-contain" />
        </div>
        <h1 class="text-2xl font-bold text-slate-900 dark:text-slate-100 tracking-tight">Kitchen Login</h1>
        <p class="text-sm text-slate-500 dark:text-slate-400 mt-1">Вход в корпоративную систему заказов</p>
      </div>

      <!-- Подсказка для старых пользователей -->
      <div class="mb-5 p-3.5 rounded-2xl bg-blue-50/80 dark:bg-blue-950/40 border border-blue-200/70 dark:border-blue-900/50 text-xs text-blue-900 dark:text-blue-200 flex items-start gap-2.5 leading-relaxed">
        <Info class="w-4 h-4 shrink-0 text-[#274eac] dark:text-blue-400 mt-0.5" />
        <div>
          <strong class="font-semibold">Пользователям старой версии:</strong> введите ваш прежний логин и пароль — система автоматически предложит привязать корпоративную учетную запись AD для переноса истории.
        </div>
      </div>

      <div v-if="error" class="mb-6 p-4 rounded-xl bg-red-50 dark:bg-rose-950/40 border border-red-200 dark:border-rose-900/60 text-red-700 dark:text-rose-300 text-sm flex items-start space-x-3">
        <ShieldAlert class="w-5 h-5 shrink-0 text-red-500 dark:text-rose-400 mt-0.5" />
        <span>{{ error }}</span>
      </div>

      <form @submit.prevent="onSubmit" class="space-y-5">
        <div>
          <label class="block text-xs font-semibold uppercase tracking-wider text-slate-600 dark:text-slate-400 mb-1.5">Доменный логин</label>
          <input
            v-model="username"
            type="text"
            required
            autocomplete="username"
            placeholder="например, ivanov"
            class="w-full px-4 py-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 placeholder-slate-400 dark:placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-[#274eac]/20 focus:border-[#274eac] transition text-sm"
          />
        </div>

        <div>
          <label class="block text-xs font-semibold uppercase tracking-wider text-slate-600 dark:text-slate-400 mb-1.5">Пароль</label>
          <input
            v-model="password"
            type="password"
            required
            autocomplete="current-password"
            placeholder="••••••••"
            class="w-full px-4 py-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 placeholder-slate-400 dark:placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-[#274eac]/20 focus:border-[#274eac] transition text-sm"
          />
        </div>

        <button
          type="submit"
          :disabled="authStore.loading"
          class="w-full py-3 px-4 rounded-xl bg-[#274eac] hover:bg-[#1f3f8d] text-white font-medium text-sm transition shadow-sm flex items-center justify-center space-x-2 disabled:opacity-70 active:scale-[0.98] cursor-pointer"
        >
          <Loader2 v-if="authStore.loading" class="w-4 h-4 animate-spin" />
          <span>{{ authStore.loading ? 'Вход в домен...' : 'Войти' }}</span>
        </button>
      </form>
    </div>
  </div>
</template>
