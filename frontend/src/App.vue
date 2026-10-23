<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from './stores/auth';
import { useToastStore } from './stores/toast';
import { useOrderStore } from './stores/orders';
import { useThemeStore } from './stores/theme';
import { LogOut, Store, Award, PlusCircle, CheckCircle, AlertCircle, Info, UtensilsCrossed, Sun, Moon, Archive } from 'lucide-vue-next';
import LinkAdModal from './components/LinkAdModal.vue';
import ProfileModal from './components/ProfileModal.vue';
import UserAvatar from './components/UserAvatar.vue';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const toastStore = useToastStore();
const orderStore = useOrderStore();
const themeStore = useThemeStore();

const showProfileModal = ref(false);

const isLoginPage = computed(() => route.name === 'login');
const unpaidCount = computed(() => orderStore.unpaidOrders?.totalCount || orderStore.unpaidOrders?.items?.length || 0);

function handleLogout() {
  authStore.logout();
  router.push('/login');
}

function handleAdLinked() {
  orderStore.fetchUnpaidOrders();
  if (route.name === 'login') {
    router.push('/');
  }
}

onMounted(() => {
  themeStore.initTheme();
  if (authStore.isAuthenticated) {
    authStore.fetchMe();
    orderStore.fetchUnpaidOrders();
  }
});
</script>

