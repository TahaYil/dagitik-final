package com.shopwave.inventory.dto;

import com.shopwave.inventory.domain.Inventory;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InventoryDto {
    private Long productId;
    private int quantity;
    private int reserved;
    private int available;

    public static InventoryDto from(Inventory inv) {
        return InventoryDto.builder()
                .productId(inv.getProductId())
                .quantity(inv.getQuantity())
                .reserved(inv.getReserved())
                .available(inv.availableQuantity())
                .build();
    }
}
