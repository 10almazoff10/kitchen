package ru.prokin.kitchen.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    @Email(message = "Некорректный формат email")
    @Size(max = 255, message = "Email не должен превышать 255 символов")
    private String email;

    @Size(max = 100, message = "Имя не должно превышать 100 символов")
    private String fullName;
}
