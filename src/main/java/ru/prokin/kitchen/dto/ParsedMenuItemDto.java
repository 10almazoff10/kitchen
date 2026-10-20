package ru.prokin.kitchen.dto;

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
}
