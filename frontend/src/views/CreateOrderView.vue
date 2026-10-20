<template>
  <div class="max-w-2xl mx-auto space-y-6">
    <!-- Шапка -->
    <div class="flex items-center gap-3">
      <router-link
        to="/"
        class="w-10 h-10 rounded-xl bg-white dark:bg-slate-800 hover:bg-slate-50 dark:hover:bg-slate-700 border border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-300 flex items-center justify-center transition shadow-xs"
        title="Назад к заказам"
      >
        <ArrowLeft class="w-5 h-5" />
      </router-link>
      <div>
        <h1 class="text-2xl font-extrabold text-slate-900 dark:text-slate-100 tracking-tight">Новый заказ</h1>
        <p class="text-slate-500 dark:text-slate-400 text-sm">Создайте заказ, чтобы коллеги могли добавить свои блюда</p>
      </div>
    </div>

    <!-- Форма создания -->
    <div class="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-3xl p-6 sm:p-8 shadow-xs">
      <form @submit.prevent="onSubmit" class="space-y-6">
        <!-- Блок выбора ресторана -->
        <div class="space-y-3">
          <label class="block text-xs font-semibold uppercase tracking-wider text-slate-600 dark:text-slate-400">
            Ресторан
          </label>

          <!-- Переключатель режима: существующий / новый -->
          <div class="inline-flex p-1 bg-slate-100 dark:bg-slate-800 rounded-xl w-full text-xs font-semibold">
            <button
              type="button"
              @click="useNewRestaurant = false"
              class="flex-1 py-2 rounded-lg transition cursor-pointer text-center"
              :class="!useNewRestaurant ? 'bg-white dark:bg-slate-700 text-slate-900 dark:text-slate-100 shadow-xs' : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-200'"
            >
              Выбрать из списка
            </button>
            <button
              type="button"
              @click="useNewRestaurant = true"
              class="flex-1 py-2 rounded-lg transition cursor-pointer text-center"
              :class="useNewRestaurant ? 'bg-white dark:bg-slate-700 text-slate-900 dark:text-slate-100 shadow-xs' : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-200'"
            >
              Добавить новый ресторан
            </button>
          </div>

          <!-- Выбор из списка -->
          <div v-if="!useNewRestaurant" class="space-y-2">
            <select
              v-model.number="restaurantId"
              required
              class="w-full px-4 py-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-[#274eac]/20 focus:border-[#274eac] transition text-sm cursor-pointer"
            >
              <option :value="null" disabled>-- Выберите ресторан из списка --</option>
              <option v-for="r in restaurants" :key="r.id" :value="r.id">
                {{ r.name }}
              </option>
            </select>

            <!-- Ссылка на сайт выбранного ресторана с фавиконкой -->
            <div v-if="selectedRestaurant?.websiteUrl" class="text-xs text-slate-500 dark:text-slate-400 flex items-center gap-2 pt-1">
              <img
                :src="getFaviconUrl(selectedRestaurant.websiteUrl)"
                alt=""
                class="w-4 h-4 rounded object-contain shrink-0"
                @error="($event.target as HTMLElement).style.display = 'none'"
              />
              <span>Сайт ресторана:</span>
              <a
                :href="selectedRestaurant.websiteUrl"
                target="_blank"
                rel="noopener noreferrer"
                class="text-[#274eac] dark:text-blue-400 hover:underline inline-flex items-center gap-1 font-medium truncate"
              >
                {{ selectedRestaurant.websiteUrl }}
                <ExternalLink class="w-3 h-3" />
              </a>
            </div>
          </div>

          <!-- Поля нового ресторана -->
          <div v-else class="space-y-3 p-4 bg-[#eff4fe] dark:bg-blue-950/40 border border-[#bfdbfe] dark:border-blue-900/60 rounded-2xl">
            <div>
              <label class="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">Название ресторана</label>
              <input
                v-model="newRestaurantName"
                type="text"
                placeholder="например, Додо Пицца"
                required
                class="w-full px-3.5 py-2 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 placeholder-slate-400 dark:placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-[#274eac]/20 focus:border-[#274eac] transition text-sm"
              />
            </div>
            <div>
              <label class="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">Ссылка на сайт или меню</label>
              <input
                v-model="newRestaurantUrl"
                type="url"
                placeholder="https://dodopizza.ru"
                required
                class="w-full px-3.5 py-2 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 placeholder-slate-400 dark:placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-[#274eac]/20 focus:border-[#274eac] transition text-sm"
              />
            </div>
          </div>
        </div>

        <!-- Дедлайн приема заказов -->
        <div class="space-y-2.5">
          <label class="block text-xs font-semibold uppercase tracking-wider text-slate-600 dark:text-slate-400">
            Дедлайн приёма заказов
          </label>
          <input
            v-model="deadlineTime"
            type="datetime-local"
            :min="minDateTime"
            required
            class="w-full px-4 py-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-[#274eac]/20 focus:border-[#274eac] transition text-sm"
          />

          <!-- Быстрые кнопки -->
          <div class="flex flex-wrap items-center gap-2 pt-1">
            <span class="text-xs text-slate-400 dark:text-slate-500">Быстро:</span>
            <button
              type="button"
              @click="addMinutes(30)"
              class="px-2.5 py-1 text-xs font-medium bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-300 rounded-lg transition cursor-pointer"
            >
              +30 мин
            </button>
            <button
              type="button"
              @click="addMinutes(60)"
              class="px-2.5 py-1 text-xs font-medium bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-300 rounded-lg transition cursor-pointer"
            >
              +1 час
            </button>
            <button
              type="button"
              @click="setTodayTime('12:00')"
              class="px-2.5 py-1 text-xs font-medium bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-300 rounded-lg transition cursor-pointer"
            >
              Сегодня 12:00
            </button>
            <button
              type="button"
              @click="setTodayTime('13:00')"
              class="px-2.5 py-1 text-xs font-medium bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-300 rounded-lg transition cursor-pointer"
            >
              Сегодня 13:00
            </button>
            <button
              type="button"
              @click="setTomorrowTime('12:00')"
              class="px-2.5 py-1 text-xs font-medium bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-300 rounded-lg transition cursor-pointer"
            >
              Завтра 12:00
            </button>
          </div>
        </div>

        <!-- Реквизиты для оплаты -->
        <div class="space-y-1.5">
          <label class="block text-xs font-semibold uppercase tracking-wider text-slate-600 dark:text-slate-400">
            Реквизиты для перевода
          </label>
          <div class="relative">
            <CreditCard class="w-4 h-4 text-slate-400 absolute left-3.5 top-3" />
            <input
              v-model="paymentData"
              type="text"
              placeholder="Т-Банк / Сбер: +7 (999) 000-00-00 или 2200..."
              class="w-full pl-10 pr-4 py-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 placeholder-slate-400 dark:placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-[#274eac]/20 focus:border-[#274eac] transition text-sm"
            />
          </div>
          <p class="text-xs text-slate-400 dark:text-slate-500">
            Участники увидят этот номер с возможностью скопировать его в один клик.
          </p>
        </div>

        <!-- Комментарий -->
        <div class="space-y-1.5">
          <label class="block text-xs font-semibold uppercase tracking-wider text-slate-600 dark:text-slate-400">
            Комментарий к заказу (необязательно)
          </label>
          <textarea
            v-model="comment"
            rows="3"
            placeholder="Например: доставка ~40 минут, делим доставку поровну на всех"
            class="w-full px-4 py-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 placeholder-slate-400 dark:placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-[#274eac]/20 focus:border-[#274eac] transition text-sm resize-none"
          ></textarea>
        </div>

        <!-- Ошибка -->
        <div v-if="error" class="p-4 rounded-xl bg-rose-50 dark:bg-rose-950/40 border border-rose-200 dark:border-rose-900/60 text-rose-700 dark:text-rose-300 text-sm flex items-center gap-2">
          <AlertCircle class="w-4 h-4 shrink-0 text-rose-500" />
          <span>{{ error }}</span>
        </div>

        <!-- Кнопки отправки -->
        <div class="pt-4 flex items-center justify-end gap-3 border-t border-slate-100 dark:border-slate-800">
          <button
            type="button"
            @click="resetForm"
            :disabled="submitting"
            class="px-5 py-2.5 rounded-xl border border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-300 hover:bg-slate-50 dark:hover:bg-slate-800 font-medium text-sm transition cursor-pointer disabled:opacity-50"
          >
            Сбросить
          </button>
          <button
            type="submit"
            :disabled="submitting"
            class="inline-flex items-center gap-2 px-6 py-2.5 rounded-xl bg-[#274eac] hover:bg-[#1f3f8d] text-white font-medium text-sm shadow-sm transition active:scale-95 disabled:opacity-60 cursor-pointer"
          >
            <Loader2 v-if="submitting" class="w-4 h-4 animate-spin" />
            <PlusCircle v-else class="w-4 h-4" />
            <span>{{ submitting ? 'Создание заказа...' : 'Создать заказ' }}</span>
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useOrderStore } from '@/stores/orders';
import { useToastStore } from '@/stores/toast';
import api from '@/api/client';
import { getFaviconUrl } from '@/utils/format';
import type { RestaurantResponse, CreateOrderRequest } from '@/types';
import {
  ArrowLeft,
  PlusCircle,
  CreditCard,
  ExternalLink,
  AlertCircle,
  Loader2,
} from 'lucide-vue-next';

