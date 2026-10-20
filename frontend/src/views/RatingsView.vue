<template>
  <div class="space-y-8 max-w-6xl mx-auto">
    <!-- Шапка страницы -->
    <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
      <div>
        <div class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-[#eff4fe] dark:bg-blue-950/60 text-[#274eac] dark:text-blue-400 mb-2 border border-[#bfdbfe] dark:border-blue-900/60">
          <Award class="w-3.5 h-3.5 text-[#274eac] dark:text-blue-400" />
          Рейтинги и статистика
        </div>
        <h1 class="text-2xl sm:text-3xl font-extrabold text-slate-900 dark:text-slate-100 tracking-tight">
          Статистика заказов
        </h1>
        <p class="text-slate-600 dark:text-slate-400 text-sm mt-1">
          Следите за личными расходами и активностью в корпоративных заказах.
        </p>
      </div>

      <button
        @click="loadData"
        title="Обновить"
        class="inline-flex items-center gap-2 self-start sm:self-auto px-4 py-2 bg-white dark:bg-slate-800 hover:bg-slate-50 dark:hover:bg-slate-700 border border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-200 text-sm font-medium rounded-xl shadow-xs transition active:scale-95 cursor-pointer"
      >
        <RefreshCw class="w-4 h-4" :class="{ 'animate-spin': loading }" />
        <span>Обновить данные</span>
      </button>
    </div>

    <!-- Загрузка -->
    <div v-if="loading && !userStats.totalOrdersAllTime" class="flex justify-center items-center py-20">
      <Loader2 class="w-8 h-8 text-[#274eac] animate-spin" />
    </div>

    <!-- Ошибка -->
    <div v-else-if="error" class="p-4 rounded-2xl bg-rose-50 dark:bg-rose-950/40 border border-rose-200 dark:border-rose-900/60 text-rose-700 dark:text-rose-300 text-sm flex items-center gap-3">
      <AlertCircle class="w-5 h-5 shrink-0 text-rose-500" />
      <span>{{ error }}</span>
    </div>

    <div v-else class="space-y-8">
      <!-- Личная статистика: 4 карточки -->
      <section class="space-y-3">
        <h2 class="text-lg font-bold text-slate-900 dark:text-slate-100 tracking-tight flex items-center gap-2">
          <UserIcon class="w-5 h-5 text-[#274eac] dark:text-blue-400" />
          <span>Моя статистика</span>
        </h2>

        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <!-- Потрачено за месяц -->
          <div class="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-5 shadow-xs flex items-center gap-4">
            <div class="w-12 h-12 rounded-xl bg-[#eff4fe] dark:bg-blue-950/60 text-[#274eac] dark:text-blue-400 flex items-center justify-center shrink-0">
              <Coins class="w-6 h-6" />
            </div>
            <div>
              <div class="text-xs text-slate-500 dark:text-slate-400 font-medium">Потрачено в этом месяце</div>
              <div class="text-xl font-black text-slate-900 dark:text-slate-100 mt-0.5">
                {{ formatPrice(userStats.totalSpentThisMonth) }}
              </div>
            </div>
          </div>

          <!-- Заказов за месяц -->
          <div class="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-5 shadow-xs flex items-center gap-4">
            <div class="w-12 h-12 rounded-xl bg-blue-50 dark:bg-blue-900/40 text-blue-600 dark:text-blue-300 flex items-center justify-center shrink-0">
              <ShoppingBag class="w-6 h-6" />
            </div>
            <div>
              <div class="text-xs text-slate-500 dark:text-slate-400 font-medium">Заказов в этом месяце</div>
              <div class="text-xl font-black text-slate-900 dark:text-slate-100 mt-0.5">
                {{ userStats.totalOrdersThisMonth }}
              </div>
            </div>
          </div>

          <!-- Потрачено за всё время -->
          <div class="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-5 shadow-xs flex items-center gap-4">
            <div class="w-12 h-12 rounded-xl bg-emerald-50 dark:bg-emerald-950/40 text-emerald-600 dark:text-emerald-300 flex items-center justify-center shrink-0">
              <TrendingUp class="w-6 h-6" />
            </div>
            <div>
              <div class="text-xs text-slate-500 dark:text-slate-400 font-medium">Потрачено за всё время</div>
              <div class="text-xl font-black text-slate-900 dark:text-slate-100 mt-0.5">
                {{ formatPrice(userStats.totalSpentAllTime) }}
              </div>
            </div>
          </div>

          <!-- Всего заказов -->
          <div class="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-5 shadow-xs flex items-center gap-4">
            <div class="w-12 h-12 rounded-xl bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 flex items-center justify-center shrink-0">
              <Award class="w-6 h-6" />
            </div>
            <div>
              <div class="text-xs text-slate-500 dark:text-slate-400 font-medium">Всего заказов</div>
              <div class="text-xl font-black text-slate-900 dark:text-slate-100 mt-0.5">
                {{ userStats.totalOrdersAllTime }}
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- Лидерборды -->
      <section class="space-y-4">
        <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <h2 class="text-lg font-bold text-slate-900 dark:text-slate-100 tracking-tight flex items-center gap-2">
            <Trophy class="w-5 h-5 text-amber-500" />
            <span>Таблица лидеров</span>
          </h2>

          <!-- Переключатель периода -->
          <div class="inline-flex p-1 bg-slate-200/70 dark:bg-slate-800 rounded-xl self-start sm:self-auto text-xs font-semibold">
            <button
              @click="activePeriod = 'month'"
              class="px-3.5 py-1.5 rounded-lg transition cursor-pointer"
              :class="activePeriod === 'month' ? 'bg-white dark:bg-slate-700 text-slate-900 dark:text-slate-100 shadow-xs' : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-200'"
            >
              За этот месяц
            </button>
            <button
              @click="activePeriod = 'allTime'"
              class="px-3.5 py-1.5 rounded-lg transition cursor-pointer"
              :class="activePeriod === 'allTime' ? 'bg-white dark:bg-slate-700 text-slate-900 dark:text-slate-100 shadow-xs' : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-200'"
            >
              За всё время
            </button>
          </div>
        </div>

        <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <!-- Топ по сумме -->
          <div class="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl shadow-xs overflow-hidden">
            <div class="px-5 py-4 border-b border-slate-100 dark:border-slate-800 bg-slate-50/70 dark:bg-slate-800/60 flex items-center justify-between">
              <div class="flex items-center gap-2">
                <Coins class="w-4 h-4 text-[#274eac] dark:text-blue-400" />
                <h3 class="text-sm font-bold text-slate-900 dark:text-slate-100">
                  {{ activePeriod === 'month' ? 'Топ по сумме за месяц' : 'Топ по сумме за всё время' }}
                </h3>
              </div>
            </div>

            <div class="p-0">
              <table class="w-full text-left text-sm text-slate-600 dark:text-slate-300">
                <thead class="text-xs text-slate-400 dark:text-slate-500 uppercase tracking-wider bg-slate-50/30 dark:bg-slate-800/40 border-b border-slate-100 dark:border-slate-800">
                  <tr>
                    <th class="px-5 py-3 w-12 text-center">#</th>
                    <th class="px-5 py-3">Участник</th>
                    <th class="px-5 py-3 text-right">Сумма</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-slate-100 dark:divide-slate-800">
                  <tr
                    v-for="(item, idx) in currentTopSpent"
                    :key="item.userId"
                    class="transition"
                    :class="
                      item.userId === authStore.user?.id
                        ? 'bg-[#eff4fe] dark:bg-blue-950/40 font-semibold'
                        : 'hover:bg-slate-50/50 dark:hover:bg-slate-800/40'
                    "
                  >
                    <td class="px-5 py-3.5 text-center text-sm font-medium">
                      <span v-if="idx === 0">🥇</span>
                      <span v-else-if="idx === 1">🥈</span>
                      <span v-else-if="idx === 2">🥉</span>
                      <span v-else class="text-slate-400 dark:text-slate-500">{{ idx + 1 }}</span>
                    </td>
                    <td class="px-5 py-3.5">
                      <div class="flex items-center gap-2.5">
                        <UserAvatar
                          :src="item.avatarUrl"
                          :name="item.fullName"
                          size-class="w-6 h-6"
                        />
                        <span class="text-slate-900 dark:text-slate-100">{{ item.fullName }}</span>
                        <span
                          v-if="item.userId === authStore.user?.id"
                          class="px-1.5 py-0.5 rounded text-[10px] bg-[#dbeafe] dark:bg-blue-900/60 text-[#183375] dark:text-blue-200 font-bold"
                        >
                          Вы
                        </span>
                      </div>
                    </td>
                    <td class="px-5 py-3.5 text-right font-bold text-slate-900 dark:text-slate-100">
                      {{ formatPrice(item.amount) }}
                    </td>
                  </tr>
                  <tr v-if="!currentTopSpent.length">
                    <td colspan="3" class="px-5 py-8 text-center text-slate-400 dark:text-slate-500 text-xs">
                      Нет данных за выбранный период
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <!-- Топ по количеству заказов -->
          <div class="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl shadow-xs overflow-hidden">
            <div class="px-5 py-4 border-b border-slate-100 dark:border-slate-800 bg-slate-50/70 dark:bg-slate-800/60 flex items-center justify-between">
              <div class="flex items-center gap-2">
                <ShoppingBag class="w-4 h-4 text-blue-600 dark:text-blue-400" />
                <h3 class="text-sm font-bold text-slate-900 dark:text-slate-100">
                  {{ activePeriod === 'month' ? 'Топ по заказам за месяц' : 'Топ по заказам за всё время' }}
                </h3>
              </div>
            </div>

            <div class="p-0">
              <table class="w-full text-left text-sm text-slate-600 dark:text-slate-300">
                <thead class="text-xs text-slate-400 dark:text-slate-500 uppercase tracking-wider bg-slate-50/30 dark:bg-slate-800/40 border-b border-slate-100 dark:border-slate-800">
                  <tr>
                    <th class="px-5 py-3 w-12 text-center">#</th>
                    <th class="px-5 py-3">Участник</th>
                    <th class="px-5 py-3 text-right">Заказов</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-slate-100 dark:divide-slate-800">
                  <tr
                    v-for="(item, idx) in currentTopOrders"
                    :key="item.userId"
                    class="transition"
                    :class="
                      item.userId === authStore.user?.id
                        ? 'bg-[#eff4fe] dark:bg-blue-950/40 font-semibold'
                        : 'hover:bg-slate-50/50 dark:hover:bg-slate-800/40'
                    "
                  >
                    <td class="px-5 py-3.5 text-center text-sm font-medium">
                      <span v-if="idx === 0">🥇</span>
                      <span v-else-if="idx === 1">🥈</span>
                      <span v-else-if="idx === 2">🥉</span>
                      <span v-else class="text-slate-400 dark:text-slate-500">{{ idx + 1 }}</span>
                    </td>
                    <td class="px-5 py-3.5">
                      <div class="flex items-center gap-2.5">
                        <UserAvatar
                          :src="item.avatarUrl"
                          :name="item.fullName"
                          size-class="w-6 h-6"
                        />
                        <span class="text-slate-900 dark:text-slate-100">{{ item.fullName }}</span>
                        <span
                          v-if="item.userId === authStore.user?.id"
                          class="px-1.5 py-0.5 rounded text-[10px] bg-[#dbeafe] dark:bg-blue-900/60 text-[#183375] dark:text-blue-200 font-bold"
                        >
                          Вы
                        </span>
                      </div>
                    </td>
                    <td class="px-5 py-3.5 text-right font-bold text-slate-900 dark:text-slate-100">
                      {{ item.count ?? 0 }}
                    </td>
                  </tr>
                  <tr v-if="!currentTopOrders.length">
                    <td colspan="3" class="px-5 py-8 text-center text-slate-400 dark:text-slate-500 text-xs">
                      Нет данных за выбранный период
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { useOrderStore } from '@/stores/orders';
import { useAuthStore } from '@/stores/auth';
import { formatPrice } from '@/utils/format';
import type { RatingLeaderboardResponse, UserStatisticResponse } from '@/types';
import UserAvatar from '@/components/UserAvatar.vue';
import {
  Award,
  RefreshCw,
  Loader2,
  AlertCircle,
  User as UserIcon,
  Coins,
  ShoppingBag,
  TrendingUp,
  Trophy,
} from 'lucide-vue-next';

const orderStore = useOrderStore();
const authStore = useAuthStore();

const loading = ref(false);
const error = ref<string | null>(null);
const activePeriod = ref<'month' | 'allTime'>('month');

const leaderboard = ref<RatingLeaderboardResponse>({
  topSpentThisMonth: [],
  topOrdersThisMonth: [],
  topSpentAllTime: [],
  topOrdersAllTime: [],
});

const userStats = ref<UserStatisticResponse>({
  totalSpentThisMonth: 0,
  totalOrdersThisMonth: 0,
  totalSpentAllTime: 0,
  totalOrdersAllTime: 0,
});

const currentTopSpent = computed(() => {
  return activePeriod.value === 'month'
    ? leaderboard.value.topSpentThisMonth
    : leaderboard.value.topSpentAllTime;
});

const currentTopOrders = computed(() => {
  return activePeriod.value === 'month'
    ? leaderboard.value.topOrdersThisMonth
    : leaderboard.value.topOrdersAllTime;
});

async function loadData() {
  loading.value = true;
  error.value = null;
  try {
    const [lb, us] = await Promise.all([
      orderStore.fetchRatingsLeaderboard(),
      orderStore.fetchUserStatistics(),
    ]);
    leaderboard.value = lb;
    userStats.value = us;
  } catch (e: any) {
    error.value = e.response?.data?.message || 'Не удалось загрузить данные рейтингов';
  } finally {
    loading.value = false;
  }
}

onMounted(loadData);
</script>
