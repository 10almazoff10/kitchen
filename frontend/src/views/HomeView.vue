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

    <!-- Голосование за выбор ресторана на сегодня -->
    <section class="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-4 sm:p-5 shadow-xs">
      <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-4">
        <div class="flex items-center gap-3">
          <div class="w-9 h-9 rounded-xl bg-amber-50 dark:bg-amber-950/60 text-amber-600 dark:text-amber-400 flex items-center justify-center shrink-0">
            <Vote class="w-5 h-5" />
          </div>
          <div>
            <div class="flex flex-wrap items-center gap-2">
              <h2 class="text-base sm:text-lg font-bold text-slate-900 dark:text-slate-100 tracking-tight">
                Голосование за выбор ресторана
              </h2>
              <span
                v-if="todayPoll"
                class="px-2 py-0.5 rounded-full text-xs font-bold bg-amber-50 dark:bg-amber-950/60 text-amber-700 dark:text-amber-400 border border-amber-200/80 dark:border-amber-900/60"
              >
                {{ todayPoll.totalVotes }} {{ getVotesWord(todayPoll.totalVotes) }}
              </span>
              <span
                v-if="todayPoll"
                class="px-2 py-0.5 rounded-full text-[11px] font-medium bg-blue-50 dark:bg-blue-950/50 text-[#274eac] dark:text-blue-300 border border-blue-200/60 dark:border-blue-900/40"
              >
                Можно выбрать несколько
              </span>
            </div>
            <p class="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
              Каждое утро в 9:00 выбираем, откуда заказываем обед
            </p>
          </div>
        </div>

        <div v-if="!todayPoll" class="flex items-center gap-2">
          <button
            @click="startPoll"
            :disabled="startingPoll"
            class="inline-flex items-center gap-2 bg-[#274eac] hover:bg-[#1f3f8d] text-white font-semibold px-3.5 py-1.5 rounded-xl text-xs shadow-xs transition active:scale-95 disabled:opacity-60 cursor-pointer"
          >
            <Loader2 v-if="startingPoll" class="w-3.5 h-3.5 animate-spin" />
            <Vote v-else class="w-3.5 h-3.5" />
            <span>Запустить голосование</span>
          </button>
        </div>
      </div>

      <!-- Состояние когда голосование запущено -->
      <div v-if="todayPoll && todayPoll.options.length" class="space-y-3">
        <!-- Баннер лидера, если есть голоса -->
        <div
          v-if="leadingOption && leadingOption.votesCount > 0"
          class="flex flex-col sm:flex-row sm:items-center justify-between gap-2.5 bg-amber-50/70 dark:bg-amber-950/30 border border-amber-200/60 dark:border-amber-900/40 rounded-xl px-3.5 py-2 text-xs sm:text-sm"
        >
          <div class="flex items-center gap-2 text-amber-900 dark:text-amber-200 font-medium">
            <span>🏆</span>
            <span>
              Лидирует: <strong>{{ leadingOption.restaurantName }}</strong> ({{ leadingOption.votesCount }} {{ getVotesWord(leadingOption.votesCount) }})
            </span>
          </div>
          <router-link
            :to="`/create-order?restaurantId=${leadingOption.restaurantId}`"
            class="inline-flex items-center gap-1.5 font-bold text-[#274eac] dark:text-blue-400 hover:text-[#1f3f8d] dark:hover:text-blue-300 transition shrink-0 text-xs"
          >
            <span>Создать заказ</span>
            <ArrowRight class="w-3.5 h-3.5" />
          </router-link>
        </div>

        <!-- Компактная сетка вариантов ресторанов -->
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2.5">
          <button
            v-for="option in todayPoll.options"
            :key="option.restaurantId"
            type="button"
            @click="vote(option.restaurantId)"
            :disabled="votingRestaurantId !== null"
            class="group relative overflow-hidden rounded-xl border p-2.5 sm:p-3 transition-all duration-150 cursor-pointer text-left select-none flex flex-col justify-between"
            :class="
              option.isMyVote
                ? 'bg-blue-50/60 dark:bg-blue-950/40 border-[#274eac] dark:border-blue-500 shadow-2xs ring-1 ring-[#274eac]/20 dark:ring-blue-500/20'
                : 'bg-white dark:bg-slate-900/90 hover:bg-slate-50 dark:hover:bg-slate-800/80 border-slate-200/80 dark:border-slate-800 hover:border-slate-300 dark:hover:border-slate-700'
            "
          >
            <!-- Верхняя строка: Чекбокс, Иконка ресторана, Название, Количество голосов -->
            <div class="flex items-center justify-between gap-2 w-full">
              <div class="flex items-center gap-2 min-w-0">
                <!-- Кастомный чекбокс множественного выбора -->
                <div
                  class="w-4 h-4 rounded-[4px] border flex items-center justify-center shrink-0 transition-colors"
                  :class="
                    option.isMyVote
                      ? 'bg-[#274eac] border-[#274eac] text-white'
                      : 'border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-800 group-hover:border-slate-400'
                  "
                >
                  <Loader2 v-if="votingRestaurantId === option.restaurantId" class="w-2.5 h-2.5 animate-spin text-[#274eac]" />
                  <Check v-else-if="option.isMyVote" class="w-3 h-3 stroke-[3]" />
                </div>

                <!-- Favicon ресторана -->
                <img
                  v-if="option.restaurantUrl"
                  :src="getFaviconUrl(option.restaurantUrl)"
                  :alt="option.restaurantName"
                  class="w-4 h-4 rounded object-contain shrink-0"
                  @error="($event.target as HTMLElement).style.display = 'none'"
                />

                <!-- Название ресторана -->
                <span
                  class="font-semibold text-slate-900 dark:text-slate-100 truncate text-xs sm:text-sm leading-tight"
                  :title="option.restaurantName"
                >
                  {{ option.restaurantName }}
                </span>
              </div>

              <!-- Процент и количество голосов -->
              <div
                class="flex items-center gap-1 shrink-0 text-xs font-bold"
                :class="option.isMyVote ? 'text-[#274eac] dark:text-blue-400' : 'text-slate-700 dark:text-slate-300'"
              >
                <span>{{ option.votesCount }}</span>
                <span class="text-[10px] font-normal text-slate-400">({{ option.percentage }}%)</span>
              </div>
            </div>

            <!-- Тонкий прогресс-бар -->
            <div class="w-full bg-slate-100 dark:bg-slate-800 h-1 rounded-full overflow-hidden mt-2">
              <div
                class="h-full rounded-full transition-all duration-300"
                :class="option.isMyVote ? 'bg-[#274eac] dark:bg-blue-500' : 'bg-slate-400/80 dark:bg-slate-600'"
                :style="{ width: `${option.percentage}%` }"
              ></div>
            </div>

            <!-- Нижняя строка: список проголосовавших (аватарки) -->
            <div class="flex items-center justify-between gap-1 mt-2 text-[11px]">
              <div class="flex items-center -space-x-1 overflow-hidden h-5">
                <template v-if="option.voters && option.voters.length">
                  <UserAvatar
                    v-for="voter in option.voters.slice(0, 5)"
                    :key="voter.id"
                    :src="voter.avatarUrl"
                    :name="voter.fullName"
                    size-class="w-5 h-5 text-[10px]"
                    border-class="border border-white dark:border-slate-900"
                    :title="voter.fullName"
                  />
                  <span
                    v-if="option.voters.length > 5"
                    class="w-5 h-5 rounded-full bg-slate-200 dark:bg-slate-700 text-slate-600 dark:text-slate-300 text-[9px] font-bold flex items-center justify-center border border-white dark:border-slate-900"
                  >
                    +{{ option.voters.length - 5 }}
                  </span>
                </template>
                <span v-else class="text-slate-400 text-[11px]">
                  Нет голосов
                </span>
              </div>

              <span
                v-if="option.isMyVote"
                class="text-[10px] font-bold text-[#274eac] dark:text-blue-400 uppercase tracking-wider"
              >
                Выбрано
              </span>
            </div>
          </button>
        </div>
      </div>

      <!-- Если голосование еще не запущено сегодня -->
      <div v-else-if="!todayPoll" class="text-center py-5">
        <p class="text-xs sm:text-sm text-slate-500 dark:text-slate-400 max-w-md mx-auto">
          Сегодня голосование еще не проводилось. Вы можете запустить его прямо сейчас, и бот уведомит команду в Telegram!
        </p>
      </div>
    </section>

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

    <!-- Секция недавних завершённых заказов (только 3 последних + переход ко всем) -->
    <section v-if="closedOrders && closedOrders.content.length" class="space-y-4 pt-4 border-t border-slate-200/80 dark:border-slate-800">
      <div class="flex items-center justify-between">
        <div>
          <h2 class="text-xl font-bold text-slate-900 dark:text-slate-100 tracking-tight">
            Недавние завершённые заказы
          </h2>
          <p class="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
            Последние выполненные заказы команды
          </p>
        </div>

        <router-link
          to="/orders/history"
          class="inline-flex items-center gap-1.5 text-xs sm:text-sm font-semibold text-[#274eac] dark:text-blue-400 hover:text-[#1f3f8d] dark:hover:text-blue-300 hover:underline transition"
        >
          <Archive class="w-4 h-4" />
          <span>Все заказы ({{ closedOrders.totalElements }})</span>
          <ArrowRight class="w-4 h-4" />
        </router-link>
      </div>

      <!-- Сетка 3 последних закрытых заказов -->
      <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
        <router-link
          v-for="order in recentClosedOrders"
          :key="order.id"
          :to="`/orders/${order.id}`"
          class="group bg-white hover:bg-slate-50/80 dark:bg-slate-900 dark:hover:bg-slate-800/80 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-4 shadow-2xs hover:shadow-xs hover:-translate-y-0.5 transition-all flex flex-col justify-between"
        >
          <div class="space-y-2">
            <div class="flex items-center justify-between gap-2">
              <div class="flex items-center gap-2 min-w-0">
                <img
                  v-if="order.restaurant.websiteUrl"
                  :src="getFaviconUrl(order.restaurant.websiteUrl)"
                  :alt="order.restaurant.name"
                  class="w-4 h-4 rounded object-contain shrink-0"
                  @error="($event.target as HTMLElement).style.display = 'none'"
                />
                <span class="font-bold text-slate-900 dark:text-slate-100 truncate text-sm group-hover:text-[#274eac] dark:group-hover:text-blue-400 transition">
                  {{ order.restaurant.name }}
                </span>
              </div>
              <span class="text-[11px] font-medium text-slate-400">
                #{{ order.id }}
              </span>
            </div>

            <div class="flex items-center gap-2 text-xs text-slate-500 dark:text-slate-400">
              <Clock class="w-3.5 h-3.5 text-slate-400" />
              <span>{{ formatDateTime(order.deadlineTime) }}</span>
            </div>

            <div class="flex items-center gap-2 text-xs text-slate-500 dark:text-slate-400">
              <UserAvatar
                :src="order.createdBy.avatarUrl"
                :name="order.createdBy.fullName"
                size-class="w-4 h-4"
              />
              <span class="truncate">{{ order.createdBy.fullName }}</span>
            </div>
          </div>

          <div class="mt-3 pt-3 border-t border-slate-100 dark:border-slate-800 flex items-center justify-between">
            <span class="text-sm font-bold text-slate-900 dark:text-slate-100">
              {{ formatPrice(order.totalAmount) }}
            </span>
            <span class="text-xs font-semibold text-[#274eac] dark:text-blue-400 group-hover:translate-x-0.5 transition-transform flex items-center gap-1">
              <span>Смотреть</span>
              <ArrowRight class="w-3 h-3" />
            </span>
          </div>
        </router-link>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, computed } from 'vue';
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
import type { UserStatisticResponse } from '@/types';
import {
  UtensilsCrossed,
  PlusCircle,
  Clock,
  ArrowRight,
  AlertCircle,
  Coins,
  RefreshCw,
  Loader2,
  ChevronDown,
  Vote,
  Check,
  Archive,
} from 'lucide-vue-next';

