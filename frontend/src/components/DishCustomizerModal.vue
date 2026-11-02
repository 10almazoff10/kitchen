<script setup lang="ts">
import { ref, computed, watch } from 'vue';
import type { ParsedMenuItemDto, MenuItemVariantDto, MenuItemOptionCategoryDto, MenuItemOptionDto } from '@/types';
import { X, Check, UtensilsCrossed, Plus } from 'lucide-vue-next';

const props = defineProps<{
  isOpen: boolean;
  dish: ParsedMenuItemDto | null;
}>();

const emit = defineEmits<{
  (e: 'close'): void;
  (e: 'add', item: { itemDescription: string; price: number; externalData?: string }): void;
}>();

const selectedVariant = ref<MenuItemVariantDto | null>(null);
const selectedOptions = ref<Record<string, string[]>>({});

function isCategorySingleChoice(category: MenuItemOptionCategoryDto): boolean {
  if (category.max === 1) return true;
  const name = category.name.toLowerCase();
  if (name.includes('соус')) return true;
  const defaultCount = category.options.filter(o => o.isDefault).length;
  if (defaultCount === 1 && category.options.length > 1 && (!category.max || category.max === 1)) {
    return true;
  }
  return false;
}

watch(
  () => props.dish,
  (newDish) => {
    if (!newDish) {
      selectedVariant.value = null;
      selectedOptions.value = {};
      return;
    }

    if (newDish.variants && newDish.variants.length > 0) {
      selectedVariant.value = newDish.variants.find(v => v.isDefault) || newDish.variants[0];
    } else {
      selectedVariant.value = null;
    }

    const initialOpts: Record<string, string[]> = {};
    if (newDish.optionCategories) {
      for (const cat of newDish.optionCategories) {
        const catKey = cat.id || cat.name;
        const isSingle = isCategorySingleChoice(cat);
        const defaults = cat.options.filter(o => o.isDefault).map(o => o.id || o.name);

        if (defaults.length > 0) {
          initialOpts[catKey] = isSingle ? [defaults[0]] : defaults;
        } else if (cat.min && cat.min > 0 && cat.options.length > 0 && isSingle) {
          initialOpts[catKey] = [cat.options[0].id || cat.options[0].name];
        } else {
          initialOpts[catKey] = [];
        }
      }
    }
    selectedOptions.value = initialOpts;
  },
  { immediate: true }
);

function selectVariant(v: MenuItemVariantDto) {
  selectedVariant.value = v;
}

function toggleOption(category: MenuItemOptionCategoryDto, option: MenuItemOptionDto) {
  const catKey = category.id || category.name;
  const optKey = option.id || option.name;
  const isSingle = isCategorySingleChoice(category);
  const current = selectedOptions.value[catKey] || [];

  if (isSingle) {
    if (current.includes(optKey)) {
      if (!category.min || category.min < 1) {
        selectedOptions.value[catKey] = [];
      }
    } else {
      selectedOptions.value[catKey] = [optKey];
    }
  } else {
    if (current.includes(optKey)) {
      selectedOptions.value[catKey] = current.filter(id => id !== optKey);
    } else {
      if (category.max && current.length >= category.max) {
        return;
      }
      selectedOptions.value[catKey] = [...current, optKey];
    }
  }
}

function isOptionSelected(category: MenuItemOptionCategoryDto, option: MenuItemOptionDto): boolean {
  const catKey = category.id || category.name;
  const optKey = option.id || option.name;
  return (selectedOptions.value[catKey] || []).includes(optKey);
}

const totalPrice = computed(() => {
  if (!props.dish) return 0;
  let sum = selectedVariant.value ? selectedVariant.value.price : (props.dish.price || 0);

  if (props.dish.optionCategories) {
    for (const cat of props.dish.optionCategories) {
      const catKey = cat.id || cat.name;
      const chosen = selectedOptions.value[catKey] || [];
      for (const opt of cat.options) {
        const optKey = opt.id || opt.name;
        if (chosen.includes(optKey) && opt.price) {
          sum += opt.price;
        }
      }
    }
  }
  return sum;
});

