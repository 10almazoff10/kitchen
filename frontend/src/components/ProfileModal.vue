<template>
  <div
    v-if="isOpen"
    class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-xs transition-opacity"
    @click.self="close"
  >
    <div
      class="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 shadow-2xl max-w-md w-full overflow-hidden transition-all transform animate-in fade-in zoom-in-95 duration-200"
    >
      <!-- Шапка -->
      <div class="px-6 py-5 border-b border-slate-100 dark:border-slate-800 flex items-center justify-between bg-slate-50/70 dark:bg-slate-800/60">
        <div class="flex items-center gap-2.5">
          <div class="w-8 h-8 rounded-xl bg-blue-50 dark:bg-blue-950/60 text-blue-600 dark:text-blue-400 flex items-center justify-center">
            <UserIcon class="w-4 h-4" />
          </div>
          <h3 class="text-base font-bold text-slate-900 dark:text-slate-100">
            Профиль пользователя
          </h3>
        </div>
        <button
          @click="close"
          class="p-1.5 rounded-xl text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition cursor-pointer"
        >
          <X class="w-5 h-5" />
        </button>
      </div>

      <!-- Содержимое -->
      <form @submit.prevent="handleSubmit" class="p-6 space-y-5">
        <!-- Аватар и статус -->
        <div class="flex items-center gap-4 p-4 rounded-2xl bg-slate-50 dark:bg-slate-800/40 border border-slate-100 dark:border-slate-800">
          <UserAvatar
            :src="authStore.user?.avatarUrl"
            :name="authStore.user?.fullName"
            size-class="w-16 h-16"
            rounded-class="rounded-2xl"
            border-class="ring-2 ring-blue-500/20"
          />
          <div class="min-w-0 flex-1">
            <div class="text-sm font-bold text-slate-900 dark:text-slate-100 truncate">
              {{ authStore.user?.fullName }}
            </div>
            <div class="text-xs text-slate-500 dark:text-slate-400 truncate">
              @{{ authStore.user?.username }}
            </div>
            <div class="mt-1 flex items-center gap-1.5">
              <span
                v-if="authStore.user?.isAdLinked"
                class="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[10px] font-semibold bg-emerald-50 dark:bg-emerald-950/50 text-emerald-700 dark:text-emerald-400"
              >
                <CheckCircle2 class="w-3 h-3" />
                Active Directory
              </span>
              <span
                v-else
                class="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[10px] font-semibold bg-amber-50 dark:bg-amber-950/50 text-amber-700 dark:text-amber-400"
              >
                Локальный аккаунт
              </span>
            </div>
          </div>
        </div>

        <!-- Поле: Email для Gravatar -->
        <div>
          <label class="block text-xs font-bold text-slate-700 dark:text-slate-300 mb-1.5">
            Email для Gravatar
          </label>
          <div class="relative">
            <input
              v-model="emailInput"
              type="email"
              placeholder="user@example.com"
              class="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition"
            />
          </div>
          <p class="mt-1.5 text-[11px] text-slate-400 dark:text-slate-500 leading-normal">
            Используется для загрузки фото с
            <a
              href="https://gravatar.com"
              target="_blank"
              rel="noopener noreferrer"
              class="text-blue-600 dark:text-blue-400 underline hover:no-underline"
            >
              gravatar.com
            </a>. Если у вас нет фото, Gravatar автоматически сгенерирует уникальный стиль.
          </p>
        </div>

        <!-- Поле: Полное имя -->
        <div>
          <label class="block text-xs font-bold text-slate-700 dark:text-slate-300 mb-1.5">
            Имя и фамилия
          </label>
          <input
            v-model="fullNameInput"
            type="text"
            required
            placeholder="Иван Иванов"
            class="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition"
          />
        </div>

        <!-- Ошибка -->
        <div
          v-if="errorMessage"
          class="p-3 rounded-xl bg-rose-50 dark:bg-rose-950/40 border border-rose-200 dark:border-rose-900/50 text-rose-600 dark:text-rose-400 text-xs flex items-center gap-2"
        >
          <AlertCircle class="w-4 h-4 shrink-0" />
          <span>{{ errorMessage }}</span>
        </div>

        <!-- Кнопки действий -->
        <div class="pt-2 flex items-center justify-end gap-3">
          <button
            type="button"
            @click="close"
            class="px-4 py-2.5 rounded-xl text-xs font-semibold text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800 transition cursor-pointer"
          >
            Отмена
          </button>
          <button
            type="submit"
            :disabled="saving"
            class="px-5 py-2.5 rounded-xl text-xs font-semibold text-white bg-blue-600 hover:bg-blue-700 dark:bg-blue-600 dark:hover:bg-blue-500 shadow-sm transition active:scale-95 cursor-pointer disabled:opacity-60 flex items-center gap-2"
          >
            <Loader2 v-if="saving" class="w-3.5 h-3.5 animate-spin" />
            <span>{{ saving ? 'Сохранение...' : 'Сохранить изменения' }}</span>
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { useAuthStore } from '@/stores/auth';
import { useToastStore } from '@/stores/toast';
import UserAvatar from '@/components/UserAvatar.vue';
import {
  User as UserIcon,
  X,
  CheckCircle2,
  AlertCircle,
  Loader2,
} from 'lucide-vue-next';

const props = defineProps<{
  isOpen: boolean;
}>();

const emit = defineEmits<{
  (e: 'close'): void;
}>();

const authStore = useAuthStore();
const toastStore = useToastStore();

const emailInput = ref('');
const fullNameInput = ref('');
const saving = ref(false);
const errorMessage = ref<string | null>(null);

watch(
  () => props.isOpen,
  (open) => {
    if (open && authStore.user) {
      emailInput.value = authStore.user.email || '';
      fullNameInput.value = authStore.user.fullName || '';
      errorMessage.value = null;
    }
  },
  { immediate: true }
);

function close() {
  emit('close');
}

async function handleSubmit() {
  if (!fullNameInput.value.trim()) {
    errorMessage.value = 'Имя не может быть пустым';
    return;
  }

  saving.value = true;
  errorMessage.value = null;
  try {
    await authStore.updateProfile({
      email: emailInput.value.trim(),
      fullName: fullNameInput.value.trim(),
    });
    toastStore.success('Профиль успешно обновлен!');
    close();
  } catch (err: any) {
    errorMessage.value = err.response?.data?.message || 'Не удалось обновить профиль';
  } finally {
    saving.value = false;
  }
}
</script>