<template>
  <div class="relative min-h-screen flex flex-col font-sans">
    <!-- Полупрозрачный паттерн еды по краям -->
    <div class="food-doodle-bg"></div>

    <!-- Фирменный хедер Kitchen -->
    <header class="sticky top-0 z-30 shadow-md bg-[#274eac] border-b border-[#1f3f8d] dark:bg-[#111e3b] dark:border-slate-800">
      <div class="header-container max-w-6xl mx-auto px-4 h-16 flex items-center justify-between">
        <!-- Левая часть: логотип + название + разделы навигации -->
        <div class="header-left flex items-center gap-3 sm:gap-5">
          <router-link to="/" class="header-logo-link flex items-center gap-3 text-white hover:opacity-90 transition">
            <img
              src="/img/logo_kitchen.png"
              alt="Kitchen"
              class="header-logo-img h-9 w-auto block shrink-0"
            />
            <span class="header-site-name text-white font-semibold text-lg tracking-tight">Kitchen</span>
          </router-link>

          <!-- Единая навигация по разделам -->
          <nav v-if="!isLoginPage" class="flex items-center gap-1 pl-3 border-l border-white/20">
            <router-link
              to="/"
              class="px-3 py-1.5 rounded-lg text-xs sm:text-sm font-semibold transition flex items-center gap-1.5"
              :class="route.path === '/' ? 'bg-white/20 text-white' : 'text-blue-100 hover:text-white hover:bg-white/10'"
            >
              <UtensilsCrossed class="w-3.5 h-3.5" />
              <span>Заказы</span>
            </router-link>

            <router-link
              to="/restaurants"
              class="px-3 py-1.5 rounded-lg text-xs sm:text-sm font-semibold transition flex items-center gap-1.5"
              :class="route.path === '/restaurants' ? 'bg-white/20 text-white' : 'text-blue-100 hover:text-white hover:bg-white/10'"
            >
              <Store class="w-3.5 h-3.5" />
              <span>Рестораны</span>
            </router-link>

            <router-link
              to="/ratings"
              class="px-3 py-1.5 rounded-lg text-xs sm:text-sm font-semibold transition flex items-center gap-1.5"
              :class="route.path === '/ratings' ? 'bg-white/20 text-white' : 'text-blue-100 hover:text-white hover:bg-white/10'"
            >
              <Award class="w-3.5 h-3.5 text-amber-300" />
              <span>Рейтинг</span>
            </router-link>

            <router-link
              to="/orders/history"
              class="px-3 py-1.5 rounded-lg text-xs sm:text-sm font-semibold transition flex items-center gap-1.5"
              :class="route.path === '/orders/history' ? 'bg-white/20 text-white' : 'text-blue-100 hover:text-white hover:bg-white/10'"
            >
              <Archive class="w-3.5 h-3.5" />
              <span>История</span>
            </router-link>
          </nav>
        </div>

        <!-- Правая часть: действия и профиль -->
        <div class="flex items-center gap-2.5 sm:gap-3">
          <!-- Ссылка на неоплаченные заказы (если есть) -->
          <router-link
            v-if="!isLoginPage && unpaidCount > 0"
            to="/"
            class="relative inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-bold transition shadow-xs bg-rose-100 hover:bg-rose-200 border border-rose-300 text-rose-800 dark:bg-rose-950/50 dark:hover:bg-rose-900/60 dark:border-rose-900/50 dark:text-rose-200 active:scale-95"
            title="У вас есть неоплаченные заказы"
          >
            <span class="text-base leading-none">🚨</span>
            <span class="hidden sm:inline">Долг</span>
            <span class="px-1.5 py-0.5 rounded-full text-[10px] font-extrabold bg-rose-600 text-white">
              {{ unpaidCount }}
            </span>
          </router-link>

          <!-- Кнопка «Новый заказ» -->
          <router-link
            v-if="!isLoginPage"
            to="/create-order"
            class="inline-flex items-center gap-1.5 bg-white/15 hover:bg-white/25 border border-white/25 text-white text-xs sm:text-sm font-semibold px-3.5 py-2 rounded-xl shadow-xs transition-all active:scale-95"
          >
            <PlusCircle class="w-4 h-4" />
            <span class="hidden sm:inline">Новый заказ</span>
          </router-link>

          <!-- Переключатель темы (светлая / тёмная) -->
          <button
            @click="themeStore.toggleTheme"
            :title="themeStore.isDark ? 'Переключить на светлую тему' : 'Переключить на тёмную тему'"
            class="p-2 rounded-xl text-blue-100 hover:text-white hover:bg-white/10 transition active:scale-95 cursor-pointer flex items-center justify-center"
          >
            <Sun v-if="themeStore.isDark" class="w-4 h-4 text-amber-300" />
            <Moon v-else class="w-4 h-4" />
          </button>

          <!-- Данные пользователя и кнопка выхода -->
          <div v-if="!isLoginPage && authStore.user" class="flex items-center gap-2 pl-2 sm:pl-3 border-l border-white/20">
            <button
              @click="showProfileModal = true"
              class="flex items-center gap-2.5 p-1 rounded-xl hover:bg-white/10 transition text-left cursor-pointer group"
              title="Настройки профиля и аватара"
            >
              <UserAvatar
                :src="authStore.user.avatarUrl"
                :name="authStore.user.fullName"
                size-class="w-8 h-8"
                border-class="ring-2 ring-white/30 group-hover:ring-white/70 transition"
              />
              <div class="text-right hidden md:block">
                <div class="text-xs font-semibold text-white leading-tight truncate max-w-[150px] group-hover:text-blue-100 transition">
                  {{ authStore.user.fullName }}
                </div>
                <div class="text-[10px] text-blue-200 truncate max-w-[150px]">
                  @{{ authStore.user.username }}
                </div>
              </div>
            </button>
            <button
              @click="handleLogout"
              title="Выйти"
              class="p-2 rounded-xl text-blue-200 hover:text-white hover:bg-white/10 transition cursor-pointer"
            >
              <LogOut class="w-4 h-4" />
            </button>
          </div>
        </div>
      </div>
    </header>

    <!-- Основное содержимое -->
    <main class="relative z-10 flex-1 max-w-6xl w-full mx-auto px-4 py-6 sm:py-8">
      <router-view v-slot="{ Component }">
        <transition name="page" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>

    <!-- Футер -->
    <footer class="relative z-10 text-center py-6 text-xs text-slate-400 dark:text-slate-500 border-t border-slate-200/60 dark:border-slate-800/80 mt-auto">
      Kitchen &copy; 2026 &bull; Сервис совместных заказов
    </footer>

    <!-- Модальное окно обязательной привязки Active Directory -->
    <LinkAdModal
      v-if="authStore.isAuthenticated && authStore.user && authStore.user.isAdLinked === false"
      :user-full-name="authStore.user.fullName"
      :legacy-username="authStore.user.legacyUsername || authStore.user.username"
      @success="handleAdLinked"
    />

    <!-- Модальное окно профиля и настроек Gravatar -->
    <ProfileModal
      :is-open="showProfileModal"
      @close="showProfileModal = false"
    />

    <!-- Toast-уведомления -->
    <div class="fixed bottom-5 right-5 z-50 flex flex-col space-y-2 pointer-events-none max-w-sm w-full px-4 sm:px-0">
      <transition-group name="toast">
        <div
          v-for="toast in toastStore.toasts"
          :key="toast.id"
          class="pointer-events-auto px-4 py-3 rounded-xl shadow-lg border text-sm font-medium flex items-center space-x-2.5 backdrop-blur-md transition-all"
          :class="{
            'bg-emerald-500/95 border-emerald-600 text-white': toast.type === 'success',
            'bg-rose-500/95 border-rose-600 text-white': toast.type === 'error',
            'bg-slate-900/95 border-slate-800 text-white': toast.type === 'info',
          }"
        >
          <CheckCircle v-if="toast.type === 'success'" class="w-4 h-4 shrink-0" />
          <AlertCircle v-else-if="toast.type === 'error'" class="w-4 h-4 shrink-0" />
          <Info v-else class="w-4 h-4 shrink-0" />
          <span>{{ toast.message }}</span>
        </div>
      </transition-group>
    </div>
  </div>
</template>

<style scoped>
.toast-enter-active,
.toast-leave-active {
  transition: all 0.25s ease;
}
.toast-enter-from {
  opacity: 0;
  transform: translateY(16px) scale(0.95);
}
.toast-leave-to {
  opacity: 0;
  transform: translateY(-8px) scale(0.95);
}
</style>