const formattedDescription = computed(() => {
  if (!props.dish) return '';
  const rawName = (props.dish.title || props.dish.name).trim();
  const category = (props.dish.category || '').trim();

  // Добавляем тип блюда в начало: "Шаурма - С Капусткой квашеной"
  let baseName = rawName;
  if (category && !rawName.toLowerCase().startsWith(category.toLowerCase())) {
    baseName = `${category} - ${rawName}`;
  }

  const tags: string[] = [];

  if (selectedVariant.value) {
    let vText = selectedVariant.value.name;
    if (selectedVariant.value.weight && !vText.includes(selectedVariant.value.weight)) {
      vText += ` (${selectedVariant.value.weight})`;
    }
    tags.push(vText);
  }

  if (props.dish.optionCategories) {
    for (const cat of props.dish.optionCategories) {
      const catKey = cat.id || cat.name;
      const chosenKeys = selectedOptions.value[catKey] || [];
      if (!chosenKeys.length) continue;

      const chosenOptions = cat.options.filter(o => chosenKeys.includes(o.id || o.name));
      const catLower = cat.name.toLowerCase();

      if (catLower.includes('соус')) {
        const sauceNames = chosenOptions.map(o => o.name).join(', ');
        tags.push(`соус: ${sauceNames}`);
      } else if (catLower.includes('убрать') || catLower.includes('исключ')) {
        const removeNames = chosenOptions.map(o => o.name).join(', ');
        tags.push(`без: ${removeNames}`);
      } else if (catLower.includes('добав') || chosenOptions.some(o => o.price > 0)) {
        for (const opt of chosenOptions) {
          tags.push(`+${opt.name}`);
        }
      } else {
        for (const opt of chosenOptions) {
          tags.push(opt.name);
        }
      }
    }
  }

  if (tags.length > 0) {
    return `${baseName}, ${tags.join(', ')}`;
  }
  return baseName;
});

function confirmAdd() {
  let externalData: string | undefined = undefined;
  if (props.dish?.rawData) {
    const selectedOptionsList: any[] = [];
    if (props.dish.optionCategories) {
      for (const cat of props.dish.optionCategories) {
        const catKey = cat.id || cat.name;
        const chosenKeys = selectedOptions.value[catKey] || [];
        for (const opt of cat.options) {
          const optKey = opt.id || opt.name;
          if (chosenKeys.includes(optKey)) {
            selectedOptionsList.push({
              optionCategoryId: cat.id,
              isProductVariant: false,
              name: opt.name,
              price: opt.price,
            });
          }
        }
      }
    }

    const basePrice = selectedVariant.value ? selectedVariant.value.price : (props.dish.price || 0);
    const grilnicaCartItem = {
      product: props.dish.rawData,
      count: 1,
      price: basePrice,
      allOptionPrice: totalPrice.value - basePrice,
      fullPrice: totalPrice.value,
      maxCountInOrder: 50,
      productId: null,
      productVariantId: selectedVariant.value?.id || null,
      selectedOptions: selectedOptionsList,
      selectedProductId: typeof crypto !== 'undefined' && crypto.randomUUID ? crypto.randomUUID() : Math.random().toString(36).substring(2),
    };
    externalData = JSON.stringify(grilnicaCartItem);
  }

  emit('add', {
    itemDescription: formattedDescription.value,
    price: totalPrice.value,
    externalData,
  });
}
</script>

