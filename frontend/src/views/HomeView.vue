<template>
  <div class="space-y-8 max-w-6xl mx-auto">
    <!-- Приветствие и личная активность -->
    <div class="bg-gradient-to-r from-[#274eac]/10 via-blue-500/5 to-transparent border border-[#274eac]/20 dark:border-[#274eac]/30 rounded-3xl p-6 sm:p-8 backdrop-blur-md">
      <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-6">
        <div>
          <h1 class="text-2xl sm:text-3xl font-extrabold text-slate-900 dark:text-slate-100 tracking-tight">
            Привет, {{ authStore.user?.fullName || 'коллега' }}! 👋
          </h1>
          <p class="text-slate-600 dark:text-slate-400 text-sm sm:text-base mt-1.5 max-w-xl">
            Организуйте совместные заказы с коллегами — быстро и удобно.
          </p>
        </div>

        <!-- Личная статистика за этот месяц с переходом в Рейтинг -->
        <router-link
          to="/ratings"
          class="flex items-center gap-4 bg-white/90 hover:bg-white dark:bg-slate-900/90 dark:hover:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-4 shadow-2xs hover:shadow-xs transition-all group self-start md:self-auto cursor-pointer"
          title="Посмотреть подробную статистику и рейтинг"
        >
          <div class="w-10 h-10 rounded-xl bg-[#eff4fe] dark:bg-blue-950/60 text-[#274eac] dark:text-blue-400 flex items-center justify-center shrink-0 group-hover:scale-105 transition-transform">
            <Coins class="w-5 h-5" />
          </div>
          <div>
            <div class="text-[11px] font-semibold text-slate-400 uppercase tracking-wider">В этом месяце</div>
            <div class="text-base font-extrabold text-slate-900 dark:text-slate-100 leading-tight">
              {{ formatPrice(userStats.totalSpentThisMonth) }}
              <span class="text-xs font-medium text-slate-500 dark:text-slate-400 ml-1">({{ userStats.totalOrdersThisMonth }} зак.)</span>
            </div>
          </div>
          <ArrowRight class="w-4 h-4 text-slate-400 group-hover:text-[#274eac] dark:group-hover:text-blue-400 group-hover:translate-x-0.5 transition-all ml-1" />
        </router-link>
      </div>
    </div>

    <!-- Баннер долгов / неоплаченных заказов -->
    <div
      v-if="unpaidOrders && unpaidOrders.totalDebt > 0"
      class="bg-rose-50 dark:bg-slate-900 border border-rose-200/80 dark:border-slate-800 dark:border-l-4 dark:border-l-rose-500 rounded-2xl p-5 sm:p-6 transition shadow-xs"
    >
      <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div class="flex items-start gap-3.5">
          <div class="w-10 h-10 rounded-xl bg-rose-100 dark:bg-rose-950/60 text-rose-600 dark:text-rose-400 flex items-center justify-center shrink-0 mt-0.5">
            <AlertCircle class="w-5 h-5" />
          </div>
          <div>
            <h2 class="text-base font-bold text-rose-900 dark:text-slate-100">
              У вас есть неоплаченные позиции: {{ formatPrice(unpaidOrders.totalDebt) }}
            </h2>
            <p class="text-sm text-rose-700 dark:text-slate-400 mt-0.5">
              Пожалуйста, переведите оплату создателям соответствующих заказов.
            </p>
          </div>
        </div>

        <button
          @click="showUnpaidDetails = !showUnpaidDetails"
          class="inline-flex items-center gap-1.5 text-xs font-semibold text-rose-700 dark:text-slate-200 hover:text-rose-900 dark:hover:text-white bg-rose-100/70 dark:bg-slate-800 hover:bg-rose-100 dark:hover:bg-slate-700 border border-transparent dark:border-slate-700 px-3.5 py-2 rounded-lg transition self-start sm:self-auto cursor-pointer"
        >
          <span>{{ showUnpaidDetails ? 'Скрыть список' : 'Показать детали долгов' }}</span>
          <ChevronDown
            class="w-4 h-4 transition-transform duration-200"
            :class="{ 'rotate-180': showUnpaidDetails }"
          />
        </button>
      </div>

      <!-- Детальный раскрывающийся список долгов с плавной анимацией -->
      <transition name="collapse">
        <div v-if="showUnpaidDetails" class="mt-4 pt-4 border-t border-rose-200/60 dark:border-slate-800 space-y-2.5">
          <div
            v-for="item in unpaidOrders.items"
            :key="item.id"
            class="flex flex-col sm:flex-row sm:items-center justify-between gap-2 bg-white/80 dark:bg-slate-800/80 p-3 rounded-xl border border-rose-100 dark:border-slate-700/60 text-sm"
          >
            <div class="flex items-center gap-2.5">
              <span class="w-2 h-2 rounded-full bg-rose-500"></span>
              <span class="font-medium text-slate-800 dark:text-slate-200">{{ item.itemDescription }}</span>
              <span class="text-xs text-slate-400">· Заказ #{{ item.orderId }}</span>
            </div>

            <div class="flex items-center justify-between sm:justify-end gap-4">
              <span class="font-bold text-slate-900 dark:text-slate-100">{{ formatPrice(item.totalCost || item.price) }}</span>
              <router-link
                :to="`/orders/${item.orderId}`"
                class="text-xs font-medium text-[#274eac] dark:text-blue-400 hover:text-[#1f3f8d] dark:hover:text-blue-300 hover:underline flex items-center gap-1"
              >
                <span>К заказу</span>
                <ArrowRight class="w-3.5 h-3.5" />
              </router-link>
            </div>
          </div>
        </div>
      </transition>
    </div>

    <!-- Секция активных заказов -->
    <section class="space-y-4">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-3">
          <h2 class="text-xl font-bold text-slate-900 dark:text-slate-100 tracking-tight">Активные заказы</h2>
          <span
            v-if="activeOrders.length"
            class="px-2.5 py-0.5 rounded-full text-xs font-bold bg-[#eff4fe] dark:bg-blue-950/60 text-[#274eac] dark:text-blue-400 border border-[#bfdbfe] dark:border-blue-900/60"
          >
            {{ activeOrders.length }}
          </span>
        </div>

        <button
          @click="loadData"
          title="Обновить"
          class="p-2 rounded-lg text-slate-400 hover:text-slate-700 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition cursor-pointer"
        >
          <RefreshCw class="w-4 h-4" :class="{ 'animate-spin': loading }" />
        </button>
      </div>

      <!-- Состояние загрузки -->
      <div v-if="loading && !activeOrders.length" class="flex justify-center items-center py-16">
        <Loader2 class="w-8 h-8 text-[#274eac] animate-spin" />
      </div>

      <!-- Пустое состояние -->
      <div
        v-else-if="!activeOrders.length"
        class="bg-white dark:bg-slate-900/60 border border-dashed border-slate-300 dark:border-slate-800 rounded-3xl p-12 text-center"
      >
        <div class="w-16 h-16 rounded-2xl bg-slate-100 dark:bg-slate-800 text-slate-400 flex items-center justify-center mx-auto mb-4">
          <UtensilsCrossed class="w-8 h-8" />
        </div>
        <h3 class="text-lg font-bold text-slate-800 dark:text-slate-200">Сейчас нет активных заказов</h3>
        <p class="text-sm text-slate-500 dark:text-slate-400 max-w-sm mx-auto mt-1 mb-6">
          Будьте первым! Создайте заказ, выберите любимый ресторан и позовите коллег.
        </p>
        <router-link
          to="/create-order"
          class="inline-flex items-center gap-2 bg-[#274eac] hover:bg-[#1f3f8d] text-white font-semibold px-5 py-2.5 rounded-xl shadow-xs transition-all active:scale-95 text-sm"
        >
          <PlusCircle class="w-4 h-4" />
          <span>Создать заказ</span>
        </router-link>
      </div>

      <!-- Сетка карточек активных заказов -->
      <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
        <router-link
          v-for="order in activeOrders"
          :key="order.id"
          :to="`/orders/${order.id}`"
          class="group bg-white hover:bg-slate-50/80 dark:bg-slate-900 dark:hover:bg-slate-800/80 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-5 shadow-xs hover:shadow-md hover:-translate-y-0.5 transition-all duration-200 flex flex-col justify-between"
        >
          <div class="space-y-3">
            <div class="flex items-start justify-between gap-3">
              <div class="flex items-center gap-2.5 min-w-0">
                <img
                  v-if="order.restaurant.websiteUrl"
                  :src="getFaviconUrl(order.restaurant.websiteUrl)"
                  :alt="order.restaurant.name"
                  class="w-5 h-5 rounded-md object-contain shrink-0"
                  @error="($event.target as HTMLElement).style.display = 'none'"
                />
                <h3 class="text-lg font-bold text-slate-900 dark:text-slate-100 group-hover:text-[#274eac] dark:group-hover:text-blue-400 transition line-clamp-1">
                  {{ order.restaurant.name }}
                </h3>
              </div>
              <span
                class="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold shrink-0"
                :class="
                  (order.acceptingOrders ?? order.isAcceptingOrders)
                    ? 'bg-emerald-50 dark:bg-emerald-950/40 text-emerald-700 dark:text-emerald-300 border border-emerald-200/80 dark:border-emerald-800/60'
                    : 'bg-amber-50 dark:bg-amber-950/40 text-amber-700 dark:text-amber-300 border border-amber-200/80 dark:border-amber-800/60'
                "
              >
                {{ (order.acceptingOrders ?? order.isAcceptingOrders) ? 'Приём открыт' : 'Приостановлен' }}
              </span>
            </div>

            <!-- Дедлайн и оставшееся время -->
            <div class="space-y-1.5 text-xs text-slate-500 dark:text-slate-400">
              <div class="flex items-center gap-2">
                <Clock class="w-4 h-4 text-slate-400" />
                <span>До {{ formatDateTime(order.deadlineTime) }}</span>
              </div>
              <div class="flex items-center gap-2 font-medium">
                <span
                  class="px-2 py-0.5 rounded-md"
                  :class="
                    isDeadlinePassed(order.deadlineTime)
                      ? 'bg-rose-50 dark:bg-rose-950/50 text-rose-600 dark:text-rose-400'
                      : 'bg-blue-50 dark:bg-blue-950/50 text-blue-700 dark:text-blue-300'
                  "
                >
                  {{ formatTimeRemaining(order.deadlineTime) }}
                </span>
              </div>
            </div>

            <!-- Создатель -->
            <div class="flex items-center gap-2 text-xs text-slate-500 dark:text-slate-400 pt-1">
              <UserAvatar
                :src="order.createdBy.avatarUrl"
                :name="order.createdBy.fullName"
                size-class="w-5 h-5"
              />
              <span>Создатель: <strong class="text-slate-700 dark:text-slate-200">{{ order.createdBy.fullName }}</strong></span>
            </div>
          </div>

          <!-- Итого и переход -->
          <div class="mt-5 pt-4 border-t border-slate-100 dark:border-slate-800 flex items-center justify-between">
            <div>
              <div class="text-xs text-slate-400">Сумма ({{ order.itemsCount }} поз.)</div>
              <div class="text-base font-bold text-slate-900 dark:text-slate-100">{{ formatPrice(order.totalAmount) }}</div>
            </div>
            <div class="w-8 h-8 rounded-lg bg-slate-100 dark:bg-slate-800 group-hover:bg-[#274eac] group-hover:text-white text-slate-500 dark:text-slate-400 flex items-center justify-center transition">
              <ArrowRight class="w-4 h-4" />
            </div>
          </div>
        </router-link>
      </div>
    </section>

    <!-- Секция завершённых заказов -->
    <section v-if="closedOrders && closedOrders.content.length" class="space-y-4 pt-4 border-t border-slate-200/80 dark:border-slate-800">
      <div class="flex items-center justify-between">
        <h2 class="text-xl font-bold text-slate-900 dark:text-slate-100 tracking-tight">Завершённые заказы</h2>
        <span class="text-xs text-slate-500 dark:text-slate-400">
          Всего: {{ closedOrders.totalElements }}
        </span>
      </div>

      <div class="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl shadow-xs overflow-hidden">
        <div class="overflow-x-auto">
          <table class="w-full text-left text-sm text-slate-600 dark:text-slate-300">
            <thead class="bg-slate-50 dark:bg-slate-800/60 text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400 border-b border-slate-200/60 dark:border-slate-800">
              <tr>
                <th class="px-5 py-3.5">Ресторан</th>
                <th class="px-5 py-3.5">Дедлайн</th>
                <th class="px-5 py-3.5">Организатор</th>
                <th class="px-5 py-3.5">Сумма</th>
                <th class="px-5 py-3.5 text-right">Действие</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-100 dark:divide-slate-800">
              <tr
                v-for="order in closedOrders.content"
                :key="order.id"
                class="hover:bg-slate-50/60 dark:hover:bg-slate-800/40 transition"
              >
                <td class="px-5 py-4 font-semibold text-slate-900 dark:text-slate-100">
                  <div class="flex items-center gap-2">
                    <img
                      v-if="order.restaurant.websiteUrl"
                      :src="getFaviconUrl(order.restaurant.websiteUrl)"
                      :alt="order.restaurant.name"
                      class="w-4 h-4 rounded object-contain shrink-0"
                      @error="($event.target as HTMLElement).style.display = 'none'"
                    />
                    <span>{{ order.restaurant.name }}</span>
                  </div>
                </td>
                <td class="px-5 py-4 text-xs text-slate-500 dark:text-slate-400">
                  {{ formatDateTime(order.deadlineTime) }}
                </td>
                <td class="px-5 py-4 text-xs text-slate-600 dark:text-slate-300">
                  <div class="flex items-center gap-2">
                    <UserAvatar
                      :src="order.createdBy.avatarUrl"
                      :name="order.createdBy.fullName"
                      size-class="w-5 h-5"
                    />
                    <span>{{ order.createdBy.fullName }}</span>
                  </div>
                </td>
                <td class="px-5 py-4 font-bold text-slate-900 dark:text-slate-100">
                  {{ formatPrice(order.totalAmount) }}
                </td>
                <td class="px-5 py-4 text-right">
                  <router-link
                    :to="`/orders/${order.id}`"
                    class="inline-flex items-center gap-1 text-xs font-semibold text-[#274eac] dark:text-blue-400 hover:text-[#1f3f8d] dark:hover:text-blue-300 bg-[#eff4fe] dark:bg-blue-950/60 hover:bg-blue-100 dark:hover:bg-blue-900/60 px-3 py-1.5 rounded-lg transition"
                  >
                    <span>Смотреть</span>
                    <ArrowRight class="w-3.5 h-3.5" />
                  </router-link>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Пагинация -->
        <div
          v-if="closedOrders.totalPages > 1"
          class="px-5 py-3.5 bg-slate-50/60 dark:bg-slate-800/60 border-t border-slate-200/60 dark:border-slate-800 flex items-center justify-between text-xs"
        >
          <button
            @click="prevPage"
            :disabled="closedPage === 0"
            class="px-3 py-1.5 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 font-medium text-slate-700 dark:text-slate-200 hover:bg-slate-50 dark:hover:bg-slate-700 disabled:opacity-40 transition cursor-pointer disabled:cursor-not-allowed"
          >
            ← Предыдущая
          </button>
          <span class="text-slate-500 dark:text-slate-400 font-medium">
            Страница {{ closedPage + 1 }} из {{ closedOrders.totalPages }}
          </span>
          <button
            @click="nextPage"
            :disabled="closedPage + 1 >= closedOrders.totalPages"
            class="px-3 py-1.5 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 font-medium text-slate-700 dark:text-slate-200 hover:bg-slate-50 dark:hover:bg-slate-700 disabled:opacity-40 transition cursor-pointer disabled:cursor-not-allowed"
          >
            Следующая →
          </button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useOrderStore } from '@/stores/orders';
