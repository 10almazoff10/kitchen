package ru.softlogic.paylogic_kitchen.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Service
public class TelegramService {

    private static final Logger logger = LoggerFactory.getLogger(TelegramService.class);

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.chat.id}")
    private String chatId;

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
}