package ru.prokin.kitchen.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.prokin.kitchen.entity.Restaurant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantResponse {
    private Long id;
    private String name;
    private String websiteUrl;

    public static RestaurantResponse fromEntity(Restaurant restaurant) {
        if (restaurant == null) return null;
        return RestaurantResponse.builder()
                .id(restaurant.getId())
                .name(restaurant.getName())
                .websiteUrl(restaurant.getWebsiteUrl())
                .build();
    }
}