import { useAuthStore } from '@/stores/auth';
import { storeToRefs } from 'pinia';
import UserAvatar from '@/components/UserAvatar.vue';
import {
  formatPrice,
  formatDateTime,
  formatTimeRemaining,
  isDeadlinePassed,
  getFaviconUrl,
} from '@/utils/format';
import type { UserStatisticResponse } from '@/types';
import {
  UtensilsCrossed,
  PlusCircle,
  Clock,
  User as UserIcon,
  ArrowRight,
  AlertCircle,
  Coins,
  RefreshCw,
  Loader2,
  ChevronDown,
} from 'lucide-vue-next';

const orderStore = useOrderStore();
const authStore = useAuthStore();

const { activeOrders, closedOrders, unpaidOrders, loading } = storeToRefs(orderStore);

const closedPage = ref(0);
const showUnpaidDetails = ref(false);

const userStats = ref<UserStatisticResponse>({
  totalSpentThisMonth: 0,
  totalOrdersThisMonth: 0,
  totalSpentAllTime: 0,
  totalOrdersAllTime: 0,
});

async function loadData() {
  await Promise.all([
    orderStore.fetchActiveOrders(),
    orderStore.fetchClosedOrders(closedPage.value),
    orderStore.fetchUnpaidOrders(),
    loadUserStats(),
  ]);
}

async function loadUserStats() {
  try {
    const stats = await orderStore.fetchUserStatistics();
    userStats.value = stats;
  } catch (e) {
    console.error('Failed to load user stats', e);
  }
}

function nextPage() {
  if (closedOrders.value && closedPage.value + 1 < closedOrders.value.totalPages) {
    closedPage.value++;
    orderStore.fetchClosedOrders(closedPage.value);
  }
}

function prevPage() {
  if (closedPage.value > 0) {
    closedPage.value--;
    orderStore.fetchClosedOrders(closedPage.value);
  }
}

onMounted(() => {
  loadData();
});
</script>
