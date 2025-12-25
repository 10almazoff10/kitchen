package ru.prokin.kitchen.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.prokin.kitchen.entity.User;
import ru.prokin.kitchen.repository.UserOrderRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RatingService {

    @Autowired
    private UserOrderRepository userOrderRepo;

    public List<RatingItem> getTopSpentThisMonth() {
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        List<Object[]> results = userOrderRepo.findTopSpentByUserInPeriod(startOfMonth);
        return results.stream()
                .limit(3)
                .map(obj -> new RatingItem((User) obj[0], (BigDecimal) obj[1]))
                .collect(Collectors.toList());
    }

    public List<RatingItem> getTopOrdersThisMonth() {
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        List<Object[]> results = userOrderRepo.findTopOrdersByUserInPeriod(startOfMonth);
        return results.stream()
                .limit(3)
                .map(obj -> new RatingItem((User) obj[0], ((Number) obj[1]).longValue()))
                .collect(Collectors.toList());
    }

    public List<RatingItem> getTopSpentAllTime() {
        List<Object[]> results = userOrderRepo.findTopSpentByUserAllTime();
        return results.stream()
                .limit(3)
                .map(obj -> new RatingItem((User) obj[0], (BigDecimal) obj[1]))
                .collect(Collectors.toList());
    }

    public List<RatingItem> getTopOrdersAllTime() {
        List<Object[]> results = userOrderRepo.findTopOrdersByUserAllTime();
        return results.stream()
                .limit(3)
                .map(obj -> new RatingItem((User) obj[0], ((Number) obj[1]).longValue()))
                .collect(Collectors.toList());
    }

    // Вспомогательный класс для возврата результата
    public static class RatingItem {
        private final User user;
        private final Object value; // BigDecimal для денег, Long для количества

        public RatingItem(User user, Object value) {
            this.user = user;
            this.value = value;
        }

        public User getUser() { return user; }
        public Object getValue() { return value; }
    }
}