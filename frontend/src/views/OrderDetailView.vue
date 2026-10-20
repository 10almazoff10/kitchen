<template>
  <div class="space-y-8 max-w-5xl mx-auto">
    <!-- Навигация назад -->
    <div class="flex items-center justify-between">
      <router-link
        to="/"
        class="inline-flex items-center gap-2 text-sm font-medium text-slate-600 dark:text-slate-300 hover:text-slate-900 dark:hover:text-slate-100 transition"
      >
        <ArrowLeft class="w-4 h-4" />
        <span>Ко всем заказам</span>
      </router-link>

      <button
        @click="loadOrder"
        title="Обновить"
        class="p-2 rounded-xl text-slate-400 hover:text-slate-700 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition cursor-pointer"
      >
        <RefreshCw class="w-4 h-4" :class="{ 'animate-spin': loading }" />
      </button>
    </div>

    <!-- Загрузка -->
    <div v-if="loading && !orderDetail" class="flex justify-center items-center py-24">
      <Loader2 class="w-8 h-8 text-[#274eac] animate-spin" />
    </div>

    <!-- Ошибка -->
    <div v-else-if="error" class="p-6 rounded-2xl bg-rose-50 dark:bg-rose-950/40 border border-rose-200 dark:border-rose-900/60 text-rose-700 dark:text-rose-300 text-sm flex items-center gap-3">
      <AlertCircle class="w-5 h-5 shrink-0 text-rose-500" />
      <span>{{ error }}</span>
    </div>

    <div v-else-if="orderDetail" class="space-y-8">
      <!-- Шапка заказа -->
      <div class="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-3xl p-6 sm:p-8 shadow-xs space-y-6">
        <div class="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-4">
          <div class="space-y-1.5">
            <div class="flex flex-wrap items-center gap-2">
              <span class="text-xs font-bold text-slate-400 dark:text-slate-500">Заказ #{{ orderDetail.order.id }}</span>
              <span
                class="px-2.5 py-0.5 rounded-full text-xs font-semibold"
                :class="
                  isOrderClosed
                    ? 'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 border border-slate-200 dark:border-slate-700'
                    : 'bg-emerald-50 dark:bg-emerald-950/40 text-emerald-700 dark:text-emerald-300 border border-emerald-200 dark:border-emerald-800/60'
                "
              >
                {{ isOrderClosed ? 'Заказ завершён' : 'Заказ активен' }}
              </span>

              <span
                v-if="!isOrderClosed"
                class="px-2.5 py-0.5 rounded-full text-xs font-semibold"
                :class="
                  isOrderAccepting
                    ? 'bg-emerald-50 dark:bg-emerald-950/40 text-emerald-700 dark:text-emerald-300 border border-emerald-200 dark:border-emerald-800/60'
                    : 'bg-amber-50 dark:bg-amber-950/40 text-amber-700 dark:text-amber-300 border border-amber-200 dark:border-amber-800/60'
                "
              >
                {{ isOrderAccepting ? 'Приём блюд открыт' : 'Приём приостановлен' }}
              </span>
            </div>

            <div class="flex items-center gap-3">
              <div class="w-10 h-10 rounded-2xl bg-slate-50 dark:bg-slate-800 border border-slate-200/80 dark:border-slate-700 p-1.5 flex items-center justify-center shrink-0">
                <img
                  v-if="orderDetail.order.restaurant.websiteUrl"
                  :src="getFaviconUrl(orderDetail.order.restaurant.websiteUrl)"
                  alt=""
                  class="w-6 h-6 rounded object-contain"
                  @error="($event.target as HTMLElement).style.display = 'none'"
                />
                <UtensilsCrossed v-else class="w-5 h-5 text-slate-400" />
              </div>

              <h1 class="text-2xl sm:text-3xl font-extrabold text-slate-900 dark:text-slate-100 tracking-tight">
                {{ orderDetail.order.restaurant.name }}
              </h1>
              <a
                v-if="orderDetail.order.restaurant.websiteUrl"
                :href="orderDetail.order.restaurant.websiteUrl"
                target="_blank"
                rel="noopener noreferrer"
                class="text-[#274eac] dark:text-blue-400 hover:text-[#1f3f8d] dark:hover:text-blue-300 p-1.5 rounded-lg hover:bg-[#eff4fe] dark:hover:bg-blue-950/50 transition"
                title="Перейти на сайт ресторана"
              >
                <ExternalLink class="w-5 h-5" />
              </a>
            </div>

            <div class="flex flex-wrap items-center gap-4 text-xs text-slate-500 dark:text-slate-400 pt-1">
              <div class="flex items-center gap-2">
                <UserAvatar
                  :src="orderDetail.order.createdBy.avatarUrl"
                  :name="orderDetail.order.createdBy.fullName"
                  size-class="w-5 h-5"
                />
                <span>Организатор: <strong class="text-slate-700 dark:text-slate-200">{{ orderDetail.order.createdBy.fullName }}</strong></span>
              </div>
              <div class="flex items-center gap-1.5">
                <Clock class="w-3.5 h-3.5 text-slate-400" />
                <span>Дедлайн: <strong class="text-slate-700 dark:text-slate-200">{{ formatDateTime(orderDetail.order.deadlineTime) }}</strong></span>
                <span
                  class="ml-1 px-2 py-0.5 rounded font-medium"
                  :class="isDeadlinePassed(orderDetail.order.deadlineTime) ? 'bg-rose-50 dark:bg-rose-950/50 text-rose-600 dark:text-rose-400' : 'bg-blue-50 dark:bg-blue-950/50 text-blue-700 dark:text-blue-300'"
                >
                  {{ formatTimeRemaining(orderDetail.order.deadlineTime) }}
                </span>
              </div>
            </div>
          </div>

          <!-- Панель управления организатора -->
          <div v-if="isCreator && !isOrderClosed" class="flex flex-wrap items-center gap-2.5 self-start sm:self-auto">
            <button
              @click="toggleAccepting"
              class="px-4 py-2 rounded-xl text-xs font-semibold border transition cursor-pointer"
              :class="
                isOrderAccepting
                  ? 'border-amber-200 dark:border-amber-800 bg-amber-50 dark:bg-amber-950/40 hover:bg-amber-100 dark:hover:bg-amber-900/60 text-amber-800 dark:text-amber-200'
                  : 'border-emerald-200 dark:border-emerald-800 bg-emerald-50 dark:bg-emerald-950/40 hover:bg-emerald-100 dark:hover:bg-emerald-900/60 text-emerald-800 dark:text-emerald-200'
              "
            >
              {{ isOrderAccepting ? 'Приостановить приём' : 'Возобновить приём' }}
            </button>

            <button
              @click="openCloseModal"
              class="px-4 py-2 rounded-xl text-xs font-semibold bg-rose-600 hover:bg-rose-700 text-white shadow-xs transition active:scale-95 cursor-pointer"
            >
              Завершить заказ
            </button>
          </div>
        </div>

        <!-- Сводка сумм -->
        <div class="grid grid-cols-2 sm:grid-cols-4 gap-4 p-4 rounded-2xl bg-slate-50 dark:bg-slate-800/50 border border-slate-100 dark:border-slate-800">
          <div>
            <div class="text-xs text-slate-500 dark:text-slate-400">Сумма блюд</div>
            <div class="text-lg font-bold text-slate-900 dark:text-slate-100 mt-0.5">
              {{ formatPrice(orderDetail.totalAmount) }}
            </div>
          </div>
          <div>
            <div class="text-xs text-slate-500 dark:text-slate-400">Доставка</div>
            <div class="text-lg font-bold text-slate-900 dark:text-slate-100 mt-0.5">
              {{ formatPrice(orderDetail.totalDeliveryCost) }}
            </div>
          </div>
          <div>
            <div class="text-xs text-slate-500 dark:text-slate-400">Итого с доставкой</div>
            <div class="text-lg font-bold text-[#274eac] dark:text-blue-400 mt-0.5">
              {{ formatPrice(orderDetail.totalAmount + orderDetail.totalDeliveryCost) }}
            </div>
          </div>
          <div>
            <div class="text-xs text-slate-500 dark:text-slate-400">Участников</div>
            <div class="text-lg font-bold text-slate-900 dark:text-slate-100 mt-0.5">
              {{ orderDetail.userSummaries.length }}
            </div>
          </div>
        </div>

        <!-- Реквизиты и комментарий -->
        <div v-if="orderDetail.order.paymentData || orderDetail.order.comment || (isCreator && !isOrderClosed)" class="grid grid-cols-1 md:grid-cols-2 gap-4 pt-2">
          <!-- Реквизиты оплаты -->
          <div
            v-if="orderDetail.order.paymentData"
            class="p-4 rounded-2xl bg-[#eff4fe] dark:bg-blue-950/40 border border-[#bfdbfe] dark:border-blue-900/60 flex items-start justify-between gap-3"
          >
            <div class="space-y-1">
              <div class="text-xs font-bold text-[#183375] dark:text-blue-200 flex items-center gap-1.5">
                <CreditCard class="w-4 h-4 text-[#274eac] dark:text-blue-400" />
                <span>Реквизиты для перевода</span>
              </div>
              <div class="text-sm font-semibold text-slate-900 dark:text-slate-100 select-all font-mono">
                {{ orderDetail.order.paymentData }}
              </div>
            </div>
            <button
              @click="copyPaymentData"
              class="shrink-0 p-2 rounded-xl bg-white dark:bg-slate-800 hover:bg-[#dbeafe] dark:hover:bg-blue-900/60 text-[#274eac] dark:text-blue-300 border border-[#bfdbfe] dark:border-blue-800 shadow-2xs transition active:scale-95 cursor-pointer"
              title="Скопировать реквизиты"
            >
              <Copy class="w-4 h-4" />
            </button>
          </div>

          <!-- Комментарий организатора -->
          <div
            v-if="orderDetail.order.comment || (isCreator && !isOrderClosed)"
            class="p-4 rounded-2xl bg-slate-50 dark:bg-slate-800/50 border border-slate-200/80 dark:border-slate-800 space-y-1.5 flex flex-col justify-between"
          >
            <div>
              <div class="text-xs font-bold text-slate-600 dark:text-slate-300 flex items-center justify-between">
                <div class="flex items-center gap-1.5">
                  <MessageSquare class="w-4 h-4 text-slate-500 dark:text-slate-400" />
                  <span>Комментарий</span>
                </div>
                <button
                  v-if="isCreator && !isOrderClosed"
                  @click="openEditCommentModal"
                  class="p-1 rounded-lg text-slate-400 hover:text-[#274eac] dark:hover:text-blue-400 hover:bg-slate-200/60 dark:hover:bg-slate-700/60 transition cursor-pointer"
                  title="Изменить комментарий"
                >
                  <Pencil class="w-3.5 h-3.5" />
                </button>
              </div>
              <p v-if="orderDetail.order.comment" class="text-xs sm:text-sm text-slate-700 dark:text-slate-300 mt-1 whitespace-pre-wrap">
                {{ orderDetail.order.comment }}
              </p>
              <p v-else class="text-xs text-slate-400 dark:text-slate-500 italic mt-1">
                Комментарий не указан. Нажмите на значок карандаша, чтобы добавить.
              </p>
            </div>
          </div>
        </div>
      </div>

      <!-- Секция добавления позиций (если заказ открыт для приема) -->
      <section
        v-if="!isOrderClosed && isOrderAccepting"
        class="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-3xl p-6 sm:p-8 shadow-xs space-y-5"
      >
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-xl bg-[#274eac] text-white flex items-center justify-center shrink-0 shadow-sm">
            <Plus class="w-5 h-5" />
          </div>
          <div>
            <h2 class="text-lg font-bold text-slate-900 dark:text-slate-100">Добавить блюдо в заказ</h2>
            <p class="text-xs text-slate-500 dark:text-slate-400">Укажите название и стоимость блюда</p>
          </div>
        </div>

        <form
          @submit.prevent="submitManualItem"
          class="grid grid-cols-1 sm:grid-cols-12 gap-3 items-end pt-1"
        >
          <div class="sm:col-span-7">
            <label class="block text-xs font-semibold uppercase tracking-wider text-slate-600 dark:text-slate-400 mb-1.5">
              Название блюда
            </label>
            <input
              v-model="manualItemDescription"
              type="text"
              required
              placeholder="Например, Пицца Пепперони 30 см"
              class="w-full px-4 py-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 placeholder-slate-400 dark:placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-[#274eac]/20 focus:border-[#274eac] text-sm"
            />
          </div>

          <div class="sm:col-span-3">
            <label class="block text-xs font-semibold uppercase tracking-wider text-slate-600 dark:text-slate-400 mb-1.5">
              Цена (₽)
            </label>
            <input
              v-model.number="manualPrice"
              type="number"
              min="0"
              step="1"
              required
              placeholder="590"
              class="w-full px-4 py-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 placeholder-slate-400 dark:placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-[#274eac]/20 focus:border-[#274eac] text-sm"
            />
          </div>

          <div class="sm:col-span-2">
            <button
              type="submit"
              :disabled="addingItem"
              class="w-full py-2.5 px-4 bg-[#274eac] hover:bg-[#1f3f8d] text-white font-medium text-sm rounded-xl shadow-sm transition active:scale-95 disabled:opacity-50 cursor-pointer flex items-center justify-center gap-1.5"
            >
              <Loader2 v-if="addingItem" class="w-4 h-4 animate-spin" />
              <Plus v-else class="w-4 h-4" />
              <span>Добавить</span>
            </button>
          </div>
        </form>
      </section>

      <!-- Список участников и их блюд -->
      <section class="space-y-4">
        <div class="flex items-center justify-between">
          <h2 class="text-xl font-bold text-slate-900 dark:text-slate-100 tracking-tight flex items-center gap-2">
            <Users class="w-5 h-5 text-[#274eac] dark:text-blue-400" />
            <span>Участники заказа ({{ orderDetail.userSummaries.length }})</span>
          </h2>
        </div>

        <div v-if="!orderDetail.userSummaries.length" class="bg-white dark:bg-slate-900/60 border border-dashed border-slate-300 dark:border-slate-800 rounded-3xl p-12 text-center text-slate-400 dark:text-slate-500">
          <UtensilsCrossed class="w-10 h-10 mx-auto mb-2 text-slate-300 dark:text-slate-600" />
          <div class="font-medium">В этом заказе пока нет добавленных блюд</div>
        </div>

        <div v-else class="space-y-4">
          <div
            v-for="userSummary in orderDetail.userSummaries"
            :key="userSummary.userId"
            class="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl shadow-xs overflow-hidden transition"
          >
            <!-- Шапка карточки участника -->
            <div class="px-5 py-4 bg-slate-50/70 dark:bg-slate-800/60 border-b border-slate-100 dark:border-slate-800 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
              <div class="flex items-center gap-3">
                <UserAvatar
                  :src="userSummary.avatarUrl"
                  :name="userSummary.userFullName"
                  size-class="w-10 h-10"
                  rounded-class="rounded-xl"
                  border-class="ring-1 ring-slate-200/80 dark:ring-slate-700"
                />
                <div>
                  <div class="flex items-center gap-2">
                    <span class="font-bold text-slate-900 dark:text-slate-100 text-sm sm:text-base">
                      {{ userSummary.userFullName }}
                    </span>
                    <span
                      v-if="userSummary.userId === authStore.user?.id"
                      class="px-2 py-0.5 rounded text-[10px] bg-[#eff4fe] dark:bg-blue-950/60 text-[#274eac] dark:text-blue-400 font-bold"
                    >
                      Вы
                    </span>
                  </div>
                  <div class="text-xs text-slate-500 dark:text-slate-400">
                    Блюд: {{ userSummary.items.length }} · Доставка: {{ formatPrice(userSummary.deliveryCost) }}
                  </div>
                </div>
              </div>

              <!-- Сумма и массовые действия для создателя -->
              <div class="flex flex-wrap items-center gap-3 self-end sm:self-auto">
                <div class="text-right">
                  <div class="text-xs text-slate-400 dark:text-slate-500">Итого с доставкой</div>
                  <div class="text-base font-extrabold text-slate-900 dark:text-slate-100">
                    {{ formatPrice(userSummary.totalAmount) }}
                  </div>
                </div>

                <div v-if="isCreator" class="flex items-center gap-1.5 pl-2 border-l border-slate-200 dark:border-slate-700">
                  <button
                    @click="markUserPaid(userSummary.userId)"
                    class="px-2.5 py-1 text-xs font-semibold rounded-lg transition cursor-pointer"
                    :class="userSummary.allPaid ? 'bg-emerald-100 dark:bg-emerald-950/60 text-emerald-800 dark:text-emerald-300' : 'bg-slate-200 dark:bg-slate-800 hover:bg-slate-300 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-300'"
                    title="Отметить все позиции пользователя как оплаченные"
                  >
                    {{ userSummary.allPaid ? 'Оплачено ✓' : 'Оплатить все' }}
                  </button>
                  <button
                    @click="markUserAdded(userSummary.userId)"
                    class="px-2.5 py-1 text-xs font-semibold rounded-lg transition cursor-pointer"
                    :class="userSummary.allAdded ? 'bg-blue-100 dark:bg-blue-950/60 text-blue-800 dark:text-blue-300' : 'bg-slate-200 dark:bg-slate-800 hover:bg-slate-300 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-300'"
                    title="Отметить все позиции пользователя в чеке"
                  >
                    {{ userSummary.allAdded ? 'В чеке ✓' : 'В чек все' }}
                  </button>
                </div>
              </div>
            </div>

            <!-- Список блюд этого участника -->
            <div class="divide-y divide-slate-100 dark:divide-slate-800">
              <div
                v-for="item in userSummary.items"
                :key="item.id"
                class="px-5 py-3.5 flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-sm hover:bg-slate-50/40 dark:hover:bg-slate-800/40 transition"
              >
                <div class="space-y-1">
                  <div class="font-medium text-slate-900 dark:text-slate-100">
                    {{ item.itemDescription }}
                  </div>
                  <div class="text-xs text-slate-400 dark:text-slate-500 flex items-center gap-2">
                    <span>{{ formatPrice(item.price) }}</span>
                    <span v-if="item.deliveryCost > 0">+ {{ formatPrice(item.deliveryCost) }} доставка</span>
                    <span class="font-semibold text-slate-700 dark:text-slate-300">= {{ formatPrice(item.totalCost) }}</span>
                  </div>
                </div>

                <!-- Статусы: Оплачено, В чеке, Оценка, Действия -->
                <div class="flex flex-wrap items-center gap-3">
                  <!-- Чекбокс / кнопка Оплачено -->
                  <button
                    @click="togglePaid(item.id)"
                    class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-semibold transition cursor-pointer"
                    :class="
                      (item.paid ?? item.isPaid)
                        ? 'bg-emerald-100 dark:bg-emerald-950/60 text-emerald-800 dark:text-emerald-300 hover:bg-emerald-200 dark:hover:bg-emerald-900/60'
                        : 'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 hover:bg-slate-200 dark:hover:bg-slate-700'
                    "
                  >
                    <CheckCircle2 v-if="item.paid ?? item.isPaid" class="w-3.5 h-3.5 text-emerald-600 dark:text-emerald-400" />
                    <Circle v-else class="w-3.5 h-3.5 text-slate-400" />
                    <span>{{ (item.paid ?? item.isPaid) ? 'Оплачено' : 'Не оплачено' }}</span>
                  </button>

                  <!-- Чекбокс / кнопка В чеке -->
                  <button
                    v-if="isCreator"
                    @click="toggleAdded(item.id)"
                    class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-semibold transition cursor-pointer"
                    :class="
                      (item.addedToRestaurantOrder ?? item.isAddedToRestaurantOrder)
                        ? 'bg-blue-100 dark:bg-blue-950/60 text-blue-800 dark:text-blue-300 hover:bg-blue-200 dark:hover:bg-blue-900/60'
                        : 'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 hover:bg-slate-200 dark:hover:bg-slate-700'
                    "
                  >
                    <CheckCircle2 v-if="item.addedToRestaurantOrder ?? item.isAddedToRestaurantOrder" class="w-3.5 h-3.5 text-blue-600 dark:text-blue-400" />
                    <Circle v-else class="w-3.5 h-3.5 text-slate-400" />
                    <span>{{ (item.addedToRestaurantOrder ?? item.isAddedToRestaurantOrder) ? 'В чеке' : 'Не в чеке' }}</span>
                  </button>
                  <span
                    v-else-if="item.addedToRestaurantOrder ?? item.isAddedToRestaurantOrder"
                    class="px-2 py-0.5 rounded text-[11px] font-semibold bg-blue-50 dark:bg-blue-950/60 text-blue-700 dark:text-blue-300"
                  >
                    В чеке
                  </span>

                  <!-- Звёздочки рейтинга (1-5) -->
                  <div class="flex items-center gap-0.5" title="Оцените блюдо">
                    <button
                      v-for="star in 5"
                      :key="star"
                      type="button"
                      @click="rateItem(item.id, star)"
                      class="p-0.5 transition cursor-pointer hover:scale-110"
                    >
                      <Star
                        class="w-4 h-4"
                        :class="
                          (item.rating || 0) >= star
                            ? 'text-amber-400 fill-amber-400'
                            : 'text-slate-200 dark:text-slate-700 hover:text-amber-300'
                        "
                      />
                    </button>
                  </div>

                  <!-- Действия владельца позиции -->
                  <div v-if="canEditItem(item)" class="flex items-center gap-1 pl-2 border-l border-slate-200 dark:border-slate-700">
                    <button
                      @click="openEditModal(item)"
                      class="p-1.5 rounded-lg text-slate-400 hover:text-slate-700 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition cursor-pointer"
                      title="Редактировать блюдо"
                    >
                      <Pencil class="w-3.5 h-3.5" />
                    </button>
                    <button
                      @click="deleteItem(item.id)"
                      class="p-1.5 rounded-lg text-slate-400 hover:text-rose-600 dark:hover:text-rose-400 hover:bg-rose-50 dark:hover:bg-rose-950/50 transition cursor-pointer"
                      title="Удалить блюдо"
                    >
                      <Trash2 class="w-3.5 h-3.5" />
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>

    <!-- Модальное окно: Завершить заказ -->
    <div
      v-if="showCloseModal"
      class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/60 backdrop-blur-xs"
      @click.self="closeCloseModal"
    >
      <div class="bg-white dark:bg-slate-900 rounded-3xl max-w-md w-full p-6 sm:p-8 shadow-2xl border border-slate-100 dark:border-slate-800 space-y-5 animate-in fade-in zoom-in-95 duration-150">
        <div class="flex items-center justify-between">
          <h3 class="text-lg font-bold text-slate-900 dark:text-slate-100">Завершить заказ</h3>
          <button @click="closeCloseModal" class="p-1 rounded-lg text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition cursor-pointer">
            ✕
          </button>
        </div>

        <p class="text-xs text-slate-500 dark:text-slate-400">
          При завершении заказа новые позиции приниматься не будут. Стоимость доставки будет равномерно распределена между участниками.
        </p>

        <div>
          <label class="block text-xs font-semibold uppercase tracking-wider text-slate-600 dark:text-slate-400 mb-1.5">
            Стоимость доставки (₽)
          </label>
          <input
            v-model.number="closeDeliveryCost"
            type="number"
            min="0"
            step="1"
            class="w-full px-4 py-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-[#274eac]/20 focus:border-[#274eac] text-sm"
          />
        </div>

        <!-- Превью расчета доставки -->
        <div class="p-3.5 rounded-xl bg-[#eff4fe] dark:bg-blue-950/40 border border-[#bfdbfe] dark:border-blue-900/60 text-xs text-[#183375] dark:text-blue-200 space-y-1">
          <div class="flex justify-between">
            <span>Участников:</span>
            <strong>{{ orderDetail?.userSummaries.length || 0 }}</strong>
          </div>
          <div class="flex justify-between">
            <span>Доставка на каждого:</span>
            <strong>
              {{
                orderDetail?.userSummaries.length
                  ? formatPrice(Math.ceil((closeDeliveryCost || 0) / orderDetail.userSummaries.length))
                  : '0 ₽'
              }}
            </strong>
          </div>
        </div>

        <div class="flex items-center justify-end gap-3 pt-3 border-t border-slate-100 dark:border-slate-800">
          <button
            type="button"
            @click="closeCloseModal"
            class="px-4 py-2 text-sm font-medium text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-xl transition cursor-pointer"
          >
            Отмена
          </button>
          <button
            @click="confirmCloseOrder"
            :disabled="closingOrder"
            class="inline-flex items-center gap-2 px-5 py-2 text-sm font-medium bg-rose-600 hover:bg-rose-700 text-white rounded-xl shadow-sm transition active:scale-95 cursor-pointer disabled:opacity-50"
          >
            <Loader2 v-if="closingOrder" class="w-4 h-4 animate-spin" />
            <span>Зафиксировать и закрыть</span>
          </button>
        </div>
      </div>
    </div>

    <!-- Модальное окно: Редактировать позицию -->
    <div
      v-if="showEditModal"
      class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/60 backdrop-blur-xs"
      @click.self="closeEditModal"
    >
      <div class="bg-white dark:bg-slate-900 rounded-3xl max-w-md w-full p-6 sm:p-8 shadow-2xl border border-slate-100 dark:border-slate-800 space-y-5 animate-in fade-in zoom-in-95 duration-150">
        <div class="flex items-center justify-between">
          <h3 class="text-lg font-bold text-slate-900 dark:text-slate-100">Редактировать блюдо</h3>
          <button @click="closeEditModal" class="p-1 rounded-lg text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition cursor-pointer">
            ✕
          </button>
        </div>

        <form @submit.prevent="confirmUpdateItem" class="space-y-4">
          <div>
            <label class="block text-xs font-semibold uppercase tracking-wider text-slate-600 dark:text-slate-400 mb-1.5">
              Название блюда
            </label>
            <input
              v-model="editItemDescription"
              type="text"
              required
              class="w-full px-4 py-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 text-sm focus:outline-none focus:ring-2 focus:ring-[#274eac]/20 focus:border-[#274eac]"
            />
          </div>

          <div>
            <label class="block text-xs font-semibold uppercase tracking-wider text-slate-600 dark:text-slate-400 mb-1.5">
              Цена (₽)
            </label>
            <input
              v-model.number="editItemPrice"
              type="number"
              min="0"
              step="1"
              required
              class="w-full px-4 py-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 text-sm focus:outline-none focus:ring-2 focus:ring-[#274eac]/20 focus:border-[#274eac]"
            />
          </div>

          <div class="flex items-center justify-end gap-3 pt-3 border-t border-slate-100 dark:border-slate-800">
            <button
              type="button"
              @click="closeEditModal"
              class="px-4 py-2 text-sm font-medium text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-xl transition cursor-pointer"
            >
              Отмена
            </button>
            <button
              type="submit"
              :disabled="updatingItem"
              class="inline-flex items-center gap-2 px-5 py-2 text-sm font-medium bg-[#274eac] hover:bg-[#1f3f8d] text-white rounded-xl shadow-sm transition active:scale-95 cursor-pointer disabled:opacity-50"
            >
              <Loader2 v-if="updatingItem" class="w-4 h-4 animate-spin" />
              <span>Сохранить</span>
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Модальное окно: Изменить комментарий к заказу -->
    <div
      v-if="showEditCommentModal"
      class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/60 backdrop-blur-xs"
      @click.self="closeEditCommentModal"
    >
      <div class="bg-white dark:bg-slate-900 rounded-3xl max-w-md w-full p-6 sm:p-8 shadow-2xl border border-slate-100 dark:border-slate-800 space-y-5 animate-in fade-in zoom-in-95 duration-150">
        <div class="flex items-center justify-between">
          <h3 class="text-lg font-bold text-slate-900 dark:text-slate-100">Комментарий к заказу</h3>
          <button @click="closeEditCommentModal" class="p-1 rounded-lg text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition cursor-pointer">
            ✕
          </button>
        </div>

        <form @submit.prevent="confirmUpdateComment" class="space-y-4">
          <div>
            <label class="block text-xs font-semibold uppercase tracking-wider text-slate-600 dark:text-slate-400 mb-1.5">
              Текст комментария
            </label>
            <textarea
              v-model="editCommentValue"
              rows="4"
              placeholder="Например: доставка ~40 минут, делим доставку поровну на всех"
              class="w-full px-4 py-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 text-sm focus:outline-none focus:ring-2 focus:ring-[#274eac]/20 focus:border-[#274eac] resize-none"
            ></textarea>
          </div>

          <div class="flex items-center justify-end gap-3 pt-3 border-t border-slate-100 dark:border-slate-800">
            <button
              type="button"
              @click="closeEditCommentModal"
              class="px-4 py-2 text-sm font-medium text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-xl transition cursor-pointer"
            >
              Отмена
            </button>
            <button
              type="submit"
              :disabled="updatingComment"
              class="inline-flex items-center gap-2 px-5 py-2 text-sm font-medium bg-[#274eac] hover:bg-[#1f3f8d] text-white rounded-xl shadow-sm transition active:scale-95 cursor-pointer disabled:opacity-50"
            >
              <Loader2 v-if="updatingComment" class="w-4 h-4 animate-spin" />
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
import { useRoute } from 'vue-router';
import { useOrderStore } from '@/stores/orders';
import { useAuthStore } from '@/stores/auth';
import { useToastStore } from '@/stores/toast';
import { storeToRefs } from 'pinia';
import UserAvatar from '@/components/UserAvatar.vue';
import {
  formatPrice,
  formatDateTime,
  formatTimeRemaining,
  isDeadlinePassed,
  getFaviconUrl,
} from '@/utils/format';
import type { OrderItemResponse } from '@/types';
import {
  ArrowLeft,
  RefreshCw,
  Loader2,
  AlertCircle,
  ExternalLink,
  User as UserIcon,
  Clock,
  CreditCard,
  Copy,
  MessageSquare,
  Plus,
  Users,
  UtensilsCrossed,
  CheckCircle2,
  Circle,
  Star,
  Pencil,
  Trash2,
} from 'lucide-vue-next';

const route = useRoute();
const orderId = Number(route.params.id);

const orderStore = useOrderStore();
const authStore = useAuthStore();
const toastStore = useToastStore();

const { currentOrder, loading, error } = storeToRefs(orderStore);
const orderDetail = computed(() => currentOrder.value);

const isCreator = computed(() => {
  return (
    authStore.user &&
    orderDetail.value &&
    authStore.user.id === orderDetail.value.order.createdBy.id
  );
});

const isOrderClosed = computed(() => {
  const o = orderDetail.value?.order;
  if (!o) return false;
  return Boolean(o.closed ?? o.isClosed);
});

const isOrderAccepting = computed(() => {
  const o = orderDetail.value?.order;
  if (!o) return false;
  return Boolean(o.acceptingOrders ?? o.isAcceptingOrders);
});

// Добавление блюд (ручной ввод)
const manualItemDescription = ref('');
const manualPrice = ref<number | null>(null);
const addingItem = ref(false);

// Модалка закрытия заказа
const showCloseModal = ref(false);
const closeDeliveryCost = ref<number>(0);
const closingOrder = ref(false);

// Модалка редактирования позиции
const showEditModal = ref(false);
const editingItemId = ref<number | null>(null);
const editItemDescription = ref('');
const editItemPrice = ref<number>(0);
const updatingItem = ref(false);

// Модалка редактирования комментария
const showEditCommentModal = ref(false);
const editCommentValue = ref('');
const updatingComment = ref(false);

async function loadOrder() {
  try {
    await orderStore.fetchOrderDetail(orderId);
  } catch (err) {
    console.error('Failed to load order detail', err);
  }
}

async function toggleAccepting() {
  try {
    await orderStore.toggleAccepting(orderId);
    toastStore.success('Статус приёма заказов изменён');
  } catch (err: any) {
    toastStore.error(err.response?.data?.message || 'Ошибка изменения статуса');
  }
}

function openCloseModal() {
  closeDeliveryCost.value = 0;
  showCloseModal.value = true;
}

function closeCloseModal() {
  showCloseModal.value = false;
}

async function confirmCloseOrder() {
  closingOrder.value = true;
  try {
    await orderStore.closeOrder(orderId, closeDeliveryCost.value || 0);
    toastStore.success('Заказ успешно завершён!');
    closeCloseModal();
  } catch (err: any) {
    toastStore.error(err.response?.data?.message || 'Не удалось завершить заказ');
  } finally {
    closingOrder.value = false;
  }
}

function copyPaymentData() {
  if (orderDetail.value?.order.paymentData) {
    navigator.clipboard.writeText(orderDetail.value.order.paymentData);
    toastStore.success('Реквизиты скопированы в буфер обмена');
  }
}

async function submitManualItem() {
  if (!manualItemDescription.value.trim() || manualPrice.value === null) return;

  addingItem.value = true;
  try {
    await orderStore.addItem(orderId, {
      itemDescription: manualItemDescription.value.trim(),
      price: manualPrice.value,
    });
    toastStore.success(`Добавлено: ${manualItemDescription.value}`);
    manualItemDescription.value = '';
    manualPrice.value = null;
  } catch (err: any) {
    toastStore.error(err.response?.data?.message || 'Не удалось добавить блюдо');
  } finally {
    addingItem.value = false;
  }
}

async function togglePaid(itemId: number) {
  try {
    await orderStore.togglePaid(itemId);
  } catch (err: any) {
    toastStore.error(err.response?.data?.message || 'Ошибка переключения оплаты');
  }
}

async function toggleAdded(itemId: number) {
  try {
    await orderStore.toggleAdded(itemId);
  } catch (err: any) {
    toastStore.error(err.response?.data?.message || 'Ошибка отметки в чеке');
  }
}

async function rateItem(itemId: number, rating: number) {
  try {
    await orderStore.rateItem(itemId, rating);
    toastStore.success(`Оценка ${rating} поставлена`);
  } catch (err: any) {
    toastStore.error(err.response?.data?.message || 'Ошибка сохранения оценки');
  }
}

async function markUserPaid(userId: number) {
  try {
    await orderStore.markUserPaid(orderId, userId);
    toastStore.success('Оплата пользователя отмечена');
  } catch (err: any) {
    toastStore.error(err.response?.data?.message || 'Ошибка отметки оплаты');
  }
}

async function markUserAdded(userId: number) {
  try {
    await orderStore.markUserAdded(orderId, userId);
    toastStore.success('Позиции пользователя отмечены в чеке');
  } catch (err: any) {
    toastStore.error(err.response?.data?.message || 'Ошибка отметки позиций');
  }
}

function canEditItem(item: OrderItemResponse) {
  return (
    authStore.user &&
    authStore.user.id === item.userId &&
    !isOrderClosed.value
  );
}

function openEditModal(item: OrderItemResponse) {
  editingItemId.value = item.id;
  editItemDescription.value = item.itemDescription;
  editItemPrice.value = item.price;
  showEditModal.value = true;
}

function closeEditModal() {
  showEditModal.value = false;
  editingItemId.value = null;
}

async function confirmUpdateItem() {
  if (!editingItemId.value || !editItemDescription.value.trim()) return;

  updatingItem.value = true;
  try {
    await orderStore.updateItem(editingItemId.value, {
      itemDescription: editItemDescription.value.trim(),
      price: editItemPrice.value,
    });
    toastStore.success('Блюдо обновлено');
    closeEditModal();
  } catch (err: any) {
    toastStore.error(err.response?.data?.message || 'Не удалось обновить блюдо');
  } finally {
    updatingItem.value = false;
  }
}

async function deleteItem(itemId: number) {
  if (!confirm('Удалить эту позицию из заказа?')) return;

  try {
    await orderStore.deleteItem(itemId);
    toastStore.success('Позиция удалена');
  } catch (err: any) {
    toastStore.error(err.response?.data?.message || 'Не удалось удалить блюдо');
  }
}

function openEditCommentModal() {
  editCommentValue.value = orderDetail.value?.order.comment || '';
  showEditCommentModal.value = true;
}

function closeEditCommentModal() {
  showEditCommentModal.value = false;
}

async function confirmUpdateComment() {
  updatingComment.value = true;
  try {
    await orderStore.updateComment(orderId, editCommentValue.value.trim());
    toastStore.success('Комментарий к заказу обновлён');
    closeEditCommentModal();
  } catch (err: any) {
    toastStore.error(err.response?.data?.message || 'Не удалось обновить комментарий');
  } finally {
    updatingComment.value = false;
  }
}

onMounted(loadOrder);
</script>
