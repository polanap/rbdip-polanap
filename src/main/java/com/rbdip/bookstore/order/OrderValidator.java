package com.rbdip.bookstore.order;

import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Проверка входных данных заказа.
 */
@Component
public class OrderValidator {

    public void validate(CreateOrderRequest request) {
        requireNotBlank(request.customerFullName(), "customerFullName is required");
        requireNotBlank(request.customerAddress(), "customerAddress is required");
        List<CreateOrderRequest.Item> items = request.items();
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("order must contain at least one item");
        }
    }

    public void validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
    }

    private void requireNotBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}
