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
import org.springframework.web.util.HtmlUtils;
import org.springframework.scheduling.annotation.Async;
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

    @Value("${telegram.bot.token:}")
    private String botToken;

    @Value("${telegram.chat.id:}")
    private String chatId;

    @Value("${kitchen.base-url:http://localhost:8080}")
    private String kitchenBaseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @Async
    public void sendMessage(String message) {
        if (!StringUtils.hasText(botToken) || !StringUtils.hasText(chatId)) {
            logger.debug("Telegram credentials not configured, skipping notification");
            return;
        }
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

    @Async
    public void sendDeadlineWarning(Order order) {
        String orderUrl = buildOrderUrl(order.getId());
        String message = String.format(
                "⚠️ <b>Внимание!</b>\n" +
                        "<a href=\"%s\">Заказ №%d</a> будет закрыт через 5 минут!\n" +
                        "Дедлайн: %s",
                orderUrl,
                order.getId(),
                order.getDeadlineTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
        );
        sendMessage(message);
    }

    @Async
    public void sendDeadlineNotification(Order order) {
        String orderUrl = buildOrderUrl(order.getId());
        String creatorName = order.getCreatedBy() != null && order.getCreatedBy().getFullName() != null
                ? HtmlUtils.htmlEscape(order.getCreatedBy().getFullName())
                : "Аноним";

        String message = String.format(
                "⏰ <b>Заказ №%d закрыт!</b>\n" +
                        "🔗 <a href=\"%s\">Перейти к заказу</a>\n" +
                        "Создатель: %s",
                order.getId(),
                orderUrl,
                creatorName
        );
        // Отправляем список блюд
        List<UserOrder> items = userOrderRepo.findByOrder_IdOrderByIdAsc(order.getId());
        if (!items.isEmpty()) {
            StringBuilder itemMessage = new StringBuilder(message + "\n\n<b>Блюда:</b>\n");
            for (UserOrder item : items) {
                String userName = item.getUser() != null && item.getUser().getFullName() != null
                        ? HtmlUtils.htmlEscape(item.getUser().getFullName())
                        : "Участник";
                String itemDesc = item.getItemDescription() != null
                        ? HtmlUtils.htmlEscape(item.getItemDescription())
                        : "-";
                itemMessage.append("• ").append(userName)
                        .append(": ").append(itemDesc).append("\n");
            }
            sendMessage(itemMessage.toString());
        } else {
            sendMessage(message);
        }
    }

    private String buildOrderUrl(Long orderId) {
        String cleanBaseUrl = StringUtils.hasText(kitchenBaseUrl) ? kitchenBaseUrl.trim().replaceAll("/+$", "") : "http://localhost:8080";
        if (!cleanBaseUrl.startsWith("http://") && !cleanBaseUrl.startsWith("https://")) {
            cleanBaseUrl = "http://" + cleanBaseUrl;
        }
        return cleanBaseUrl + "/orders/" + orderId;
    }

    @Async
    public void sendPollStartedNotification(List<ru.prokin.kitchen.entity.Restaurant> restaurants) {
        String cleanBaseUrl = StringUtils.hasText(kitchenBaseUrl) ? kitchenBaseUrl.trim().replaceAll("/+$", "") : "http://localhost:8080";
        if (!cleanBaseUrl.startsWith("http://") && !cleanBaseUrl.startsWith("https://")) {
            cleanBaseUrl = "http://" + cleanBaseUrl;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("🗳️ <b>Голосование за выбор ресторана открыто!</b>\n\n");
        sb.append("Куда закажем обед сегодня? Доступные варианты:\n");
        if (restaurants != null && !restaurants.isEmpty()) {
            for (ru.prokin.kitchen.entity.Restaurant r : restaurants) {
                sb.append("• ").append(HtmlUtils.htmlEscape(r.getName())).append("\n");
            }
        }
        sb.append("\n🔗 <a href=\"").append(cleanBaseUrl).append("\">Перейти к голосованию на сайте</a>");
        sendMessage(sb.toString());
    }
}