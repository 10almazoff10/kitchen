<template>
  <div class="space-y-8 max-w-6xl mx-auto">
    <!-- Заголовок страницы и кнопка добавления -->
    <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
      <div>
        <h1 class="text-2xl sm:text-3xl font-extrabold text-slate-900 dark:text-slate-100 tracking-tight">
          Рестораны
        </h1>
        <p class="text-slate-500 dark:text-slate-400 text-sm mt-1">
          Каталог заведений для заказа еды. Выберите ресторан или добавьте новый.
        </p>
      </div>

      <button
        @click="openAddModal"
        class="inline-flex items-center gap-2 px-5 py-2.5 bg-[#274eac] hover:bg-[#1f3f8d] text-white font-semibold text-sm rounded-xl shadow-xs transition-all active:scale-95 cursor-pointer self-start sm:self-auto"
      >
        <Plus class="w-4 h-4" />
        <span>Добавить ресторан</span>
      </button>
    </div>

    <!-- Поиск ресторана -->
    <div class="relative max-w-md">
      <Search class="w-4 h-4 text-slate-400 absolute left-3.5 top-3" />
      <input
        v-model="searchQuery"
        type="text"
        placeholder="Поиск по названию заведения..."
        class="w-full pl-10 pr-4 py-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 placeholder-slate-400 dark:placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-[#274eac]/25 focus:border-[#274eac] transition text-sm"
      />
    </div>

    <!-- Состояние загрузки -->
    <div v-if="loading" class="flex justify-center items-center py-16">
      <Loader2 class="w-8 h-8 text-[#274eac] animate-spin" />
    </div>

    <!-- Ошибка -->
    <div v-else-if="error" class="p-4 rounded-2xl bg-rose-50 dark:bg-rose-950/40 border border-rose-200 dark:border-rose-900/60 text-rose-700 dark:text-rose-300 text-sm flex items-center gap-3">
      <AlertCircle class="w-5 h-5 shrink-0 text-rose-500" />
      <span>{{ error }}</span>
    </div>

    <!-- Сетка ресторанов -->
    <div v-else-if="filteredRestaurants.length" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
      <div
        v-for="r in filteredRestaurants"
        :key="r.id"
        class="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-5 shadow-xs hover:shadow-md hover:-translate-y-0.5 transition-all duration-200 flex flex-col justify-between"
      >
        <div class="space-y-3">
          <div class="flex items-start justify-between gap-3">
            <h3 class="text-lg font-bold text-slate-900 dark:text-slate-100 line-clamp-1">
              {{ r.name }}
            </h3>
            <div class="w-10 h-10 rounded-xl bg-[#eff4fe] dark:bg-blue-950/60 flex items-center justify-center shrink-0 border border-[#bfdbfe]/60 dark:border-blue-900/60 overflow-hidden">
              <img
                v-if="r.websiteUrl"
                :src="getFaviconUrl(r.websiteUrl)"
                :alt="r.name"
                class="w-5 h-5 object-contain"
                @error="($event.target as HTMLElement).style.display = 'none'"
              />
              <Store v-else class="w-4 h-4 text-[#274eac] dark:text-blue-400" />
            </div>
          </div>

          <!-- Ссылка на сайт -->
          <div v-if="r.websiteUrl" class="text-xs">
            <a
              :href="r.websiteUrl"
              target="_blank"
              rel="noopener noreferrer"
              class="text-[#274eac] dark:text-blue-400 hover:text-[#1f3f8d] dark:hover:text-blue-300 hover:underline inline-flex items-center gap-1 font-semibold truncate max-w-full"
            >
              <span>Сайт меню</span>
              <ExternalLink class="w-3.5 h-3.5 shrink-0" />
            </a>
          </div>
          <div v-else class="text-xs text-slate-400 dark:text-slate-500">
            Сайт не указан
          </div>
        </div>

        <!-- Действия -->
        <div class="mt-6 pt-4 border-t border-slate-100 dark:border-slate-800 flex items-center justify-between gap-2">
          <button
            @click="createOrderFrom(r.id)"
            class="inline-flex items-center gap-1.5 px-3 py-1.5 bg-[#eff4fe] dark:bg-blue-950/60 hover:bg-blue-100 dark:hover:bg-blue-900/60 text-[#274eac] dark:text-blue-400 text-xs font-semibold rounded-lg transition active:scale-95 cursor-pointer"
          >
            <PlusCircle class="w-3.5 h-3.5" />
            <span>Создать заказ</span>
          </button>

          <div class="flex items-center gap-1">
            <button
              @click="openEditModal(r)"
              title="Редактировать"
              class="p-2 text-slate-400 hover:text-slate-700 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg transition cursor-pointer"
            >
              <Pencil class="w-4 h-4" />
            </button>
            <button
              @click="deleteRestaurant(r)"
              title="Удалить ресторан"
              class="p-2 text-slate-400 hover:text-rose-600 dark:hover:text-rose-400 hover:bg-rose-50 dark:hover:bg-rose-950/50 rounded-lg transition cursor-pointer"
            >
              <Trash2 class="w-4 h-4" />
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Пустое состояние -->
    <div
      v-else
      class="bg-white dark:bg-slate-900/60 border border-dashed border-slate-300 dark:border-slate-800 rounded-3xl p-12 text-center"
    >
      <Store class="w-12 h-12 text-slate-300 dark:text-slate-600 mx-auto mb-3" />
      <h3 class="text-base font-bold text-slate-700 dark:text-slate-200">Ничего не найдено</h3>
      <p class="text-sm text-slate-400 dark:text-slate-500 mt-1">Попробуйте изменить поисковый запрос или добавьте ресторан</p>
    </div>

    <!-- Модальное окно: Добавить / Редактировать ресторан с плавной анимацией -->
    <div
      v-if="showModal"
      class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 dark:bg-slate-950/60 backdrop-blur-xs transition-opacity duration-200"
      @click.self="closeModal"
    >
      <div class="bg-white dark:bg-slate-900 rounded-3xl max-w-md w-full p-6 sm:p-8 shadow-2xl border border-slate-100 dark:border-slate-800 space-y-5 animate-in fade-in zoom-in-95 duration-200">
        <div class="flex items-center justify-between">
          <h3 class="text-lg font-bold text-slate-900 dark:text-slate-100">
            {{ editingRestaurant ? 'Редактировать ресторан' : 'Новый ресторан' }}
          </h3>
          <button
            @click="closeModal"
            class="p-1 rounded-lg text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition cursor-pointer"
          >
            ✕
          </button>
        </div>

        <form @submit.prevent="saveRestaurant" class="space-y-4">
          <div>
            <label class="block text-xs font-semibold uppercase tracking-wider text-slate-600 dark:text-slate-400 mb-1.5">
              Название
            </label>
            <input
              v-model="modalName"
              type="text"
              required
              placeholder="Например, Грильница"
              class="w-full px-4 py-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 placeholder-slate-400 dark:placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-[#274eac]/25 focus:border-[#274eac] text-sm transition"
            />
          </div>

          <div>
            <label class="block text-xs font-semibold uppercase tracking-wider text-slate-600 dark:text-slate-400 mb-1.5">
              Ссылка на сайт / меню
            </label>
            <input
              v-model="modalUrl"
              type="url"
              required
              placeholder="https://example.com"
              class="w-full px-4 py-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 placeholder-slate-400 dark:placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-[#274eac]/25 focus:border-[#274eac] text-sm transition"
            />
          </div>

          <div class="flex items-center justify-end gap-3 pt-3 border-t border-slate-100 dark:border-slate-800">
            <button
              type="button"
              @click="closeModal"
              class="px-4 py-2 text-sm font-medium text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-xl transition cursor-pointer"
            >
              Отмена
            </button>
            <button
              type="submit"
              :disabled="saving"
              class="inline-flex items-center gap-2 px-5 py-2 text-sm font-semibold bg-[#274eac] hover:bg-[#1f3f8d] text-white rounded-xl shadow-xs transition-all active:scale-95 cursor-pointer disabled:opacity-50"
            >
              <Loader2 v-if="saving" class="w-4 h-4 animate-spin" />
              <span>Сохранить</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import api from '@/api/client';
