package ru.prokin.kitchen.service;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ru.prokin.kitchen.dto.ParsedMenuItemDto;
import ru.prokin.kitchen.exception.BadRequestException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class MenuParserService {

    private static final Logger logger = LoggerFactory.getLogger(MenuParserService.class);

    private static final Pattern PRICE_PATTERN = Pattern.compile("(\\d[\\d\\s\\u00A0]*[.,]?\\d*)\\s*(?:₽|руб|rur|rub)?", Pattern.CASE_INSENSITIVE);

    public List<ParsedMenuItemDto> parseMenu(String url) {
        if (!StringUtils.hasText(url)) {
            throw new BadRequestException("URL для парсинга не может быть пустым");
        }

        try {
            logger.info("Parsing menu from URL: {}", url);
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .timeout(10000)
                    .followRedirects(true)
                    .get();

            List<ParsedMenuItemDto> items = new ArrayList<>();

            // 1. Попытка парсинга через Schema.org / Microdata
            Elements schemaItems = doc.select("[itemtype*='MenuItem'], [itemtype*='Product']");
            if (!schemaItems.isEmpty()) {
                for (Element el : schemaItems) {
                    ParsedMenuItemDto item = parseSchemaElement(el);
                    if (item != null) {
                        items.add(item);
                    }
                }
            }

            // 2. Если schema.org не найдено, ищем по популярным селекторам карточек меню
            if (items.isEmpty()) {
                String[] cardSelectors = {
                        ".menu-item", ".product-card", ".catalog-item", ".dish-item",
                        ".food-item", ".item-card", ".product", ".menu__item",
                        ".catalog__item", ".card-product", "[class*='dish']", "[class*='menu-item']"
                };

                for (String selector : cardSelectors) {
                    Elements elements = doc.select(selector);
                    if (!elements.isEmpty()) {
                        for (Element el : elements) {
                            ParsedMenuItemDto item = parseGenericCardElement(el);
                            if (item != null && item.getTitle() != null && item.getPrice() != null) {
                                items.add(item);
                            }
                        }
                        if (!items.isEmpty()) {
                            break;
                        }
                    }
                }
            }

            // 3. Fallback: поиск заголовков и цен в блоках
            if (items.isEmpty()) {
                Elements headers = doc.select("h2, h3, h4, h5");
                for (Element header : headers) {
                    Element parent = header.parent();
                    if (parent != null) {
                        ParsedMenuItemDto item = parseGenericCardElement(parent);
                        if (item != null && item.getTitle() != null && item.getPrice() != null) {
                            items.add(item);
                        }
                    }
                }
            }

            logger.info("Successfully parsed {} menu items from {}", items.size(), url);
            return items;
        } catch (Exception e) {
            logger.error("Failed to parse menu from URL: {}", url, e);
            throw new BadRequestException("Не удалось спарсить меню по указанному URL: " + e.getMessage());
        }
    }

    private ParsedMenuItemDto parseSchemaElement(Element el) {
        Element nameEl = el.selectFirst("[itemprop='name']");
        Element priceEl = el.selectFirst("[itemprop='price'], [itemprop='priceCurrency']");
        Element descEl = el.selectFirst("[itemprop='description']");
        Element imageEl = el.selectFirst("[itemprop='image'], img");

        String title = nameEl != null ? nameEl.text().trim() : null;
        BigDecimal price = priceEl != null ? extractPrice(priceEl.text()) : null;
        String desc = descEl != null ? descEl.text().trim() : null;
        String imageUrl = imageEl != null ? imageEl.absUrl("src") : null;

        if (title != null) {
            return ParsedMenuItemDto.builder()
                    .title(title)
                    .price(price != null ? price : BigDecimal.ZERO)
                    .description(desc)
                    .imageUrl(imageUrl)
                    .build();
        }
        return null;
    }

    private ParsedMenuItemDto parseGenericCardElement(Element el) {
        Element titleEl = el.selectFirst("h1, h2, h3, h4, h5, [class*='title'], [class*='name']");
        Element priceEl = el.selectFirst("[class*='price'], [class*='cost']");
        Element descEl = el.selectFirst("[class*='desc'], [class*='text'], [class*='info']");
        Element imgEl = el.selectFirst("img");

        String title = titleEl != null ? titleEl.text().trim() : null;
        BigDecimal price = priceEl != null ? extractPrice(priceEl.text()) : null;

        if (price == null) {
            price = extractPrice(el.text());
        }

        String desc = descEl != null ? descEl.text().trim() : null;
        String imageUrl = imgEl != null ? imgEl.absUrl("src") : null;
        if (!StringUtils.hasText(imageUrl) && imgEl != null) {
            imageUrl = imgEl.attr("data-src");
        }

        if (StringUtils.hasText(title)) {
            return ParsedMenuItemDto.builder()
                    .title(title)
                    .price(price)
                    .description(desc)
                    .imageUrl(imageUrl)
                    .build();
        }
        return null;
    }

    private BigDecimal extractPrice(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }

        Matcher matcher = PRICE_PATTERN.matcher(text);
        if (matcher.find()) {
            String clean = matcher.group(1).replaceAll("[\\s\\u00A0]", "").replace(',', '.');
            try {
                return new BigDecimal(clean);
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }
}
