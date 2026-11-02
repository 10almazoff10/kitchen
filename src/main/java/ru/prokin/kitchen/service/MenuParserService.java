package ru.prokin.kitchen.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ru.prokin.kitchen.dto.ParsedMenuItemDto;
import ru.prokin.kitchen.service.provider.RestaurantMenuProvider;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MenuParserService {

    private static final Logger logger = LoggerFactory.getLogger(MenuParserService.class);

    private final List<RestaurantMenuProvider> providers;

    public boolean hasProvider(String url) {
        if (!StringUtils.hasText(url) || providers == null || providers.isEmpty()) {
            return false;
        }
        return providers.stream().anyMatch(p -> p.supports(url));
    }

    public boolean hasProvider(String url, String name) {
        if (providers == null || providers.isEmpty()) {
            return false;
        }
        return (StringUtils.hasText(url) && providers.stream().anyMatch(p -> p.supports(url)))
                || (StringUtils.hasText(name) && providers.stream().anyMatch(p -> p.supports(name)));
    }

    public List<ParsedMenuItemDto> parseMenu(String url) {
        if (!StringUtils.hasText(url)) {
            return Collections.emptyList();
        }

        if (providers != null && !providers.isEmpty()) {
            List<RestaurantMenuProvider> sortedProviders = providers.stream()
                    .sorted(Comparator.comparingInt(RestaurantMenuProvider::getOrder))
                    .toList();

            for (RestaurantMenuProvider provider : sortedProviders) {
                if (provider.supports(url)) {
                    logger.info("Using {} for menu: {}", provider.getClass().getSimpleName(), url);
                    List<ParsedMenuItemDto> providerItems = provider.getMenu(url);
                    if (providerItems != null && !providerItems.isEmpty()) {
                        return providerItems;
                    }
                }
            }
        }

        logger.debug("No API menu provider supports URL: {}", url);
        return Collections.emptyList();
    }
}
