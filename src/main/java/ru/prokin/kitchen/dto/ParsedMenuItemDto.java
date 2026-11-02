package ru.prokin.kitchen.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsedMenuItemDto {
    private String title;
    private BigDecimal price;
    private String description;
    private String imageUrl;
    private String category;
    private String weight;
    private String externalId;
    private java.util.List<MenuItemVariantDto> variants;
    private java.util.List<MenuItemOptionCategoryDto> optionCategories;
    private Object rawData;

    @JsonProperty("name")
    public String getName() {
        return title;
    }
}
