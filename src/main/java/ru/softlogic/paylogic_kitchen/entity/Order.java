package ru.softlogic.paylogic_kitchen.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String restaurantUrl;
    private LocalDateTime deadlineTime;
    private boolean isClosed = false;

    @ManyToOne
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    // constructors, getters, setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRestaurantUrl() { return restaurantUrl; }
    public void setRestaurantUrl(String restaurantUrl) { this.restaurantUrl = restaurantUrl; }

    public LocalDateTime getDeadlineTime() { return deadlineTime; }
    public void setDeadlineTime(LocalDateTime deadlineTime) { this.deadlineTime = deadlineTime; }

    public boolean isClosed() { return isClosed; }
    public void setClosed(boolean closed) { isClosed = closed; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
}