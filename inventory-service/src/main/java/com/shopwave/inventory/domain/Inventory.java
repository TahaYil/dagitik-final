package com.shopwave.inventory.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "inventory")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false, unique = true)
    private Long productId;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private int reserved;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() { this.updatedAt = OffsetDateTime.now(); }

    public int availableQuantity() {
        return quantity - reserved;
    }

    public boolean canReserve(int amount) {
        return availableQuantity() >= amount;
    }

    public void reserve(int amount) {
        if (!canReserve(amount)) {
            throw new IllegalStateException(
                "Insufficient stock: available=%d, requested=%d"
                    .formatted(availableQuantity(), amount));
        }
        this.reserved += amount;
    }

    public void release(int amount) {
        this.reserved = Math.max(0, this.reserved - amount);
    }

    public void deduct(int amount) {
        this.reserved = Math.max(0, this.reserved - amount);
        this.quantity = Math.max(0, this.quantity - amount);
    }
}
