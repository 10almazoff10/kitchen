package ru.prokin.kitchen.service.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import ru.prokin.kitchen.dto.MenuItemOptionCategoryDto;
import ru.prokin.kitchen.dto.MenuItemOptionDto;
import ru.prokin.kitchen.dto.MenuItemVariantDto;
import ru.prokin.kitchen.dto.ParsedMenuItemDto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Базовый провайдер меню для сайтов на платформе FoodPicasso (Pizz Burg, Шампур и др.).
 */
public abstract class AbstractFoodPicassoMenuProvider implements RestaurantMenuProvider {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    protected static final Duration MENU_CACHE_TTL = Duration.ofHours(1);

    private static final Pattern MODIFIER_DICT_PATTERN = Pattern.compile(
            "<script[^>]*data-modifier-dictionary[^>]*>(.*?)</script>", Pattern.DOTALL);

    private static final Pattern SECTIONS_PATTERN = Pattern.compile(
            "<div[^>]*class=[\"'][^\"']*sect nav__anchor[^\"']*[\"'][^>]*data-id=[\"'](\\d+)[\"'][^>]*>(.*?)(?=(?:<div[^>]*class=[\"'][^\"']*sect nav__anchor|\\Z))", Pattern.DOTALL);

    private static final Pattern SECTION_TITLE_PATTERN = Pattern.compile(
            "<h2[^>]*class=[\"']sect__title[\"']>([^<]+)</h2>");

    private static final Pattern DISHES_PATTERN = Pattern.compile(
            "<div[^>]*class=[\"'][^\"']*ddish [^\"']*[\"'][^>]*data-id=[\"'](\\d+)[\"'](.*?)<div class=[\"']ddish__plus[\"']", Pattern.DOTALL);

    private static final Pattern DISH_NAME_PATTERN = Pattern.compile(
            "<a[^>]*class=[\"']ddish__link[\"']>([^<]+)</a>");

    private static final Pattern DISH_PRICE_PATTERN = Pattern.compile(
            "data-price=[\"'](\\d+)[\"']");

    private static final Pattern DISH_DESC_PATTERN = Pattern.compile(
            "<div[^>]*class=[\"']ddish__ingredients[\"']>([^<]+)</div>");

    private static final Pattern DISH_IMG_PATTERN = Pattern.compile(
            "data-src=[\"']([^\"']+)[\"']");

    private static final Pattern DISH_SIZE_PATTERN = Pattern.compile(
            "<span[^>]*class=[\"']ddish__size[\"'][^>]*>([^<]+)</span>");

    private static final Pattern DISH_MODIFIERS_PATTERN = Pattern.compile(
            "data-modifier-refs=[\"']([^\"']+)[\"']");

    protected final RestClient restClient;
    protected final ObjectMapper objectMapper;
    protected final String defaultBaseUrl;
    protected final String externalIdPrefix;

    protected final Map<String, CachedMenu> menuCache = new ConcurrentHashMap<>();

    protected record CachedMenu(List<ParsedMenuItemDto> items, Instant expiry) {}

    protected AbstractFoodPicassoMenuProvider(ObjectMapper objectMapper, String defaultBaseUrl, String externalIdPrefix) {
        this.objectMapper = objectMapper;
        this.defaultBaseUrl = defaultBaseUrl;
        this.externalIdPrefix = externalIdPrefix;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(15000);

        this.restClient = RestClient.builder()
                .baseUrl(defaultBaseUrl)
                .requestFactory(requestFactory)
                .defaultHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .defaultHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .defaultHeader("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7")
                .build();
    }

    @Override
    public List<ParsedMenuItemDto> getMenu(String url) {
        CachedMenu cached = menuCache.get(defaultBaseUrl);
        if (cached != null && Instant.now().isBefore(cached.expiry())) {
            logger.debug("Returning cached menu for {} ({} items)", defaultBaseUrl, cached.items().size());
            return cached.items();
        }

        try {
            logger.info("Fetching {} menu from {}", getClass().getSimpleName(), defaultBaseUrl);
            String html = restClient.get()
                    .uri("/")
                    .retrieve()
                    .body(String.class);

            if (!StringUtils.hasText(html)) {
                logger.warn("Received empty HTML from {}", defaultBaseUrl);
                return Collections.emptyList();
            }

            List<ParsedMenuItemDto> items = parseHtml(html);
            if (!items.isEmpty()) {
                menuCache.put(defaultBaseUrl, new CachedMenu(items, Instant.now().plus(MENU_CACHE_TTL)));
                logger.info("Successfully parsed and cached {} items from {}", items.size(), defaultBaseUrl);
            }

            return items;
        } catch (Exception e) {
            logger.error("Failed to fetch/parse {} menu: {}", getClass().getSimpleName(), e.getMessage(), e);
            if (cached != null) {
                logger.warn("Returning stale cached menu for {} due to error", defaultBaseUrl);
                return cached.items();
            }
            return Collections.emptyList();
        }
    }

