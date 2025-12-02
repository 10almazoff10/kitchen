package ru.softlogic.paylogic_kitchen.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    private LocalDateTime deadlineTime;
    private boolean isClosed = false;

    @Column(length = 500)
    private String paymentData;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean isAcceptingOrders = true;

    @ManyToOne
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    // constructors, getters, setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Restaurant getRestaurant() { return restaurant; }
    public void setRestaurant(Restaurant restaurant) { this.restaurant = restaurant; }

    public String getRestaurantUrl() { // getter для совместимости
        return restaurant != null ? restaurant.getWebsiteUrl() : null;
    }

    public LocalDateTime getDeadlineTime() { return deadlineTime; }
    public void setDeadlineTime(LocalDateTime deadlineTime) { this.deadlineTime = deadlineTime; }

    public boolean isClosed() { return isClosed; }
    public void setClosed(boolean closed) { isClosed = closed; }

    public boolean isAcceptingOrders() { return isAcceptingOrders; }
    public void setAcceptingOrders(boolean acceptingOrders) { isAcceptingOrders = acceptingOrders; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    public String getPaymentData() { return paymentData; }
    public void setPaymentData(String paymentData) { this.paymentData = paymentData; }
}