import { useToastStore } from '@/stores/toast';
import { getFaviconUrl } from '@/utils/format';
import type { RestaurantResponse } from '@/types';
import {
  Store,
  Plus,
  Search,
  ExternalLink,
  PlusCircle,
  Pencil,
  Trash2,
  Loader2,
  AlertCircle,
} from 'lucide-vue-next';

const router = useRouter();
const toastStore = useToastStore();

const restaurants = ref<RestaurantResponse[]>([]);
const loading = ref(false);
const error = ref<string | null>(null);
const searchQuery = ref('');

// Модальное окно
const showModal = ref(false);
const editingRestaurant = ref<RestaurantResponse | null>(null);
const modalName = ref('');
const modalUrl = ref('');
const saving = ref(false);

const filteredRestaurants = computed(() => {
  if (!searchQuery.value.trim()) return restaurants.value;
  const q = searchQuery.value.toLowerCase().trim();
  return restaurants.value.filter((r) => r.name.toLowerCase().includes(q));
});

async function loadRestaurants() {
  loading.value = true;
  error.value = null;
  try {
    const resp = await api.get<RestaurantResponse[]>('/restaurants');
    restaurants.value = resp.data;
  } catch (err: any) {
    error.value = err.response?.data?.message || 'Не удалось загрузить список ресторанов';
  } finally {
    loading.value = false;
  }
}

function openAddModal() {
  editingRestaurant.value = null;
  modalName.value = '';
  modalUrl.value = '';
  showModal.value = true;
}

function openEditModal(r: RestaurantResponse) {
  editingRestaurant.value = r;
  modalName.value = r.name;
  modalUrl.value = r.websiteUrl || '';
  showModal.value = true;
}

function closeModal() {
  showModal.value = false;
}

async function saveRestaurant() {
  if (!modalName.value.trim() || !modalUrl.value.trim()) return;

  saving.value = true;
  try {
    if (editingRestaurant.value) {
      await api.put(`/restaurants/${editingRestaurant.value.id}`, {
        name: modalName.value.trim(),
        websiteUrl: modalUrl.value.trim(),
      });
      toastStore.success('Ресторан обновлён');
    } else {
      await api.post('/restaurants', {
        name: modalName.value.trim(),
        websiteUrl: modalUrl.value.trim(),
      });
      toastStore.success('Ресторан добавлен');
    }
    closeModal();
    await loadRestaurants();
  } catch (err: any) {
    toastStore.error(err.response?.data?.message || 'Ошибка сохранения ресторана');
  } finally {
    saving.value = false;
  }
}

async function deleteRestaurant(r: RestaurantResponse) {
  if (!confirm(`Удалить ресторан «${r.name}»?`)) return;

  try {
    await api.delete(`/restaurants/${r.id}`);
    toastStore.success('Ресторан удалён');
    await loadRestaurants();
  } catch (err: any) {
    toastStore.error(err.response?.data?.message || 'Не удалось удалить ресторан');
  }
}

function createOrderFrom(restaurantId: number) {
  router.push({ path: '/create-order', query: { restaurantId } });
}

onMounted(loadRestaurants);
</script>
