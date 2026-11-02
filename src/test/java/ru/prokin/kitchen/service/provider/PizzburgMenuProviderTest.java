package ru.prokin.kitchen.service.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.prokin.kitchen.dto.ParsedMenuItemDto;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PizzburgMenuProviderTest {

    private PizzburgMenuProvider provider;

    @BeforeEach
    void setUp() {
        provider = new PizzburgMenuProvider(new ObjectMapper());
    }

    @Test
    void supportsPizzburgUrls() {
        assertTrue(provider.supports("https://pizzburg22.ru"));
        assertTrue(provider.supports("https://pizzburg22.ru/"));
        assertTrue(provider.supports("https://pizzburg22.ru/s/wok_1074"));
        assertTrue(provider.supports("pizzburg"));
        assertTrue(provider.supports("Пицбург Барнаул"));
        assertFalse(provider.supports("https://grilnica.ru"));
        assertFalse(provider.supports("https://dodopizza.ru"));
        assertFalse(provider.supports(null));
        assertFalse(provider.supports(""));
    }

    @Test
    void parseHtmlStructure() {
        String sampleHtml = """
                <div class="sect nav__anchor" data-id="1074">
                  <h2 class="sect__title">WOK</h2>
                  <div class="sect__dishes">
                    <div class="ddish ddish--with-photo" data-id="7385" data-modifier-refs="m_sizes,m_remove" id="dish7385">
                      <div class="ddish__photos">
                        <img src="data:," alt="WOK Сливочный Удон" data-src="https://cdn-ru17.foodpicasso.com/sample.jpg" class="ddish__photo">
                      </div>
                      <div class="ddish__content">
                        <div class="ddish__name">
                          <a href="/product/7385" class="ddish__link">WOK Сливочный Удон</a>
                          <span class="ddish__size">400 г</span>
                        </div>
                        <div class="ddish__ingredients">Лапша Удон, куриное филе, шампиньоны</div>
                        <div class="ddish__footer">
                          <div class="ddish__sum" data-price="380">380 ₽</div>
                          <div class="ddish__plus" data-id="7385"></div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
                <script type="application/json" data-modifier-dictionary>
                {
                  "m_sizes": {
                    "title": "Выберите размер",
                    "type": "sizes",
                    "options": [
                      {"name": "Стандарт 400г", "price": 3800000},
                      {"name": "Большой 550г", "price": 4500000}
                    ]
                  },
                  "m_remove": {
                    "title": "-Убрать ингредиенты",
                    "type": "many_many",
                    "min": 0,
                    "max": 2,
                    "options": [
                      {"id": "opt1", "name": "-Без лука", "price": 0},
                      {"id": "opt2", "name": "-Без шампиньонов", "price": 0}
                    ]
                  }
                }
                </script>
                """;

        List<ParsedMenuItemDto> items = provider.parseHtml(sampleHtml);
        assertNotNull(items);
        assertEquals(1, items.size());

        ParsedMenuItemDto dish = items.get(0);
        assertEquals("pizzburg_7385", dish.getExternalId());
        assertEquals("WOK Сливочный Удон", dish.getTitle());
        assertEquals("WOK", dish.getCategory());
        assertEquals(new BigDecimal("380.00"), dish.getPrice());
        assertEquals("Лапша Удон, куриное филе, шампиньоны", dish.getDescription());
        assertEquals("https://cdn-ru17.foodpicasso.com/sample.jpg", dish.getImageUrl());
        assertEquals("400 г", dish.getWeight());

        // Проверяем варианты (размеры)
        assertNotNull(dish.getVariants());
        assertEquals(2, dish.getVariants().size());
        assertEquals("Стандарт 400г", dish.getVariants().get(0).getName());
        assertEquals(new BigDecimal("380.00"), dish.getVariants().get(0).getPrice());
        assertTrue(dish.getVariants().get(0).isDefault());
        assertEquals("Большой 550г", dish.getVariants().get(1).getName());
        assertEquals(new BigDecimal("450.00"), dish.getVariants().get(1).getPrice());

        // Проверяем категории опций
        assertNotNull(dish.getOptionCategories());
        assertEquals(1, dish.getOptionCategories().size());
        var optCat = dish.getOptionCategories().get(0);
        assertEquals("-Убрать ингредиенты", optCat.getName());
        assertEquals(2, optCat.getOptions().size());
        assertEquals("-Без лука", optCat.getOptions().get(0).getName());
        assertEquals(BigDecimal.ZERO, optCat.getOptions().get(0).getPrice());
    }

    @Test
    void fetchMenuSmokeTest() {
        List<ParsedMenuItemDto> items = provider.getMenu("https://pizzburg22.ru");
        assertNotNull(items);
        assertFalse(items.isEmpty(), "Меню Пиццбурга не должно быть пустым");

        assertTrue(items.size() > 100, "В меню Пиццбурга должно быть больше 100 блюд");
        ParsedMenuItemDto first = items.get(0);
        assertNotNull(first.getTitle());
        assertNotNull(first.getPrice());
        assertNotNull(first.getCategory());
    }
}
