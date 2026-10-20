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
public class UserDto {
    private Long id;
    private String username;
    private String fullName;
    private String email;
    private String avatarUrl;
    private String adUsername;
    private String legacyUsername;
    @com.fasterxml.jackson.annotation.JsonProperty("isAdLinked")
    private boolean isAdLinked;

    public static UserDto fromEntity(User user) {
        if (user == null) return null;
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEffectiveEmail())
                .avatarUrl(user.getAvatarUrl())
                .adUsername(user.getAdUsername())
                .legacyUsername(user.getLegacyUsername())
                .isAdLinked(user.isAdLinked())
                .build();
    }
}
