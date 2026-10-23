<template>
  <div class="space-y-6 max-w-6xl mx-auto">
    <!-- Шапка страницы -->
    <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
      <div class="space-y-1">
        <router-link
          to="/"
          class="inline-flex items-center gap-1.5 text-xs font-semibold text-slate-500 hover:text-[#274eac] dark:text-slate-400 dark:hover:text-blue-400 transition mb-1"
        >
          <ArrowLeft class="w-3.5 h-3.5" />
          <span>Назад на главную</span>
        </router-link>
        <h1 class="text-2xl sm:text-3xl font-extrabold text-slate-900 dark:text-slate-100 tracking-tight flex items-center gap-2.5">
          <Archive class="w-7 h-7 text-[#274eac] dark:text-blue-400" />
          <span>История заказов</span>
        </h1>
        <p class="text-xs sm:text-sm text-slate-500 dark:text-slate-400">
          Архив всех завершённых совместных заказов
        </p>
      </div>

      <div class="flex items-center gap-2">
        <button
          @click="loadData"
          title="Обновить список"
          class="p-2.5 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 text-slate-500 dark:text-slate-400 hover:text-slate-800 dark:hover:text-slate-200 hover:bg-slate-50 dark:hover:bg-slate-800 transition cursor-pointer shadow-2xs"
        >
          <RefreshCw class="w-4 h-4" :class="{ 'animate-spin': loading }" />
        </button>
      </div>
    </div>

    <!-- Загрузка -->
    <div v-if="loading && !closedOrders" class="flex justify-center items-center py-20">
      <Loader2 class="w-8 h-8 text-[#274eac] animate-spin" />
    </div>

    <!-- Пустое состояние -->
    <div
      v-else-if="!closedOrders || !closedOrders.content.length"
      class="bg-white dark:bg-slate-900 border border-dashed border-slate-300 dark:border-slate-800 rounded-3xl p-12 text-center"
    >
      <div class="w-16 h-16 rounded-2xl bg-slate-100 dark:bg-slate-800 text-slate-400 flex items-center justify-center mx-auto mb-4">
        <Archive class="w-8 h-8" />
      </div>
      <h3 class="text-lg font-bold text-slate-800 dark:text-slate-200">В архиве пока нет завершённых заказов</h3>
      <p class="text-sm text-slate-500 dark:text-slate-400 max-w-sm mx-auto mt-1 mb-6">
        Когда один из активных заказов будет закрыт создателем, он появится здесь.
      </p>
      <router-link
        to="/"
        class="inline-flex items-center gap-2 bg-[#274eac] hover:bg-[#1f3f8d] text-white font-semibold px-5 py-2.5 rounded-xl shadow-xs transition-all active:scale-95 text-sm"
      >
        <span>К активным заказам</span>
      </router-link>
    </div>

    <!-- Основная таблица -->
    <div v-else class="space-y-4">
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
                  <div class="flex items-center gap-2.5">
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
            class="px-3.5 py-1.5 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 font-medium text-slate-700 dark:text-slate-200 hover:bg-slate-50 dark:hover:bg-slate-700 disabled:opacity-40 transition cursor-pointer disabled:cursor-not-allowed"
          >
            ← Предыдущая
          </button>
          <span class="text-slate-500 dark:text-slate-400 font-medium">
            Страница {{ closedPage + 1 }} из {{ closedOrders.totalPages }} (Всего: {{ closedOrders.totalElements }})
          </span>
          <button
            @click="nextPage"
            :disabled="closedPage + 1 >= closedOrders.totalPages"
            class="px-3.5 py-1.5 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 font-medium text-slate-700 dark:text-slate-200 hover:bg-slate-50 dark:hover:bg-slate-700 disabled:opacity-40 transition cursor-pointer disabled:cursor-not-allowed"
          >
            Следующая →
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useOrderStore } from '@/stores/orders';
import { storeToRefs } from 'pinia';
import UserAvatar from '@/components/UserAvatar.vue';
import { formatPrice, formatDateTime, getFaviconUrl } from '@/utils/format';
import {
  Archive,
  ArrowLeft,
  ArrowRight,
  RefreshCw,
  Loader2,
} from 'lucide-vue-next';

const orderStore = useOrderStore();
const { closedOrders, loading } = storeToRefs(orderStore);

const closedPage = ref(0);

async function loadData() {
  await orderStore.fetchClosedOrders(closedPage.value, 15);
}

function nextPage() {
  if (closedOrders.value && closedPage.value + 1 < closedOrders.value.totalPages) {
    closedPage.value++;
    loadData();
  }
}

function prevPage() {
  if (closedPage.value > 0) {
    closedPage.value--;
    loadData();
  }
}

onMounted(() => {
  loadData();
});
</script>