const router = useRouter();
const route = useRoute();
const orderStore = useOrderStore();
const toastStore = useToastStore();

const useNewRestaurant = ref(false);
const restaurantId = ref<number | null>(null);
const newRestaurantName = ref('');
const newRestaurantUrl = ref('');
const deadlineTime = ref('');
const paymentData = ref('');
const comment = ref('');
const submitting = ref(false);
const error = ref<string | null>(null);

const restaurants = ref<RestaurantResponse[]>([]);

const selectedRestaurant = computed(() => {
  return restaurants.value.find((r) => r.id === restaurantId.value);
});

const minDateTime = computed(() => {
  return formatLocalDateTime(new Date());
});

async function loadRestaurants() {
  try {
    const resp = await api.get<RestaurantResponse[]>('/restaurants');
    restaurants.value = resp.data;

    // Проверяем параметр из URL query ?restaurantId=
    const qRestaurantId = Number(route.query.restaurantId);
    if (qRestaurantId && restaurants.value.some((r) => r.id === qRestaurantId)) {
      restaurantId.value = qRestaurantId;
    } else if (restaurants.value.length && !restaurantId.value) {
      restaurantId.value = restaurants.value[0].id;
    }
  } catch (e: any) {
    error.value = e.response?.data?.message || 'Не удалось загрузить рестораны';
  }
}

