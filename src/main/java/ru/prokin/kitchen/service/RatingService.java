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

    // Метод получения личной статистики пользователя
    public UserStatistic getUserStatistic(User user) {
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        BigDecimal spentThisMonth = userOrderRepo.findSpentByUserInPeriod(user, startOfMonth);
        long ordersThisMonth = userOrderRepo.countOrdersByUserInPeriod(user, startOfMonth);

        BigDecimal spentAllTime = userOrderRepo.findSpentByUserAllTime(user);
        long ordersAllTime = userOrderRepo.countOrdersByUserAllTime(user);

        return new UserStatistic(spentThisMonth, ordersThisMonth, spentAllTime, ordersAllTime);
    }

    // Вспомогательный класс-контейнер для личной статистики
    public static class UserStatistic {
        private final BigDecimal totalSpentThisMonth;
        private final long totalOrdersThisMonth;
        private final BigDecimal totalSpentAllTime;
        private final long totalOrdersAllTime;

        public UserStatistic(BigDecimal totalSpentThisMonth, long totalOrdersThisMonth,
                             BigDecimal totalSpentAllTime, long totalOrdersAllTime) {
            this.totalSpentThisMonth = totalSpentThisMonth != null ? totalSpentThisMonth : BigDecimal.ZERO;
            this.totalOrdersThisMonth = totalOrdersThisMonth;
            this.totalSpentAllTime = totalSpentAllTime != null ? totalSpentAllTime : BigDecimal.ZERO;
            this.totalOrdersAllTime = totalOrdersAllTime;
        }

        public BigDecimal getTotalSpentThisMonth() { return totalSpentThisMonth; }
        public long getTotalOrdersThisMonth() { return totalOrdersThisMonth; }
        public BigDecimal getTotalSpentAllTime() { return totalSpentAllTime; }
        public long getTotalOrdersAllTime() { return totalOrdersAllTime; }
    }


}