package ru.prokin.kitchen.service.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Провайдер меню для ресторана Шампур (shampur-brn.ru, платформа FoodPicasso).
 */
@Component
public class ShampurMenuProvider extends AbstractFoodPicassoMenuProvider {

    private static final String DEFAULT_BASE_URL = "https://shampur-brn.ru";
    private static final String EXTERNAL_ID_PREFIX = "shampur";

    public ShampurMenuProvider(ObjectMapper objectMapper) {
        super(objectMapper, DEFAULT_BASE_URL, EXTERNAL_ID_PREFIX);
    }

    @Override
    public boolean supports(String url) {
        if (!StringUtils.hasText(url)) {
            return false;
        }
        String lower = url.toLowerCase();
        return lower.contains("shampur-brn.ru") || lower.contains("shampur") || lower.contains("шампур");
    }

    @Override
    public int getOrder() {
        return 10;
    }
}
