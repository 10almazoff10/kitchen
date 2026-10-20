package ru.prokin.kitchen.entity;


import jakarta.persistence.*;

@Entity
@Table(name = "rating")
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private Short score;

    @ManyToOne
    @JoinColumn(name = "restaurant_id", nullable = true)
    private Restaurant restaurant;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = true)
    private Order order;

    // constructors, getters, setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Short getScore() {
        return score;
    }

    public void setScore(Short score) {
        this.score = score;
    }

    public Restaurant getRestaurant() {
        return restaurant;
    }

    public void setRestaurant(Restaurant restaurant) {
        this.restaurant = restaurant;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }
}
