package ru.prokin.kitchen.service.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.prokin.kitchen.dto.ParsedMenuItemDto;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GrilnicaMenuProviderTest {

    private GrilnicaMenuProvider provider;

    @BeforeEach
    void setUp() {
        provider = new GrilnicaMenuProvider(new ObjectMapper());
    }

    @Test
    void supportsGrilnicaUrls() {
        assertTrue(provider.supports("https://barnaul.grilnica.ru"));
        assertTrue(provider.supports("https://barnaul.grilnica.ru/menu/rolls"));
        assertTrue(provider.supports("https://grilnica.ru"));
        assertTrue(provider.supports("https://novosibirsk.grilnica.ru"));
        assertFalse(provider.supports("https://dodopizza.ru"));
        assertFalse(provider.supports("https://eda.yandex.ru"));
        assertFalse(provider.supports(null));
        assertFalse(provider.supports(""));
    }

    @Test
    void fetchMenuSmokeTest() {
        List<ParsedMenuItemDto> items = provider.getMenu("https://barnaul.grilnica.ru");
        assertNotNull(items);
        assertFalse(items.isEmpty(), "Меню Грильницы не должно быть пустым");

        ParsedMenuItemDto first = items.get(0);
        assertNotNull(first.getTitle());
        assertNotNull(first.getPrice());
        assertNotNull(first.getCategory());
        assertNotNull(first.getName());
    }
}
