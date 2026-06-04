package com.shopwave.client;

import com.shopwave.exception.InsufficientStockException;
import com.shopwave.exception.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class InventoryClient {

    private final RestClient restClient;

    public InventoryClient(@Value("${shopwave.inventory.base-url:http://localhost:8081}") String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(2).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(5).toMillis());

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
        log.info("InventoryClient initialized with baseUrl={}", baseUrl);
    }

    public InventoryView getByProductId(Long productId) {
        try {
            return restClient.get()
                    .uri("/api/v1/inventory/products/{id}", productId)
                    .retrieve()
                    .body(InventoryView.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new NotFoundException("Inventory not found for product: " + productId);
        }
    }

    public List<InventoryView> getByProductIds(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) return List.of();
        String ids = productIds.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("");
        try {
            InventoryView[] arr = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/api/v1/inventory/products").queryParam("ids", ids).build())
                    .retrieve()
                    .body(InventoryView[].class);
            return arr == null ? List.of() : List.of(arr);
        } catch (ResourceAccessException ex) {
            log.warn("inventory-service unreachable on batch fetch: {}", ex.getMessage());
            return List.of();
        }
    }

    public List<InventoryView> getLowStock(int threshold) {
        InventoryView[] arr = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/inventory/low-stock").queryParam("threshold", threshold).build())
                .retrieve()
                .body(InventoryView[].class);
        return arr == null ? List.of() : List.of(arr);
    }

    public InventoryView create(Long productId, int quantity) {
        return restClient.post()
                .uri("/api/v1/inventory/products")
                .body(Map.of("productId", productId, "quantity", quantity))
                .retrieve()
                .body(InventoryView.class);
    }

    public void reserve(Long productId, int quantity) {
        post("/api/v1/inventory/products/" + productId + "/reserve", quantity, productId);
    }

    public void release(Long productId, int quantity) {
        post("/api/v1/inventory/products/" + productId + "/release", quantity, productId);
    }

    public void deduct(Long productId, int quantity) {
        post("/api/v1/inventory/products/" + productId + "/deduct", quantity, productId);
    }

    public void addStock(Long productId, int quantity) {
        post("/api/v1/inventory/products/" + productId + "/add", quantity, productId);
    }

    private void post(String path, int quantity, Long productId) {
        try {
            restClient.post()
                    .uri(path)
                    .body(Map.of("quantity", quantity))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.NotFound e) {
            throw new NotFoundException("Inventory not found for product: " + productId);
        } catch (HttpClientErrorException.Conflict e) {
            throw new InsufficientStockException(productId, -1, quantity);
        }
    }

    public record InventoryView(Long productId, int quantity, int reserved, int available) {}
}
