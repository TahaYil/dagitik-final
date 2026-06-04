package com.shopwave.controller;

import com.shopwave.client.InventoryClient;
import com.shopwave.client.InventoryClient.InventoryView;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryClient inventoryClient;

    @GetMapping("/low-stock")
    public List<InventoryView> lowStock(@RequestParam(defaultValue = "10") int threshold) {
        return inventoryClient.getLowStock(threshold);
    }

    @PostMapping("/products/{productId}/add")
    public Map<String, String> addStock(@PathVariable Long productId,
                                        @RequestBody Map<String, Integer> body) {
        inventoryClient.addStock(productId, body.get("quantity"));
        return Map.of("message", "Stock added");
    }
}
