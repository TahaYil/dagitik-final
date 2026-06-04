package com.shopwave.service;

import com.shopwave.client.InventoryClient;
import com.shopwave.client.InventoryClient.InventoryView;
import com.shopwave.domain.Category;
import com.shopwave.domain.Product;
import com.shopwave.dto.ProductDto;
import com.shopwave.exception.NotFoundException;
import com.shopwave.repository.CategoryRepository;
import com.shopwave.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository  productRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryClient    inventoryClient;
    private final AuditService       auditService;

    // ─── Queries ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ProductDto> listAll() {
        List<Product> products = productRepository.findByActiveTrue();
        Map<Long, Integer> stocks = fetchStocks(products);
        return products.stream().map(p -> toDto(p, stocks.get(p.getId()))).toList();
    }

    @Transactional(readOnly = true)
    public ProductDto getById(Long id) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found: " + id));
        return toDto(p, fetchStock(p.getId()));
    }

    @Transactional(readOnly = true)
    public ProductDto getBySku(String sku) {
        Product p = productRepository.findBySku(sku)
                .orElseThrow(() -> new NotFoundException("Product not found: " + sku));
        return toDto(p, fetchStock(p.getId()));
    }

    @Transactional(readOnly = true)
    public List<ProductDto> listByCategory(Long categoryId) {
        List<Product> products = productRepository.findByCategoryIdAndActiveTrue(categoryId);
        Map<Long, Integer> stocks = fetchStocks(products);
        return products.stream().map(p -> toDto(p, stocks.get(p.getId()))).toList();
    }

    // ─── Commands ─────────────────────────────────────────────

    @Transactional
    public ProductDto create(String sku, String name, String description,
                             BigDecimal price, Long categoryId, int initialStock) {
        if (productRepository.findBySku(sku).isPresent()) {
            throw new IllegalArgumentException("SKU already exists: " + sku);
        }

        Category category = categoryId != null
                ? categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new NotFoundException("Category not found: " + categoryId))
                : null;

        Product product = Product.builder()
                .sku(sku).name(name).description(description)
                .price(price).category(category).active(true)
                .build();
        productRepository.save(product);

        inventoryClient.create(product.getId(), initialStock);

        auditService.log("PRODUCT_CREATED", "Product", product.getId(),
                "sku=" + sku + " stock=" + initialStock);

        log.info("Product created sku={} id={}", sku, product.getId());
        return toDto(product, initialStock);
    }

    @Transactional
    public ProductDto updatePrice(Long id, BigDecimal newPrice) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found: " + id));

        BigDecimal oldPrice = product.getPrice();
        product.setPrice(newPrice);
        productRepository.save(product);

        auditService.log("PRODUCT_PRICE_UPDATED", "Product", id,
                "old=" + oldPrice + " new=" + newPrice);
        return toDto(product, fetchStock(id));
    }

    @Transactional
    public void deactivate(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found: " + id));
        product.setActive(false);
        productRepository.save(product);
        auditService.log("PRODUCT_DEACTIVATED", "Product", id, null);
    }

    // ─── Helpers ──────────────────────────────────────────────

    private Integer fetchStock(Long productId) {
        try {
            return inventoryClient.getByProductId(productId).available();
        } catch (RuntimeException ex) {
            log.warn("inventory-service stock lookup failed productId={} err={}", productId, ex.getMessage());
            return null;
        }
    }

    private Map<Long, Integer> fetchStocks(List<Product> products) {
        if (products.isEmpty()) return Map.of();
        List<Long> ids = products.stream().map(Product::getId).toList();
        try {
            List<InventoryView> views = inventoryClient.getByProductIds(ids);
            Map<Long, Integer> map = new HashMap<>();
            for (InventoryView v : views) map.put(v.productId(), v.available());
            return map;
        } catch (RuntimeException ex) {
            log.warn("inventory-service batch stock lookup failed err={}", ex.getMessage());
            return Map.of();
        }
    }

    private ProductDto toDto(Product p, Integer availableStock) {
        return ProductDto.builder()
                .id(p.getId())
                .sku(p.getSku())
                .name(p.getName())
                .description(p.getDescription())
                .price(p.getPrice())
                .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                .active(p.isActive())
                .availableStock(availableStock)
                .createdAt(p.getCreatedAt())
                .build();
    }
}
