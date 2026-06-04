package com.shopwave.inventory.service;

import com.shopwave.inventory.domain.Inventory;
import com.shopwave.inventory.exception.InsufficientStockException;
import com.shopwave.inventory.exception.NotFoundException;
import com.shopwave.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    @Transactional(readOnly = true)
    public Inventory getByProductId(Long productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new NotFoundException("Inventory not found for product: " + productId));
    }

    @Transactional(readOnly = true)
    public List<Inventory> getByProductIds(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) return List.of();
        return inventoryRepository.findByProductIdIn(productIds);
    }

    @Transactional(readOnly = true)
    public List<Inventory> getLowStock(int threshold) {
        return inventoryRepository.findLowStock(threshold);
    }

    @Transactional
    public Inventory create(Long productId, int quantity) {
        inventoryRepository.findByProductId(productId).ifPresent(i -> {
            throw new IllegalArgumentException("Inventory already exists for product: " + productId);
        });
        Inventory inv = Inventory.builder()
                .productId(productId)
                .quantity(quantity)
                .reserved(0)
                .build();
        inventoryRepository.save(inv);
        log.info("Inventory created productId={} quantity={}", productId, quantity);
        return inv;
    }

    @Transactional
    public void reserve(Long productId, int quantity) {
        Inventory inv = inventoryRepository.findByProductIdWithLock(productId)
                .orElseThrow(() -> new NotFoundException("Inventory not found: " + productId));

        if (!inv.canReserve(quantity)) {
            throw new InsufficientStockException(productId, inv.availableQuantity(), quantity);
        }

        inv.reserve(quantity);
        inventoryRepository.save(inv);
        log.info("Stock reserved productId={} qty={} available={}", productId, quantity, inv.availableQuantity());
    }

    @Transactional
    public void release(Long productId, int quantity) {
        Inventory inv = inventoryRepository.findByProductIdWithLock(productId)
                .orElseThrow(() -> new NotFoundException("Inventory not found: " + productId));
        inv.release(quantity);
        inventoryRepository.save(inv);
        log.info("Stock released productId={} qty={}", productId, quantity);
    }

    @Transactional
    public void deduct(Long productId, int quantity) {
        Inventory inv = inventoryRepository.findByProductIdWithLock(productId)
                .orElseThrow(() -> new NotFoundException("Inventory not found: " + productId));
        inv.deduct(quantity);
        inventoryRepository.save(inv);
        log.info("Stock deducted productId={} qty={} remaining={}", productId, quantity, inv.availableQuantity());
    }

    @Transactional
    public void addStock(Long productId, int quantity) {
        Inventory inv = inventoryRepository.findByProductIdWithLock(productId)
                .orElseThrow(() -> new NotFoundException("Inventory not found: " + productId));
        inv.setQuantity(inv.getQuantity() + quantity);
        inventoryRepository.save(inv);
        log.info("Stock added productId={} qty={} total={}", productId, quantity, inv.getQuantity());
    }
}
