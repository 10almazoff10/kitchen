package ru.prokin.kitchen.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.prokin.kitchen.entity.User;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResult {
    private User user;
    private boolean requiresAdLink;
}
