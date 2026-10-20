<template>
  <div
    class="relative shrink-0 flex items-center justify-center overflow-hidden select-none font-semibold text-center"
    :class="[sizeClass, roundedClass, borderClass, bgFallbackColor]"
    :title="name || 'Пользователь'"
  >
    <!-- Fallback инициалы (показываются пока картинка грузится или если произошла ошибка) -->
    <span
      v-if="!loaded || hasError"
      class="text-xs uppercase tracking-tight font-bold"
      :class="textColor"
    >
      {{ initials }}
    </span>

    <!-- Gravatar изображение -->
    <img
      v-if="avatarSrc && !hasError"
      :src="avatarSrc"
      :alt="name || 'Аватар'"
      class="absolute inset-0 w-full h-full object-cover transition-opacity duration-300"
      :class="[loaded ? 'opacity-100' : 'opacity-0']"
      loading="lazy"
      @load="handleLoad"
      @error="handleError"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue';

const props = withDefaults(
  defineProps<{
    src?: string | null;
    name?: string | null;
    sizeClass?: string;
    roundedClass?: string;
    borderClass?: string;
  }>(),
  {
    src: null,
    name: '',
    sizeClass: 'w-8 h-8',
    roundedClass: 'rounded-full',
    borderClass: '',
  }
);

const loaded = ref(false);
const hasError = ref(false);

const avatarSrc = computed(() => props.src || null);

watch(avatarSrc, () => {
  loaded.value = false;
  hasError.value = false;
});

function handleLoad() {
  loaded.value = true;
  hasError.value = false;
}

function handleError() {
  hasError.value = true;
  loaded.value = false;
}

// Вычисление инициалов из имени (например "Олег Прокин" -> "ОП", "makovskiy.v" -> "М")
const initials = computed(() => {
  if (!props.name || !props.name.trim()) return '?';
  const clean = props.name.trim();
  const parts = clean.split(/[\s._-]+/).filter(Boolean);
  if (parts.length >= 2) {
    const first = parts[0][0];
    const second = parts[1][0];
    return (first + second).toUpperCase();
  }
  return clean.slice(0, 2).toUpperCase();
});

// Пул приятных цветов для fallback инициалов на основе хеша имени
const colorPalettes = [
  { bg: 'bg-blue-100 dark:bg-blue-950/70', text: 'text-blue-700 dark:text-blue-300' },
  { bg: 'bg-indigo-100 dark:bg-indigo-950/70', text: 'text-indigo-700 dark:text-indigo-300' },
  { bg: 'bg-emerald-100 dark:bg-emerald-950/70', text: 'text-emerald-700 dark:text-emerald-300' },
  { bg: 'bg-teal-100 dark:bg-teal-950/70', text: 'text-teal-700 dark:text-teal-300' },
  { bg: 'bg-amber-100 dark:bg-amber-950/70', text: 'text-amber-800 dark:text-amber-300' },
  { bg: 'bg-rose-100 dark:bg-rose-950/70', text: 'text-rose-700 dark:text-rose-300' },
  { bg: 'bg-purple-100 dark:bg-purple-950/70', text: 'text-purple-700 dark:text-purple-300' },
  { bg: 'bg-cyan-100 dark:bg-cyan-950/70', text: 'text-cyan-700 dark:text-cyan-300' },
];

const paletteIndex = computed(() => {
  const str = props.name || 'user';
  let hash = 0;
  for (let i = 0; i < str.length; i++) {
    hash = (hash << 5) - hash + str.charCodeAt(i);
    hash |= 0;
  }
  return Math.abs(hash) % colorPalettes.length;
});

const bgFallbackColor = computed(() => colorPalettes[paletteIndex.value].bg);
const textColor = computed(() => colorPalettes[paletteIndex.value].text);
</script>