const orderStore = useOrderStore();
const authStore = useAuthStore();
const toastStore = useToastStore();

const { activeOrders, closedOrders, unpaidOrders, todayPoll, loading } = storeToRefs(orderStore);

const showUnpaidDetails = ref(false);
const votingRestaurantId = ref<number | null>(null);
const startingPoll = ref(false);

const recentClosedOrders = computed(() => closedOrders.value?.content.slice(0, 3) || []);

const leadingOption = computed(() => {
  if (!todayPoll.value || !todayPoll.value.options.length) return null;
  return todayPoll.value.options[0];
});

const userStats = ref<UserStatisticResponse>({
  totalSpentThisMonth: 0,
  totalOrdersThisMonth: 0,
  totalSpentAllTime: 0,
  totalOrdersAllTime: 0,
});

function getVotesWord(count: number): string {
  const rem10 = count % 10;
  const rem100 = count % 100;
  if (rem100 >= 11 && rem100 <= 19) return 'голосов';
  if (rem10 === 1) return 'голос';
  if (rem10 >= 2 && rem10 <= 4) return 'голоса';
  return 'голосов';
}

async function vote(restaurantId: number) {
  votingRestaurantId.value = restaurantId;
  try {
    await orderStore.votePoll(restaurantId);
  } catch (e: any) {
    toastStore.error(e.response?.data?.message || 'Ошибка при голосовании');
  } finally {
    votingRestaurantId.value = null;
  }
}

async function startPoll() {
  startingPoll.value = true;
  try {
    await orderStore.startTodayPoll();
    toastStore.success('Голосование запущено! Оповещение отправлено в Telegram.');
  } catch (e: any) {
    toastStore.error(e.response?.data?.message || 'Не удалось запустить голосование');
  } finally {
    startingPoll.value = false;
  }
}

async function loadData() {
  await Promise.all([
    orderStore.fetchActiveOrders(),
    orderStore.fetchClosedOrders(0, 3),
    orderStore.fetchTodayPoll(),
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

onMounted(() => {
  loadData();
});
</script>