function addMinutes(mins: number) {
  const now = new Date();
  now.setMinutes(now.getMinutes() + mins);
  deadlineTime.value = formatLocalDateTime(now);
}

function setTodayTime(time: string) {
  const [hours, mins] = time.split(':');
  const d = new Date();
  d.setHours(Number(hours), Number(mins), 0, 0);
  deadlineTime.value = formatLocalDateTime(d);
}

function setTomorrowTime(time: string) {
  const [hours, mins] = time.split(':');
  const d = new Date();
  d.setDate(d.getDate() + 1);
  d.setHours(Number(hours), Number(mins), 0, 0);
  deadlineTime.value = formatLocalDateTime(d);
}

function formatLocalDateTime(d: Date): string {
  const pad = (n: number) => (n < 10 ? '0' + n : n);
  const year = d.getFullYear();
  const month = pad(d.getMonth() + 1);
  const day = pad(d.getDate());
  const hours = pad(d.getHours());
  const minutes = pad(d.getMinutes());
  return `${year}-${month}-${day}T${hours}:${minutes}`;
}

function resetForm() {
  useNewRestaurant.value = false;
  restaurantId.value = restaurants.value.length ? restaurants.value[0].id : null;
  newRestaurantName.value = '';
  newRestaurantUrl.value = '';
  deadlineTime.value = '';
  paymentData.value = '';
  comment.value = '';
  error.value = null;
}

async function onSubmit() {
  if (submitting.value) return;

  if (!deadlineTime.value) {
    error.value = 'Пожалуйста, укажите дедлайн заказа';
    return;
  }

  const deadlineDate = new Date(deadlineTime.value);
  if (deadlineDate.getTime() <= Date.now()) {
    error.value = 'Дедлайн должен быть в будущем';
    return;
  }

  submitting.value = true;
  error.value = null;

  try {
    let targetRestaurantId = restaurantId.value;

    // Если создается новый ресторан, сохраняем его
    if (useNewRestaurant.value) {
      if (!newRestaurantName.value.trim() || !newRestaurantUrl.value.trim()) {
        error.value = 'Укажите название и сайт нового ресторана';
        submitting.value = false;
        return;
      }

      const res = await api.post<RestaurantResponse>('/restaurants', {
        name: newRestaurantName.value.trim(),
        websiteUrl: newRestaurantUrl.value.trim(),
      });
      targetRestaurantId = res.data.id;
    }

    if (!targetRestaurantId) {
      error.value = 'Выберите ресторан для заказа';
      submitting.value = false;
      return;
    }

    const formattedDeadline =
      deadlineTime.value.length === 16 ? `${deadlineTime.value}:00` : deadlineTime.value;

    const payload: CreateOrderRequest = {
      restaurantId: targetRestaurantId,
      deadlineTime: formattedDeadline,
      paymentData: paymentData.value.trim() || undefined,
      comment: comment.value.trim() || undefined,
    };

    const newOrder = await orderStore.createOrder(payload);
    toastStore.success('Заказ успешно создан!');
    router.push(`/orders/${newOrder.id}`);
  } catch (err: any) {
    error.value = err.response?.data?.message || 'Не удалось создать заказ';
  } finally {
    submitting.value = false;
  }
}

onMounted(() => {
  loadRestaurants();
  // Дефолтный дедлайн: через 45 минут
  addMinutes(45);
});
</script>
