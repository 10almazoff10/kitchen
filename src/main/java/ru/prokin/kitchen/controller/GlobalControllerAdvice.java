package ru.prokin.kitchen.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import ru.prokin.kitchen.entity.User;
import ru.prokin.kitchen.service.KitchenService;

@ControllerAdvice
public class GlobalControllerAdvice {

    @Autowired
    private KitchenService kitchenService;

    /**
     * Этот метод автоматически добавляет атрибут 'notPaidOrdersCount'
     * во все модели (Model) для каждого отображаемого HTML-шаблона.
     */
    @ModelAttribute("notPaidOrdersCount")
    public long addNotPaidOrdersCountToModel(Authentication auth) {
        // Если пользователь не авторизован (например, страница логина), возвращаем 0
        if (auth == null || !auth.isAuthenticated()) {
            return 0;
        }

        try {
            // Извлекаем текущего пользователя и запрашиваем количество долгов
            User user = (User) kitchenService.loadUserByUsername(auth.getName());
            return kitchenService.getNotPaidOrdersCount(user);
        } catch (Exception e) {
            // Защита на случай непредвиденных ошибок при загрузке пользователя
            return 0;
        }
    }
}
