package ru.prokin.kitchen.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.prokin.kitchen.entity.RefreshToken;
import ru.prokin.kitchen.entity.User;
import ru.prokin.kitchen.repository.RefreshTokenRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenService.class);

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-token.expiration-ms:2592000000}") // Default: 30 days
    private long refreshTokenDurationMs;

    @Transactional
    public RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .createdAt(Instant.now())
                .expiryDate(Instant.now().plusMillis(refreshTokenDurationMs))
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public RefreshToken rotateRefreshToken(String requestRefreshToken) {
        if (requestRefreshToken == null || requestRefreshToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh-токен не указан");
        }

        RefreshToken token = refreshTokenRepository.findByToken(requestRefreshToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh-токен не найден в системе"));

        if (token.isExpired()) {
            refreshTokenRepository.delete(token);
            logger.warn("Expired refresh token rejected for user: {}", token.getUser().getUsername());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Срок действия refresh-токена истек. Пожалуйста, выполните вход заново.");
        }

        User user = token.getUser();
        // Remove the consumed old refresh token
        refreshTokenRepository.delete(token);

        // Issue new refresh token extending the session
        RefreshToken newRefreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .createdAt(Instant.now())
                .expiryDate(Instant.now().plusMillis(refreshTokenDurationMs))
                .build();

        return refreshTokenRepository.save(newRefreshToken);
    }

    @Transactional
    public void revokeToken(String token) {
        if (token != null && !token.isBlank()) {
            refreshTokenRepository.deleteByToken(token);
        }
    }

    @Transactional
    public void revokeUserTokens(User user) {
        if (user != null) {
            refreshTokenRepository.deleteByUser(user);
        }
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }
}
