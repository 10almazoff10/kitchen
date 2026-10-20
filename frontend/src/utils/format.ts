export function formatPrice(price?: number | null): string {
  if (price === undefined || price === null) return '0 ₽';
  return new Intl.NumberFormat('ru-RU', {
    style: 'currency',
    currency: 'RUB',
    maximumFractionDigits: 0,
  }).format(price);
}

export function formatDateTime(dateStr?: string | null): string {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  return new Intl.DateTimeFormat('ru-RU', {
    timeZone: 'Asia/Barnaul',
    day: 'numeric',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
}

export function formatTime(dateStr?: string | null): string {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  return new Intl.DateTimeFormat('ru-RU', {
    timeZone: 'Asia/Barnaul',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
}

export function isDeadlinePassed(deadlineStr?: string | null): boolean {
  if (!deadlineStr) return false;
  return new Date(deadlineStr).getTime() < Date.now();
}

export function formatTimeRemaining(deadlineStr?: string | null): string {
  if (!deadlineStr) return '';
  const diffMs = new Date(deadlineStr).getTime() - Date.now();
  if (diffMs <= 0) return 'Дедлайн прошёл';
  const mins = Math.floor(diffMs / 60000);
  if (mins < 60) return `Осталось ${mins} мин`;
  const hours = Math.floor(mins / 60);
  const remMins = mins % 60;
  if (remMins === 0) return `Осталось ${hours} ч`;
  return `Осталось ${hours} ч ${remMins} мин`;
}

export function getFaviconUrl(url?: string | null): string {
  if (!url) return '';
  try {
    let clean = url.trim();
    if (!clean.startsWith('http://') && !clean.startsWith('https://')) {
      clean = 'https://' + clean;
    }
    const host = new URL(clean).hostname;
    if (!host) return '';
    return `https://t3.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=${encodeURIComponent('http://' + host)}&size=64`;
  } catch {
    return '';
  }
}