<template>
  <div
    v-if="isOpen && dish"
    class="fixed inset-0 z-60 flex items-center justify-center p-3 sm:p-6 bg-slate-950/65 backdrop-blur-xs animate-in fade-in duration-150"
    @click.self="emit('close')"
  >
    <div
      class="bg-white dark:bg-slate-900 rounded-3xl max-w-lg w-full max-h-[92vh] flex flex-col shadow-2xl border border-slate-200 dark:border-slate-800 animate-in zoom-in-95 duration-150 overflow-hidden"
    >
      <!-- Шапка с превью блюда -->
      <div class="relative p-5 sm:p-6 border-b border-slate-100 dark:border-slate-800 flex gap-4 items-start">
        <div class="w-20 h-20 rounded-2xl overflow-hidden bg-slate-100 dark:bg-slate-800 shrink-0 border border-slate-200/60 dark:border-slate-700/60">
          <img
            v-if="dish.imageUrl"
            :src="dish.imageUrl"
            :alt="dish.title || dish.name"
            class="w-full h-full object-cover"
          />
          <div v-else class="w-full h-full flex items-center justify-center text-slate-300 dark:text-slate-600">
            <UtensilsCrossed class="w-8 h-8" />
          </div>
        </div>

        <div class="min-w-0 flex-1 pr-6">
          <span v-if="dish.category" class="text-xs font-semibold uppercase tracking-wider text-[#274eac] dark:text-blue-400">
            {{ dish.category }}
          </span>
          <h3 class="text-lg font-bold text-slate-900 dark:text-slate-100 leading-snug">
            {{ dish.title || dish.name }}
          </h3>
          <p v-if="dish.description" class="text-xs text-slate-500 dark:text-slate-400 line-clamp-2 mt-1">
            {{ dish.description }}
          </p>
        </div>

        <button
          @click="emit('close')"
          class="absolute top-4 right-4 p-2 rounded-xl text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition cursor-pointer"
        >
          <X class="w-5 h-5" />
        </button>
      </div>

      <!-- Контейнер настроек блюда -->
      <div class="p-5 sm:p-6 overflow-y-auto flex-1 space-y-6 divide-y divide-slate-100 dark:divide-slate-800/80">
        <!-- 1. Варианты / Размер / Порция -->
        <div v-if="dish.variants && dish.variants.length > 0" class="space-y-3">
          <div class="flex items-center justify-between">
            <h4 class="text-sm font-bold text-slate-900 dark:text-slate-100">
              Размер / Порция
            </h4>
            <span class="text-xs font-medium text-slate-400">Обязательно</span>
          </div>

          <div class="grid grid-cols-2 sm:grid-cols-3 gap-2.5">
            <button
              v-for="variant in dish.variants"
              :key="variant.id || variant.name"
              type="button"
              @click="selectVariant(variant)"
              class="p-3 rounded-2xl border text-left transition cursor-pointer flex flex-col justify-between gap-1.5"
              :class="
                selectedVariant?.id === variant.id || selectedVariant?.name === variant.name
                  ? 'border-[#274eac] bg-[#eff4fe] dark:bg-blue-950/40 dark:border-blue-700 ring-2 ring-[#274eac]/20'
                  : 'border-slate-200 dark:border-slate-800 hover:border-slate-300 dark:hover:border-slate-700 bg-slate-50/50 dark:bg-slate-800/40'
              "
            >
              <div class="flex items-center justify-between">
                <span class="text-xs font-bold text-slate-900 dark:text-slate-100">
                  {{ variant.name }}
                </span>
                <span
                  v-if="selectedVariant?.id === variant.id || selectedVariant?.name === variant.name"
                  class="w-4 h-4 rounded-full bg-[#274eac] text-white flex items-center justify-center text-[10px]"
                >
                  <Check class="w-2.5 h-2.5" />
                </span>
              </div>
              <div class="flex items-center justify-between text-xs">
                <span v-if="variant.weight" class="text-slate-400 text-[11px]">{{ variant.weight }}</span>
                <span class="font-mono font-bold text-[#274eac] dark:text-blue-400 ml-auto">{{ variant.price }} ₽</span>
              </div>
            </button>
          </div>
        </div>

        <!-- 2. Категории опций (Соус, Добавки, Убрать и др.) -->
        <div
          v-for="category in dish.optionCategories"
          :key="category.id || category.name"
          class="pt-5 space-y-3"
        >
          <div class="flex items-center justify-between">
            <div class="flex items-center gap-2">
              <h4 class="text-sm font-bold text-slate-900 dark:text-slate-100">
                {{ category.name }}
              </h4>
              <span
                v-if="category.min && category.min > 0"
                class="px-1.5 py-0.5 rounded text-[10px] font-semibold bg-rose-50 text-rose-600 dark:bg-rose-950/40 dark:text-rose-400"
              >
                Обязательно
              </span>
            </div>
            <span class="text-xs text-slate-400">
              <template v-if="isCategorySingleChoice(category)">Выберите 1</template>
              <template v-else-if="category.max">До {{ category.max }}</template>
              <template v-else>Несколько</template>
            </span>
          </div>

          <!-- Опции категории -->
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-2">
            <button
              v-for="option in category.options"
              :key="option.id || option.name"
              type="button"
              @click="toggleOption(category, option)"
              class="px-3.5 py-2.5 rounded-xl border text-left transition cursor-pointer flex items-center justify-between gap-2.5 group"
              :class="
                isOptionSelected(category, option)
                  ? 'border-[#274eac] bg-[#eff4fe] dark:bg-blue-950/40 dark:border-blue-700 ring-1 ring-[#274eac]/20'
                  : 'border-slate-200 dark:border-slate-800 hover:border-slate-300 dark:hover:border-slate-700 bg-white dark:bg-slate-800/40'
              "
            >
              <div class="flex items-center gap-2.5 min-w-0">
                <!-- Индикатор выбора (радио или чекбокс) -->
                <div
                  v-if="isCategorySingleChoice(category)"
                  class="w-4 h-4 rounded-full border flex items-center justify-center shrink-0 transition"
                  :class="
                    isOptionSelected(category, option)
                      ? 'border-[#274eac] bg-[#274eac] text-white'
                      : 'border-slate-300 dark:border-slate-600'
                  "
                >
                  <div
                    v-if="isOptionSelected(category, option)"
                    class="w-1.5 h-1.5 rounded-full bg-white"
                  ></div>
                </div>
                <div
                  v-else
                  class="w-4 h-4 rounded-md border flex items-center justify-center shrink-0 transition text-white"
                  :class="
                    isOptionSelected(category, option)
                      ? 'border-[#274eac] bg-[#274eac]'
                      : 'border-slate-300 dark:border-slate-600'
                  "
                >
                  <Check v-if="isOptionSelected(category, option)" class="w-3 h-3" />
                </div>

                <span
                  class="text-xs font-medium text-slate-800 dark:text-slate-200 truncate group-hover:text-slate-900 dark:group-hover:text-white"
                >
                  {{ option.name }}
                </span>
              </div>

              <!-- Стоимость опции -->
              <span
                class="text-xs font-mono font-semibold shrink-0"
                :class="
                  option.price > 0
                    ? 'text-[#274eac] dark:text-blue-400'
                    : 'text-slate-400 dark:text-slate-500'
                "
              >
                {{ option.price > 0 ? `+${option.price} ₽` : 'Бесплатно' }}
              </span>
            </button>
          </div>
        </div>
      </div>

      <!-- Нижняя панель с итогом и кнопкой добавления -->
      <div class="p-5 sm:p-6 border-t border-slate-100 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-900/60 space-y-3">
        <!-- Превью итогового названия -->
        <div class="text-xs text-slate-500 dark:text-slate-400 line-clamp-2">
          <span class="font-semibold text-slate-700 dark:text-slate-300">В заказ: </span>
          <span>{{ formattedDescription }}</span>
        </div>

        <div class="flex items-center justify-between gap-4">
          <div>
            <div class="text-[11px] font-semibold uppercase tracking-wider text-slate-400">Итоговая стоимость</div>
            <div class="text-2xl font-black font-mono text-slate-900 dark:text-slate-100">
              {{ totalPrice }} ₽
            </div>
          </div>

          <button
            type="button"
            @click="confirmAdd"
            class="px-6 py-3 rounded-2xl bg-[#274eac] hover:bg-[#1f3f8d] text-white font-semibold text-sm shadow-md shadow-[#274eac]/20 transition active:scale-95 cursor-pointer flex items-center gap-2"
          >
            <Plus class="w-4 h-4" />
            <span>Добавить в заказ</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
