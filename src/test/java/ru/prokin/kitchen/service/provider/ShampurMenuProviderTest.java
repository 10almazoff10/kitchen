package ru.prokin.kitchen.service.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.prokin.kitchen.dto.ParsedMenuItemDto;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ShampurMenuProviderTest {

    private ShampurMenuProvider provider;

    @BeforeEach
    void setUp() {
        provider = new ShampurMenuProvider(new ObjectMapper());
    }

    @Test
    void supportsShampurUrls() {
        assertTrue(provider.supports("https://shampur-brn.ru"));
        assertTrue(provider.supports("https://shampur-brn.ru/"));
        assertTrue(provider.supports("https://shampur-brn.ru/product/4187-svinina-sheya-1"));
        assertTrue(provider.supports("shampur"));
        assertTrue(provider.supports("Шампур Барнаул"));
        assertFalse(provider.supports("https://pizzburg22.ru"));
        assertFalse(provider.supports("https://grilnica.ru"));
        assertFalse(provider.supports("https://dodopizza.ru"));
        assertFalse(provider.supports(null));
        assertFalse(provider.supports(""));
    }

    @Test
    void parseHtmlStructure() {
        String sampleHtml = """
                <div class="sect nav__anchor" data-id="101">
                  <h2 class="sect__title">Шашлык</h2>
                  <div class="sect__dishes">
                    <div class="ddish ddish--with-photo" data-id="4187" data-modifier-refs="m_weight,m_options" id="dish4187">
                      <div class="ddish__photos">
                        <img src="data:," alt="Свинина Шея 1" data-src="https://cdn-ru20.foodpicasso.com/sample_meat.png" class="ddish__photo">
                      </div>
                      <div class="ddish__content">
                        <div class="ddish__name">
                          <a href="/product/4187-svinina-sheya-1" class="ddish__link">Свинина Шея 1</a>
                          <span class="ddish__size" hidden></span>
                        </div>
                        <div class="ddish__footer">
                          <div class="ddish__sum" data-price="430">от 430 ₽</div>
                          <div class="ddish__plus" data-id="4187"></div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
                <script type="application/json" data-modifier-dictionary>
                {
                  "m_weight": {
                    "title": "Вес Свинина шея-1:",
                    "type": "many_many",
                    "min": 1,
                    "max": 1,
                    "options": [
                      {
                        "id": 3862,
                        "name": "200гр",
                        "prices": [{"price": 4300000, "priceFormatted": "430 ₽"}]
                      },
                      {
                        "id": 3909,
                        "name": "300гр",
                        "price": 6450000
                      }
                    ]
                  },
                  "m_options": {
                    "title": "Для блюда",
                    "type": "many_many",
                    "min": 0,
                    "max": 5,
                    "options": [
                      {"id": "opt1", "name": "- Без лука", "prices": [{"price": 0}]},
                      {"id": "opt2", "name": "- Завернуть в фольгу", "price": 200000}
                    ]
                  }
                }
                </script>
                """;

        List<ParsedMenuItemDto> items = provider.parseHtml(sampleHtml);
        assertNotNull(items);
        assertEquals(1, items.size());

        ParsedMenuItemDto dish = items.get(0);
        assertEquals("shampur_4187", dish.getExternalId());
        assertEquals("Свинина Шея 1", dish.getTitle());
        assertEquals("Шашлык", dish.getCategory());
        assertEquals(new BigDecimal("430.00"), dish.getPrice());
        assertEquals("https://cdn-ru20.foodpicasso.com/sample_meat.png", dish.getImageUrl());

        // Проверяем варианты (порции/вес)
        assertNotNull(dish.getVariants());
        assertEquals(2, dish.getVariants().size());
        assertEquals("200гр", dish.getVariants().get(0).getName());
        assertEquals(new BigDecimal("430.00"), dish.getVariants().get(0).getPrice());
        assertTrue(dish.getVariants().get(0).isDefault());
        assertEquals("300гр", dish.getVariants().get(1).getName());
        assertEquals(new BigDecimal("645.00"), dish.getVariants().get(1).getPrice());

        // Проверяем опции
        assertNotNull(dish.getOptionCategories());
        assertEquals(1, dish.getOptionCategories().size());
        var optCat = dish.getOptionCategories().get(0);
        assertEquals("Для блюда", optCat.getName());
        assertEquals(2, optCat.getOptions().size());
        assertEquals("- Без лука", optCat.getOptions().get(0).getName());
        assertEquals(BigDecimal.ZERO, optCat.getOptions().get(0).getPrice());
        assertEquals("- Завернуть в фольгу", optCat.getOptions().get(1).getName());
        assertEquals(new BigDecimal("20.00"), optCat.getOptions().get(1).getPrice());
    }

    @Test
    void fetchMenuSmokeTest() {
        List<ParsedMenuItemDto> items = provider.getMenu("https://shampur-brn.ru");
        assertNotNull(items);
        assertFalse(items.isEmpty(), "Меню Шампура не должно быть пустым");

        assertTrue(items.size() > 50, "В меню Шампура должно быть больше 50 блюд");
        ParsedMenuItemDto first = items.get(0);
        assertNotNull(first.getTitle());
        assertNotNull(first.getPrice());
        assertNotNull(first.getCategory());
    }
}