    /**
     * Парсит HTML-страницу со структурой FoodPicasso и извлекает блюда с категориями и модификаторами.
     */
    public List<ParsedMenuItemDto> parseHtml(String html) {
        if (!StringUtils.hasText(html)) {
            return Collections.emptyList();
        }

        // 1. Извлекаем словарь модификаторов
        JsonNode modifierDict = extractModifierDictionary(html);

        // 2. Извлекаем разделы и блюда
        List<ParsedMenuItemDto> result = new ArrayList<>();
        Matcher sectionMatcher = SECTIONS_PATTERN.matcher(html);

        while (sectionMatcher.find()) {
            String sectionContent = sectionMatcher.group(2);

            Matcher titleMatcher = SECTION_TITLE_PATTERN.matcher(sectionContent);
            String category = titleMatcher.find() ? cleanText(titleMatcher.group(1)) : "Общее";

            Matcher dishMatcher = DISHES_PATTERN.matcher(sectionContent);
            while (dishMatcher.find()) {
                String dishId = dishMatcher.group(1);
                String dishBlock = dishMatcher.group(2);

                ParsedMenuItemDto item = parseDish(dishId, dishBlock, dishMatcher.group(0), category, modifierDict);
                if (item != null) {
                    result.add(item);
                }
            }
        }

        return result;
    }

    protected JsonNode extractModifierDictionary(String html) {
        Matcher matcher = MODIFIER_DICT_PATTERN.matcher(html);
        if (matcher.find()) {
            String json = matcher.group(1).trim();
            try {
                return objectMapper.readTree(json);
            } catch (Exception e) {
                logger.warn("Failed to parse modifier dictionary JSON from {}: {}", defaultBaseUrl, e.getMessage());
            }
        }
        return null;
    }

