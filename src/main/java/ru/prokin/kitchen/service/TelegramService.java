package ru.prokin.kitchen.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import ru.prokin.kitchen.entity.Order;
import ru.prokin.kitchen.entity.UserOrder;
import ru.prokin.kitchen.repository.UserOrderRepository;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class TelegramService {

    private static final Logger logger = LoggerFactory.getLogger(TelegramService.class);

    @Autowired
    private UserOrderRepository userOrderRepo;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.chat.id}")
    private String chatId;

    @Value("${kitchen.base-url}")
    private String kitchenBaseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendMessage(String message) {
        if (!StringUtils.hasText(message)) {
            logger.warn("Cannot send empty message to Telegram");
            return;
        }

        String url = "https://api.telegram.org/bot" + botToken.trim() + "/sendMessage";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("chat_id", chatId.trim());
        map.add("text", message);
        map.add("parse_mode", "HTML");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        logger.info("Sending Telegram message: {}", message);

        try {
            String response = restTemplate.postForObject(url, request, String.class);
            logger.info("Telegram API response: {}", response);
        } catch (Exception e) {
            logger.error("Failed to send Telegram message", e);
        }
    }

    public void sendDeadlineWarning(Order order) {
        String message = String.format(
                "⚠️ <b>Внимание!</b>\n" +
                        "<a href=\"%s/order/%d\">Заказ №%d</a> будет закрыт через 5 минут!\n" +
                        "Дедлайн: %s",
                kitchenBaseUrl,
                order.getId(),
                order.getId(),
                order.getDeadlineTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
        );
        sendMessage(message);
    }

    public void sendDeadlineNotification(Order order) {
        String message = String.format(
                "⏰ <b>Заказ №%d закрыт!</b>\n" +
                        "🔗 <a href=\"%s/order/%d\">Перейти к заказу</a>\n" +
                        "Создатель: %s",
                order.getId(),
                kitchenBaseUrl,
                order.getId(),
                order.getCreatedBy().getFullName()
        );
        // Отправляем список блюд
        List<UserOrder> items = userOrderRepo.findByOrder_IdOrderByIdAsc(order.getId());
        if (!items.isEmpty()) {
            StringBuilder itemMessage = new StringBuilder(message + "\n\n<b>Блюда:</b>\n");
            for (UserOrder item : items) {
                itemMessage.append("• ").append(item.getUser().getFullName())
                        .append(": ").append(item.getItemDescription()).append("\n");
            }
            sendMessage(itemMessage.toString());
        } else {
            sendMessage(message);
        }
    }
}