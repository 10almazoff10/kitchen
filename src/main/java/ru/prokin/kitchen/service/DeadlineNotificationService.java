package ru.prokin.kitchen.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.prokin.kitchen.entity.Order;
import ru.prokin.kitchen.repository.OrderRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DeadlineNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(DeadlineNotificationService.class);

    @Autowired private OrderRepository orderRepo;
    @Autowired private TelegramService telegramService;

    // Запускаем каждую минуту
    @Scheduled(fixedRate = 60000) // 60 секунд
    public void checkDeadlines() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime fiveMinBefore = now.plusMinutes(5);

        // Проверяем, нужно ли отправить предупреждение
        List<Order> ordersForWarning = orderRepo.findOrdersForWarning(fiveMinBefore, now);
        for (Order order : ordersForWarning) {
            telegramService.sendDeadlineWarning(order);
            order.setDeadlineWarningSent(true);
            orderRepo.save(order);
            logger.info("Sent deadline warning for order ID: {}", order.getId());
        }

        // Проверяем, нужно ли отправить уведомление о закрытии
        LocalDateTime oneMinBefore = now.plusSeconds(30); // чтобы быть уверенным, что дедлайн наступил
        List<Order> ordersForNotification = orderRepo.findOrdersForNotification(now, oneMinBefore);
        for (Order order : ordersForNotification) {
            telegramService.sendDeadlineNotification(order);
            order.setDeadlineNotified(true);
            orderRepo.save(order);
            logger.info("Sent deadline notification for order ID: {}", order.getId());
        }
    }
}