package com.shopwave.inventory.controller;

import com.shopwave.inventory.dto.CreateInventoryRequest;
import com.shopwave.inventory.dto.InventoryDto;
import com.shopwave.inventory.dto.QuantityRequest;
import com.shopwave.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/products/{productId}")
    public InventoryDto get(@PathVariable Long productId) {
        return InventoryDto.from(inventoryService.getByProductId(productId));
    }

    @GetMapping("/products")
    public List<InventoryDto> getBatch(@RequestParam("ids") List<Long> ids) {
        return inventoryService.getByProductIds(ids).stream().map(InventoryDto::from).toList();
    }

    @GetMapping("/low-stock")
    public List<InventoryDto> lowStock(@RequestParam(defaultValue = "10") int threshold) {
        return inventoryService.getLowStock(threshold).stream().map(InventoryDto::from).toList();
    }

    @PostMapping("/products")
    public ResponseEntity<InventoryDto> create(@Valid @RequestBody CreateInventoryRequest req) {
        InventoryDto dto = InventoryDto.from(inventoryService.create(req.getProductId(), req.getQuantity()));
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PostMapping("/products/{productId}/reserve")
    public Map<String, String> reserve(@PathVariable Long productId, @Valid @RequestBody QuantityRequest req) {
        inventoryService.reserve(productId, req.getQuantity());
        return Map.of("message", "Stock reserved");
    }

    @PostMapping("/products/{productId}/release")
    public Map<String, String> release(@PathVariable Long productId, @Valid @RequestBody QuantityRequest req) {
        inventoryService.release(productId, req.getQuantity());
        return Map.of("message", "Stock released");
    }

    @PostMapping("/products/{productId}/deduct")
    public Map<String, String> deduct(@PathVariable Long productId, @Valid @RequestBody QuantityRequest req) {
        inventoryService.deduct(productId, req.getQuantity());
        return Map.of("message", "Stock deducted");
    }

    @PostMapping("/products/{productId}/add")
    public Map<String, String> addStock(@PathVariable Long productId, @Valid @RequestBody QuantityRequest req) {
        inventoryService.addStock(productId, req.getQuantity());
        return Map.of("message", "Stock added");
    }
}
