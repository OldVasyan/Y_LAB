package com.marketplace.model;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Простейшая модель
 */

public class Product implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String category;
    private String brand;
    private double price;
    private final Instant createdAt;
    private Instant updatedAt;

    public Product(String name, String category, String brand, double price) {
        this(null, name, category, brand, price, Instant.now(), Instant.now());
    }

    public Product(Long id, String name, String category, String brand, double price,
                   Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name != null ? name : "";
        this.category = category != null ? category : "";
        this.brand = brand != null ? brand : "";
        this.price = price;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getBrand() { return brand; }
    public double getPrice() { return price; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setName(String name) {
        this.name = name != null ? name : this.name;
        touch();
    }
    public void setId(Long id){
        this.id = id;
    }
    public void setCategory(String category) {
        this.category = category != null ? category : this.category;
        touch();
    }

    public void setBrand(String brand) {
        this.brand = brand != null ? brand : this.brand;
        touch();
    }

    public void setPrice(double price) {
        this.price = price;
        touch();
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Product product = (Product) o;
        return Objects.equals(id, product.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return String.format("Product{id=%s, name='%s', category='%s', brand='%s', price=%.2f}",
                id, name, category, brand, price);
    }
}