    protected ParsedMenuItemDto parseDish(String dishId, String dishBlock, String fullMatch, String category, JsonNode modifierDict) {
        Matcher nameMatcher = DISH_NAME_PATTERN.matcher(dishBlock);
        if (!nameMatcher.find()) {
            return null;
        }
        String name = cleanText(nameMatcher.group(1));

        Matcher priceMatcher = DISH_PRICE_PATTERN.matcher(dishBlock);
        BigDecimal price = BigDecimal.ZERO;
        if (priceMatcher.find()) {
            try {
                price = new BigDecimal(priceMatcher.group(1));
            } catch (Exception ignored) {}
        }

        Matcher descMatcher = DISH_DESC_PATTERN.matcher(dishBlock);
        String description = descMatcher.find() ? cleanText(descMatcher.group(1)) : null;

        Matcher imgMatcher = DISH_IMG_PATTERN.matcher(dishBlock);
        String imageUrl = imgMatcher.find() ? imgMatcher.group(1) : null;

        Matcher sizeMatcher = DISH_SIZE_PATTERN.matcher(dishBlock);
        String weight = sizeMatcher.find() ? cleanText(sizeMatcher.group(1)) : null;
        if (!StringUtils.hasText(weight)) {
            weight = null;
        }

        // Извлекаем ключи модификаторов
        Matcher modMatcher = DISH_MODIFIERS_PATTERN.matcher(fullMatch);
        List<String> modifierRefs = Collections.emptyList();
        if (modMatcher.find()) {
            modifierRefs = Arrays.stream(modMatcher.group(1).split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .toList();
        }

        List<MenuItemVariantDto> variants = new ArrayList<>();
        List<MenuItemOptionCategoryDto> optionCategories = new ArrayList<>();

        if (modifierDict != null && !modifierRefs.isEmpty()) {
            for (String ref : modifierRefs) {
                JsonNode modNode = modifierDict.get(ref);
                if (modNode == null || !modNode.isObject()) {
                    continue;
                }

                String type = modNode.path("type").asText("");
                String title = cleanText(modNode.path("title").asText(""));
                JsonNode optionsNode = modNode.path("options");

                if (!optionsNode.isArray() || optionsNode.isEmpty()) {
                    continue;
                }

                int min = modNode.path("min").asInt(0);
                int max = modNode.path("max").asInt(optionsNode.size());
                boolean required = modNode.path("required").asBoolean(false);
                if (required && min == 0) {
                    min = 1;
                }

                // Определяем, является ли группа выбором размеров/порций (вариантов блюда):
                // 1) Стандартный тип 'sizes' с > 1 опцией (например, пицца 25см, 30см, 35см)
                // 2) Обязательный одиночный выбор (min == 1 && max == 1) с > 1 опцией, где опции имеют стоимость (например, 'Вес Свинина шея-1: 200гр, 300гр...')
                boolean isSizesGroup = "sizes".equalsIgnoreCase(type) && optionsNode.size() > 1;
                boolean isPortionWeightGroup = min == 1 && max == 1 && optionsNode.size() > 1 && anyOptionHasPositivePrice(optionsNode);

                if (isSizesGroup || isPortionWeightGroup) {
                    for (int i = 0; i < optionsNode.size(); i++) {
                        JsonNode opt = optionsNode.get(i);
                        String optName = cleanText(opt.path("name").asText(""));
                        BigDecimal variantPrice = extractOptionPrice(opt);
                        if (variantPrice.compareTo(BigDecimal.ZERO) <= 0) {
                            variantPrice = price;
                        }

                        variants.add(MenuItemVariantDto.builder()
                                .id(dishId + "_" + ref + "_" + i)
                                .name(StringUtils.hasText(optName) ? optName : ("Вариант " + (i + 1)))
                                .price(variantPrice)
                                .isDefault(i == 0)
                                .build());
                    }
                } else if (!"sizes".equalsIgnoreCase(type)) {
                    // Группы опций (добавки, соусы, исключение ингредиентов, подача и т.д.)
                    List<MenuItemOptionDto> options = new ArrayList<>();
                    for (int i = 0; i < optionsNode.size(); i++) {
                        JsonNode opt = optionsNode.get(i);
                        String optName = cleanText(opt.path("name").asText(""));
                        if (!StringUtils.hasText(optName)) {
                            continue;
                        }

                        BigDecimal optPrice = extractOptionPrice(opt);
                        String optId = opt.has("id") ? opt.path("id").asText() : (ref + "_" + i);

                        options.add(MenuItemOptionDto.builder()
                                .id(optId)
                                .name(optName)
                                .price(optPrice)
                                .isDefault(false)
                                .build());
                    }

                    if (!options.isEmpty()) {
                        optionCategories.add(MenuItemOptionCategoryDto.builder()
                                .id(ref)
                                .name(StringUtils.hasText(title) ? title : "Опции")
                                .min(min)
                                .max(max)
                                .options(options)
                                .build());
                    }
                }
            }
        }

        // Если есть варианты, обновляем базовую цену ценой варианта по умолчанию
        if (!variants.isEmpty() && variants.get(0).getPrice() != null && variants.get(0).getPrice().compareTo(BigDecimal.ZERO) > 0) {
            price = variants.get(0).getPrice();
        }

        return ParsedMenuItemDto.builder()
                .externalId(externalIdPrefix + "_" + dishId)
                .title(name)
                .price(price)
                .description(description)
                .imageUrl(imageUrl)
                .category(category)
                .weight(weight)
                .variants(variants.isEmpty() ? null : variants)
                .optionCategories(optionCategories.isEmpty() ? null : optionCategories)
                .build();
    }

    private boolean anyOptionHasPositivePrice(JsonNode optionsNode) {
        for (JsonNode opt : optionsNode) {
            if (extractOptionPrice(opt).compareTo(BigDecimal.ZERO) > 0) {
                return true;
            }
        }
        return false;
    }

    private BigDecimal extractOptionPrice(JsonNode opt) {
        long rawPrice = opt.path("price").asLong(0);
        if (rawPrice <= 0 && opt.has("prices") && opt.path("prices").isArray() && !opt.path("prices").isEmpty()) {
            rawPrice = opt.path("prices").get(0).path("price").asLong(0);
        }
        return parseInternalPrice(rawPrice);
    }

    /**
     * Цены в JSON FoodPicasso хранятся в формате 1/10000 рубля (например, 3500000 = 350.00 руб).
     */
    private BigDecimal parseInternalPrice(long rawPrice) {
        if (rawPrice <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(rawPrice)
                .divide(BigDecimal.valueOf(10000), 2, RoundingMode.HALF_UP);
    }

    protected String cleanText(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&nbsp;", " ")
                .replace("\u00a0", " ")
                .trim();
    }
}
