package ru.prokin.kitchen.service.provider;

import ru.prokin.kitchen.dto.ParsedMenuItemDto;

import java.util.List;

public interface RestaurantMenuProvider {

    /**
     * Проверяет, поддерживается ли данный URL рестораном (например, grilnica.ru).
     */
    boolean supports(String url);

    /**
     * Загружает актуальное меню ресторана по переданному URL.
     */
    List<ParsedMenuItemDto> getMenu(String url);

    /**
     * Порядок вызова провайдера (меньшее число - выше приоритет).
     */
    default int getOrder() {
        return 0;
    }
}
