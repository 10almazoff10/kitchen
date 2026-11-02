package ru.prokin.kitchen.service.provider;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import ru.prokin.kitchen.dto.ParsedMenuItemDto;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GrilnicaMenuProvider implements RestaurantMenuProvider {

    private static final Logger logger = LoggerFactory.getLogger(GrilnicaMenuProvider.class);

    private static final String API_BASE_URL = "https://api.grilnica.ru/store/api";
    private static final String CDN_BASE_URL = "https://grilnica.ru/cdn";
    private static final String DEFAULT_CITY_ID = "ba169397-f746-4451-a719-797d8ffa2d67"; // Барнаул

    private static final Duration CITIES_CACHE_TTL = Duration.ofHours(24);
    private static final Duration MENU_CACHE_TTL = Duration.ofHours(1);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    // Кэш городов: alias -> cityId
    private volatile Map<String, String> cityAliasToIdCache = new HashMap<>();
    private volatile Instant citiesCacheExpiry = Instant.MIN;

    // Кэш меню: cityId -> CachedMenu
    private final Map<String, CachedMenu> menuCache = new ConcurrentHashMap<>();

    private record CachedMenu(List<ParsedMenuItemDto> items, Instant expiry) {}

    public GrilnicaMenuProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(15000);

        this.restClient = RestClient.builder()
                .baseUrl(API_BASE_URL)
                .requestFactory(requestFactory)
                .defaultHeader("User-Agent", "Kitchen/1.0 (Food Order Service)")
                .defaultHeader("Accept", "application/json")
                .build();

        // Инициализируем резервный словарь городов
        initFallbackCities();
    }

    private void initFallbackCities() {
        Map<String, String> fallbacks = new HashMap<>();
        fallbacks.put("barnaul", "ba169397-f746-4451-a719-797d8ffa2d67");
        fallbacks.put("novosibirsk", "08432584-3c2c-41a7-8023-4676084b01e3");
        fallbacks.put("krasnoyarsk", "6ba71d80-83c1-48a8-99e7-bb74e0c8e0c7");
        fallbacks.put("kemerovo", "0153afbd-105a-4894-beb1-f0fc5af96225");
        fallbacks.put("biysk", "e1a1d9ff-38b1-4a20-bed1-137865e48fbb");
        fallbacks.put("rubtsovsk", "9b470ee8-8df3-45ad-87f0-3a660574f0fa");
        fallbacks.put("novoaltaisk", "6f58b874-d17b-4be2-b7c7-fab223225b32");
        fallbacks.put("gorno-altaysk", "cf12519d-d605-4be2-b963-f58957b98145");
        this.cityAliasToIdCache = fallbacks;
    }

    @Override
    public boolean supports(String url) {
        if (!StringUtils.hasText(url)) {
            return false;
        }
        String lower = url.toLowerCase(Locale.ROOT);
        return lower.contains("grilnica.ru") || lower.contains("грильница");
    }

    @Override
    public List<ParsedMenuItemDto> getMenu(String url) {
        String cityId = resolveCityId(url);
        logger.info("Fetching Grilnica menu for cityId: {} (source url: {})", cityId, url);

        CachedMenu cached = menuCache.get(cityId);
        if (cached != null && Instant.now().isBefore(cached.expiry())) {
            logger.info("Returning {} cached menu items for cityId: {}", cached.items().size(), cityId);
            return cached.items();
        }

        try {
            // 1. Получаем карту категорий
            Map<String, String> categoryMap = fetchCategoryMap(cityId);

            // 2. Получаем каталог блюд по категориям
            List<ParsedMenuItemDto> items = fetchProducts(cityId, categoryMap);

            logger.info("Fetched {} products from Grilnica API for cityId: {}", items.size(), cityId);
            menuCache.put(cityId, new CachedMenu(items, Instant.now().plus(MENU_CACHE_TTL)));
            return items;
        } catch (Exception e) {
            logger.error("Failed to fetch menu from Grilnica API for cityId: {}", cityId, e);
            if (cached != null) {
                logger.warn("Using stale cached menu for cityId: {} as fallback", cityId);
                return cached.items();
            }
            return Collections.emptyList();
        }
    }

    private String resolveCityId(String url) {
        refreshCitiesCacheIfNeeded();

        if (!StringUtils.hasText(url)) {
            return DEFAULT_CITY_ID;
        }

        try {
            String uriStr = url.trim();
            if (!uriStr.startsWith("http://") && !uriStr.startsWith("https://")) {
                uriStr = "https://" + uriStr;
            }
            URI uri = URI.create(uriStr);
            String host = uri.getHost();
            if (host != null) {
                String[] parts = host.toLowerCase(Locale.ROOT).split("\\.");
                if (parts.length >= 3) {
                    String sub = parts[0];
                    String foundId = cityAliasToIdCache.get(sub);
                    if (foundId != null) {
                        return foundId;
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Could not parse host from url: {}", url, e);
        }

        return DEFAULT_CITY_ID;
    }

    private synchronized void refreshCitiesCacheIfNeeded() {
        if (Instant.now().isBefore(citiesCacheExpiry)) {
            return;
        }

        try {
            logger.debug("Refreshing Grilnica cities list from API");
            CityDto[] cities = restClient.get()
                    .uri("/city")
                    .retrieve()
                    .body(CityDto[].class);

            if (cities != null && cities.length > 0) {
                Map<String, String> map = new HashMap<>(cityAliasToIdCache);
                for (CityDto c : cities) {
                    if (StringUtils.hasText(c.alias()) && StringUtils.hasText(c.cityId())) {
                        map.put(c.alias().toLowerCase(Locale.ROOT), c.cityId());
                    }
                }
                this.cityAliasToIdCache = map;
                this.citiesCacheExpiry = Instant.now().plus(CITIES_CACHE_TTL);
                logger.info("Successfully updated Grilnica cities cache: {} cities", map.size());
            }
        } catch (Exception e) {
            logger.warn("Failed to refresh Grilnica cities from API, using existing cache: {}", e.getMessage());
            this.citiesCacheExpiry = Instant.now().plus(Duration.ofMinutes(15));
        }
    }

    private Map<String, String> fetchCategoryMap(String cityId) {
        Map<String, String> map = new HashMap<>();
        try {
            MenuCategoryDto[] categories = restClient.get()
                    .uri("/menu/{cityId}", cityId)
                    .retrieve()
                    .body(MenuCategoryDto[].class);

            if (categories != null) {
                for (MenuCategoryDto cat : categories) {
                    if (StringUtils.hasText(cat.menuCategoryId()) && StringUtils.hasText(cat.name())) {
                        map.put(cat.menuCategoryId(), cat.name());
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to fetch menu categories for cityId: {}, continuing without category names: {}", cityId, e.getMessage());
        }
        return map;
    }

    private List<ParsedMenuItemDto> fetchProducts(String cityId, Map<String, String> categoryMap) {
        List<ParsedMenuItemDto> result = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();

        String responseJson = restClient.get()
                .uri("/product/map-by-category/{cityId}", cityId)
                .retrieve()
                .body(String.class);

        if (!StringUtils.hasText(responseJson)) {
            return result;
        }

        try {
            JsonNode root = objectMapper.readTree(responseJson);
            if (root.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> field = fields.next();
                    String categoryId = field.getKey();
                    String categoryName = categoryMap.getOrDefault(categoryId, "Каталог");

                    JsonNode productsArray = field.getValue();
                    if (productsArray != null && productsArray.isArray()) {
                        for (JsonNode prod : productsArray) {
                            ParsedMenuItemDto item = mapProductToDto(prod, categoryName);
                            if (item != null && (item.getExternalId() == null || seenIds.add(item.getExternalId()))) {
                                result.add(item);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to parse products json for cityId: {}", cityId, e);
        }

        return result;
    }

    private ParsedMenuItemDto mapProductToDto(JsonNode prod, String categoryName) {
        if (prod == null || !prod.has("name")) {
            return null;
        }

        String name = prod.path("name").asText(null);
        if (!StringUtils.hasText(name)) {
            return null;
        }

        boolean isHideInApp = prod.path("isHideInApp").asBoolean(false);
        if (isHideInApp) {
            return null;
        }

        BigDecimal price = null;
        if (prod.hasNonNull("price")) {
            price = BigDecimal.valueOf(prod.path("price").asDouble());
        } else if (prod.hasNonNull("salePrice")) {
            price = BigDecimal.valueOf(prod.path("salePrice").asDouble());
        }

        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        String description = prod.path("description").asText(null);
        String imageFileName = prod.path("imageFileName").asText(null);
        String imageUrl = null;
        if (StringUtils.hasText(imageFileName)) {
            imageUrl = CDN_BASE_URL + "/" + imageFileName.trim();
        }

        String productId = prod.path("productId").asText(null);

        String weightStr = formatWeight(prod.path("weight"));

        // 1. Парсим категории опций и опции
        Map<String, String> optionIdToNameMap = new HashMap<>();
        List<ru.prokin.kitchen.dto.MenuItemOptionCategoryDto> optionCategoryDtos = new ArrayList<>();
        Set<String> variantCategoryIds = new HashSet<>();
        JsonNode optCatsArray = prod.path("optionCategories");
        if (optCatsArray.isArray()) {
            for (JsonNode catNode : optCatsArray) {
                String catName = catNode.path("name").asText(null);
                if (!StringUtils.hasText(catName)) continue;

                String catId = catNode.path("optionCategoryId").asText(null);
                boolean isVariantDef = catNode.path("isProductVariantDefine").asBoolean(false);
                if (isVariantDef && StringUtils.hasText(catId)) {
                    variantCategoryIds.add(catId);
                }

                List<ru.prokin.kitchen.dto.MenuItemOptionDto> options = new ArrayList<>();
                JsonNode optionsArray = catNode.path("options");
                if (optionsArray.isArray()) {
                    for (JsonNode opt : optionsArray) {
                        String optId = opt.path("optionId").asText(null);
                        String optName = opt.path("name").asText(null);
                        if (!StringUtils.hasText(optName)) continue;

                        if (StringUtils.hasText(optId)) {
                            optionIdToNameMap.put(optId, optName);
                        }

                        BigDecimal optPrice = BigDecimal.valueOf(opt.path("price").asDouble(0));
                        boolean isDefault = opt.path("restrictions").path("isDefault").asBoolean(false);

                        options.add(ru.prokin.kitchen.dto.MenuItemOptionDto.builder()
                                .id(optId)
                                .name(optName)
                                .price(optPrice)
                                .isDefault(isDefault)
                                .build());
                    }
                }

                if (!options.isEmpty()) {
                    Integer min = catNode.path("restrictions").hasNonNull("min") ? catNode.path("restrictions").path("min").asInt() : null;
                    Integer max = catNode.path("restrictions").hasNonNull("max") ? catNode.path("restrictions").path("max").asInt() : null;

                    optionCategoryDtos.add(ru.prokin.kitchen.dto.MenuItemOptionCategoryDto.builder()
                            .id(catId)
                            .name(catName)
                            .min(min)
                            .max(max)
                            .options(options)
                            .build());
                }
            }
        }

        // 2. Парсим варианты продукта (размеры / порции)
        List<ru.prokin.kitchen.dto.MenuItemVariantDto> variantDtos = new ArrayList<>();
        JsonNode variantsArray = prod.path("productVariants");
        if (variantsArray.isArray() && !variantsArray.isEmpty()) {
            for (JsonNode v : variantsArray) {
                String vId = v.path("productVariantId").asText(null);
                double vPriceVal = v.path("price").asDouble(0);
                if (vPriceVal <= 0) continue;

                BigDecimal vPrice = BigDecimal.valueOf(vPriceVal);
                String vWeight = formatWeight(v.path("weight"));

                // Определяем имя варианта (например "M", "L", "XXL" из сопоставления optionIds)
                String vName = null;
                JsonNode optionIdsArray = v.path("optionIds");
                if (optionIdsArray.isArray() && !optionIdsArray.isEmpty()) {
                    for (JsonNode optIdNode : optionIdsArray) {
                        String optId = optIdNode.asText();
                        if (optionIdToNameMap.containsKey(optId)) {
                            vName = optionIdToNameMap.get(optId);
                            break;
                        }
                    }
                }
                if (!StringUtils.hasText(vName)) {
                    vName = v.path("name").asText(null);
                }
                if (!StringUtils.hasText(vName)) {
                    vName = "Порция " + (vWeight != null ? vWeight : "");
                }

                boolean isDefault = vPrice.compareTo(price) == 0;

                variantDtos.add(ru.prokin.kitchen.dto.MenuItemVariantDto.builder()
                        .id(vId)
                        .name(vName.trim())
                        .price(vPrice)
                        .weight(vWeight)
                        .isDefault(isDefault)
                        .build());
            }
        }

        // Если варианты сформированы, удаляем категорию размеров/объема из опций, чтобы не дублировать
        if (!variantDtos.isEmpty()) {
            optionCategoryDtos.removeIf(c ->
                    (c.getId() != null && variantCategoryIds.contains(c.getId())) ||
                    "Размер".equalsIgnoreCase(c.getName()) ||
                    "Выберите объем".equalsIgnoreCase(c.getName())
            );
        }

        return ParsedMenuItemDto.builder()
                .title(name)
                .price(price)
                .description(description)
                .imageUrl(imageUrl)
                .category(categoryName)
                .weight(weightStr)
                .externalId(productId)
                .variants(variantDtos.isEmpty() ? null : variantDtos)
                .optionCategories(optionCategoryDtos.isEmpty() ? null : optionCategoryDtos)
                .rawData(prod)
                .build();
    }

    private String formatWeight(JsonNode weightNode) {
        if (weightNode != null && !weightNode.isNull() && weightNode.isNumber()) {
            double weightVal = weightNode.asDouble();
            if (weightVal > 0) {
                if (weightVal < 5) {
                    return Math.round(weightVal * 1000) + " г";
                } else {
                    return Math.round(weightVal) + " г";
                }
            }
        }
        return null;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CityDto(String cityId, String name, String alias) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record MenuCategoryDto(String menuCategoryId, String name, String alias) {}
}
