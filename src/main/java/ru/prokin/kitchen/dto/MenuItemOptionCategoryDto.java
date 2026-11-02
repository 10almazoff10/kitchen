package ru.prokin.kitchen.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuItemOptionCategoryDto {
    private String id;
    private String name;
    private Integer min;
    private Integer max;
    private List<MenuItemOptionDto> options;
}
