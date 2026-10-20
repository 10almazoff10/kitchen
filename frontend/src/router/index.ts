import { createRouter, createWebHistory } from 'vue-router';
import HomeView from '../views/HomeView.vue';
import LoginView from '../views/LoginView.vue';
import OrderDetailView from '../views/OrderDetailView.vue';
import CreateOrderView from '../views/CreateOrderView.vue';
import RestaurantsView from '../views/RestaurantsView.vue';
import RatingsView from '../views/RatingsView.vue';
import { useAuthStore } from '../stores/auth';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: { guestOnly: true },
    },
    {
      path: '/',
      name: 'home',
      component: HomeView,
      meta: { requiresAuth: true },
    },
    {
      path: '/orders/:id',
      name: 'order-detail',
      component: OrderDetailView,
      meta: { requiresAuth: true },
    },
    {
      path: '/create-order',
      name: 'create-order',
      component: CreateOrderView,
      meta: { requiresAuth: true },
    },
    {
      path: '/restaurants',
      name: 'restaurants',
      component: RestaurantsView,
      meta: { requiresAuth: true },
    },
    {
      path: '/ratings',
      name: 'ratings',
      component: RatingsView,
      meta: { requiresAuth: true },
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/',
    },
  ],
});

router.beforeEach((to, from, next) => {
  const authStore = useAuthStore();
  const token = localStorage.getItem('kitchen_token');

  if (to.meta.requiresAuth && !token) {
    next({ name: 'login' });
  } else if (to.meta.guestOnly && token) {
    next({ name: 'home' });
  } else {
    next();
  }
});

export default router;
