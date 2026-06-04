package com.shopwave.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class QuantityRequest {
    @NotNull
    @Min(1)
    private Integer quantity;
}
