package ru.prokin.kitchen.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_orders")
public class UserOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String itemDescription;
    private BigDecimal price;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean isAddedToRestaurantOrder = false;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean isPaid = false;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate = LocalDateTime.now();

    @Column(name = "rating", columnDefinition = "int default 0")
    private Integer rating = 0;

    // constructors, getters, setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getItemDescription() { return itemDescription; }
    public void setItemDescription(String itemDescription) { this.itemDescription = itemDescription; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public boolean isAddedToRestaurantOrder() { return isAddedToRestaurantOrder; }
    public void setAddedToRestaurantOrder(boolean addedToRestaurantOrder) { isAddedToRestaurantOrder = addedToRestaurantOrder; }

    public boolean isPaid() { return isPaid; }
    public void setPaid(boolean paid) { isPaid = paid; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
}