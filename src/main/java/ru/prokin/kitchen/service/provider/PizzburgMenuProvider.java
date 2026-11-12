package ru.prokin.kitchen.service.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Провайдер меню для ресторана Pizz Burg (pizzburg22.ru, платформа FoodPicasso).
 */
@Component
public class PizzburgMenuProvider extends AbstractFoodPicassoMenuProvider {

    private static final String DEFAULT_BASE_URL = "https://pizzburg22.ru";
    private static final String EXTERNAL_ID_PREFIX = "pizzburg";

    public PizzburgMenuProvider(ObjectMapper objectMapper) {
        super(objectMapper, DEFAULT_BASE_URL, EXTERNAL_ID_PREFIX);
    }

    @Override
    public boolean supports(String url) {
        if (!StringUtils.hasText(url)) {
            return false;
        }
        String lower = url.toLowerCase();
        return lower.contains("pizzburg22.ru") || lower.contains("pizzburg") || lower.contains("пицбург");
    }

    @Override
    public int getOrder() {
        return 10;
    }
}
