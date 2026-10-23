import { defineStore } from 'pinia';
import { ref } from 'vue';
import api from '../api/client';
import type {
  OrderResponse,
  OrderDetailResponse,
  Page,
  CreateOrderRequest,
  AddOrderItemRequest,
  UpdateOrderItemRequest,
  NotPaidOrdersResponse,
  RatingLeaderboardResponse,
  UserStatisticResponse,
  DishSuggestionDto,
  DailyPollResponse,
} from '../types';

export const useOrderStore = defineStore('orders', () => {
  const activeOrders = ref<OrderResponse[]>([]);
  const closedOrders = ref<Page<OrderResponse> | null>(null);
  const currentOrder = ref<OrderDetailResponse | null>(null);
  const unpaidOrders = ref<NotPaidOrdersResponse | null>(null);
  const todayPoll = ref<DailyPollResponse | null>(null);
  const loading = ref(false);
  const error = ref<string | null>(null);

  async function fetchActiveOrders() {
    loading.value = true;
    error.value = null;
    try {
      const response = await api.get<OrderResponse[]>('/orders/active');
      activeOrders.value = response.data;
      return response.data;
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Не удалось загрузить активные заказы';
      throw err;
    } finally {
      loading.value = false;
    }
  }

  async function fetchClosedOrders(page = 0, size = 10) {
    loading.value = true;
    error.value = null;
    try {
      const response = await api.get<Page<OrderResponse>>('/orders/closed', {
        params: { page, size },
      });
      closedOrders.value = response.data;
      return response.data;
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Не удалось загрузить завершенные заказы';
      throw err;
    } finally {
      loading.value = false;
    }
  }

  async function fetchOrderDetail(id: number) {
    loading.value = true;
    error.value = null;
    try {
      const response = await api.get<OrderDetailResponse>(`/orders/${id}`);
      currentOrder.value = response.data;
      return response.data;
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Не удалось загрузить информацию о заказе';
      throw err;
    } finally {
      loading.value = false;
    }
  }

  async function createOrder(data: CreateOrderRequest): Promise<OrderResponse> {
    loading.value = true;
    error.value = null;
    try {
      const response = await api.post<OrderResponse>('/orders', data);
      return response.data;
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Не удалось создать заказ';
      throw err;
    } finally {
      loading.value = false;
    }
  }

  async function toggleAccepting(orderId: number) {
    try {
      await api.post(`/orders/${orderId}/toggle-accepting`);
      await fetchOrderDetail(orderId);
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Не удалось изменить статус приема заказов';
      throw err;
    }
  }

  async function closeOrder(orderId: number, deliveryCost: number) {
    try {
      await api.post(`/orders/${orderId}/close`, { deliveryCost });
      await fetchOrderDetail(orderId);
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Не удалось закрыть заказ';
      throw err;
    }
  }

  async function updateComment(orderId: number, comment: string) {
    try {
      await api.patch(`/orders/${orderId}/comment`, { comment });
      await fetchOrderDetail(orderId);
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Не удалось обновить комментарий';
      throw err;
    }
  }

  async function addItem(orderId: number, data: AddOrderItemRequest) {
    try {
      const res = await api.post(`/orders/${orderId}/items`, data);
      await fetchOrderDetail(orderId);
      return res.data;
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Не удалось добавить позицию';
      throw err;
    }
  }

  async function updateItem(itemId: number, data: UpdateOrderItemRequest) {
    try {
      const res = await api.put(`/items/${itemId}`, data);
      if (currentOrder.value) {
        await fetchOrderDetail(currentOrder.value.order.id);
      }
      return res.data;
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Не удалось обновить позицию';
      throw err;
    }
  }

  async function deleteItem(itemId: number) {
    try {
      await api.delete(`/items/${itemId}`);
      if (currentOrder.value) {
        await fetchOrderDetail(currentOrder.value.order.id);
      }
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Не удалось удалить позицию';
      throw err;
    }
  }

  async function togglePaid(itemId: number) {
    try {
      const res = await api.patch(`/items/${itemId}/toggle-paid`);
      if (currentOrder.value) {
        await fetchOrderDetail(currentOrder.value.order.id);
      }
      return res.data;
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Не удалось изменить статус оплаты';
      throw err;
    }
  }

  async function toggleAdded(itemId: number) {
    try {
      const res = await api.patch(`/items/${itemId}/toggle-added`);
      if (currentOrder.value) {
        await fetchOrderDetail(currentOrder.value.order.id);
      }
      return res.data;
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Не удалось изменить статус чека';
      throw err;
    }
  }

  async function rateItem(itemId: number, rating: number) {
    try {
      const res = await api.post(`/items/${itemId}/rate`, { rating });
      if (currentOrder.value) {
        await fetchOrderDetail(currentOrder.value.order.id);
      }
      return res.data;
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Не удалось сохранить оценку блюда';
      throw err;
    }
  }
  // Fetch ratings leaderboard
  async function fetchRatingsLeaderboard(): Promise<RatingLeaderboardResponse> {
    loading.value = true;
    error.value = null;
    try {
      const resp = await api.get<RatingLeaderboardResponse>('/ratings/leaderboards');
      return resp.data;
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Не удалось загрузить таблицу лидеров';
      throw err;
    } finally {
      loading.value = false;
    }
  }

  // Fetch user statistics for ratings
  async function fetchUserStatistics(): Promise<UserStatisticResponse> {
    loading.value = true;
    error.value = null;
    try {
      const resp = await api.get<UserStatisticResponse>('/ratings/my-stats');
      return resp.data;
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Не удалось загрузить пользовательскую статистику';
      throw err;
    } finally {
      loading.value = false;
    }
  }

  async function markUserPaid(orderId: number, userId: number) {
    try {
      await api.post(`/orders/${orderId}/users/${userId}/mark-paid`);
      await fetchOrderDetail(orderId);
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Не удалось отметить оплату';
      throw err;
    }
  }

  async function markUserAdded(orderId: number, userId: number) {
    try {
      await api.post(`/orders/${orderId}/users/${userId}/mark-added`);
      await fetchOrderDetail(orderId);
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Не удалось отметить позиции в чеке';
      throw err;
    }
  }

  async function fetchUnpaidOrders() {
    try {
      const res = await api.get<NotPaidOrdersResponse>('/orders/unpaid');
      unpaidOrders.value = res.data;
      return res.data;
    } catch (err: any) {
      console.error('Не удалось загрузить неоплаченные заказы', err);
    }
  }

  async function fetchPreviousDishes(restaurantId: number): Promise<DishSuggestionDto[]> {
    try {
      const res = await api.get<DishSuggestionDto[]>(`/restaurants/${restaurantId}/previous-items`);
      return res.data;
    } catch (err: any) {
      console.error('Не удалось загрузить историю блюд', err);
      return [];
    }
  }

  async function fetchTodayPoll(): Promise<DailyPollResponse | null> {
    try {
      const res = await api.get<DailyPollResponse>('/polls/today');
      if (res.status === 204 || !res.data) {
        todayPoll.value = null;
        return null;
      }
      todayPoll.value = res.data;
      return res.data;
    } catch (err: any) {
      console.error('Не удалось загрузить голосование', err);
      todayPoll.value = null;
      return null;
    }
  }

  async function votePoll(restaurantId: number): Promise<DailyPollResponse | null> {
    try {
      const res = await api.post<DailyPollResponse>('/polls/today/vote', { restaurantId });
      todayPoll.value = res.data;
      return res.data;
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Ошибка голосования';
      throw err;
    }
  }

  async function startTodayPoll(): Promise<DailyPollResponse | null> {
    try {
      const res = await api.post<DailyPollResponse>('/polls/today/start');
      todayPoll.value = res.data;
      return res.data;
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Не удалось запустить голосование';
      throw err;
    }
  }

  return {
    activeOrders,
    closedOrders,
    currentOrder,
    unpaidOrders,
    todayPoll,
    loading,
    error,
    fetchActiveOrders,
    fetchClosedOrders,
    fetchOrderDetail,
    createOrder,
    toggleAccepting,
    closeOrder,
    updateComment,
    addItem,
    updateItem,
    deleteItem,
    togglePaid,
    toggleAdded,
    rateItem,
    markUserPaid,
    markUserAdded,
    fetchUnpaidOrders,
    fetchRatingsLeaderboard,
    fetchUserStatistics,
    fetchPreviousDishes,
    fetchTodayPoll,
    votePoll,
    startTodayPoll,
  };
});
